#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <time.h>
#include <pthread.h>
#include <dlfcn.h>
#include <sys/sysinfo.h>
#include <sys/resource.h>

#define MAX_TASKS 16384

static char APP[256] = "";
static char MODE = 'n';
static int LAUNCH = 0;
static int VERBOSE = 0;
static int THREADS = 0;
static int SAVELOG = 0;
static FILE *logfile = NULL;

#define LOG(...) do { \
    time_t t=time(NULL); struct tm *tm=localtime(&t); \
    char msg[2048]; \
    snprintf(msg,sizeof(msg),"[%02d:%02d:%02d] ",tm->tm_hour,tm->tm_min,tm->tm_sec); \
    printf("%s",msg); printf(__VA_ARGS__); printf("\n"); fflush(stdout); \
    if(SAVELOG && logfile){fprintf(logfile,"%s",msg); fprintf(logfile,__VA_ARGS__); fprintf(logfile,"\n"); fflush(logfile);} \
} while(0)

typedef struct {
    char path[1024];
    int real;
} preload_task_t;

static preload_task_t tasks[MAX_TASKS];
static int task_count = 0;
static pthread_mutex_t lock = PTHREAD_MUTEX_INITIALIZER;

static void usage(const char *self) {
    printf("\n============================================\n");
    printf(" Preload by Andi\n");
    printf(" Android Preload Utility\n");
    printf(" Modified by @Zexshia\n");
    printf("============================================\n\n");

    printf("Usage:\n");
    printf("  %s -a <package> [options]\n\n", self);

    printf("Options:\n");
    printf("  -a <package> Target package name (required)\n");
    printf("  -m <mode> Preload mode:\n");
    printf("   n = Normal I/O preload (fast, safe)\n");
    printf("   d = Deep preload (dlopen .so files)\n");
    printf("   x = Extreme mode (deep + high priority)\n");
    printf("   r = Recursive preload (looped deep check)\n");
    printf("  -l Launch the app after preload\n");
    printf("  -v Verbose logging (show all actions)\n");
    printf("  -t <num> Number of worker threads (default: auto)\n");
    printf("  -s Save log to /storage/emulated/0/\n");
    printf("  -h, --help Show this help screen and exit\n\n");

    printf("Examples:\n");
    printf("  %s -a com.roblox.client -m d -v -l\n", self);
    printf("  %s -a com.mobile.legends -m x -s -t 8\n", self);
    printf("  %s -a com.pubg.imobile -m r\n", self);

    printf("\nNotes:\n");
    printf(" • Requires Android shell environment (cmd, am, monkey).\n");
    printf(" • Deep and extreme modes may increase memory load.\n");
    printf(" • Log file is saved automatically if -s flag is used.\n");
    printf(" • Best used with root access for deeper preload coverage.\n");

    printf("\n============================================\n");
    exit(0);
}

static void env_check(){
    if(system("which cmd >/dev/null 2>&1")!=0){
        LOG("Missing Android environment. Please run inside Android shell.");
        exit(1);
    }
}

static void preload_file_io(const char *path){
    FILE *fp=fopen(path,"rb");
    if(!fp) return;
    if(VERBOSE) LOG("→ [I/O] %s",path);
    char buf[4096];
    while(fread(buf,1,sizeof(buf),fp)>0);
    fclose(fp);
}

static void preload_file_dlopen(const char *path){
    void *h=dlopen(path,RTLD_LAZY|RTLD_GLOBAL);
    if(h){
        if(VERBOSE) LOG("→ [dlopen] %s",path);
        dlclose(h);
    } else if(VERBOSE){
        LOG("failed: %s (%s)", path, dlerror());
    }
}

void *worker(void *arg){
    (void)arg;
    while(1){
        pthread_mutex_lock(&lock);
        if(task_count<=0){
            pthread_mutex_unlock(&lock);
            break;
        }
        preload_task_t task=tasks[--task_count];
        pthread_mutex_unlock(&lock);

        if(task.real) {
            preload_file_dlopen(task.path);
        } else {
            preload_file_io(task.path);
        }
    }
    return NULL;
}

