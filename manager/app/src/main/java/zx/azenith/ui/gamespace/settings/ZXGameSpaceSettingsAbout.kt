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

package zx.azenith.ui.gamespace.settings

import android.util.Log
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import zx.azenith.BuildConfig
import zx.azenith.R
import zx.azenith.ui.component.GamepadInput
import zx.azenith.ui.component.LocalGamepadFocusEnabled
import zx.azenith.ui.component.RegisterGamepadActions
import zx.azenith.ui.component.gamepadFocusOutline
import zx.azenith.ui.component.gamepadKeyWatch
import zx.azenith.ui.gamespace.BindGameSpaceDialogGamepad
import zx.azenith.ui.gamespace.GameSpaceInputSource
import zx.azenith.ui.gamespace.GameSpaceOverlayState
import zx.azenith.ui.gamespace.SettingsActionCard
import zx.azenith.ui.gamespace.springPressBounce
import zx.azenith.ui.util.AudioUtil
import zx.azenith.ui.util.tapHaptic

@Composable
internal fun ZXGameSpaceSettingsAbout(
    deviceName: String = "",
    chipsetName: String = "",
    modifier: Modifier = Modifier
) {
    SettingsAboutPane(
        deviceName = deviceName,
        chipsetName = chipsetName,
        modifier = modifier
    )
}

