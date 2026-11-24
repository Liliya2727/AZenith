#!/bin/sh
# Differential test harness for C preloadbin (vmtouch v1.4.1) vs Rust preload.
# Usage: sh tests/preload_diff.sh <C_BIN> <RUST_BIN> [SUBCMD_MODE_BIN]
#
# Stores C reference goldens in ${GOLDEN_DIR:-.binarytest/golden}.
# Classifies diffs as:
#   PASS               - identical exit code, normalized stdout, normalized stderr
#   EXPECTED_DEVIATION - allow-listed intentional bug fix (items 9, 10, 11)
#   GETOPT_WORDING     - stderr-only wording diff on getopt invalid-option cases
#   REAL_MISMATCH      - unexpected difference (causes non-zero exit)

set -u

if [ $# -lt 2 ]; then
    echo "Usage: $0 <C_BIN> <RUST_BIN> [SUBCMD_BIN]" >&2
    exit 2
fi

C_BIN="$1"
RUST_BIN="$2"
SUBCMD_BIN="${3:-}"
GOLDEN_DIR="${GOLDEN_DIR:-.binarytest/golden}"

mkdir -p "$GOLDEN_DIR"

WORK_DIR=$(mktemp -d "${TMPDIR:-/tmp}/preload_diff.XXXXXX")
cleanup() {
    chmod -R u+rwX "$WORK_DIR" 2>/dev/null || true
    rm -rf "$WORK_DIR"
}
trap cleanup EXIT INT TERM

# --- Build deterministic fixture tree ---
FIX="$WORK_DIR/fix"
mkdir -p "$FIX/nested/skipdir" "$FIX/loop_dir" "$FIX/space dir" "$FIX/hard" "$FIX/sym"

dd if=/dev/zero of="$FIX/small.bin" bs=4096 count=2 2>/dev/null
dd if=/dev/zero of="$FIX/large.bin" bs=4096 count=8 2>/dev/null
dd if=/dev/zero of="$FIX/nested/a.so" bs=4096 count=1 2>/dev/null
dd if=/dev/zero of="$FIX/nested/b.apk" bs=4096 count=2 2>/dev/null
dd if=/dev/zero of="$FIX/nested/skip.tmp" bs=4096 count=1 2>/dev/null
dd if=/dev/zero of="$FIX/nested/skipdir/c.so" bs=4096 count=1 2>/dev/null
: > "$FIX/empty.bin"
mkfifo "$FIX/fifo.pipe" 2>/dev/null || true
dd if=/dev/zero of="$FIX/unreadable.bin" bs=4096 count=1 2>/dev/null
chmod 000 "$FIX/unreadable.bin" 2>/dev/null || true

ln -s "$FIX/small.bin" "$FIX/sym/link_file"
ln -s "$FIX/nested" "$FIX/sym/link_dir"
ln -s "$FIX/loop_dir" "$FIX/loop_dir/self"

dd if=/dev/zero of="$FIX/hard/a.bin" bs=4096 count=1 2>/dev/null
HAS_HARDLINK=0
if ln "$FIX/hard/a.bin" "$FIX/hard/b.bin" 2>/dev/null; then
    HAS_HARDLINK=1
else
    echo "NOTE: hardlink creation denied on this host (SELinux/fs); skipping hard_default/hard_h" >&2
fi

dd if=/dev/zero of="$FIX/space dir/file with spaces.so" bs=4096 count=1 2>/dev/null

# Batch files
printf '%s\n%s\n' "$FIX/small.bin" "$FIX/nested/a.so" > "$FIX/list.txt"
printf '%s\0%s\0' "$FIX/small.bin" "$FIX/nested/a.so" > "$FIX/nul.list"

# Filename with trailing newline (for deviation #10: -0 must not strip trailing \n)
NL_FILE="$FIX/trailing_nl
"
HAS_NL_FILE=0
if dd if=/dev/zero of="$NL_FILE" bs=4096 count=1 2>/dev/null; then
    printf '%s\0' "$NL_FILE" > "$FIX/nul_nl.list"
    HAS_NL_FILE=1
fi

# Symlink with argv[0] = sys.azenith-preloadbin for Rust shim test
SHIM_DIR="$WORK_DIR/shim"
mkdir -p "$SHIM_DIR"
RUST_ABS=$(cd "$(dirname "$RUST_BIN")" && pwd)/$(basename "$RUST_BIN")
ln -s "$RUST_ABS" "$SHIM_DIR/sys.azenith-preloadbin"
SHIM_BIN="$SHIM_DIR/sys.azenith-preloadbin"

# --- Normalization ---
# Normalizes:
#   1. Temp dir path -> <FIX>
#   2. Elapsed time (human & kv)
#   3. Page-cache-dependent residency counts (query mode human & kv)
#   4. Verbose residency chart lines (\r[...] X/Y -> <CHART>/Y)
#   5. Binary path in getopt stderr messages
normalize_stdout() {
    awk -v fix="$FIX" '
    {
        gsub(fix, "<FIX>")
        # Strip CR-delimited intermediate chart updates, keep final chart segment
        if (index($0, "\r") > 0) {
            n = split($0, parts, "\r")
            $0 = parts[n]
        }
        # Normalize chart "[O o ] X/Y" -> "[<CHART>] <RES>/Y"
        if ($0 ~ /^\[[Oo ]+\] [0-9]+\/[0-9]+$/) {
            sub(/^\[[Oo ]+\] [0-9]+\//, "[<CHART>] <RES>/")
        }
        # Normalize Elapsed in human output
        sub(/Elapsed: [0-9.eE+-]+ seconds/, "Elapsed: <ELAPSED> seconds")
        # Normalize Elapsed in kv output
        sub(/Elapsed=[0-9.eE+-]+/, "Elapsed=<ELAPSED>")
        # Normalize Resident Pages line (keep TotalPages and TotalSize)
        if ($0 ~ /Resident Pages:/) {
            sub(/Resident Pages: [0-9]+\//, "Resident Pages: <RES>/")
            sub(/  [0-9]+[KMG]?\//, "  <RSZ>/")
            sub(/  [0-9.]+%/, "  <PCT>%")
        }
        # Normalize kv Resident counters (keep TotalPages and TotalSize)
        if ($0 ~ /ResidentPages=/) {
            sub(/ResidentPages=[0-9]+/, "ResidentPages=<RES>")
            sub(/ResidentSize=[0-9]+/, "ResidentSize=<RSZ>")
            sub(/ResidentPercent=[0-9.]+/, "ResidentPercent=<PCT>")
        }
        # Normalize kv TouchedPercent / TouchedPages only for pre-touch C bug comparison (do NOT mask! we want to detect deviation #9)
        print
    }'
}

normalize_stderr() {
    awk -v fix="$FIX" -v cbin="$C_BIN" -v rbin="$RUST_BIN" -v sbin="$SHIM_BIN" '
    {
        gsub(fix, "<FIX>")
        gsub(cbin, "<BIN>")
        gsub(rbin, "<BIN>")
        gsub(sbin, "<BIN>")
        print
    }'
}

is_expected_deviation() {
    case "$1" in
        touch_kv|evict_kv|batch_nul_newline|permute_opts) return 0 ;;
        *) return 1 ;;
    esac
}

is_getopt_case() {
    case "$1" in
        opt_qmark|opt_long_help|opt_unknown_Z|shim_argv0_help_err) return 0 ;;
        *) return 1 ;;
    esac
}