static void auto_smart_paths(char paths[][512], int *count) {
    *count = 0;
    char cmd[1024];
    char line[1024];
    FILE *fp;

    snprintf(cmd, sizeof(cmd), "pm path %s", APP);
    fp = popen(cmd, "r");
    if (!fp) {
        LOG("⚠️  error cum cum.");
        return;
    }

    while (fgets(line, sizeof(line), fp)) {
        char *nl = strchr(line, '\n'); if (nl) *nl = 0;
        if (line[0] == '\0') continue;
        
        char *path_start = strstr(line, "package:");
        if (path_start) {
            path_start += strlen("package:");
            
            char *last_slash = strrchr(path_start, '/');
            if (last_slash) {
                *last_slash = '\0'; 
                
                int exists = 0;
                for(int i=0; i < *count; i++) {
                    if(strncmp(paths[i], path_start, 511) == 0) {
                        exists = 1;
                        break;
                    }
                }

                if (!exists && *count < 32) { 
                    strncpy(paths[*count], path_start, 511);
                    paths[*count][511] = '\0';
                    (*count)++;
                    if (VERBOSE) LOG("Added APK path: %s", path_start);
                }
            }
        }
    }
    pclose(fp);

    const char *candidates[] = {
        "/storage/emulated/0/Android/data/%s",
        "/storage/emulated/0/Android/obb/%s",
        "/data/data/%s",
        "/sdcard/PreloadTest/%s"
    };
    
    char buf[512];
    int num_candidates = (int)(sizeof(candidates) / sizeof(candidates[0]));
    for (int i = 0; i < num_candidates; i++) {
        snprintf(buf, sizeof(buf), candidates[i], APP);
        
        int exists = 0;
        for(int j=0; j < *count; j++) {
            if(strncmp(paths[j], buf, 511) == 0) {
                exists = 1;
                break;
            }
        }

        if (!exists && *count < 32) {
             strncpy(paths[*count], buf, 511);
             paths[*count][511] = '\0';
             (*count)++;
             if (VERBOSE) LOG("Added candidate path: %s", buf);
        }
    }
}
static void walk_dir_find(const char *dir,int real){
    char cmd[2048];
    snprintf(cmd,sizeof(cmd),
        "find '%s' -type f \\( -name '*.so' -o -name '*.pak' -o -name '*.bin' -o -name '*.dat' -o -name '*.cache' -o -name '*.json' -o -name '*.xml' -o -name '*.dex' -o -name '*.txt' -o -name '*.obb' -o -name '*.lua' -o -name '*.cfg' -o -name '*.mp3' -o -name '*.mp4' \\) 2>/dev/null",
        dir);

    FILE *fp=popen(cmd,"r");
    if(!fp){
         LOG("⚠️  Cannot access: %s (permission denied?)", dir);
        return;
    }

    char path[1024];
    while(fgets(path,sizeof(path),fp)){
        char *nl=strchr(path,'\n'); if(nl) *nl=0;
        pthread_mutex_lock(&lock);
        if(task_count<MAX_TASKS){
            strncpy(tasks[task_count].path,path,sizeof(tasks[0].path)-1);
            tasks[task_count].path[sizeof(tasks[0].path)-1] = '\0';
            tasks[task_count].real=real;
            task_count++;
            if(VERBOSE) LOG("Found: %s",path);
        }
        pthread_mutex_unlock(&lock);
    }
    pclose(fp);
}

static void smart_walk_all(){
    char dirs[32][512]; int dircount=0;
    auto_smart_paths(dirs,&dircount);
    
    if (dircount == 0) {
        LOG("⚠️  No paths found. Is package '%s' installed?", APP);
    }

    int use_real = (MODE != 'n');
    for(int i=0;i<dircount;i++){
        LOG("Scanning: %s",dirs[i]);
        walk_dir_find(dirs[i],use_real);
    }
    LOG("Preload found %d files",task_count);
}

