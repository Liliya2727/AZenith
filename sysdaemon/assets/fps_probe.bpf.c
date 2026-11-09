/* SPDX-License-Identifier: GPL-3.0-only */
/*
 * Copyright (C) 2026 Zexshia
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Kernel side of the fps uprobe. The daemon loads this object, attaches
 * `fps_probe` to the Surface::queueBuffer* family in libgui for the focused
 * pid and counts the ring records to derive the frame rate.
 *
 * Each record carries the frame timestamp plus a stable per-surface stream
 * key so userspace can isolate ONE presenting surface. Without it, every
 * SurfaceView / overlay / WebView queueBuffer of the app interleaves into
 * one timestamp stream and consecutive intervals are tiny and wrong.
 * The stream key is the callee's `this` pointer (arm64 x0), which is
 * stable for a given Surface object across the queueBuffer call chain;
 * tgid is kept as a fallback key when the ctx read is unavailable (0).
 */
//
// Rebuild after editing (the object is committed, consumers need no clang):
//
//   clang -O2 -g -target bpfel -c fps_probe.bpf.c -o fps_probe.bpf.o
//
// -g is required: the map below is declared BTF style (`.maps`), which aya
// reads to learn the map type and size.

#define SEC(NAME) __attribute__((section(NAME), used))

typedef unsigned long long u64;
typedef unsigned int u32;

#define __uint(name, val) int (*name)[val]

struct frame_record {
    u64 ktime_ns;
    u64 surface_key; /* callee `this` (arm64 x0); 0 when unavailable */
    u32 tgid;
    u32 pad;
};

struct {
    __uint(type, 27); /* BPF_MAP_TYPE_RINGBUF */
    __uint(max_entries, 262144);
} RING_BUF SEC(".maps");

/* Minimal arm64 pt_regs: only the argument registers are needed. x0 is
 * the first integer argument (the Surface* / ANativeWindow* this/window). */
struct pt_regs {
    u64 regs[31];
    u64 sp;
    u64 pc;
    u64 pstate;
};

static u64 (*bpf_ktime_get_ns)(void) = (void *)5;
static u64 (*bpf_get_current_pid_tgid)(void) = (void *)15;
static void *(*bpf_ringbuf_reserve)(void *ringbuf, u64 size, u64 flags) = (void *)131;
static void (*bpf_ringbuf_submit)(void *data, u64 flags) = (void *)132;

SEC("uprobe")
int fps_probe(void *ctx)
{
    struct frame_record *record = bpf_ringbuf_reserve(&RING_BUF, sizeof(*record), 0);
    if (!record)
        return 0;

    struct pt_regs *regs = (struct pt_regs *)ctx;
    u64 tgid = 0;
    if (regs) {
        record->surface_key = regs->regs[0];
        u64 pid_tgid = bpf_get_current_pid_tgid();
        tgid = (u32)(pid_tgid >> 32);
        if (tgid == 0)
            tgid = (u32)pid_tgid;
    } else {
        record->surface_key = 0;
    }
    record->ktime_ns = bpf_ktime_get_ns();
    record->tgid = (u32)tgid;
    record->pad = 0;
    bpf_ringbuf_submit(record, 0);
    return 0;
}

// The verifier reads this string to decide which helpers are reachable; it is
// not the copyright licence of this file (see the SPDX tag above). "GPL" is the
// value that keeps every helper available on every kernel.
char _license[] SEC("license") = "GPL";