PASS=0
EXP_DEV=0
GETOPT_DIFF=0
FAIL=0
TOTAL=0

run_case() {
    id="$1"
    stdin_file="$2"
    shift 2

    TOTAL=$((TOTAL + 1))

    c_out="$WORK_DIR/c_${id}.out"
    c_err="$WORK_DIR/c_${id}.err"
    r_out="$WORK_DIR/r_${id}.out"
    r_err="$WORK_DIR/r_${id}.err"

    if [ -n "$stdin_file" ]; then
        "$C_BIN" "$@" < "$stdin_file" > "$c_out" 2> "$c_err"
        c_rc=$?
    else
        "$C_BIN" "$@" > "$c_out" 2> "$c_err"
        c_rc=$?
    fi

    # Save C golden (raw)
    printf '%s\n' "$c_rc" > "$GOLDEN_DIR/${id}.rc"
    cp "$c_out" "$GOLDEN_DIR/${id}.out"
    cp "$c_err" "$GOLDEN_DIR/${id}.err"

    if [ -n "$stdin_file" ]; then
        "$RUST_BIN" "$@" < "$stdin_file" > "$r_out" 2> "$r_err"
        r_rc=$?
    else
        "$RUST_BIN" "$@" > "$r_out" 2> "$r_err"
        r_rc=$?
    fi

    c_out_n="$WORK_DIR/c_${id}.out.n"
    c_err_n="$WORK_DIR/c_${id}.err.n"
    r_out_n="$WORK_DIR/r_${id}.out.n"
    r_err_n="$WORK_DIR/r_${id}.err.n"

    normalize_stdout < "$c_out" > "$c_out_n"
    normalize_stderr < "$c_err" > "$c_err_n"
    normalize_stdout < "$r_out" > "$r_out_n"
    normalize_stderr < "$r_err" > "$r_err_n"

    rc_ok=1
    out_ok=1
    err_ok=1
    [ "$c_rc" -eq "$r_rc" ] || rc_ok=0
    cmp -s "$c_out_n" "$r_out_n" || out_ok=0
    cmp -s "$c_err_n" "$r_err_n" || err_ok=0

    if [ "$rc_ok" -eq 1 ] && [ "$out_ok" -eq 1 ] && [ "$err_ok" -eq 1 ]; then
        PASS=$((PASS + 1))
        printf 'PASS               %s (rc=%d)\n' "$id" "$r_rc"
    elif is_expected_deviation "$id"; then
        EXP_DEV=$((EXP_DEV + 1))
        printf 'EXPECTED_DEVIATION %s (c_rc=%d r_rc=%d)\n' "$id" "$c_rc" "$r_rc"
        if [ "$out_ok" -eq 0 ]; then
            echo "  [stdout diff]"
            diff -u "$c_out_n" "$r_out_n" | sed 's/^/    /' || true
        fi
        if [ "$err_ok" -eq 0 ]; then
            echo "  [stderr diff]"
            diff -u "$c_err_n" "$r_err_n" | sed 's/^/    /' || true
        fi
    elif is_getopt_case "$id" && [ "$rc_ok" -eq 1 ] && [ "$out_ok" -eq 1 ] && [ "$err_ok" -eq 0 ]; then
        GETOPT_DIFF=$((GETOPT_DIFF + 1))
        printf 'GETOPT_WORDING     %s (rc=%d, stderr-only)\n' "$id" "$r_rc"
        diff -u "$c_err_n" "$r_err_n" | sed 's/^/    /' || true
    else
        FAIL=$((FAIL + 1))
        printf 'REAL_MISMATCH      %s (c_rc=%d r_rc=%d)\n' "$id" "$c_rc" "$r_rc"
        if [ "$out_ok" -eq 0 ]; then
            echo "  [stdout diff]"
            diff -u "$c_out_n" "$r_out_n" | sed 's/^/    /' || true
        fi
        if [ "$err_ok" -eq 0 ]; then
            echo "  [stderr diff]"
            diff -u "$c_err_n" "$r_err_n" | sed 's/^/    /' || true
        fi
    fi
}

