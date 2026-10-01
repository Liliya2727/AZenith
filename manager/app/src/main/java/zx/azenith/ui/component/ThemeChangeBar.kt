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
 * Floating confirmation bar for theme edits.
 *
 * Theme choices are staged rather than written straight to
 * SharedPreferences, so the screen can show the pending result and the user
 * can back out of it. The bar is the only place that commits.
 *
 * Phase motion:
 *  - [ThemeBarPhase.Applying] shows the same morphing
 *    [LoadingIndicator] the rest of the app uses while the write and the
 *    resulting recomposition land.
 *  - [ThemeBarPhase.Applied] swaps the spinner for a check that scales in,
 *    holds, then the bar dismisses itself via [appliedAutoDismissMs].
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
    modifier: Modifier = Modifier,
    appliedAutoDismissMs: Long = 900L
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
                    enabled = phase == ThemeBarPhase.Idle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    // One slot, three states. The content key is the phase, so
                    // each state gets its own transition instead of one
                    // animation trying to serve all three.
                    AnimatedContent(
                        targetState = phase,
                        transitionSpec = {
                            (fadeIn(tween(180)) + scaleIn(tween(220), initialScale = 0.7f))
                                .togetherWith(
                                    fadeOut(tween(120)) + scaleOut(tween(160), targetScale = 0.7f)
                                )
                        },
                        label = "ThemeBarAction"
                    ) { state ->
                        when (state) {
                            ThemeBarPhase.Idle -> Text(stringResource(R.string.theme_save))
                            ThemeBarPhase.Applying -> LoadingIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            ThemeBarPhase.Applied -> Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = stringResource(R.string.theme_applied)
                            )
                        }
                    }
                }
            }
        }
    }
}
