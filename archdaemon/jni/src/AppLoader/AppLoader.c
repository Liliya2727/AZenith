/*
 * Copyright (C) 2024-2025 Zexshia
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

#include "AZenith.h"

void free_gamelist_cache(void) {
    pthread_mutex_lock(&cache_mutex);
    if (g_game_cache != NULL) {
        free(g_game_cache);
        g_game_cache = NULL;
    }
    g_game_cache_count = 0;
    pthread_mutex_unlock(&cache_mutex);
}

void reload_gamelist_cache(DaemonContext* ctx) {
    free_gamelist_cache();
    FILE* fp = fopen(GAMELIST, "r");
    if (!fp) {
        log_zenith(LOG_ERROR, "AppLoader: Failed to open GAMELIST for caching.");
        return;
    }
    fseek(fp, 0, SEEK_END);
    long size = ftell(fp);
    fseek(fp, 0, SEEK_SET);

    if (size <= 0) {
        fclose(fp);
        log_zenith(LOG_WARN, "AppLoader: GAMELIST is empty or invalid.");
        return;
    }

    char* buf = malloc(size + 1);
    if (!buf) {
        fclose(fp);
        log_zenith(LOG_FATAL, "AppLoader: Failed to allocate GAMELIST read buffer");
        return;
    }

    if (fread(buf, 1, size, fp) != (size_t)size) {
        fclose(fp);
        free(buf);
        log_zenith(LOG_ERROR, "AppLoader: Failed to read data from GAMELIST file");
        return;
    }
    fclose(fp);
    buf[size] = '\0';

    pthread_mutex_lock(&cache_mutex);
    int capacity = 16;
    g_game_cache = malloc(capacity * sizeof(GameConfig));
    if (!g_game_cache) {
        free(buf);
        pthread_mutex_unlock(&cache_mutex);
        log_zenith(LOG_FATAL, "AppLoader: Failed to allocate game cache array");
        return;
    }

    char* ptr = buf;
    while ((ptr = strstr(ptr, "\": {")) != NULL) {
        char* start_quote = ptr - 1;
        while (start_quote > buf && *start_quote != '"')
            start_quote--;

        if (*start_quote == '"') {
            if (g_game_cache_count >= capacity) {
                capacity *= 2;
                GameConfig* temp = realloc(g_game_cache, capacity * sizeof(GameConfig));
                if (!temp) {
                    log_zenith(LOG_FATAL, "AppLoader: Realloc failed while parsing gamelist");
                    free(g_game_cache);
                    g_game_cache = NULL;
                    g_game_cache_count = 0;
                    break;
                }
                g_game_cache = temp;
            }

            size_t pkg_len = ptr - (start_quote + 1);
            if (pkg_len >= sizeof(g_game_cache[g_game_cache_count].package)) {
                pkg_len = sizeof(g_game_cache[g_game_cache_count].package) - 1;
            }
            strncpy(g_game_cache[g_game_cache_count].package, start_quote + 1, pkg_len);
            g_game_cache[g_game_cache_count].package[pkg_len] = '\0';

            char* next_block = strstr(ptr + 4, "\": {");
            char* p;

            p = strstr(ptr, "\"perf_lite_mode\":");
            if (p && (!next_block || p < next_block))
                extract_string_value(g_game_cache[g_game_cache_count].perf_lite_mode, p,
                                     sizeof(g_game_cache[g_game_cache_count].perf_lite_mode));
            else
                strcpy(g_game_cache[g_game_cache_count].perf_lite_mode, "default");

            p = strstr(ptr, "\"dnd_on_gaming\":");
            if (p && (!next_block || p < next_block))
                extract_string_value(g_game_cache[g_game_cache_count].dnd_on_gaming, p,
                                     sizeof(g_game_cache[g_game_cache_count].dnd_on_gaming));
            else
                strcpy(g_game_cache[g_game_cache_count].dnd_on_gaming, "default");

            p = strstr(ptr, "\"app_priority\":");
            if (p && (!next_block || p < next_block))
                extract_string_value(g_game_cache[g_game_cache_count].app_priority, p,
                                     sizeof(g_game_cache[g_game_cache_count].app_priority));
            else
                strcpy(g_game_cache[g_game_cache_count].app_priority, "default");

            p = strstr(ptr, "\"game_preload\":");
            if (p && (!next_block || p < next_block))
                extract_string_value(g_game_cache[g_game_cache_count].game_preload, p,
                                     sizeof(g_game_cache[g_game_cache_count].game_preload));
            else
                strcpy(g_game_cache[g_game_cache_count].game_preload, "default");

            p = strstr(ptr, "\"refresh_rate\":");
            if (p && (!next_block || p < next_block))
                extract_string_value(g_game_cache[g_game_cache_count].refresh_rate, p,
                                     sizeof(g_game_cache[g_game_cache_count].refresh_rate));
            else
                strcpy(g_game_cache[g_game_cache_count].refresh_rate, "default");

            p = strstr(ptr, "\"renderer\":");
            if (p && (!next_block || p < next_block))
                extract_string_value(g_game_cache[g_game_cache_count].renderer, p, sizeof(g_game_cache[g_game_cache_count].renderer));
            else
                strcpy(g_game_cache[g_game_cache_count].renderer, "default");

            g_game_cache_count++;
        }
        ptr += 4;
    }
    free(buf);
    pthread_mutex_unlock(&cache_mutex);

    if (!ctx->is_initialize_complete) {
        log_zenith(LOG_INFO, "AppLoader: Gamelist in-memory cache loaded successfully. Total: %d games registered.", g_game_cache_count);
    }
}
