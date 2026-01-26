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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ApplistScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: ApplistViewmodel = viewModel()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
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

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ApplistTopAppBar(
                scrollBehavior = scrollBehavior,
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
                .padding(PaddingValues(top = innerPadding.calculateTopPadding()))
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
                contentPadding = PaddingValues(16.dp)
            ) {
                itemsIndexed(
                    items = appsToDisplay, 
                    key = { _, app -> app.packageName }
                ) { index, app ->
                    val shape = when {
                        appsToDisplay.size == 1 -> RoundedCornerShape(20.dp)
                        index == 0 -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                        index == appsToDisplay.lastIndex -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
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
                modifier = Modifier.align(Alignment.TopCenter)
            )
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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

    TopAppBar(
        scrollBehavior = scrollBehavior,
        title = {
            AnimatedContent(
                targetState = isSearchMode,
                transitionSpec = { fadeIn() togetherWith fadeOut() }
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
                                .background(MaterialTheme.colorScheme.surfaceVariant)
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
                            fontWeight = FontWeight.SemiBold
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
