

#define VMTOUCH_VERSION "1.4.1"
#define RESIDENCY_CHART_WIDTH 60
#define CHART_UPDATE_INTERVAL 0.1
#define MAX_CRAWL_DEPTH 1024
#define MAX_NUMBER_OF_IGNORES 1024
#define MAX_NUMBER_OF_FILENAME_FILTERS 1024
#define MAX_FILENAME_LENGTH 1024

#if defined(__linux__) || (defined(__hpux) && !defined(__LP64__))
#define _FILE_OFFSET_BITS 64
#endif

#ifdef __linux__
#define _XOPEN_SOURCE 600
#define _DEFAULT_SOURCE
#define _BSD_SOURCE
#define _GNU_SOURCE
#endif

#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <stdlib.h>
#include <ctype.h>
#include <stdarg.h>
#include <stdint.h>
#include <stdbool.h>
#include <time.h>
#include <signal.h>
#include <sys/select.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/time.h>
#include <sys/resource.h>
#include <sys/mman.h>
#include <sys/wait.h>
#include <limits.h>
#include <inttypes.h>
#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <math.h>
#include <search.h>
#include <libgen.h>
#include <fnmatch.h>

#if defined(__linux__)
#include <sys/ioctl.h>
#include <sys/mount.h>
#include <sys/utsname.h>
#endif

/* --- Structures --- */

typedef struct {
    bool touch;
    bool evict;
    bool quiet;
    int verbose;
    bool lock;
    bool lockall;
    bool daemon;
    bool follow_symlinks;
    bool single_fs;
    bool ignore_hardlinks;
    bool wait;
    bool batch_0_delim;
    size_t max_file_size;
    int64_t offset;
    int64_t max_len;
    char *batch_file;
    char *pidfile;
    char *output_type;
    
    char *ignore_list[MAX_NUMBER_OF_IGNORES];
    int num_ignores;
    char *filter_list[MAX_NUMBER_OF_FILENAME_FILTERS];
    int num_filters;
} VmtouchConfig;

typedef struct {
    int64_t total_pages;
    int64_t total_pages_in_core;
    int64_t total_files;
    int64_t total_dirs;
} VmtouchStats;

struct dev_and_inode {
    dev_t dev;
    ino_t ino;
};

/* --- Global State --- */

static VmtouchConfig config = { .max_file_size = SIZE_MAX };
static VmtouchStats stats = {0};

static long page_size;
static int exit_pipe[2];
static pid_t daemon_pid = 0;
static unsigned int junk_counter; 

static int curr_crawl_depth = 0;
static ino_t crawl_inodes[MAX_CRAWL_DEPTH];

static void *seen_inodes = NULL;
static dev_t orig_device = 0;
static bool orig_device_inited = false;

/* --- Core Utilities --- */

static void send_exit_signal(char code) {
    if (daemon_pid == 0 && config.wait) {
        if (write(exit_pipe[1], &code, 1) < 0) {
            fprintf(stderr, "vmtouch: FATAL: write: %s\n", strerror(errno));
        }
    }
}

static void fatal(const char *fmt, ...) {
    va_list ap;
    va_start(ap, fmt);
    fprintf(stderr, "vmtouch: FATAL: ");
    vfprintf(stderr, fmt, ap);
    fprintf(stderr, "\n");
    va_end(ap);
    send_exit_signal(1);
    exit(EXIT_FAILURE);
}

static void warning(const char *fmt, ...) {
    if (config.quiet) return;
    va_list ap;
    va_start(ap, fmt);
    fprintf(stderr, "vmtouch: WARNING: ");
    vfprintf(stderr, fmt, ap);
    fprintf(stderr, "\n");
    va_end(ap);
}

static void reopen_all(void) {
    if (!freopen("/dev/null", "r", stdin) ||
        !freopen("/dev/null", "w", stdout) ||
        !freopen("/dev/null", "w", stderr)) {
        fatal("freopen: %s", strerror(errno));
    }
}

/* --- Process Management --- */