# --- Section 9 Item 1: Help & bad arguments ---
run_case "no_args"         ""
run_case "opt_qmark"       "" -?
run_case "opt_long_help"   "" --help
run_case "opt_unknown_Z"   "" -Z
run_case "bad_t_e"         "" -t -e "$FIX/small.bin"
run_case "bad_e_l"         "" -e -l "$FIX/small.bin"
run_case "bad_e_L"         "" -e -L "$FIX/small.bin"
run_case "bad_l_L"         "" -l -L "$FIX/small.bin"
run_case "bad_d_only"      "" -d "$FIX/small.bin"
run_case "bad_w_only"      "" -w "$FIX/small.bin"
run_case "bad_w_l_no_d"    "" -w -l "$FIX/small.bin"
run_case "bad_P_no_lock"   "" -P "$WORK_DIR/test.pid" "$FIX/small.bin"
run_case "bad_q_v"         "" -q -v "$FIX/small.bin"
run_case "bad_m_empty"     "" -m "" "$FIX/small.bin"
run_case "bad_m_neg"       "" -m -1 "$FIX/small.bin"
run_case "bad_m_suffix"    "" -m 10X "$FIX/small.bin"
run_case "p_subpage_10_5"  "" -p 10-5 "$FIX/small.bin"
run_case "bad_p_order"     "" -p 8K-4K "$FIX/small.bin"
run_case "bad_p_hyphens"   "" -p 1-2-3 "$FIX/small.bin"