static void launch_app(){
    char cmd[512],activity[256]=""; FILE *fp;
    snprintf(cmd,sizeof(cmd),"cmd package resolve-activity --brief %s 2>/dev/null | tail -n1",APP);
    fp=popen(cmd,"r");
    if(fp){ fgets(activity,sizeof(activity),fp); pclose(fp); }
    char *nl=strchr(activity,'\n'); if(nl) *nl=0;
    
    if(strlen(activity)>0 && strchr(activity,'/')){
        LOG("Launching: %s",activity);
        snprintf(cmd,sizeof(cmd),"am start -n %s >/dev/null 2>&1",activity);
        if(system(cmd)!=0){
            LOG("Fallback monkey...");
            snprintf(cmd,sizeof(cmd),"monkey -p %s -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1",APP);
            system(cmd);
        }
    } else {
        LOG("No valid activity, using monkey...");
        snprintf(cmd,sizeof(cmd),"monkey -p %s -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1",APP);
        system(cmd);
    }
}

static void init_logfile(){
    if(!SAVELOG) return;
    char fname[512];
    time_t t=time(NULL); struct tm *tm=localtime(&t);
    snprintf(fname,sizeof(fname),"/storage/emulated/0/DDload_%s_%04d%02d%02d_%02d%02d.log",
             APP,tm->tm_year+1900,tm->tm_mon+1,tm->tm_mday,tm->tm_hour,tm->tm_min);
    logfile=fopen(fname,"w");
    if(logfile) LOG("Log file: %s",fname);
    else LOG("Failed to open log file (permission?)");
}

int main(int argc,char *argv[]){
    for(int i=1;i<argc;i++) if(strcmp(argv[i],"--help")==0) usage(argv[0]);

    int opt;
    while((opt=getopt(argc,argv,"a:m:lvht:s"))!=-1){
        switch(opt){
            case 'a': strncpy(APP,optarg,sizeof(APP)-1); APP[sizeof(APP)-1] = '\0'; break;
            case 'm': MODE=optarg[0]; break;
            case 'l': LAUNCH=1; break;
            case 'v': VERBOSE=1; break;
            case 't': THREADS=atoi(optarg); break;
            case 's': SAVELOG=1; break;
            case 'h': usage(argv[0]); break;
            default: usage(argv[0]);
        }
    }

    if(APP[0]=='\0') usage(argv[0]);
    env_check();
    init_logfile();

    LOG("============================================");
    LOG(" DDload starting for: %s (mode=%c)",APP,MODE);
    LOG("============================================");

    if(THREADS<=0) THREADS=get_nprocs();
    LOG("Using %d threads",THREADS);

    if(MODE=='x') setpriority(PRIO_PROCESS,0,-15);

    do {
        clock_t start=clock();
        task_count = 0;
        smart_walk_all();
        
        if (task_count == 0) {
            LOG("⚠️  No files found to preload.");
            LOG("If app is installed, check storage permissions or try with root.");
            if(MODE == 'r') {
                 LOG("Retrying in 5 seconds...");
                 sleep(5);
                 continue;
            } else {
                break;
            }
        }
    
        pthread_t th[THREADS];
        for(int i=0;i<THREADS;i++) pthread_create(&th[i],NULL,worker,NULL);
        for(int i=0;i<THREADS;i++) pthread_join(th[i],NULL);
    
        double dur=(double)(clock()-start)/CLOCKS_PER_SEC;
        LOG("Preload complete in %.2fs",dur);

        if(MODE == 'r') {
            LOG("Recursive mode: Re-scanning in 5 seconds...");
            sleep(5);
        }

    } while (MODE == 'r');

    if(LAUNCH) launch_app();
    LOG("All done!");

    if(SAVELOG && logfile){
        LOG("Saving log...");
        fclose(logfile);
    }
    return 0;
}