@Composable
internal fun SettingsAboutPane(
    deviceName: String = "",
    chipsetName: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()
    zx.azenith.ui.component.RegisterGamepadActions(columnScrollState = scrollState)

    var showLicensesDialog by remember { mutableStateOf(false) }
    var showLibrariesDialog by remember { mutableStateOf(false) }
    var showChangelogDialog by remember { mutableStateOf(false) }
    var changelogText by remember { mutableStateOf("") }

    val changelogRequester = remember { FocusRequester() }
    val licensesRequester = remember { FocusRequester() }
    val librariesRequester = remember { FocusRequester() }
    var lastDialogOriginRequester by remember { mutableStateOf<FocusRequester?>(null) }
    var wasAnyDialogOpen by remember { mutableStateOf(false) }
    val anyDialogOpen = showLicensesDialog || showLibrariesDialog || showChangelogDialog

    LaunchedEffect(anyDialogOpen) {
        if (anyDialogOpen) {
            wasAnyDialogOpen = true
        } else if (wasAnyDialogOpen) {
            wasAnyDialogOpen = false
            val target = lastDialogOriginRequester
            val useGamepad = GameSpaceInputSource.isGamepadLast ||
                GamepadInput.dpadMode ||
                GamepadInput.controllerConnected
            if (target != null && useGamepad) {
                GamepadInput.onDpadKey()
                for (attempt in 1..12) {
                    withFrameNanos { }
                    if (runCatching { target.requestFocus() }.getOrDefault(false)) {
                        Log.d("PAD", "gs about restoreDialogOrigin attempt=$attempt")
                        break
                    }
                }
            }
        }
    }

    LaunchedEffect(showChangelogDialog) {
        if (showChangelogDialog && changelogText.isEmpty()) {
            withContext(Dispatchers.IO) {
                try {
                    changelogText = context.assets.open("changelog.md").bufferedReader().use { it.readText() }
                } catch (e: Exception) {
                    changelogText = context.getString(R.string.err_failed_load_changelog) + "\n${e.message}"
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.section_about),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = scheme.onSurface
            )
            Text(
                text = "Project information, credits, open-source licenses, and community links",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }

        // 1. Hero App Info & Community Links Bento Card
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Image(
                        painter = rememberDrawablePainter(
                            context.applicationInfo.loadIcon(context.packageManager)
                        ),
                        contentDescription = stringResource(R.string.app_name),
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(scheme.surfaceVariant)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                            color = scheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.str_version_buildconfig_version_na, BuildConfig.VERSION_NAME),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = scheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (deviceName.isNotBlank() || chipsetName.isNotBlank()) {
                            Text(
                                text = listOf(deviceName, chipsetName).filter { it.isNotBlank() }.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val btnShape = RoundedCornerShape(16.dp)
                    val openChangelog = {
                        AudioUtil.playOk(context)
                        lastDialogOriginRequester = changelogRequester
                        showChangelogDialog = true
                    }
                    val openSupportGroup = {
                        AudioUtil.playOk(context)
                        uriHandler.openUri("https://t.me/ArchHavenDisc")
                    }
                    val openUpdateChannel = {
                        AudioUtil.playOk(context)
                        uriHandler.openUri("https://t.me/ZeshArch")
                    }
                    if (maxWidth >= 600.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = btnShape,
                                color = scheme.primaryContainer,
                                contentColor = scheme.onPrimaryContainer,
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 44.dp)
                                    .focusRequester(changelogRequester)
                                    .springPressBounce()
                                    .gamepadFocusOutline(
                                        shape = btnShape,
                                        register = true,
                                        onActivate = openChangelog
                                    )
                                    .clip(btnShape)
                                    .focusProperties { canFocus = false }
                                    .clickable(onClick = openChangelog)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Notes,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.str_changelog),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Surface(
                                shape = btnShape,
                                color = scheme.surfaceContainerHighest,
                                contentColor = scheme.onSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 44.dp)
                                    .springPressBounce()
                                    .gamepadFocusOutline(
                                        shape = btnShape,
                                        register = true,
                                        onActivate = openSupportGroup
                                    )
                                    .clip(btnShape)
                                    .focusProperties { canFocus = false }
                                    .clickable(onClick = openSupportGroup)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_telegram),
                                        contentDescription = null,
                                        tint = scheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.str_support_group),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Surface(
                                shape = btnShape,
                                color = scheme.surfaceContainerHighest,
                                contentColor = scheme.onSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 44.dp)
                                    .springPressBounce()
                                    .gamepadFocusOutline(
                                        shape = btnShape,
                                        register = true,
                                        onActivate = openUpdateChannel
                                    )
                                    .clip(btnShape)
                                    .focusProperties { canFocus = false }
                                    .clickable(onClick = openUpdateChannel)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_telegram),
                                        contentDescription = null,
                                        tint = scheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.str_update_channel),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = btnShape,
                                color = scheme.primaryContainer,
                                contentColor = scheme.onPrimaryContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 44.dp)
                                    .focusRequester(changelogRequester)
                                    .springPressBounce()
                                    .gamepadFocusOutline(
                                        shape = btnShape,
                                        register = true,
                                        onActivate = openChangelog
                                    )
                                    .clip(btnShape)
                                    .focusProperties { canFocus = false }
                                    .clickable(onClick = openChangelog)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Notes,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.str_changelog),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = btnShape,
                                    color = scheme.surfaceContainerHighest,
                                    contentColor = scheme.onSurface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 42.dp)
                                        .springPressBounce()
                                        .gamepadFocusOutline(
                                            shape = btnShape,
                                            register = true,
                                            onActivate = openSupportGroup
                                        )
                                        .clip(btnShape)
                                        .focusProperties { canFocus = false }
                                        .clickable(onClick = openSupportGroup)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_telegram),
                                            contentDescription = null,
                                            tint = scheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.str_support_group),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Surface(
                                    shape = btnShape,
                                    color = scheme.surfaceContainerHighest,
                                    contentColor = scheme.onSurface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 42.dp)
                                        .springPressBounce()
                                        .gamepadFocusOutline(
                                            shape = btnShape,
                                            register = true,
                                            onActivate = openUpdateChannel
                                        )
                                        .clip(btnShape)
                                        .focusProperties { canFocus = false }
                                        .clickable(onClick = openUpdateChannel)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_telegram),
                                            contentDescription = null,
                                            tint = scheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.str_update_channel),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Adaptive Content Split (Stacked on < 800.dp, Two-Column on >= 800.dp)
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth >= 800.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Left Column: Open Source (1 header + 3 cards)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.str_open_source).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            ),
                            color = scheme.primary,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        SettingsActionCard(
                            icon = Icons.Rounded.Gavel,
                            title = stringResource(R.string.str_licenses),
                            summary = stringResource(R.string.str_apache_license_2),
                            trailingBadge = "Apache 2.0",
                            focusRequester = licensesRequester,
                            onClick = {
                                lastDialogOriginRequester = licensesRequester
                                showLicensesDialog = true
                            }
                        )

                        SettingsActionCard(
                            icon = Icons.AutoMirrored.Rounded.LibraryBooks,
                            title = stringResource(R.string.str_libraries_used),
                            summary = stringResource(R.string.str_libraries_hint),
                            focusRequester = librariesRequester,
                            onClick = {
                                lastDialogOriginRequester = librariesRequester
                                showLibrariesDialog = true
                            }
                        )

                        SettingsActionCard(
                            icon = Icons.Rounded.Code,
                            title = stringResource(R.string.str_source_code),
                            summary = stringResource(R.string.str_view_the_source_code_on_github),
                            onClick = { uriHandler.openUri("https://github.com/Liliya2727/AZenith") }
                        )
                    }

                    // Right Column: Maintainer & Collaborators (1 header + 3 cards - perfectly matching left)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "${stringResource(R.string.str_maintainer)} & ${stringResource(R.string.str_collaborators)}".uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            ),
                            color = scheme.primary,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        DeveloperCreditCard(
                            avatarRes = R.drawable.avatar_liliya,
                            name = stringResource(R.string.str_liliya),
                            role = stringResource(R.string.str_creator_maintainer),
                            githubUrl = "https://github.com/Liliya2727",
                            telegramUrl = "https://t.me/Zexshia",
                            onOpenUrl = { uriHandler.openUri(it) }
                        )

                        DeveloperCreditCard(
                            avatarRes = R.drawable.avatar_xia,
                            name = stringResource(R.string.str_rianixia),
                            role = stringResource(R.string.str_co_maintainer),
                            githubUrl = "https://github.com/ryanistr",
                            telegramUrl = "https://t.me/rianixia",
                            onOpenUrl = { uriHandler.openUri(it) }
                        )

                        DeveloperCreditCard(
                            avatarRes = R.drawable.avatar_kanao,
                            name = stringResource(R.string.str_kanaochar),
                            role = stringResource(R.string.str_co_maintainer),
                            githubUrl = "https://github.com/kanaodnd",
                            telegramUrl = "https://t.me/kanaochar",
                            onOpenUrl = { uriHandler.openUri(it) }
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Open Source Section (1 header + 3 cards)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.str_open_source).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            ),
                            color = scheme.primary,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        SettingsActionCard(
                            icon = Icons.Rounded.Gavel,
                            title = stringResource(R.string.str_licenses),
                            summary = stringResource(R.string.str_apache_license_2),
                            trailingBadge = "Apache 2.0",
                            focusRequester = licensesRequester,
                            onClick = {
                                lastDialogOriginRequester = licensesRequester
                                showLicensesDialog = true
                            }
                        )

                        SettingsActionCard(
                            icon = Icons.AutoMirrored.Rounded.LibraryBooks,
                            title = stringResource(R.string.str_libraries_used),
                            summary = stringResource(R.string.str_libraries_hint),
                            focusRequester = librariesRequester,
                            onClick = {
                                lastDialogOriginRequester = librariesRequester
                                showLibrariesDialog = true
                            }
                        )

                        SettingsActionCard(
                            icon = Icons.Rounded.Code,
                            title = stringResource(R.string.str_source_code),
                            summary = stringResource(R.string.str_view_the_source_code_on_github),
                            onClick = { uriHandler.openUri("https://github.com/Liliya2727/AZenith") }
                        )
                    }

                    // Maintainer & Collaborators Section (1 header + 3 cards)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "${stringResource(R.string.str_maintainer)} & ${stringResource(R.string.str_collaborators)}".uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            ),
                            color = scheme.primary,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        DeveloperCreditCard(
                            avatarRes = R.drawable.avatar_liliya,
                            name = stringResource(R.string.str_liliya),
                            role = stringResource(R.string.str_creator_maintainer),
                            githubUrl = "https://github.com/Liliya2727",
                            telegramUrl = "https://t.me/Zexshia",
                            onOpenUrl = { uriHandler.openUri(it) }
                        )

                        DeveloperCreditCard(
                            avatarRes = R.drawable.avatar_xia,
                            name = stringResource(R.string.str_rianixia),
                            role = stringResource(R.string.str_co_maintainer),
                            githubUrl = "https://github.com/ryanistr",
                            telegramUrl = "https://t.me/rianixia",
                            onOpenUrl = { uriHandler.openUri(it) }
                        )

                        DeveloperCreditCard(
                            avatarRes = R.drawable.avatar_kanao,
                            name = stringResource(R.string.str_kanaochar),
                            role = stringResource(R.string.str_co_maintainer),
                            githubUrl = "https://github.com/kanaodnd",
                            telegramUrl = "https://t.me/kanaochar",
                            onOpenUrl = { uriHandler.openUri(it) }
                        )
                    }
                }
            }
        }
    }

    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 720

    if (showLicensesDialog) {
        val licensesToken = remember { Any() }
        val licensesFirstRequester = remember { FocusRequester() }
        val licensesScrollState = rememberScrollState()
        DisposableEffect(licensesToken) {
            GameSpaceOverlayState.acquire(licensesToken, "AboutLicensesDialog")
            onDispose {
                GameSpaceOverlayState.release(licensesToken)
            }
        }
        val dismissLicenses = {
            Log.d("PAD", "gs about dismiss LicensesDialog")
            AudioUtil.playCancel(context)
            showLicensesDialog = false
        }
        Dialog(
            onDismissRequest = dismissLicenses,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            BindGameSpaceDialogGamepad()
            val dialogScope = rememberCoroutineScope()
            val dialogFocusManager = LocalFocusManager.current
            val dialogDensity = LocalDensity.current
            val stepPx = with(dialogDensity) { 120.dp.toPx() }
            var firstAttached by remember { mutableStateOf(false) }

            BackHandler(enabled = true, onBack = dismissLicenses)

            LaunchedEffect(Unit) {
                if (GameSpaceInputSource.isGamepadLast || GamepadInput.dpadMode || GamepadInput.controllerConnected) {
                    GamepadInput.onDpadKey()
                }
                withTimeoutOrNull(600L) {
                    snapshotFlow { firstAttached }.first { it }
                }
                var seeded = false
                for (attempt in 1..12) {
                    withFrameNanos { }
                    seeded = runCatching { licensesFirstRequester.requestFocus() }.getOrDefault(false)
                    if (seeded) break
                }
                Log.d("PAD", "gs about seed LicensesDialog attached=$firstAttached seeded=$seeded")
            }

            CompositionLocalProvider(LocalGamepadFocusEnabled provides true) {
                RegisterGamepadActions(
                    enabled = true,
                    columnScrollState = licensesScrollState,
                    onCloseOverlay = {
                        dismissLicenses()
                        true
                    }
                )
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = scheme.surfaceContainerHigh,
                    contentColor = scheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth(if (isWideScreen) 0.72f else 0.92f)
                        .padding(16.dp)
                        .tapHaptic()
                        .gamepadKeyWatch()
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (event.nativeKeyEvent.keyCode) {
                                KeyEvent.KEYCODE_BUTTON_B,
                                KeyEvent.KEYCODE_ESCAPE -> {
                                    dismissLicenses()
                                    true
                                }
                                KeyEvent.KEYCODE_DPAD_DOWN -> {
                                    val moved = dialogFocusManager.moveFocus(FocusDirection.Down)
                                    if (!moved && licensesScrollState.value < licensesScrollState.maxValue) {
                                        dialogScope.launch { licensesScrollState.animateScrollBy(stepPx) }
                                    }
                                    true
                                }
                                KeyEvent.KEYCODE_DPAD_UP -> {
                                    val moved = dialogFocusManager.moveFocus(FocusDirection.Up)
                                    if (!moved && licensesScrollState.value > 0) {
                                        dialogScope.launch { licensesScrollState.animateScrollBy(-stepPx) }
                                    }
                                    true
                                }
                                else -> false
                            }
                        }
                        .focusProperties {
                            canFocus = false
                            onExit = { cancelFocusChange() }
                        }
                        .focusGroup()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(22.dp)
                            .verticalScroll(licensesScrollState),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = scheme.primaryContainer,
                                    contentColor = scheme.onPrimaryContainer,
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Gavel,
                                            contentDescription = null,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = stringResource(R.string.str_apache_license_2),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = scheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.str_license_copyright),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = scheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = dismissLicenses,
                                modifier = Modifier.focusProperties { canFocus = false }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = null,
                                    tint = scheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.str_project_license_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val openFullLicense = {
                                AudioUtil.playOk(context)
                                uriHandler.openUri("https://www.apache.org/licenses/LICENSE-2.0")
                            }
                            val openGithubLicense = {
                                AudioUtil.playOk(context)
                                uriHandler.openUri("https://github.com/Liliya2727/AZenith/blob/main/LICENSE")
                            }
                            FilledTonalButton(
                                onClick = openFullLicense,
                                modifier = Modifier
                                    .weight(1f)
                                    .onGloballyPositioned { firstAttached = true }
                                    .focusRequester(licensesFirstRequester)
                                    .gamepadFocusOutline(
                                        shape = CircleShape,
                                        register = true,
                                        onActivate = openFullLicense
                                    )
                                    .focusProperties { canFocus = false }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.str_view_full_license),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            OutlinedButton(
                                onClick = openGithubLicense,
                                modifier = Modifier
                                    .weight(1f)
                                    .gamepadFocusOutline(
                                        shape = CircleShape,
                                        register = true,
                                        onActivate = openGithubLicense
                                    )
                                    .focusProperties { canFocus = false }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_github),
                                    contentDescription = stringResource(R.string.cd_github),
                                    modifier = Modifier.size(18.dp),
                                    tint = scheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.str_view_on_github),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        SettingsActionCard(
                            icon = Icons.AutoMirrored.Rounded.LibraryBooks,
                            title = stringResource(R.string.str_libraries_used),
                            summary = stringResource(R.string.str_libraries_hint),
                            onClick = {
                                showLicensesDialog = false
                                showLibrariesDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showLibrariesDialog) {
        val libraries by produceLibraries(R.raw.aboutlibraries)
        val librariesToken = remember { Any() }
        val librariesBoxRequester = remember { FocusRequester() }
        val librariesLazyListState = rememberLazyListState()
        DisposableEffect(librariesToken) {
            GameSpaceOverlayState.acquire(librariesToken, "AboutLibrariesDialog")
            onDispose {
                GameSpaceOverlayState.release(librariesToken)
            }
        }
        val dismissLibraries = {
            Log.d("PAD", "gs about dismiss LibrariesDialog")
            AudioUtil.playCancel(context)
            showLibrariesDialog = false
        }
        Dialog(
            onDismissRequest = dismissLibraries,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            BindGameSpaceDialogGamepad()
            val dialogScope = rememberCoroutineScope()
            val dialogDensity = LocalDensity.current
            val stepPx = with(dialogDensity) { 180.dp.toPx() }
            var boxAttached by remember { mutableStateOf(false) }

            BackHandler(enabled = true, onBack = dismissLibraries)

            LaunchedEffect(libraries) {
                if (GameSpaceInputSource.isGamepadLast || GamepadInput.dpadMode || GamepadInput.controllerConnected) {
                    GamepadInput.onDpadKey()
                }
                withTimeoutOrNull(600L) {
                    snapshotFlow { boxAttached }.first { it }
                }
                var seeded = false
                for (attempt in 1..12) {
                    withFrameNanos { }
                    seeded = runCatching { librariesBoxRequester.requestFocus() }.getOrDefault(false)
                    if (seeded) break
                }
                Log.d("PAD", "gs about seed LibrariesDialog attached=$boxAttached seeded=$seeded")
            }

            CompositionLocalProvider(LocalGamepadFocusEnabled provides true) {
                RegisterGamepadActions(
                    enabled = true,
                    scrollState = librariesLazyListState,
                    onCloseOverlay = {
                        dismissLibraries()
                        true
                    }
                )
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = scheme.surfaceContainerHigh,
                    contentColor = scheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth(if (isWideScreen) 0.84f else 0.94f)
                        .fillMaxHeight(0.86f)
                        .tapHaptic()
                        .gamepadKeyWatch()
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (event.nativeKeyEvent.keyCode) {
                                KeyEvent.KEYCODE_BUTTON_B,
                                KeyEvent.KEYCODE_ESCAPE -> {
                                    dismissLibraries()
                                    true
                                }
                                KeyEvent.KEYCODE_DPAD_DOWN -> {
                                    dialogScope.launch {
                                        runCatching { librariesLazyListState.animateScrollBy(stepPx) }
                                    }
                                    true
                                }
                                KeyEvent.KEYCODE_DPAD_UP -> {
                                    dialogScope.launch {
                                        runCatching { librariesLazyListState.animateScrollBy(-stepPx) }
                                    }
                                    true
                                }
                                else -> false
                            }
                        }
                        .focusProperties {
                            canFocus = false
                            onExit = { cancelFocusChange() }
                        }
                        .focusGroup()
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = scheme.primaryContainer,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.LibraryBooks,
                                            contentDescription = null,
                                            tint = scheme.onPrimaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = stringResource(R.string.str_libraries_used),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = scheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.str_libraries_hint),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = scheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = dismissLibraries,
                                modifier = Modifier.focusProperties { canFocus = false }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = null,
                                    tint = scheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))

                        val contentShape = RoundedCornerShape(20.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp)
                                .onGloballyPositioned { boxAttached = true }
                                .focusRequester(librariesBoxRequester)
                                .gamepadFocusOutline(
                                    shape = contentShape,
                                    register = true
                                )
                                .clip(contentShape)
                                .focusProperties { canFocus = false }
                        ) {
                            if (libraries == null) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            } else {
                                LibrariesContainer(
                                    libraries = libraries,
                                    lazyListState = librariesLazyListState,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showChangelogDialog) {
        val changelogToken = remember { Any() }
        val changelogScrollState = rememberScrollState()
        val changelogBoxRequester = remember { FocusRequester() }
        DisposableEffect(changelogToken) {
            GameSpaceOverlayState.acquire(changelogToken, "AboutChangelogDialog")
            onDispose {
                GameSpaceOverlayState.release(changelogToken)
            }
        }
        val dismissChangelog = {
            Log.d("PAD", "gs about dismiss ChangelogDialog")
            AudioUtil.playCancel(context)
            showChangelogDialog = false
        }
        Dialog(
            onDismissRequest = dismissChangelog,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            BindGameSpaceDialogGamepad()
            val dialogScope = rememberCoroutineScope()
            val dialogDensity = LocalDensity.current
            val stepPx = with(dialogDensity) { 180.dp.toPx() }
            var boxAttached by remember { mutableStateOf(false) }

            BackHandler(enabled = true, onBack = dismissChangelog)

            LaunchedEffect(Unit) {
                if (GameSpaceInputSource.isGamepadLast || GamepadInput.dpadMode || GamepadInput.controllerConnected) {
                    GamepadInput.onDpadKey()
                }
                withTimeoutOrNull(600L) {
                    snapshotFlow { boxAttached }.first { it }
                }
                var seeded = false
                for (attempt in 1..12) {
                    withFrameNanos { }
                    seeded = runCatching { changelogBoxRequester.requestFocus() }.getOrDefault(false)
                    if (seeded) break
                }
                Log.d("PAD", "gs about seed ChangelogDialog attached=$boxAttached seeded=$seeded")
            }

            CompositionLocalProvider(LocalGamepadFocusEnabled provides true) {
                RegisterGamepadActions(
                    enabled = true,
                    columnScrollState = changelogScrollState,
                    onCloseOverlay = {
                        dismissChangelog()
                        true
                    }
                )
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = scheme.surfaceContainerHigh,
                    contentColor = scheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth(if (isWideScreen) 0.72f else 0.94f)
                        .fillMaxHeight(0.86f)
                        .tapHaptic()
                        .gamepadKeyWatch()
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (event.nativeKeyEvent.keyCode) {
                                KeyEvent.KEYCODE_BUTTON_B,
                                KeyEvent.KEYCODE_ESCAPE -> {
                                    dismissChangelog()
                                    true
                                }
                                KeyEvent.KEYCODE_DPAD_DOWN -> {
                                    dialogScope.launch {
                                        runCatching { changelogScrollState.animateScrollBy(stepPx) }
                                    }
                                    true
                                }
                                KeyEvent.KEYCODE_DPAD_UP -> {
                                    dialogScope.launch {
                                        runCatching { changelogScrollState.animateScrollBy(-stepPx) }
                                    }
                                    true
                                }
                                else -> false
                            }
                        }
                        .focusProperties {
                            canFocus = false
                            onExit = { cancelFocusChange() }
                        }
                        .focusGroup()
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = scheme.primaryContainer,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.Notes,
                                            contentDescription = null,
                                            tint = scheme.onPrimaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = stringResource(R.string.str_changelog),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = scheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = stringResource(R.string.str_version_buildconfig_version_na, BuildConfig.VERSION_NAME),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = scheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            IconButton(
                                onClick = dismissChangelog,
                                modifier = Modifier.focusProperties { canFocus = false }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = null,
                                    tint = scheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))

                        val contentShape = RoundedCornerShape(20.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(10.dp)
                                .onGloballyPositioned { boxAttached = true }
                                .focusRequester(changelogBoxRequester)
                                .gamepadFocusOutline(
                                    shape = contentShape,
                                    register = true
                                )
                                .clip(contentShape)
                                .focusProperties { canFocus = false }
                        ) {
                            if (changelogText.isBlank()) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(changelogScrollState)
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    MarkdownText(
                                        markdown = changelogText,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = scheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeveloperCreditCard(
    avatarRes: Int,
    name: String,
    role: String,
    githubUrl: String,
    telegramUrl: String,
    onOpenUrl: (String) -> Unit
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)

    Surface(
        shape = shape,
        color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
        contentColor = scheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(avatarRes),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 18.sp
                    ),
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = role,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 15.sp),
                    color = scheme.onSurfaceVariant.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val iconBtnShape = RoundedCornerShape(10.dp)
                val openGithub = {
                    AudioUtil.playOk(context)
                    onOpenUrl(githubUrl)
                }
                val openTelegram = {
                    AudioUtil.playOk(context)
                    onOpenUrl(telegramUrl)
                }
                Surface(
                    shape = iconBtnShape,
                    color = scheme.surfaceContainerHighest,
                    modifier = Modifier
                        .size(36.dp)
                        .springPressBounce()
                        .gamepadFocusOutline(
                            shape = iconBtnShape,
                            register = true,
                            onActivate = openGithub
                        )
                        .clip(iconBtnShape)
                        .focusProperties { canFocus = false }
                        .clickable(onClick = openGithub)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_github),
                            contentDescription = stringResource(R.string.cd_github),
                            modifier = Modifier.size(18.dp),
                            tint = scheme.primary
                        )
                    }
                }

                Surface(
                    shape = iconBtnShape,
                    color = scheme.surfaceContainerHighest,
                    modifier = Modifier
                        .size(36.dp)
                        .springPressBounce()
                        .gamepadFocusOutline(
                            shape = iconBtnShape,
                            register = true,
                            onActivate = openTelegram
                        )
                        .clip(iconBtnShape)
                        .focusProperties { canFocus = false }
                        .clickable(onClick = openTelegram)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_telegram),
                            contentDescription = stringResource(R.string.cd_telegram),
                            modifier = Modifier.size(18.dp),
                            tint = scheme.primary
                        )
                    }
                }
            }
        }
    }
}

