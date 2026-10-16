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

package zx.azenith.ui.subscreens

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.RocketLaunch
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import zx.azenith.R
import zx.azenith.ui.component.ExpressiveList
import zx.azenith.ui.component.ExpressiveListItem
import zx.azenith.ui.component.ExpressiveSwitchItem
import zx.azenith.ui.component.LeadingIcon
import zx.azenith.ui.component.ScaffoldContainerColor
import zx.azenith.ui.component.ScaffoldContentColor
import zx.azenith.ui.component.gamepadScreenEntry
import zx.azenith.ui.gamespace.ZXGameSpaceActivity
import zx.azenith.ui.gamespace.isGameSpaceEnabled
import zx.azenith.ui.gamespace.setGameSpaceEnabled
import zx.azenith.ui.navigation.safePopBackStack

/**
 * Settings entry for Game Space: the mode switch, a manual way in, and what the shell currently
 * holds. Everything the shell can do is still the same handful of screens, so this menu is the
 * place that describes them until the control centre lands.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ZXGameSpaceScreen(navController: NavController) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme

    var modeOn by remember { mutableStateOf(isGameSpaceEnabled(context)) }
    var entering by remember { mutableStateOf(false) }
    val fade = remember { Animatable(0f) }

    // Manual entry: the same black-out the mode does on start, minus the mode.
    LaunchedEffect(entering) {
        if (!entering) return@LaunchedEffect
        fade.animateTo(1f, motion.defaultEffectsSpec())
        context.startActivity(Intent(context, ZXGameSpaceActivity::class.java))
        delay(320)
        fade.animateTo(0f, motion.defaultEffectsSpec())
        entering = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                AboutTopAppBar(
                    title = stringResource(R.string.section_gamespace),
                    onBack = { navController.safePopBackStack() },
                    scrollBehavior = scrollBehavior
                )
            },
            contentColor = ScaffoldContentColor(),
            containerColor = ScaffoldContainerColor(scheme.surface)
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().gamepadScreenEntry(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item { ZXGameSpaceHeader() }

                item {
                    ExpressiveList(
                        content = listOf(
                            {
                                ExpressiveSwitchItem(
                                    icon = Icons.Rounded.SportsEsports,
                                    title = stringResource(R.string.gamespace_enable),
                                    summary = stringResource(R.string.gamespace_enable_desc),
                                    checked = modeOn,
                                    onCheckedChange = { checked ->
                                        modeOn = checked
                                        setGameSpaceEnabled(context, checked)
                                    }
                                )
                            },
                            {
                                ExpressiveListItem(
                                    onClick = { entering = true },
                                    headlineContent = { Text(stringResource(R.string.gamespace_enter)) },
                                    supportingContent = { Text(stringResource(R.string.gamespace_enter_desc)) },
                                    leadingContent = { LeadingIcon(Icons.Rounded.RocketLaunch) },
                                    trailingContent = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = scheme.onSurfaceVariant
                                        )
                                    }
                                )
                            }
                        )
                    )
                }

                item {
                    ExpressiveList(
                        title = stringResource(R.string.gamespace_inside),
                        content = listOf(
                            {
                                InsideRow(
                                    title = stringResource(R.string.gamespace_rail_library),
                                    summary = stringResource(R.string.gamespace_inside_library)
                                )
                            },
                            {
                                InsideRow(
                                    title = stringResource(R.string.gamespace_rail_perf),
                                    summary = stringResource(R.string.gamespace_inside_perf)
                                )
                            },
                            {
                                InsideRow(
                                    title = stringResource(R.string.gamespace_rail_settings),
                                    summary = stringResource(R.string.gamespace_inside_control)
                                )
                            }
                        )
                    )
                }

                item {
                    Text(
                        text = stringResource(R.string.gamespace_preview_only),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .graphicsLayer { alpha = fade.value }
        )
    }
}

@Composable
private fun ZXGameSpaceHeader() {
    val scheme = MaterialTheme.colorScheme

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = scheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = scheme.primaryContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_monochrome),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(scheme.onPrimaryContainer),
                        modifier = Modifier.size(34.dp).clip(CircleShape)
                    )
                }
            }
            Spacer(Modifier.size(18.dp))
            Column {
                Text(
                    text = stringResource(R.string.gamespace_mode_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.gamespace_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InsideRow(title: String, summary: String) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = scheme.onSurface
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant
        )
    }
}