static int wait_for_child(void) {
    char exit_value = 0;
    while (1) {
        struct timeval tv = { .tv_sec = 1, .tv_usec = 0 };
        fd_set rfds;
        FD_ZERO(&rfds);
        FD_SET(exit_pipe[0], &rfds);

        if (select(exit_pipe[0] + 1, &rfds, NULL, NULL, &tv) < 0)
            fatal("select: %s", strerror(errno));

        int wait_status;
        if (waitpid(daemon_pid, &wait_status, WNOHANG) > 0)
            fatal("daemon shut down unexpectedly");

        if (FD_ISSET(exit_pipe[0], &rfds)) break;
    }
    
    if (read(exit_pipe[0], &exit_value, 1) < 0)
        fatal("read: %s", strerror(errno));
    
    return exit_value;
}

static void go_daemon(void) {
    daemon_pid = fork();
    if (daemon_pid == -1) fatal("fork: %s", strerror(errno));
    
    if (daemon_pid > 0) {
        if (config.wait) exit(wait_for_child());
        exit(EXIT_SUCCESS);
    }

    if (setsid() == -1) fatal("setsid: %s", strerror(errno));
    if (!config.wait) reopen_all();
}

static void remove_pidfile(void) {
    if (unlink(config.pidfile) < 0 && errno != ENOENT) {
        warning("unable to remove pidfile %s (%s)", config.pidfile, strerror(errno));
    }
}

static void write_pidfile(void) {
    FILE *f = fopen(config.pidfile, "w");
    if (!f) {
        warning("unable to open pidfile %s (%s), skipping", config.pidfile, strerror(errno));
        return;
    }
    if (fprintf(f, "%d\n", getpid()) < 0) {
        warning("unable to write to pidfile %s (%s), deleting it", config.pidfile, strerror(errno));
        remove_pidfile();
    }
    fclose(f);
}

static void signal_handler_clear_pidfile(int signal_num) {
    remove_pidfile();
}

static void register_signals_for_pidfile(void) {
    struct sigaction sa = {0};
    sa.sa_handler = signal_handler_clear_pidfile;
    if (sigaction(SIGINT, &sa, NULL) < 0 ||
        sigaction(SIGTERM, &sa, NULL) < 0 ||
        sigaction(SIGQUIT, &sa, NULL) < 0) {
        warning("unable to register signals for pidfile (%s), skipping", strerror(errno));
    }
}

/* --- Parsing & Formatting --- */

static char *pretty_print_size(int64_t inp) {
    static char output[100];
    if (inp < 1024) { snprintf(output, sizeof(output), "%" PRId64, inp); return output; }
    inp /= 1024;
    if (inp < 1024) { snprintf(output, sizeof(output), "%" PRId64 "K", inp); return output; }
    inp /= 1024;
    if (inp < 1024) { snprintf(output, sizeof(output), "%" PRId64 "M", inp); return output; }
    inp /= 1024;
    snprintf(output, sizeof(output), "%" PRId64 "G", inp);
    return output;
}

static int64_t parse_size(char *inp) {
    char *tp;
    size_t len = strlen(inp);
    if (len < 1) fatal("bad size format");

    char mult_char = tolower((unsigned char)inp[len-1]);
    int64_t mult = 1;

    if (isalpha(mult_char)) {
        switch(mult_char) {
            case 'k': mult = 1024; break;
            case 'm': mult = 1024 * 1024; break;
            case 'g': mult = 1024 * 1024 * 1024; break;
            default: fatal("unknown size multiplier: %c", mult_char);
        }
        inp[len-1] = '\0';
    }

    double val = strtod(inp, &tp);
    if (val < 0 || val == HUGE_VAL || *tp != '\0') fatal("bad size format");
    val *= mult;
    if (val > INT64_MAX) fatal("size too large");

    return (int64_t)val;
}

static int64_t bytes2pages(int64_t bytes) {
    return (bytes + page_size - 1) / page_size;
}

