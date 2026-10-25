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

package zx.azenith.ui.gamespace

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import zx.azenith.R
import zx.azenith.ui.util.RootUtils
import zx.azenith.ui.viewmodel.ApplistViewmodel

/**
 * Game Space in one place: the boot intro hands off to the hub, and both sit on the same scheme
 * surface the manager fades through, so there is no seam between the two.
 *
 * The install side of the mode (the manager covering the screen and starting this) lives in
 * `ui/subscreens/ExperimentalScreen.kt`; this composable only owns the shell itself.
 */
@Composable
fun ZXGameSpaceShell(
    onExit: () -> Unit,
    onExitApp: () -> Unit = {}
) {
    val context = LocalContext.current
    val motion = MaterialTheme.motionScheme
    val applist: ApplistViewmodel = viewModel()
    val scope = rememberCoroutineScope()

    var introActive by remember { mutableStateOf(true) }
    var leaving by remember { mutableStateOf(false) }
    val hubFade = remember { Animatable(0f) }
    val hubScale = remember { Animatable(0.95f) }
    val exitFade = remember { Animatable(0f) }

    LaunchedEffect(Unit) { applist.loadApps(context) }

    // The list is a companion-level snapshot state, so it re-reads itself when the load lands.
    val installed = ApplistViewmodel.apps
    val games = remember(installed) {
        installed
            .filter { it.isRecommended || it.isEnabledInConfig }
            .sortedWith(
                compareByDescending<ApplistViewmodel.AppInfo> { it.isEnabledInConfig }
                    .thenBy { it.label.lowercase() }
            )
    }
    val userApps = remember(installed) {
        installed
            .filter { !it.isSystem }
            .sortedWith(
                compareByDescending<ApplistViewmodel.AppInfo> { it.isEnabledInConfig }
                    .thenByDescending { it.isRecommended }
                    .thenBy { it.label.lowercase() }
            )
    }

    val profileRes by RootUtils.observeProfileRes().collectAsState(initial = R.string.status_unknown)
    val profileValue by RootUtils.observeProfileValue().collectAsState(initial = "")
    val profile = stringResource(profileRes)

    LaunchedEffect(leaving) {
        if (!leaving) return@LaunchedEffect
        exitFade.animateTo(1f, motion.defaultEffectsSpec())
        onExit()
    }

    // Back skips the intro, then hub handles back navigation and confirmation dialogue.
    BackHandler(enabled = introActive) {
        introActive = false
        scope.launch {
            hubFade.snapTo(1f)
            hubScale.snapTo(1f)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        // The Hub sits underneath, smoothly scaling and fading into view as the Intro blooms and dissolves.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = hubFade.value
                    scaleX = hubScale.value
                    scaleY = hubScale.value
                }
        ) {
            ZXGameSpaceHub(
                introActive = introActive,
                profile = profile,
                profileValue = profileValue,
                games = games,
                allApps = userApps,
                onApplyProfile = { id ->
                    com.topjohnwu.superuser.Shell.cmd(
                        "/data/adb/modules/AZenith/system/bin/sys.azenith-service -p $id"
                    ).submit()
                },
                onLaunch = { app -> launchGame(context, app.packageName) },
                onExit = { leaving = true },
                onExitApp = onExitApp,
                loading = applist.isRefreshing
            )
        }

        if (introActive) {
            ZXGameSpaceIntro(
                onTransitionStart = {
                    scope.launch {
                        launch {
                            hubFade.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
                            )
                        }
                        launch {
                            hubScale.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        }
                    }
                },
                onFinished = {
                    introActive = false
                    scope.launch {
                        hubFade.snapTo(1f)
                        hubScale.snapTo(1f)
                    }
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = exitFade.value }
                .background(MaterialTheme.colorScheme.surface)
        )
    }
}

/**
 * Hands the game to the launcher and lets it take the foreground: the shell is a front end, so it
 * stays where it is rather than tearing itself down behind the game.
 */
internal fun launchGame(context: android.content.Context, packageName: String) {
    zx.azenith.ui.gamespace.ZXSidePanelService.registerAppOpenedFromAzenith(packageName)
    zx.azenith.ui.gamespace.ZXSidePanelService.show(context, packageName)
    val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
