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

import android.content.Context
import android.os.BatteryManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import zx.azenith.R
import zx.azenith.ui.component.AppIconImage
import zx.azenith.ui.component.GamepadFocusRoot
import zx.azenith.ui.component.gamepadFocusOutline
import zx.azenith.ui.component.gamepadScreenEntry
import zx.azenith.ui.viewmodel.ApplistViewmodel

/** The rail's destinations. Only the library is wired to real data in the prototype. */
private enum class GameSpaceSection(val labelRes: Int, val icon: ImageVector) {
    Library(R.string.gamespace_rail_library, Icons.Rounded.GridView),
    Performance(R.string.gamespace_rail_perf, Icons.Rounded.Speed),
    Control(R.string.gamespace_rail_settings, Icons.Rounded.Tune)
}

/**
 * The Game Space shell: a landscape, gamepad-first board in the shape of a
 * big-picture launcher. Deliberately not a [GamepadFocusRoot] consumer of the app's
 * screen scaffold -- it owns the whole window, so the focus root lives here.
 */
@Composable
fun GameSpaceHub(
    profile: String,
    games: List<ApplistViewmodel.AppInfo>,
    onLaunch: (ApplistViewmodel.AppInfo) -> Unit,
    onExit: () -> Unit
) {
    var section by remember { mutableStateOf(GameSpaceSection.Library) }
    var focusedIndex by remember { mutableStateOf(0) }
    val scheme = MaterialTheme.colorScheme

    GamepadFocusRoot(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(scheme.surface, scheme.surfaceContainerLowest)
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopStrip(profile = profile)
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Rail(
                    selected = section,
                    onSelect = { section = it },
                    onExit = onExit
                )
                when (section) {
                    GameSpaceSection.Library -> LibraryPane(
                        games = games,
                        focusedIndex = focusedIndex,
                        onFocus = { focusedIndex = it },
                        onLaunch = onLaunch
                    )
                    GameSpaceSection.Performance -> PerformancePane(profile = profile)
                    GameSpaceSection.Control -> ControlPane()
                }
            }
            HintStrip()
        }
    }
}

/** Identity mark, live clock, battery and the profile the daemon is running. */
@Composable
private fun TopStrip(profile: String) {
    val scheme = MaterialTheme.colorScheme
    val clock = rememberClock()
    val battery = rememberBatteryState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_launcher_monochrome),
            contentDescription = null,
            tint = scheme.primary,
            modifier = Modifier.size(26.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.app_name).uppercase(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.sp
            ),
            color = scheme.onSurface
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.gamespace_mode_name).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 3.sp),
            color = scheme.primary
        )

        Spacer(Modifier.weight(1f))

        ProfileChip(profile = profile)
        Spacer(Modifier.width(18.dp))
        Text(
            text = clock,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
            color = scheme.onSurface
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = "${battery.first}%${if (battery.second) " ⚡" else ""}",
            style = MaterialTheme.typography.labelLarge,
            color = scheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileChip(profile: String) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = scheme.primaryContainer,
        contentColor = scheme.onPrimaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.gamespace_profile).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                color = scheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = profile.ifBlank { "—" },
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

/** Vertical section rail. The selected pill is the accent, so focus reads separately. */
@Composable
private fun Rail(
    selected: GameSpaceSection,
    onSelect: (GameSpaceSection) -> Unit,
    onExit: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(104.dp)
            .padding(start = 14.dp, top = 8.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        GameSpaceSection.entries.forEach { entry ->
            RailItem(
                labelRes = entry.labelRes,
                icon = entry.icon,
                selected = entry == selected,
                onClick = { onSelect(entry) }
            )
        }
        Spacer(Modifier.weight(1f))
        RailItem(
            labelRes = R.string.gamespace_rail_exit,
            icon = Icons.Rounded.PowerSettingsNew,
            selected = false,
            destructive = true,
            onClick = onExit
        )
    }
}