static void parse_range(char *inp) {
    char *token = strsep(&inp, "-");
    int64_t lower_range = 0, upper_range = 0;

    if (inp == NULL) {
        upper_range = parse_size(token);
    } else {
        if (*token != '\0') lower_range = parse_size(token);
        token = strsep(&inp, "-");
        if (*token != '\0') upper_range = parse_size(token);
        if (strsep(&inp, "-") != NULL) fatal("malformed range: multiple hyphens");
    }

    config.offset = (lower_range / page_size) * page_size;
    if (upper_range) {
        if (upper_range <= config.offset) fatal("range limits out of order");
        config.max_len = upper_range - config.offset;
    }
}

static void parse_ignore_item(char *inp) {
    if (!inp) return;
    if (strlen(inp) > MAX_FILENAME_LENGTH) fatal("pattern too long for -i: %s", inp);
    if (config.num_ignores >= MAX_NUMBER_OF_IGNORES) fatal("too many -i patterns");
    config.ignore_list[config.num_ignores++] = strdup(inp);
}

static void parse_filename_filter_item(char *inp) {
    if (!inp) return;
    if (strlen(inp) > MAX_FILENAME_LENGTH) fatal("pattern too long for -I: %s", inp);
    if (config.num_filters >= MAX_NUMBER_OF_FILENAME_FILTERS) fatal("too many -I patterns");
    config.filter_list[config.num_filters++] = strdup(inp);
}

/* --- System Helpers --- */

static bool is_mincore_page_resident(unsigned char p) {
    return (p & 0x1) != 0;
}

static void increment_nofile_rlimit(void) {
    struct rlimit r;
    if (getrlimit(RLIMIT_NOFILE, &r)) fatal("getrlimit: %s", strerror(errno));

    r.rlim_cur = r.rlim_max + 1;
    r.rlim_max = r.rlim_max + 1;

    if (setrlimit(RLIMIT_NOFILE, &r)) {
        if (errno == EPERM) {
            if (getuid() == 0 || geteuid() == 0) fatal("system open file limit reached");
            fatal("open file limit reached, retry as root");
        }
        fatal("setrlimit: %s", strerror(errno));
    }
}

static double gettimeofday_as_double(void) {
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return tv.tv_sec + (tv.tv_usec / 1000000.0);
}

static void print_page_residency_chart(FILE *out, unsigned char *mincore_array, int64_t pages_in_file) {
    int64_t pages_in_core = 0;
    int64_t pages_per_char = (pages_in_file <= RESIDENCY_CHART_WIDTH) ? 1 : (pages_in_file / RESIDENCY_CHART_WIDTH) + 1;
    int64_t curr = 0, j = 0;

    fprintf(out, "\r[");
    for (int64_t i = 0; i < pages_in_file; i++) {
        if (is_mincore_page_resident(mincore_array[i])) {
            curr++;
            pages_in_core++;
        }
        j++;
        if (j == pages_per_char) {
            fprintf(out, "%s", (curr == pages_per_char) ? "O" : (curr == 0) ? " " : "o");
            j = curr = 0;
        }
    }
    if (j) fprintf(out, "%s", (curr == j) ? "O" : (curr == 0) ? " " : "o");
    
    fprintf(out, "] %" PRId64 "/%" PRId64, pages_in_core, pages_in_file);
    fflush(out);
}

#ifdef __linux__
static bool can_do_mincore(struct stat *st) {
    struct utsname utsinfo;
    if (uname(&utsinfo) == 0) {
        unsigned long ver[2] = {0};
        char *p = utsinfo.release;
        for (int i = 0; i < 2 && *p; ) {
            if (isdigit(*p)) {
                ver[i++] = strtol(p, &p, 10);
            } else {
                p++;
            }
        }
        if (ver[0] < 5 || (ver[0] == 5 && ver[1] < 2)) return true;
    }
    uid_t uid = getuid();
    return (st->st_uid == uid) ||
           (st->st_gid == getgid() && (st->st_mode & S_IWGRP)) ||
           (st->st_mode & S_IWOTH) ||
           (uid == 0);
}
#endif

/* --- Core Engine --- */

