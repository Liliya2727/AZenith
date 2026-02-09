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

package zx.azenith.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.lifecycle.viewmodel.compose.viewModel
import zx.azenith.R
import zx.azenith.ui.component.AppIconImage
import zx.azenith.ui.viewmodel.ApplistViewmodel
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalFocusManager
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource

@Composable
fun ApplistScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: ApplistViewmodel = viewModel()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    
    val pullToRefreshState = rememberPullToRefreshState()
    
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    LaunchedEffect(Unit) {
        viewModel.showSystemApps = prefs.getBoolean("show_system_apps", false)
        if (ApplistViewmodel.apps.isEmpty()) {
            viewModel.loadApps(context)
        }
    }

    var isSearchMode by remember { mutableStateOf(false) }
    
    BackHandler(enabled = isSearchMode) {
        viewModel.clearSearch()
        isSearchMode = false
        focusManager.clearFocus()
    }

    LaunchedEffect(isSearchMode) {
        if (isSearchMode) {
            focusRequester.requestFocus()
        } else {
            viewModel.clearSearch()
            focusManager.clearFocus()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (ApplistViewmodel.apps.isNotEmpty()) {
                    viewModel.refreshAppConfigStatus()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
    
    MaterialExpressiveTheme {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),     
            topBar = {
                ApplistTopAppBar(
                    scrollBehavior,
                    isSearchMode = isSearchMode,
                    onSearchModeChange = { 
                        isSearchMode = it 
                        if (!it) viewModel.clearSearch() 
                    },
                    searchQuery = viewModel.searchTextFieldValue,
                    onSearchChange = { viewModel.updateSearch(it) },
                    showSystemApps = viewModel.showSystemApps,
                    onToggleSystem = { newValue ->
                        viewModel.showSystemApps = newValue
                        prefs.edit().putBoolean("show_system_apps", newValue).apply()
                    },
                    onRefresh = { viewModel.loadApps(context, forceRefresh = true) },
                    focusRequester = focusRequester
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pullToRefresh(
                        state = pullToRefreshState,
                        isRefreshing = viewModel.isRefreshing,
                        onRefresh = { viewModel.loadApps(context, forceRefresh = true) }
                    )
            ) {
                val appsToDisplay = viewModel.filteredApps
                
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding(),
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 110.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    )
                ) {
                    itemsIndexed(
                        items = appsToDisplay, 
                        key = { _, app -> app.packageName }
                    ) { index, app ->
                        val shape = when {
                            appsToDisplay.size == 1 -> RoundedCornerShape(20.dp)
                            index == 0 -> RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                            index == appsToDisplay.lastIndex -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 26.dp, bottomEnd = 26.dp)
                            else -> RoundedCornerShape(4.dp)
                        }
                        
                        AppListItem(
                            app = app, 
                            shape = shape,
                            navController = navController
                        )
                        
                        if (index < appsToDisplay.lastIndex) {
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                    }
                }
    
                PullToRefreshDefaults.LoadingIndicator(
                    state = pullToRefreshState,
                    isRefreshing = viewModel.isRefreshing,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = innerPadding.calculateTopPadding())
                )
            }
        }
    }
}

@Composable
fun AppListItem(
    app: ApplistViewmodel.AppInfo,
    shape: RoundedCornerShape,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        onClick = { navController.navigate("app_settings/${app.packageName}") }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconImage(
                packageName = app.packageName,
                size = 55.dp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    if (app.isEnabledInConfig) {
                        LabelText(
                            text = stringResource(R.string.label_enabled),
                            color = Color(0xFF4CAF50)
                        )
                    } else {
                        LabelText(stringResource(R.string.label_disabled), MaterialTheme.colorScheme.error)
                    }
                    if (app.isRecommended) {
                        LabelText(stringResource(R.string.label_recommended), MaterialTheme.colorScheme.primary)
                    }
                    if (app.isSystem) {
                        LabelText(stringResource(R.string.label_system), MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}

@Composable
fun ApplistTopAppBar(
    scrollBehavior: TopAppBarScrollBehavior,
    isSearchMode: Boolean,
    onSearchModeChange: (Boolean) -> Unit,
    searchQuery: TextFieldValue,
    onSearchChange: (TextFieldValue) -> Unit,
    showSystemApps: Boolean,
    onToggleSystem: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    focusRequester: FocusRequester
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme

    val smoothGradient = Brush.verticalGradient(
        0.0f to colorScheme.surface,
        0.4f to colorScheme.surface.copy(alpha = 0.9f),
        0.5f to colorScheme.surface.copy(alpha = 0.8f),
        0.6f to colorScheme.surface.copy(alpha = 0.7f),
        0.7f to colorScheme.surface.copy(alpha = 0.5f),
        0.8f to colorScheme.surface.copy(alpha = 0.4f),
        0.9f to colorScheme.surface.copy(alpha = 0.3f),
        1.0f to Color.Transparent 
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(smoothGradient)
            .statusBarsPadding()
    ) {
        TopAppBar(
            scrollBehavior = scrollBehavior,
            windowInsets = WindowInsets(0, 0, 0, 0),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
            title = {
                AnimatedContent(
                    targetState = isSearchMode,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "search_animation"
                ) { searching ->
                    if (searching) {
                        TextField(
                            value = searchQuery,
                            onValueChange = onSearchChange,
                            placeholder = { Text(stringResource(R.string.search_apps)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(colorScheme.surfaceVariant)
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.avatar),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.applist_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                if (isSearchMode) {
                    IconButton(onClick = { onSearchModeChange(false) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                }
            },
            actions = {
                if (!isSearchMode) {
                    IconButton(onClick = { onSearchModeChange(true) }) {
                        Icon(Icons.Default.Search, stringResource(R.string.cd_search))
                    }
                }
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, stringResource(R.string.cd_menu))
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_refresh)) },
                        onClick = { onRefresh(); menuExpanded = false },
                        leadingIcon = { Icon(Icons.Default.Refresh, null) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_show_system_apps)) },
                        trailingIcon = { Checkbox(showSystemApps, null) },
                        onClick = { onToggleSystem(!showSystemApps); menuExpanded = false }
                    )
                }
            }
        )
    }
}


@Composable
fun LabelText(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.padding(end = 6.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
    }
}
