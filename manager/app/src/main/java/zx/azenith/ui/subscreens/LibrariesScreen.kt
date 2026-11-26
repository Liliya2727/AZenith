/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalMaterial3Api::class)

package zx.azenith.ui.subscreens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import zx.azenith.R
import zx.azenith.ui.component.*
import zx.azenith.ui.navigation.safePopBackStack
import zx.azenith.ui.theme.azShape

@Composable
fun LibrariesScreen(navController: NavController) {
    val context = LocalContext.current
    val libraries by produceLibraries(R.raw.aboutlibraries)
    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    var selectedLibrary by remember { mutableStateOf<Library?>(null) }

    RegisterGamepadActions(
        enabled = true,
        onBack = {
            navController.safePopBackStack()
            true
        },
        scrollState = listState,
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AboutTopAppBar(
                title = stringResource(R.string.str_libraries_used),
                onBack = { navController.safePopBackStack() },
                scrollBehavior = scrollBehavior
            )
        },
        contentColor = ScaffoldContentColor(),
        containerColor = ScaffoldContainerColor(MaterialTheme.colorScheme.surface)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazePageSource()
                .gamepadScreenEntry(scopeKey = "subscreen:libraries")
        ) {
            val libraryList = libraries?.libraries
            if (libraryList == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                ExpressiveLazyList(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = innerPadding.calculateTopPadding() + 8.dp,
                        bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    ),
                    key = { it.uniqueId },
                    items = libraryList,
                    itemContent = { library ->
                        val author = remember(library) {
                            library.developers
                                .mapNotNull { it.name?.takeIf(String::isNotBlank) }
                                .joinToString(", ")
                                .ifBlank { library.organization?.name.orEmpty() }
                        }
                        val licenseNames = remember(library) {
                            library.licenses
                                .mapNotNull { it.name.takeIf(String::isNotBlank) }
                                .joinToString(", ")
                        }
                        val url = remember(library) {
                            library.website?.takeIf(String::isNotBlank)
                                ?: library.licenses.firstOrNull()?.url?.takeIf(String::isNotBlank)
                        }
                        val hasLicenseText = remember(library) {
                            library.licenses.any { !it.licenseContent.isNullOrBlank() }
                        }

                        ExpressiveListItem(
                            onClick = if (hasLicenseText || url != null) {
                                {
                                    if (hasLicenseText) {
                                        selectedLibrary = library
                                    } else if (url != null) {
                                        runCatching {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        }
                                    }
                                }
                            } else null,
                            headlineContent = {
                                Text(
                                    text = library.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            supportingContent = if (author.isNotBlank() || licenseNames.isNotBlank()) {
                                {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        if (author.isNotBlank()) {
                                            Text(
                                                text = author,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (licenseNames.isNotBlank()) {
                                            Text(
                                                text = licenseNames,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            } else null,
                            trailingContent = library.artifactVersion?.takeIf(String::isNotBlank)?.let { version ->
                                {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = azShape(12.dp)
                                    ) {
                                        Text(
                                            text = version,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        )
                    }
                )
            }
        }

        selectedLibrary?.let { library ->
            val licenseText = remember(library) {
                library.licenses
                    .mapNotNull { it.licenseContent?.takeIf(String::isNotBlank) }
                    .joinToString("\n\n")
            }
            val url = remember(library) {
                library.website?.takeIf(String::isNotBlank)
                    ?: library.licenses.firstOrNull()?.url?.takeIf(String::isNotBlank)
            }
            AlertDialog(
                onDismissRequest = { selectedLibrary = null },
                title = {
                    Text(
                        text = library.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = licenseText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                dismissButton = if (url != null) {
                    {
                        TextButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            }
                        ) {
                            Text(stringResource(R.string.str_view_full_license))
                        }
                    }
                } else null,
                confirmButton = {
                    TextButton(onClick = { selectedLibrary = null }) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            )
        }
    }
}
