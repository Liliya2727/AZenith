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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.withContext
import zx.azenith.BuildConfig
import zx.azenith.R
import zx.azenith.ui.component.gamepadFocusOutline
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

    var showLicensesDialog by remember { mutableStateOf(false) }
    var showLibrariesDialog by remember { mutableStateOf(false) }
    var showChangelogDialog by remember { mutableStateOf(false) }
    var changelogText by remember { mutableStateOf("") }

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
                                    .springPressBounce()
                                    .gamepadFocusOutline(shape = btnShape, register = true)
                                    .clip(btnShape)
                                    .clickable {
                                        AudioUtil.playOk(context)
                                        showChangelogDialog = true
                                    }
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
                                    .gamepadFocusOutline(shape = btnShape, register = true)
                                    .clip(btnShape)
                                    .clickable {
                                        AudioUtil.playOk(context)
                                        uriHandler.openUri("https://t.me/ArchHavenDisc")
                                    }
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
                                    .gamepadFocusOutline(shape = btnShape, register = true)
                                    .clip(btnShape)
                                    .clickable {
                                        AudioUtil.playOk(context)
                                        uriHandler.openUri("https://t.me/ZeshArch")
                                    }
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
                                    .springPressBounce()
                                    .gamepadFocusOutline(shape = btnShape, register = true)
                                    .clip(btnShape)
                                    .clickable {
                                        AudioUtil.playOk(context)
                                        showChangelogDialog = true
                                    }
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
                                        .gamepadFocusOutline(shape = btnShape, register = true)
                                        .clip(btnShape)
                                        .clickable {
                                            AudioUtil.playOk(context)
                                            uriHandler.openUri("https://t.me/ArchHavenDisc")
                                        }
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
                                        .gamepadFocusOutline(shape = btnShape, register = true)
                                        .clip(btnShape)
                                        .clickable {
                                            AudioUtil.playOk(context)
                                            uriHandler.openUri("https://t.me/ZeshArch")
                                        }
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
                            onClick = { showLicensesDialog = true }
                        )

                        SettingsActionCard(
                            icon = Icons.AutoMirrored.Rounded.LibraryBooks,
                            title = stringResource(R.string.str_libraries_used),
                            summary = stringResource(R.string.str_libraries_hint),
                            onClick = { showLibrariesDialog = true }
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
                            onClick = { showLicensesDialog = true }
                        )

                        SettingsActionCard(
                            icon = Icons.AutoMirrored.Rounded.LibraryBooks,
                            title = stringResource(R.string.str_libraries_used),
                            summary = stringResource(R.string.str_libraries_hint),
                            onClick = { showLibrariesDialog = true }
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
        Dialog(
            onDismissRequest = { showLicensesDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = scheme.surfaceContainerHigh,
                contentColor = scheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth(if (isWideScreen) 0.72f else 0.92f)
                    .padding(16.dp)
                    .tapHaptic()
            ) {
                Column(
                    modifier = Modifier
                        .padding(22.dp)
                        .verticalScroll(rememberScrollState()),
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

                        IconButton(onClick = { showLicensesDialog = false }) {
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
                        FilledTonalButton(
                            onClick = { uriHandler.openUri("https://www.apache.org/licenses/LICENSE-2.0") },
                            modifier = Modifier.weight(1f)
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
                            onClick = { uriHandler.openUri("https://github.com/Liliya2727/AZenith/blob/main/LICENSE") },
                            modifier = Modifier.weight(1f)
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

    if (showLibrariesDialog) {
        val libraries by produceLibraries(R.raw.aboutlibraries)
        Dialog(
            onDismissRequest = { showLibrariesDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = scheme.surfaceContainerHigh,
                contentColor = scheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth(if (isWideScreen) 0.84f else 0.94f)
                    .fillMaxHeight(0.86f)
                    .tapHaptic()
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

                        IconButton(onClick = { showLibrariesDialog = false }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = null,
                                tint = scheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (libraries == null) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        } else {
                            LibrariesContainer(
                                libraries = libraries,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    if (showChangelogDialog) {
        Dialog(
            onDismissRequest = { showChangelogDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = scheme.surfaceContainerHigh,
                contentColor = scheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth(if (isWideScreen) 0.72f else 0.94f)
                    .fillMaxHeight(0.86f)
                    .tapHaptic()
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

                        IconButton(onClick = { showChangelogDialog = false }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = null,
                                tint = scheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (changelogText.isBlank()) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 22.dp, vertical = 16.dp)
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
                Surface(
                    shape = iconBtnShape,
                    color = scheme.surfaceContainerHighest,
                    modifier = Modifier
                        .size(36.dp)
                        .springPressBounce()
                        .gamepadFocusOutline(shape = iconBtnShape, register = true)
                        .clip(iconBtnShape)
                        .clickable {
                            AudioUtil.playOk(context)
                            onOpenUrl(githubUrl)
                        }
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
                        .gamepadFocusOutline(shape = iconBtnShape, register = true)
                        .clip(iconBtnShape)
                        .clickable {
                            AudioUtil.playOk(context)
                            onOpenUrl(telegramUrl)
                        }
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

