/*
 * Copyright (C) 2026-2027 Zexshia
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

/**
 * @brief Prints all available AZenith Daemon CLI commands, usage instructions, and examples to
 * stdout.
 */
void print_help(void) {
    printf("AZenith Daemon CLI (by @Zexshia)\n"
           "Version: %s\n"
           "\n"
           "Usage: sys.azenith-service [options]\n"
           "\n"
           "Options:\n"
           "     -r,    --run              Start AZenith daemon service\n"
           "\n"
           "     -p,    --profile <1|2|3>  Apply AZenith profiles via CLI\n"
           "                               1 : Performance\n"
           "                               2 : Balanced\n"
           "                               3 : Eco Mode\n"
           "\n"
           "     -l,    --log <TAG> <LVL> <MSG>\n"
           "                               Write a log message via AZenith logging service\n"
           "                               LEVELs: 0=DEBUG, 1=INFO, 2=WARN, 3=ERROR, 4=FATAL\n"
           "\n"
           "     -vl,   --verboselog <TAG> <LVL> <MSG>\n"
           "                               Write a verbose log message via AZenith logging service\n"
           "\n"
           "     -actv, --appactivity      Open AZenith App Main Activity\n"
           "\n"
           "     -cbc,  --checkbypasschg   Check bypass charge compatibility\n"
           "\n"
           "     -bpl,  --bypasspathlist   Show all embedded bypass charging paths\n"
           "\n"
           "     -V,    --version          Show AZenith current version\n"
           "\n"
           "     -h,    --help             Display this help message and exit\n"
           "\n"
           "Examples:\n"
           "     sys.azenith-service --run\n"
           "     sys.azenith-service --profile 2\n"
           "     sys.azenith-service --bypasspathlist\n"
           "     sys.azenith-service --help\n",
           MODULE_VERSION);
}
