/*
 * Copyright (C) 2026-2027 KowX
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

package zx.azenith.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import zx.azenith.R

/** What the floating theme bar is currently showing. */
enum class ThemeBarPhase { Idle, Applying, Applied }

/**
 * Centered confirmation box shown while a saved theme is being applied.
 *
 * The bar itself only carries a 20dp action button, which is too small to read
 * as progress. This puts the same morphing [LoadingIndicator] at a size you can
 * actually see, then swaps it for a check that draws itself before the box
 * leaves.
 *
 * It does not block touches: the theme is already committed by the time this
 * appears, and there is nothing to cancel.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThemeAppliedOverlay(
    phase: ThemeBarPhase,
    modifier: Modifier = Modifier
) {
    if (phase == ThemeBarPhase.Idle) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(140)) + scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                initialScale = 0.7f
            ),
            exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.85f)
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp
            ) {
                Box(
                    modifier = Modifier.size(132.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = phase,
                        transitionSpec = {
                            (fadeIn(tween(200)) + scaleIn(tween(260), initialScale = 0.4f))
                                .togetherWith(
                                    fadeOut(tween(140)) + scaleOut(tween(180), targetScale = 0.5f)
                                )
                        },
                        label = "ThemeAppliedPhase"
                    ) { state ->
                        when (state) {
                            ThemeBarPhase.Applying -> LoadingIndicator(
                                modifier = Modifier.size(56.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            // A plain tick pops in rather than reading as a
                            // state change the user has to interpret.
                            ThemeBarPhase.Applied -> Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = stringResource(R.string.theme_applied),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(56.dp)
                            )
                            ThemeBarPhase.Idle -> Unit
                        }
                    }
                }
            }
        }
    }
}

/**
 * Floating confirmation bar for theme edits.
 *
 * Theme choices are staged rather than written straight to
 * SharedPreferences, so the screen can show the pending result and the user
 * can back out of it. The bar is the only place that commits.
 *
 * Phase motion:
 *  - [ThemeBarPhase.Applying] and [ThemeBarPhase.Applied] disable Save. The
 *    progress itself is reported by [ThemeAppliedOverlay], which has room for
 *    an indicator that is actually readable; the bar stays a Save/Discard
 *    control and does not resize while the phase changes.
 *
 * The bar is always composed; only its visibility is animated, so the enter
 * and exit transitions have real content to grow and shrink.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThemeChangeBar(
    visible: Boolean,
    phase: ThemeBarPhase,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(160)) + expandVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ),
        exit = fadeOut(animationSpec = tween(140)) + shrinkVertically(
            animationSpec = tween(220)
        ),
        modifier = modifier
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 3.dp,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(onClick = onDiscard) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.theme_discard),
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }

                Button(
                    onClick = onSave,
                    // Disabled for the length of the apply animation so the
                    // write cannot be fired twice; the centered box is what the
                    // user watches during that time.
                    enabled = phase == ThemeBarPhase.Idle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    // The spinner and the tick both live in the centered box,
                    // where they are large enough to read. The button keeps a
                    // stable width so the bar does not resize mid-animation.
                    Text(
                        text = stringResource(
                            if (phase == ThemeBarPhase.Idle) R.string.theme_save
                            else R.string.theme_apply_action
                        )
                    )
                }
            }
        }
    }
}