static void vmtouch_file(const char *path) {
    int open_flags = O_RDONLY;
#if defined(O_NOATIME)
    open_flags |= O_NOATIME;
#endif

retry_open:;
    int fd = open(path, open_flags, 0);

#if defined(O_NOATIME)
    if (fd == -1 && errno == EPERM) {
        open_flags &= ~O_NOATIME;
        fd = open(path, open_flags, 0);
    }
#endif

    if (fd == -1) {
        if (errno == ENFILE || errno == EMFILE) {
            increment_nofile_rlimit();
            goto retry_open;
        }
        warning("unable to open %s (%s), skipping", path, strerror(errno));
        return;
    }

    struct stat sb;
    if (fstat(fd, &sb)) {
        warning("unable to fstat %s (%s), skipping", path, strerror(errno));
        close(fd);
        return;
    }

    int64_t len_of_file = 0;
    if (S_ISBLK(sb.st_mode)) {
#if defined(__linux__)
        if (ioctl(fd, BLKGETSIZE64, &len_of_file)) {
            warning("unable to ioctl %s (%s), skipping", path, strerror(errno));
            close(fd);
            return;
        }
#else
        fatal("discovering size of block devices not supported on this platform");
#endif
    } else {
        len_of_file = sb.st_size;
    }

    if (len_of_file == 0 || len_of_file > config.max_file_size) {
        if (len_of_file > config.max_file_size) warning("file %s too large, skipping", path);
        close(fd);
        return;
    }

    int64_t len_of_range = len_of_file - config.offset;
    if (config.max_len > 0 && (config.offset + config.max_len) < len_of_file) {
        len_of_range = config.max_len;
    } else if (config.offset >= len_of_file) {
        warning("file %s smaller than offset, skipping", path);
        close(fd);
        return;
    }

    void *mem = mmap(NULL, len_of_range, PROT_READ, MAP_SHARED, fd, config.offset);
    if (mem == MAP_FAILED) {
        warning("unable to mmap file %s (%s), skipping", path, strerror(errno));
        close(fd);
        return;
    }

    int64_t pages_in_range = bytes2pages(len_of_range);
    stats.total_pages += pages_in_range;

    if (config.evict) {
        if (config.verbose) printf("Evicting %s\n", path);
#if defined(__linux__) || defined(__hpux)
        if (posix_fadvise(fd, config.offset, len_of_range, POSIX_FADV_DONTNEED))
            warning("unable to posix_fadvise file %s (%s)", path, strerror(errno));
#elif defined(__FreeBSD__) || defined(__sun__) || defined(__APPLE__)
        if (msync(mem, len_of_range, MS_INVALIDATE))
            warning("unable to msync invalidate file %s (%s)", path, strerror(errno));
#else
        fatal("cache eviction not supported on this platform");
#endif
    } else {
        unsigned char *mincore_array = malloc(pages_in_range);
        if (!mincore_array) fatal("Failed to allocate memory for mincore array");

        if (mincore(mem, len_of_range, (void*)mincore_array)) 
            fatal("mincore %s (%s)", path, strerror(errno));

        for (int64_t i = 0; i < pages_in_range; i++) {
            if (is_mincore_page_resident(mincore_array[i])) stats.total_pages_in_core++;
        }

        double last_chart_print_time = 0.0;
        if (config.verbose) {
            printf("%s\n", path);
#ifdef __linux__
            if (!can_do_mincore(&sb)) warning("No write permission, residency chart may be inaccurate");
#endif
            last_chart_print_time = gettimeofday_as_double();
            print_page_residency_chart(stdout, mincore_array, pages_in_range);
        }

        if (config.touch) {
            for (int64_t i = 0; i < pages_in_range; i++) {
                junk_counter += ((volatile char*)mem)[i * page_size];
                mincore_array[i] = 1;

                if (config.verbose) {
                    double temp_time = gettimeofday_as_double();
                    if (temp_time > (last_chart_print_time + CHART_UPDATE_INTERVAL)) {
                        last_chart_print_time = temp_time;
                        print_page_residency_chart(stdout, mincore_array, pages_in_range);
                    }
                }
            }
        }

        if (config.verbose) {
            print_page_residency_chart(stdout, mincore_array, pages_in_range);
            printf("\n");
        }
        free(mincore_array);
    }

    if (config.lock) {
        if (mlock(mem, len_of_range)) fatal("mlock: %s (%s)", path, strerror(errno));
    }

    if (!config.lock && !config.lockall) {
        if (munmap(mem, len_of_range)) warning("unable to munmap file %s (%s)", path, strerror(errno));
    }

    close(fd);
}