# --- Section 9 Item 2: Query, touch, evict, verbosity, output formats ---
run_case "query_default"   "" "$FIX/small.bin"
run_case "query_v"         "" -v "$FIX/small.bin"
run_case "query_q"         "" -q "$FIX/small.bin"
run_case "query_kv"        "" -o kv "$FIX/small.bin"
run_case "query_o_unknown" "" -o unknown "$FIX/small.bin"

run_case "touch_default"   "" -t "$FIX/small.bin"
run_case "touch_v"         "" -t -v "$FIX/small.bin"
run_case "touch_q"         "" -t -q "$FIX/small.bin"
run_case "touch_kv"        "" -t -o kv "$FIX/small.bin"

run_case "evict_default"   "" -e "$FIX/small.bin"
run_case "evict_v"         "" -e -v "$FIX/small.bin"
run_case "evict_q"         "" -e -q "$FIX/small.bin"
run_case "evict_kv"        "" -e -o kv "$FIX/small.bin"

# Combined flags, permutation, -- terminator, last value wins
run_case "combo_vt_m500M"  "" -vt -m500M "$FIX/nested"
run_case "combo_vtm_500M"  "" -vtm 500M "$FIX/nested"
run_case "permute_opts"    "" "$FIX/small.bin" -t
run_case "double_dash"     "" -t -- "$FIX/small.bin"
run_case "last_m_wins"     "" -t -m 1K -m 500M "$FIX/large.bin"

# --- Section 9 Item 3: Size & range parsing ---
run_case "m_zero"          "" -t -m 0 "$FIX/small.bin"
run_case "m_4096"          "" -t -m 4096 "$FIX/small.bin"
run_case "m_4k"            "" -t -m 4k "$FIX/nested/a.so"
run_case "m_1_5M"          "" -t -m 1.5M "$FIX/large.bin"
run_case "m_1G"            "" -t -m 1G "$FIX/large.bin"
run_case "p_4K"            "" -t -p 4K "$FIX/large.bin"
run_case "p_4K_8K"         "" -t -p 4K-8K "$FIX/large.bin"
run_case "p_4K_open"       "" -t -p 4K- "$FIX/large.bin"
run_case "p_open_8K"       "" -t -p -8K "$FIX/large.bin"
run_case "p_past_eof"      "" -t -p 999M "$FIX/small.bin"

# --- Section 9 Item 4: Crawling, filters, links, batch ---
run_case "crawl_nested"    "" -t "$FIX/nested"
run_case "crawl_empty"     "" -t "$FIX/empty.bin"
if [ -p "$FIX/fifo.pipe" ]; then
    run_case "crawl_fifo"  "" -t "$FIX/fifo.pipe"
fi
run_case "crawl_unreadable" "" -t "$FIX/unreadable.bin"
run_case "crawl_spaces"    "" -v -t "$FIX/space dir"
run_case "sym_default"     "" -t "$FIX/sym"
run_case "sym_follow"      "" -t -f "$FIX/sym"
run_case "sym_loop"        "" -t -f "$FIX/loop_dir"
if [ "$HAS_HARDLINK" -eq 1 ]; then
    run_case "hard_default" "" -t "$FIX/hard"
    run_case "hard_h"       "" -t -h "$FIX/hard"