@Composable
private fun RailItem(
    labelRes: Int,
    icon: ImageVector,
    selected: Boolean,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    val container = when {
        selected -> scheme.primaryContainer
        else -> Color.Transparent
    }
    val content = when {
        selected -> scheme.onPrimaryContainer
        destructive -> scheme.error
        else -> scheme.onSurfaceVariant
    }

    var focused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .gamepadFocusOutline(shape = shape)
            .clickable(onClick = onClick)
            .background(color = container, shape = shape)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = content, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (focused) scheme.onSurface else content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/** Hero for the focused title, above the tile row it is driven by. */
@Composable
private fun LibraryPane(
    games: List<ApplistViewmodel.AppInfo>,
    focusedIndex: Int,
    onFocus: (Int) -> Unit,
    onLaunch: (ApplistViewmodel.AppInfo) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val current = games.getOrNull(focusedIndex.coerceIn(0, (games.size - 1).coerceAtLeast(0)))

    Column(modifier = Modifier.fillMaxSize().padding(end = 24.dp, top = 8.dp, bottom = 8.dp)) {
        if (current != null) {
            HeroBand(app = current, onLaunch = { onLaunch(current) })
            Spacer(Modifier.height(14.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.gamespace_library_title).uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 3.sp),
                color = scheme.onSurfaceVariant
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = games.size.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        Spacer(Modifier.height(8.dp))

        if (games.isEmpty()) {
            EmptyPane(
                icon = Icons.Rounded.GridView,
                titleRes = R.string.gamespace_no_games,
                bodyRes = R.string.gamespace_no_games_desc,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        } else {
            val listState = rememberLazyListState()
            LaunchedEffect(focusedIndex, games.size) {
                runCatching { listState.animateScrollToItem(focusedIndex.coerceAtLeast(0)) }
            }
            LazyRow(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().gamepadScreenEntry(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(end = 24.dp)
            ) {
                itemsIndexed(games, key = { _, app -> app.packageName }) { index, app ->
                    GameTile(
                        app = app,
                        focused = index == focusedIndex,
                        onFocus = { onFocus(index) },
                        onClick = { onLaunch(app) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroBand(app: ApplistViewmodel.AppInfo, onLaunch: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = scheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth().height(150.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            scheme.primary.copy(alpha = 0.16f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIconImage(app = app, size = 84.dp)
                Spacer(Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusChip(
                            text = stringResource(
                                if (app.isEnabledInConfig) R.string.gamespace_tuned
                                else R.string.gamespace_untuned
                            ),
                            accent = app.isEnabledInConfig
                        )
                        StatusChip(text = "uid ${app.uid}", accent = false)
                    }
                }
                Spacer(Modifier.width(18.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Button(onClick = onLaunch) {
                        Text(stringResource(R.string.gamespace_launch))
                    }
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = onLaunch) {
                        Text(
                            text = stringResource(R.string.gamespace_tune),
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, accent: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = if (accent) scheme.primaryContainer else scheme.surfaceContainerHighest,
        contentColor = if (accent) scheme.onPrimaryContainer else scheme.onSurfaceVariant
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            maxLines = 1
        )
    }
}

/** One title card. Focus scales it and swaps it to the accent container. */
@Composable
private fun GameTile(
    app: ApplistViewmodel.AppInfo,
    focused: Boolean,
    onFocus: () -> Unit,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large
    val motion = MaterialTheme.motionScheme
    // Kept as State so the focus scale resolves in the draw phase; only graphicsLayer reads it.
    val scale = animateFloatAsState(
        targetValue = if (focused) 1.05f else 1f,
        animationSpec = motion.defaultSpatialSpec(),
        label = "GameTileScale"
    )
    val container by androidx.compose.animation.animateColorAsState(
        targetValue = if (focused) scheme.primaryContainer else scheme.surfaceContainerHigh,
        animationSpec = motion.defaultEffectsSpec(),
        label = "GameTileContainer"
    )

    Surface(
        shape = shape,
        color = container,
        modifier = Modifier
            .width(196.dp)
            .height(132.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .onFocusChanged { if (it.isFocused) onFocus() }
            .gamepadFocusOutline(shape = shape, width = 3.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            AppIconImage(app = app, size = 44.dp)
            Spacer(Modifier.weight(1f))
            Text(
                text = app.label,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (focused) scheme.onPrimaryContainer else scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(
                    if (app.isEnabledInConfig) R.string.gamespace_tuned
                    else R.string.gamespace_untuned
                ).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = (if (focused) scheme.onPrimaryContainer else scheme.onSurfaceVariant).copy(alpha = 0.75f)
            )
        }
    }
}

/** Read-only snapshot of what the daemon is running, plus the switches to come. */
@Composable
private fun PerformancePane(profile: String) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxSize().padding(end = 24.dp, top = 8.dp, bottom = 8.dp)) {
        Text(
            text = stringResource(R.string.section_performance).uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 3.sp),
            color = scheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ProfileCard(R.string.Profile_Performance, R.drawable.ic_performance, active = false)
            ProfileCard(R.string.Profile_Balanced, R.drawable.ic_balanced, active = true)
            ProfileCard(R.string.Profile_ECO_mode, R.drawable.ic_eco, active = false)
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = profile.ifBlank { "—" },
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            color = scheme.onSurface
        )
        Text(
            text = stringResource(R.string.current_profile),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.gamespace_preview_only),
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ProfileCard(labelRes: Int, iconRes: Int, active: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (active) scheme.primaryContainer else scheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.width(148.dp).padding(vertical = 18.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = if (active) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.labelLarge,
                color = if (active) scheme.onPrimaryContainer else scheme.onSurface
            )
        }
    }
}

@Composable
private fun ControlPane() {
    Column(modifier = Modifier.fillMaxSize().padding(end = 24.dp, top = 8.dp, bottom = 8.dp)) {
        Text(
            text = stringResource(R.string.section_gamespace).uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 3.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        EmptyPane(
            icon = Icons.Rounded.Tune,
            titleRes = R.string.gamespace_section_soon,
            bodyRes = R.string.gamespace_preview_only,
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
    }
}

/** Big-picture footer: the controls the shell actually answers to. */
@Composable
private fun HintStrip() {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.gamespace_hint),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
            color = scheme.onSurfaceVariant.copy(alpha = 0.65f)
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(R.string.gamespace_mode_name).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
            color = scheme.primary.copy(alpha = 0.7f)
        )
    }
}

/** Tonal badge over a headline: the same resting state the experimental list uses. */
@Composable
internal fun EmptyPane(
    icon: ImageVector,
    titleRes: Int,
    bodyRes: Int,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = scheme.primaryContainer,
                contentColor = scheme.onPrimaryContainer,
                modifier = Modifier.size(84.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(38.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(bodyRes),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(300.dp)
            )
        }
    }
}

/** Ticks a session clock; a big-picture board is read at a glance, not to the second. */
@Composable
private fun rememberClock(): String {
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(10_000)
        }
    }
    return remember(now) { now.format(DateTimeFormatter.ofPattern("HH:mm")) }
}

/** Battery percentage and charging flag, re-read on the clock's cadence. */
@Composable
private fun rememberBatteryState(): Pair<Int, Boolean> {
    val context = LocalContext.current
    var state by remember { mutableStateOf(readBattery(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            state = readBattery(context)
            delay(30_000)
        }
    }
    return state
}

private fun readBattery(context: Context): Pair<Int, Boolean> {
    val manager = context.getSystemService(BatteryManager::class.java) ?: return 0 to false
    val level = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    val status = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
    return level to charging
}
