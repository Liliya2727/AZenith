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

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import zx.azenith.R
import zx.azenith.ui.component.ExpressiveList
import zx.azenith.ui.component.ExpressiveListItem
import zx.azenith.ui.component.JumpRow
import zx.azenith.ui.component.LeadingIcon
import zx.azenith.ui.component.SearchJump
import zx.azenith.ui.component.ScaffoldContainerColor
import zx.azenith.ui.component.ScaffoldContentColor
import zx.azenith.ui.component.hazePageSource
import zx.azenith.ui.navigation.safePopBackStack
import zx.azenith.ui.util.DebugUtils
import zx.azenith.ui.util.PropertyUtils
import zx.azenith.ui.util.getChipsetVendor
import zx.azenith.ui.viewmodel.SearchCapabilities
import zx.azenith.ui.viewmodel.SearchEntry
import zx.azenith.ui.viewmodel.SearchIndex
import zx.azenith.ui.viewmodel.SearchTab
import zx.azenith.ui.viewmodel.SearchTarget

/** The three shapes the body takes; each enters with the motion its meaning wants. */
private enum class SearchPhase { Prompt, NotFound, Results }

/**
 * Search inside one tab, pushed from that tab's top bar.
 *
 * [tab] is the whole scope: a Tweaks search never offers a Settings row and the
 * other way round. Results are grouped by the screen each hit lives on, so a
 * hit on a sub-screen is one tap from the tab that owns it.
 */
