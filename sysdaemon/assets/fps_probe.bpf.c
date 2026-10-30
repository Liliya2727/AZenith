// SPDX-License-Identifier: Apache-2.0 OR GPL-2.0-only
//
// Kernel side of the fps uprobe. The daemon loads this object, attaches
// `fps_probe` to the Surface::queueBuffer* family in libgui for the focused
// pid and counts the ring records to derive the frame rate.
//
// Rebuild after editing (the object is committed, consumers need no clang):
//
//   clang -O2 -g -target bpfel -c fps_probe.bpf.c -o fps_probe.bpf.o
//
// -g is required: the map below is declared BTF style (`.maps`), which aya
// reads to learn the map type and size.

#define SEC(NAME) __attribute__((section(NAME), used))

typedef unsigned long long u64;

#define __uint(name, val) int (*name)[val]

struct frame_record {
    u64 ktime_ns;
};

struct {
    __uint(type, 27); /* BPF_MAP_TYPE_RINGBUF */
    __uint(max_entries, 262144);
} RING_BUF SEC(".maps");

static u64 (*bpf_ktime_get_ns)(void) = (void *)5;
static void *(*bpf_ringbuf_reserve)(void *ringbuf, u64 size, u64 flags) = (void *)131;
static void (*bpf_ringbuf_submit)(void *data, u64 flags) = (void *)132;

SEC("uprobe")
int fps_probe(void *ctx)
{
    (void)ctx;

    struct frame_record *record = bpf_ringbuf_reserve(&RING_BUF, sizeof(*record), 0);
    if (!record)
        return 0;

    record->ktime_ns = bpf_ktime_get_ns();
    bpf_ringbuf_submit(record, 0);
    return 0;
}

// The verifier reads this string to decide which helpers are reachable; it is
// not the copyright licence of this file (see the SPDX tag above). "GPL" is the
// value that keeps every helper available on every kernel.
char _license[] SEC("license") = "GPL";