/* --- Crawling & Deduplication --- */

static int compare_func(const void *p1, const void *p2) {
    const struct dev_and_inode *kp1 = p1, *kp2 = p2;
    if (kp1->ino != kp2->ino) return (kp1->ino > kp2->ino) - (kp1->ino < kp2->ino);
    return (kp1->dev > kp2->dev) - (kp1->dev < kp2->dev);
}

static void add_object(struct stat *st) {
    struct dev_and_inode *newp = malloc(sizeof(*newp));
    if (!newp) fatal("malloc: out of memory");
    newp->dev = st->st_dev;
    newp->ino = st->st_ino;
    if (tsearch(newp, &seen_inodes, compare_func) == NULL) fatal("tsearch: out of memory");
}

static bool find_object(struct stat *st) {
    struct dev_and_inode obj = { .dev = st->st_dev, .ino = st->st_ino };
    return tfind(&obj, &seen_inodes, compare_func) != NULL;
}

static bool is_ignored(const char *path) {
    if (!config.num_ignores) return false;
    char *path_copy = strdup(path);
    char *fname = basename(path_copy);
    bool match = false;
    for (int i = 0; i < config.num_ignores; i++) {
        if (fnmatch(config.ignore_list[i], fname, 0) == 0) {
            match = true;
            break;
        }
    }
    free(path_copy);
    return match;
}

static bool is_filename_filtered(const char *path) {
    if (!config.num_filters) return true;
    char *path_copy = strdup(path);
    char *fname = basename(path_copy);
    bool match = false;
    for (int i = 0; i < config.num_filters; i++) {
        if (fnmatch(config.filter_list[i], fname, 0) == 0) {
            match = true;
            break;
        }
    }
    free(path_copy);
    return match;
}

static void vmtouch_crawl(const char *path) {
    char clean_path[PATH_MAX];
    strncpy(clean_path, path, sizeof(clean_path) - 1);
    clean_path[sizeof(clean_path) - 1] = '\0';
    
    size_t len = strlen(clean_path);
    if (len > 1 && clean_path[len - 1] == '/') clean_path[len - 1] = '\0';

    if (is_ignored(clean_path)) return;

    struct stat sb;
    if ((config.follow_symlinks ? stat : lstat)(clean_path, &sb)) {
        warning("unable to stat %s (%s)", clean_path, strerror(errno));
        return;
    }

    if (S_ISLNK(sb.st_mode)) {
        warning("not following symbolic link %s", clean_path);
        return;
    }

    if (config.single_fs) {
        if (!orig_device_inited) {
            orig_device = sb.st_dev;
            orig_device_inited = true;
        } else if (sb.st_dev != orig_device) {
            warning("not recursing into separate filesystem %s", clean_path);
            return;
        }
    }

    if (!config.ignore_hardlinks && sb.st_nlink > 1) {
        if (find_object(&sb)) return;
        add_object(&sb);
    }

    if (S_ISDIR(sb.st_mode)) {
        for (int i = 0; i < curr_crawl_depth; i++) {
            if (crawl_inodes[i] == sb.st_ino) {
                warning("symbolic link loop detected: %s", clean_path);
                return;
            }
        }
        if (curr_crawl_depth >= MAX_CRAWL_DEPTH) fatal("maximum directory crawl depth reached: %s", clean_path);

        stats.total_dirs++;
        crawl_inodes[curr_crawl_depth] = sb.st_ino;

    retry_opendir:;
        DIR *dirp = opendir(clean_path);
        if (!dirp) {
            if (errno == ENFILE || errno == EMFILE) {
                increment_nofile_rlimit();
                goto retry_opendir;
            }
            warning("unable to opendir %s (%s), skipping", clean_path, strerror(errno));
            return;
        }

        struct dirent *de;
        while ((de = readdir(dirp))) {
            if (strcmp(de->d_name, ".") == 0 || strcmp(de->d_name, "..") == 0) continue;
            
            char npath[PATH_MAX];
            if (snprintf(npath, sizeof(npath), "%s/%s", clean_path, de->d_name) >= sizeof(npath)) {
                warning("path too long %s", clean_path);
                break;
            }

            curr_crawl_depth++;
            vmtouch_crawl(npath);
            curr_crawl_depth--;
        }
        closedir(dirp);
    } else if (S_ISREG(sb.st_mode) || S_ISBLK(sb.st_mode)) {
        if (is_filename_filtered(clean_path)) {
            stats.total_files++;
            vmtouch_file(clean_path);
        }
    } else {
        warning("skipping non-regular file: %s", clean_path);
    }
}