fi
run_case "filter_ignore"   "" -t -i '*.tmp' -i 'skipdir' "$FIX/nested"
run_case "filter_include"  "" -t -I '*.so' -I '*.apk' "$FIX/nested"
run_case "batch_file"      "" -t -b "$FIX/list.txt"
run_case "batch_stdin"     "$FIX/list.txt" -t -b -
run_case "batch_nul"       "" -t -b "$FIX/nul.list" -0
if [ "$HAS_NL_FILE" -eq 1 ]; then
    run_case "batch_nul_newline" "" -t -b "$FIX/nul_nl.list" -0
fi

# --- Section 9 Item 5: Stage 3 locking & daemon tests (small 4 KiB file within RLIMIT_MEMLOCK) ---
run_lock_fg_case() {
    bin="$1"
    pidf="$2"
    outf="$3"
    errf="$4"
    "$bin" -l -P "$pidf" "$FIX/nested/a.so" > "$outf" 2> "$errf" &
    bg_pid=$!
    # Wait up to 2s for pidfile to appear
    i=0
    while [ "$i" -lt 40 ] && [ ! -s "$pidf" ]; do
        sleep 0.05
        i=$((i + 1))
    done
    pid_created=0
    [ -s "$pidf" ] && pid_created=1
    kill -TERM "$bg_pid" 2>/dev/null || true
    wait "$bg_pid" 2>/dev/null
    rc=$?
    pid_removed=0
    [ ! -e "$pidf" ] && pid_removed=1
    printf '%d %d %d\n' "$rc" "$pid_created" "$pid_removed"
}

run_lock_daemon_case() {
    bin="$1"
    pidf="$2"
    outf="$3"
    errf="$4"
    "$bin" -d -l -w -P "$pidf" "$FIX/nested/a.so" > "$outf" 2> "$errf"
    rc=$?
    pid_created=0
    alive=0
    pid_removed=0
    if [ -s "$pidf" ]; then
        pid_created=1
        dpid=$(cat "$pidf")
        if kill -0 "$dpid" 2>/dev/null; then
            alive=1
        fi
        kill -TERM "$dpid" 2>/dev/null || true
        i=0
        while [ "$i" -lt 40 ] && [ -e "$pidf" ]; do
            sleep 0.05
            i=$((i + 1))
        done
        [ ! -e "$pidf" ] && pid_removed=1
    fi
    printf '%d %d %d %d\n' "$rc" "$pid_created" "$alive" "$pid_removed"
}