@Composable
fun SearchScreen(
    navController: NavController,
    tab: SearchTab,
    onOpenPagerPage: (String) -> Unit = { navController.safePopBackStack() }
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val colorScheme = MaterialTheme.colorScheme
    val motionScheme = MaterialTheme.motionScheme
    var query by rememberSaveable { mutableStateOf("") }

    // Probed once, on IO: getChipsetVendor reads props, full mode goes through a
    // root shell, and the Mali check is the same helper command GovSettings uses.
    var capabilities by remember { mutableStateOf(SearchCapabilities()) }
    LaunchedEffect(Unit) {
        capabilities = withContext(Dispatchers.IO) {
            val bypassPath = PropertyUtils.get("persist.sys.azenithconf.bypasspath", "")
            val maliCheck = Shell.cmd(
                "/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf checkmalipath"
            ).exec().out.joinToString("").trim()
            SearchCapabilities(
                vendor = getChipsetVendor(context),
                fullMode = DebugUtils.isFullModeEnabled(),
                hasMaliGpu = maliCheck == "true",
                bypassSupported = bypassPath != "UNSUPPORTED" && bypassPath != "NEED_SETUP"
            )
        }
    }

    val groupedEntries = remember(query, tab, capabilities) {
        SearchIndex.search(query, tab, capabilities) { context.getString(it) }
            .groupBy { it.group }
            .toList()
            .sortedBy { it.first.ordinal }
    }

    // The whole body swaps on this, so one enum beats several booleans: each
    // branch needs its own entered motion and only one can be on screen.
    val phase = when {
        query.isBlank() -> SearchPhase.Prompt
        groupedEntries.isEmpty() -> SearchPhase.NotFound
        else -> SearchPhase.Results
    }

    // The field takes focus on open, so leaving has to give it back. A focused
    // TextField the pop disposes keeps the IME targeted at a window that no
    // longer has an editor, and the keyboard then sits over the screen behind
    // with nothing drawn under it. Every exit path -- the arrow, the system
    // back gesture and a result tap -- goes through here or the onDispose below.
    val dismissEditor = {
        keyboard?.hide()
        focusManager.clearFocus()
    }

    DisposableEffect(Unit) {
        onDispose { dismissEditor() }
    }

    BackHandler { dismissEditor(); navController.safePopBackStack() }

    LaunchedEffect(Unit) {
        // Requesting focus before the field is attached throws, and on the first
        // frame of a nav transition it is not attached yet.
        withFrameNanos { }
        focusRequester.requestFocus()
    }

    val openEntry: (SearchEntry) -> Unit = { entry ->
        dismissEditor()
        // A row result has to survive the pop: the destination is still composed
        // underneath, so it is told which row to land on before we leave.
        SearchJump.request(entry.id)
        when (val target = entry.target) {
            // The pager page is the tab already underneath, so this one is a
            // pop, not a push.
            is SearchTarget.Pager -> onOpenPagerPage(target.route)
            is SearchTarget.Route -> navController.navigate(target.route)
            is SearchTarget.Nested -> {
                navController.navigate(target.parent) {
                    popUpTo(target.parent) { inclusive = false }
                }
                navController.navigate(target.route)
            }
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                TopAppBar(
                    title = {
                        TextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = {
                                Text(stringResource(R.string.search_in_tab, stringResource(tab.labelRes)))
                            },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { query = "" }) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = stringResource(R.string.cd_clear)
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(28.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 12.dp)
                                .focusRequester(focusRequester),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = colorScheme.surfaceContainerHighest,
                                unfocusedContainerColor = colorScheme.surfaceContainerHighest,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                cursorColor = colorScheme.primary
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            dismissEditor()
                            navController.safePopBackStack()
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    ),
                    windowInsets = WindowInsets(0, 0, 0, 0)
                )
            }
        },
        contentColor = ScaffoldContentColor(),
        containerColor = ScaffoldContainerColor(colorScheme.surface)
    ) { innerPadding ->
        // One region, three trees, so AnimatedContent is what fits. The key is
        // the branch, which keeps typing inside a phase from restarting the swap.
        AnimatedContent(
            targetState = phase,
            transitionSpec = {
                when (targetState) {
                    // Results rise into place from under the field and land on the
                    // expressive spatial spring, so the group cards settle with a
                    // small overshoot instead of stopping dead.
                    SearchPhase.Results ->
                        (fadeIn(motionScheme.defaultEffectsSpec()) + slideInVertically(
                            animationSpec = motionScheme.defaultSpatialSpec(),
                            initialOffsetY = { it / 6 }
                        )) togetherWith fadeOut(motionScheme.defaultEffectsSpec())

                    // "Nothing found" pops rather than slides, so it reads as a
                    // verdict on the query and not as more content still arriving.
                    SearchPhase.NotFound ->
                        (fadeIn(motionScheme.defaultEffectsSpec()) + scaleIn(
                            animationSpec = motionScheme.defaultSpatialSpec(),
                            initialScale = 0.85f
                        )) togetherWith fadeOut(motionScheme.defaultEffectsSpec())

                    SearchPhase.Prompt ->
                        fadeIn(motionScheme.defaultEffectsSpec()) togetherWith
                            fadeOut(motionScheme.defaultEffectsSpec())
                }
            },
            contentKey = { it },
            modifier = Modifier.fillMaxSize(),
            label = "searchPhase"
        ) { target ->
            when (target) {
                SearchPhase.Prompt -> SearchMessage(
                    icon = Icons.Rounded.Search,
                    text = stringResource(R.string.search_empty_state)
                )

                SearchPhase.NotFound -> SearchMessage(
                    icon = Icons.Rounded.SearchOff,
                    text = stringResource(R.string.search_no_results)
                )

                SearchPhase.Results -> LazyColumn(
                    modifier = Modifier.fillMaxSize().hazePageSource(),
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding(),
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    )
                ) {
                    groupedEntries.forEach { (group, entries) ->
                        item(key = "group_${group.name}") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    // Refining the query adds and drops whole
                                    // groups; the placements spring so the rows
                                    // below slide rather than jump.
                                    .animateItem(
                                        fadeInSpec = motionScheme.defaultEffectsSpec(),
                                        fadeOutSpec = motionScheme.defaultEffectsSpec(),
                                        placementSpec = motionScheme.defaultSpatialSpec()
                                    )
                            ) {
                                Text(
                                    text = stringResource(group.labelRes),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colorScheme.primary,
                                    modifier = Modifier.padding(
                                        start = 16.dp,
                                        top = 16.dp,
                                        bottom = 8.dp
                                    )
                                )
                                // Same joined rounded container as the Settings
                                // tab, so hits read as one grouped card per screen.
                                ExpressiveList(
                                    content = entries.map { entry ->
                                        { SearchResultRow(entry = entry, onOpen = { openEntry(entry) }) }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(entry: SearchEntry, onOpen: () -> Unit) {
    // Only a row that pushes a screen gets the chevron; one that just lands back
    // on the tab underneath has nowhere to point. Both types are written out
    // because a @Composable function type does not survive inference through a
    // plain `let` or `if`.
    val chevron: (@Composable () -> Unit)? =
        if (entry.target is SearchTarget.Route || entry.target is SearchTarget.Nested) {
            { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
        } else null
    val summary: (@Composable () -> Unit)? =
        entry.summaryRes?.let { res -> { Text(stringResource(res)) } }

    ExpressiveListItem(
        onClick = onOpen,
        headlineContent = { Text(stringResource(entry.titleRes)) },
        supportingContent = summary,
        leadingContent = { LeadingIcon(icon = entry.icon) },
        trailingContent = chevron
    )
}

@Composable
private fun SearchMessage(icon: ImageVector, text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}