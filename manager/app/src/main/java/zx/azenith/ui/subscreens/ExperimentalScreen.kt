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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import zx.azenith.R
import zx.azenith.ui.component.ExpressiveList
import zx.azenith.ui.component.ExpressiveListItem
import zx.azenith.ui.component.ExpressiveSwitchItem
import zx.azenith.ui.component.LeadingIcon
import zx.azenith.ui.component.ScaffoldContainerColor
import zx.azenith.ui.component.ScaffoldContentColor
import zx.azenith.ui.component.gamepadScreenEntry
import zx.azenith.ui.gamespace.ZXGameSpaceEntry
import zx.azenith.ui.gamespace.isGameSpaceLauncherEnabled
import zx.azenith.ui.gamespace.setGameSpaceLauncherEnabled
import zx.azenith.ui.navigation.safePopBackStack

/**
 * Game Space is the one experiment, and it has no page of its own: the mode switch and the way in
 * both sit here. The switch makes every launch open Game Space; the row enters it on demand,
 * which is what a switch that is off leaves you needing.
 */
@Composable
fun ExperimentalScreen(navController: NavController) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var launcherOn by remember { mutableStateOf(isGameSpaceLauncherEnabled(context)) }
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
            LazyColumn(
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
                        content = listOf(
                            {
                                ExpressiveSwitchItem(
                                    icon = Icons.Rounded.SportsEsports,
                                    title = stringResource(R.string.gamespace_enable),
                                    summary = stringResource(R.string.gamespace_enable_desc),
                                    checked = launcherOn,
                                    onCheckedChange = { checked ->
                                        launcherOn = checked
                                        setGameSpaceLauncherEnabled(context, checked)
                                    }
                                )
                            },
                            {
                                ExpressiveListItem(
                                    onClick = { enteringGameSpace = true },
                                    headlineContent = { Text(stringResource(R.string.gamespace_enter)) },
                                    leadingContent = { LeadingIcon(icon = Icons.Rounded.RocketLaunch) },
                                    trailingContent = { Icon(Icons.Rounded.OpenInNew, null) }
                                )
                            }
                        )
                    )
                }
            }
        }

        // Last in the Box, so the cover lands over the screen the row was tapped on.
        ZXGameSpaceEntry(armed = enteringGameSpace, onLifted = { enteringGameSpace = false })
    }
}