static void vmtouch_batch_crawl(const char *path) {
    FILE *f = strcmp(path, "-") == 0 ? stdin : fopen(path, "r");
    if (!f) {
        warning("unable to open %s (%s), skipping", path, strerror(errno));
        return;
    }

    char *line = NULL;
    size_t len = 0;
    int delim = config.batch_0_delim ? '\0' : '\n';

    while (getdelim(&line, &len, delim, f) != -1) {
        size_t line_len = strlen(line);
        if (line_len > 0 && line[line_len - 1] == '\n') line[line_len - 1] = '\0';
        vmtouch_crawl(line);
    }

    free(line);
    if (f != stdin) fclose(f);
}

static void usage(void) {
    printf("\nvmtouch v%s - the Virtual Memory Toucher\n", VMTOUCH_VERSION);
    printf("Portable file system cache diagnostics and control\n\n");
    printf("Usage: vmtouch [OPTIONS] ... FILES OR DIRECTORIES ...\n\nOptions:\n");
    printf("  -t touch pages into memory\n");
    printf("  -e evict pages from memory\n");
    printf("  -l lock pages in physical memory with mlock(2)\n");
    printf("  -L lock pages in physical memory with mlockall(2)\n");
    printf("  -d daemon mode\n");
    printf("  -m <size> max file size to touch\n");
    printf("  -p <range> use the specified portion instead of the entire file\n");
    printf("  -f follow symbolic links\n");
    printf("  -F don't crawl different filesystems\n");
    printf("  -h also count hardlinked copies\n");
    printf("  -i <pattern> ignores files and directories that match this pattern\n");
    printf("  -I <pattern> only process files that match this pattern\n");
    printf("  -b <list file> get files or directories from the list file\n");
    printf("  -0 in batch mode (-b) separate paths with NUL byte instead of newline\n");
    printf("  -w wait until all pages are locked (only useful together with -d)\n");
    printf("  -P <pidfile> write a pidfile (only useful together with -l or -L)\n");
    printf("  -o <type> output in machine friendly format. 'kv' for key=value pairs.\n");
    printf("  -v verbose\n");
    printf("  -q quiet\n");
    exit(EXIT_FAILURE);
}

/* --- Main Entry --- */

