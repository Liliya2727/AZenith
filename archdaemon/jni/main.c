/*
 * Copyright (C) 2025-2026 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <AZenith.h>
#include <libgen.h>
#include <string.h>
#define IS_CMD(arg, long_cmd, short_cmd) (strcmp(arg, long_cmd) == 0 || strcmp(arg, short_cmd) == 0)

int main(int argc, char* argv[]) {

    if (getuid() != 0) {
        fprintf(stderr, "\033[31mERROR:\033[0m Please run this program as root\n");
        return 1;
    }
    
    if (argc == 1 || IS_CMD(argv[1], "--help", "-h")) {
        print_help();
        return 0;
    }

    char* cmd = argv[1];
    
    if (IS_CMD(cmd, "--appactivity", "-actv")) { openAppMainActivity(); return 0; }
    if (IS_CMD(cmd, "--run", "-r"))            { main_daemon(); return 0; }
    if (IS_CMD(cmd, "--version", "-V"))        { printversion(); return 0; }
    if (IS_CMD(cmd, "--clearlogs", "-c"))       { clearlogs(); return 0; }
    
    if (!require_daemon_running()) {
        return 1;
    }

    if (IS_CMD(cmd, "--profile", "-p"))          return handle_profile(argc, argv);
    if (IS_CMD(cmd, "--log", "-l"))              return handle_log(argc, argv);
    if (IS_CMD(cmd, "--verboselog", "-vl"))      return handle_verboselog(argc, argv);
    if (IS_CMD(cmd, "--checkbypasschg", "-cbc")) return check_bypass_compatibility();
    
    fprintf(stderr, "\033[31mERROR:\033[0m Unknown command: %s\n", cmd);
    return 1;
}
