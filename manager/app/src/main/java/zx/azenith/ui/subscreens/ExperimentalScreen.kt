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

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package zx.azenith.ui.subscreens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import zx.azenith.R
import zx.azenith.ui.component.ExpressiveList
import zx.azenith.ui.component.ExpressiveListItem
import zx.azenith.ui.component.LeadingIcon
import zx.azenith.ui.component.ScaffoldContainerColor
import zx.azenith.ui.component.ScaffoldContentColor
import zx.azenith.ui.component.gamepadScreenEntry
import zx.azenith.ui.gamespace.ZXGameSpaceEntry
import zx.azenith.ui.navigation.safePopBackStack

/** What a row does when it is tapped: push a manager route, or hand the display to the shell. */
private sealed interface ExperimentalAction {
    data class Open(val route: String) : ExperimentalAction
    data object EnterGameSpace : ExperimentalAction
}

/** One row on the experimental screen; the list length is what decides empty vs populated. */
private data class ExperimentalFeature(
    val icon: ImageVector,
    val titleRes: Int,
    val summaryRes: Int,
    val action: ExperimentalAction
)

// Game Space leads it: the row is the way in, and its switches sit one row below.
private val EXPERIMENTAL_FEATURES: List<ExperimentalFeature> = listOf(
    ExperimentalFeature(
        icon = Icons.Rounded.RocketLaunch,
        titleRes = R.string.gamespace_mode_name,
        summaryRes = R.string.gamespace_desc,
        action = ExperimentalAction.EnterGameSpace
    ),
    ExperimentalFeature(
        icon = Icons.Rounded.SportsEsports,
        titleRes = R.string.gamespace_enable,
        summaryRes = R.string.gamespace_enable_desc,
        action = ExperimentalAction.Open("gamespacescreen")
    )
)

@Composable
fun ExperimentalScreen(navController: NavController) {
    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var enteringGameSpace by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                AboutTopAppBar(
                    title = stringResource(R.string.section_experimental),
                    onBack = { navController.safePopBackStack() },
                    scrollBehavior = scrollBehavior
                )
            },
            contentColor = ScaffoldContentColor(),
            containerColor = ScaffoldContainerColor(MaterialTheme.colorScheme.surface)
        ) { innerPadding ->
            if (EXPERIMENTAL_FEATURES.isEmpty()) {
                ExperimentalEmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .gamepadScreenEntry()
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().gamepadScreenEntry(),
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding() + 8.dp,
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    )
                ) {
                    item {
                        ExpressiveList(
                            content = EXPERIMENTAL_FEATURES.map { feature ->
                                {
                                    ExpressiveListItem(
                                        onClick = {
                                            when (val action = feature.action) {
                                                is ExperimentalAction.Open -> navController.navigate(action.route)
                                                ExperimentalAction.EnterGameSpace -> enteringGameSpace = true
                                            }
                                        },
                                        headlineContent = { Text(stringResource(feature.titleRes)) },
                                        supportingContent = { Text(stringResource(feature.summaryRes)) },
                                        leadingContent = { LeadingIcon(icon = feature.icon) },
                                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        // Last in the Box, so the cover lands over the screen the row was tapped on.
        ZXGameSpaceEntry(armed = enteringGameSpace, onLifted = { enteringGameSpace = false })
    }
}

/** Resting state while the list is empty: tonal icon badge over a headline and its explanation. */
@Composable
private fun ExperimentalEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 32.dp,
                    end = 32.dp,
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(112.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Science,
                        contentDescription = null,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.experimental_empty_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.experimental_empty_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 320.dp)
            )
        }
    }
}