int main(int argc, char **argv) {
    if (pipe(exit_pipe)) fatal("pipe: %s", strerror(errno));
    page_size = sysconf(_SC_PAGESIZE);

    int ch;
    while ((ch = getopt(argc, argv, "tevqlLdfFh0i:I:p:b:m:P:wo:")) != -1) {
        switch (ch) {
            case 't': config.touch = true; break;
            case 'e': config.evict = true; break;
            case 'q': config.quiet = true; break;
            case 'v': config.verbose++; break;
            case 'l': config.lock = true; config.touch = true; break;
            case 'L': config.lockall = true; config.touch = true; break;
            case 'd': config.daemon = true; break;
            case 'f': config.follow_symlinks = true; break;
            case 'F': config.single_fs = true; break;
            case 'h': config.ignore_hardlinks = true; break;
            case 'p': parse_range(optarg); break;
            case 'i': parse_ignore_item(optarg); break;
            case 'I': parse_filename_filter_item(optarg); break;
            case 'm': config.max_file_size = parse_size(optarg); break;
            case 'w': config.wait = true; break;
            case 'b': config.batch_file = optarg; break;
            case '0': config.batch_0_delim = true; break;
            case 'P': config.pidfile = optarg; break;
            case 'o': config.output_type = optarg; break;
            default: usage(); break;
        }
    }

    argc -= optind;
    argv += optind;

    if (config.touch && config.evict) fatal("invalid combination: -t and -e");
    if (config.evict && config.lock) fatal("invalid combination: -e and -l");
    if (config.lock && config.lockall) fatal("invalid combination: -l and -L");
    if (config.daemon) {
        if (!config.lock && !config.lockall) fatal("daemon mode needs -l or -L");
        if (!config.wait) { config.quiet = true; config.verbose = 0; }
    }
    if (config.wait && !config.daemon) fatal("wait mode needs -d");
    if (config.quiet && config.verbose) fatal("invalid combination: -q and -v");
    if (config.pidfile && !config.lock && !config.lockall) fatal("pidfile needs -l or -L");
    if (!argc && !config.batch_file) {
        printf("no files or directories specified\n");
        usage();
    }

    if (config.daemon) go_daemon();

    struct timeval start_time, end_time;
    gettimeofday(&start_time, NULL);

    if (config.batch_file) vmtouch_batch_crawl(config.batch_file);
    for (int i = 0; i < argc; i++) vmtouch_crawl(argv[i]);

    gettimeofday(&end_time, NULL);

    int64_t total_in_core_size = stats.total_pages_in_core * page_size;
    int64_t total_size = stats.total_pages * page_size;
    double perc = stats.total_pages ? (100.0 * stats.total_pages_in_core / stats.total_pages) : 0.0;
    double elapsed = (end_time.tv_sec - start_time.tv_sec) + (end_time.tv_usec - start_time.tv_usec) / 1000000.0;

    if (config.lock || config.lockall) {
        if (config.lockall && mlockall(MCL_CURRENT)) fatal("mlockall: %s", strerror(errno));
        if (config.pidfile) { register_signals_for_pidfile(); write_pidfile(); }
        if (!config.quiet) printf("LOCKED %" PRId64 " pages (%s)\n", stats.total_pages, pretty_print_size(total_size));
        if (config.wait) reopen_all();
        
        send_exit_signal(0);
        select(0, NULL, NULL, NULL, NULL); 
        exit(EXIT_SUCCESS);
    }

    if (!config.quiet) {
        if (!config.output_type) {
            if (config.verbose) printf("\n");
            printf("           Files: %" PRId64 "\n", stats.total_files);
            printf("     Directories: %" PRId64 "\n", stats.total_dirs);
            if (config.touch)
                printf("   Touched Pages: %" PRId64 " (%s)\n", stats.total_pages, pretty_print_size(total_size));
            else if (config.evict)
                printf("   Evicted Pages: %" PRId64 " (%s)\n", stats.total_pages, pretty_print_size(total_size));
            else {
                printf("  Resident Pages: %" PRId64 "/%" PRId64 "  %s/%s  %.3g%%\n",
                       stats.total_pages_in_core, stats.total_pages,
                       pretty_print_size(total_in_core_size), pretty_print_size(total_size), perc);
            }
            printf("         Elapsed: %.5g seconds\n", elapsed);
        } else if (strncmp(config.output_type, "kv", 2) == 0) {
            const char *desc = config.touch ? "Touched" : (config.evict ? "Evicted" : "Resident");
            printf("Files=%" PRId64 " Directories=%" PRId64 " %sPages=%" PRId64 " TotalPages=%" PRId64 " %sSize=%" PRId64 " TotalSize=%" PRId64 " %sPercent=%.3g Elapsed=%.5g\n", 
                stats.total_files, stats.total_dirs, desc, stats.total_pages_in_core, stats.total_pages, desc, total_in_core_size, total_size, desc, perc, elapsed);
        }
    }

    return 0;
}