# Probe whether mlock(4K) is permitted under current RLIMIT_MEMLOCK
PROBE_OUT="$WORK_DIR/mlock_probe.out"
PROBE_ERR="$WORK_DIR/mlock_probe.err"
PROBE_RES=$(run_lock_fg_case "$C_BIN" "$WORK_DIR/probe.pid" "$PROBE_OUT" "$PROBE_ERR")
if grep -q "^LOCKED 1 pages" "$PROBE_OUT" 2>/dev/null; then
    # 1. Foreground -l -P + SIGTERM
    TOTAL=$((TOTAL + 1))
    c_res=$(run_lock_fg_case "$C_BIN" "$WORK_DIR/c_fg.pid" "$WORK_DIR/c_lock_fg.out" "$WORK_DIR/c_lock_fg.err")
    r_res=$(run_lock_fg_case "$RUST_BIN" "$WORK_DIR/r_fg.pid" "$WORK_DIR/r_lock_fg.out" "$WORK_DIR/r_lock_fg.err")
    if [ "$c_res" = "$r_res" ] && cmp -s "$WORK_DIR/c_lock_fg.out" "$WORK_DIR/r_lock_fg.out" && cmp -s "$WORK_DIR/c_lock_fg.err" "$WORK_DIR/r_lock_fg.err"; then
        PASS=$((PASS + 1))
        printf 'PASS               lock_fg_pid (%s)\n' "$r_res"
    else
        FAIL=$((FAIL + 1))
        printf 'REAL_MISMATCH      lock_fg_pid (c=[%s] r=[%s])\n' "$c_res" "$r_res"
        diff -u "$WORK_DIR/c_lock_fg.out" "$WORK_DIR/r_lock_fg.out" | sed 's/^/    /' || true
        diff -u "$WORK_DIR/c_lock_fg.err" "$WORK_DIR/r_lock_fg.err" | sed 's/^/    /' || true
    fi

    # 2. Daemon -d -l -w -P + SIGTERM
    TOTAL=$((TOTAL + 1))
    c_res=$(run_lock_daemon_case "$C_BIN" "$WORK_DIR/c_dm.pid" "$WORK_DIR/c_lock_dm.out" "$WORK_DIR/c_lock_dm.err")
    r_res=$(run_lock_daemon_case "$RUST_BIN" "$WORK_DIR/r_dm.pid" "$WORK_DIR/r_lock_dm.out" "$WORK_DIR/r_lock_dm.err")
    if [ "$c_res" = "$r_res" ] && cmp -s "$WORK_DIR/c_lock_dm.out" "$WORK_DIR/r_lock_dm.out" && cmp -s "$WORK_DIR/c_lock_dm.err" "$WORK_DIR/r_lock_dm.err"; then
        PASS=$((PASS + 1))
        printf 'PASS               lock_daemon_wait_pid (%s)\n' "$r_res"
    else
        FAIL=$((FAIL + 1))
        printf 'REAL_MISMATCH      lock_daemon_wait_pid (c=[%s] r=[%s])\n' "$c_res" "$r_res"
        diff -u "$WORK_DIR/c_lock_dm.out" "$WORK_DIR/r_lock_dm.out" | sed 's/^/    /' || true
        diff -u "$WORK_DIR/c_lock_dm.err" "$WORK_DIR/r_lock_dm.err" | sed 's/^/    /' || true
    fi
else
    echo "NOTE: mlock(4K) unavailable on this host (RLIMIT_MEMLOCK); skipping Stage 3 lock tests" >&2
fi

# --- Section 9 Item 6: argv[0] shim (only when RUST_BIN != C_BIN) ---
if [ "$C_BIN" != "$RUST_BIN" ]; then
    OLD_RUST_BIN="$RUST_BIN"
    RUST_BIN="$SHIM_BIN"
    run_case "shim_argv0_touch" "" -v -t -m 500M "$FIX/nested"
    run_case "shim_argv0_help_err" "" --help
    RUST_BIN="$OLD_RUST_BIN"

    if [ -n "$SUBCMD_BIN" ]; then
        TOTAL=$((TOTAL + 1))
        s_out="$WORK_DIR/sub_help.out"
        s_err="$WORK_DIR/sub_help.err"
        "$SUBCMD_BIN" --help > "$s_out" 2> "$s_err"
        s_rc=$?
        if [ "$s_rc" -eq 0 ] && grep -q "^Usage: sys.azenith-service preload \[OPTIONS\]" "$s_out" && [ ! -s "$s_err" ]; then
            PASS=$((PASS + 1))
            printf 'PASS               subcmd_help (rc=%d)\n' "$s_rc"
        else
            FAIL=$((FAIL + 1))
            printf 'REAL_MISMATCH      subcmd_help (rc=%d)\n' "$s_rc"
            cat "$s_out" | sed 's/^/    /'
            cat "$s_err" | sed 's/^/    /'
        fi
    fi
fi

echo "---"
printf 'TOTAL=%d PASS=%d EXPECTED_DEVIATION=%d GETOPT_WORDING=%d REAL_MISMATCH=%d\n' \
    "$TOTAL" "$PASS" "$EXP_DEV" "$GETOPT_DIFF" "$FAIL"

[ "$FAIL" -eq 0 ]
