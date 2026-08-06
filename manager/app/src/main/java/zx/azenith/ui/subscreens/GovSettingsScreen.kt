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


import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import zx.azenith.R
import zx.azenith.ui.component.*
import zx.azenith.ui.mainscreens.*
import zx.azenith.ui.util.PropertyUtils
import zx.azenith.ui.util.*
import zx.azenith.ui.viewmodel.TweakViewModel


@Composable
fun GovSettings(
    navController: NavController,
    viewModel: TweakViewModel = viewModel()
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val colorScheme = MaterialTheme.colorScheme
    val snackbarHostState = remember { SnackbarHostState() }
    
    MaterialExpressiveTheme {        
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = { GovSettingsTopAppBar(
                scrollBehavior,
                onBack = { navController.popBackStack() }
                ) 
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) { innerPadding ->
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    ExpressiveList(
                        content = listOf( 
                            {
                                ExpressiveInfoCard(
                                    supportingContent = { Text(text = stringResource(R.string.gov_settingsdesc)) },
                                    leadingContent = { LeadingIcon(icon = Icons.Filled.Info) },
                                    containerColor = colorScheme.surfaceContainerLow,
                                    onClick = {}
                                )
                            }
                        )
                    )
                }
                
                item { TweaksSectionTitle(stringResource(R.string.section_CPUSettings)) }
                item {
                    if (viewModel.defaultGovIndex != null && 
                        viewModel.powersaveGovIndex != null && 
                        viewModel.performanceGovIndex != null && 
                        viewModel.freqOffsetIndex != null) {
                        ExpressiveList(
                            content = listOf(
                                {
                                    ExpressiveDropdownItem(
                                        icon = Icons.Outlined.Water,
                                        title = stringResource(R.string.default_cpu_gov),
                                        summary = stringResource(R.string.default_cpu_gov_desc),
                                        items = viewModel.availableGovernors ?: emptyList(),
                                        selectedIndex = viewModel.defaultGovIndex!!,
                                        onItemSelected = { viewModel.updateDefaultGovernor(it) }
                                    )
                                },
                                {
                                    ExpressiveDropdownItem(
                                        icon = Icons.Outlined.OfflineBolt,
                                        title = stringResource(R.string.performance_cpu_gov),
                                        summary = stringResource(R.string.performance_cpu_gov_desc),
                                        items = viewModel.availableGovernors ?: emptyList(),
                                        selectedIndex = viewModel.performanceGovIndex!!,
                                        onItemSelected = { viewModel.updatePerformanceGovernor(it) }
                                    )
                                },
                                {
                                    ExpressiveDropdownItem(
                                        icon = Icons.Outlined.EnergySavingsLeaf,
                                        title = stringResource(R.string.powersave_cpu_gov),
                                        summary = stringResource(R.string.powersave_cpu_gov_desc),
                                        items = viewModel.availableGovernors ?: emptyList(),
                                        selectedIndex = viewModel.powersaveGovIndex!!,
                                        onItemSelected = { viewModel.updatePowersaveGovernor(it) }
                                    )
                                },
                                {
                                    FreqLimitSliderItem(
                                        icon = Icons.Outlined.Tune,
                                        initialValue = viewModel.freqOffsetIndex!!,
                                        labels = viewModel.offsetLabels,
                                        onSaved = { viewModel.saveFreqOffset(it) }
                                    )
                                }
                            )
                        )
                    } else {
                        SectionLoadingIndicator()
                    }
                }

                item { TweaksSectionTitle(stringResource(R.string.io_settings)) }
                item {
                    if (viewModel.availableIOSchedulers == null) {
                        SectionLoadingIndicator()
                    } else if (viewModel.availableIOSchedulers!!.isNotEmpty()) {
                        if (viewModel.balancedIOIndex != null && 
                            viewModel.performanceIOIndex != null && 
                            viewModel.powersaveIOIndex != null) {
                            ExpressiveList(
                                content = listOf(
                                    {
                                        ExpressiveDropdownItem(
                                            icon = Icons.Outlined.Water,
                                            title = stringResource(R.string.balanced_io_scheduler),
                                            summary = stringResource(R.string.balanced_io_scheduler_desc),
                                            items = viewModel.availableIOSchedulers ?: emptyList(),
                                            selectedIndex = viewModel.balancedIOIndex!!,
                                            onItemSelected = { viewModel.updateBalancedIO(it) }
                                        )
                                    },
                                    {
                                        ExpressiveDropdownItem(
                                            icon = Icons.Outlined.OfflineBolt,
                                            title = stringResource(R.string.performance_io_scheduler),
                                            summary = stringResource(R.string.performance_io_scheduler_desc),
                                            items = viewModel.availableIOSchedulers ?: emptyList(),
                                            selectedIndex = viewModel.performanceIOIndex!!,
                                            onItemSelected = { viewModel.updatePerformanceIO(it) }
                                        )
                                    },
                                    {
                                        ExpressiveDropdownItem(
                                            icon = Icons.Outlined.EnergySavingsLeaf,
                                            title = stringResource(R.string.powersave_io_scheduler),
                                            summary = stringResource(R.string.powersave_io_scheduler_desc),
                                            items = viewModel.availableIOSchedulers ?: emptyList(),
                                            selectedIndex = viewModel.powersaveIOIndex!!,
                                            onItemSelected = { viewModel.updatePowersaveIO(it) }
                                        )
                                    }
                                )
                            )
                        } else {
                            SectionLoadingIndicator()
                        }
                    } else {

                    }
                }
                

                if (viewModel.isMaliGpuAvailable == true) {
                    item { TweaksSectionTitle(text = stringResource(R.string.section_mali_gpu)) }
                    item {
                        if (viewModel.availableMaliGovernors == null) {
                            SectionLoadingIndicator()
                        } else if (viewModel.availableMaliGovernors!!.isNotEmpty()) {
                            if (viewModel.balancedMaliGovIndex != null && 
                                viewModel.performanceMaliGovIndex != null && 
                                viewModel.powersaveMaliGovIndex != null) {
                                ExpressiveList(
                                    content = listOf(
                                        {
                                            ExpressiveDropdownItem(
                                                icon = Icons.Outlined.Water,
                                                title = stringResource(R.string.balanced_mali_gov),
                                                summary = stringResource(R.string.balanced_mali_gov_desc),
                                                items = viewModel.availableMaliGovernors ?: emptyList(),
                                                selectedIndex = viewModel.balancedMaliGovIndex!!,
                                                onItemSelected = { viewModel.updateBalancedMaliGov(it) }
                                            )
                                        },
                                        {
                                            ExpressiveDropdownItem(
                                                icon = Icons.Outlined.OfflineBolt,
                                                title = stringResource(R.string.performance_mali_gov),
                                                summary = stringResource(R.string.performance_mali_gov_desc),
                                                items = viewModel.availableMaliGovernors ?: emptyList(),
                                                selectedIndex = viewModel.performanceMaliGovIndex!!,
                                                onItemSelected = { viewModel.updatePerformanceMaliGov(it) }
                                            )
                                        },
                                        {
                                            ExpressiveDropdownItem(
                                                icon = Icons.Outlined.EnergySavingsLeaf,
                                                title = stringResource(R.string.powersave_mali_gov),
                                                summary = stringResource(R.string.powersave_mali_gov_desc),
                                                items = viewModel.availableMaliGovernors ?: emptyList(),
                                                selectedIndex = viewModel.powersaveMaliGovIndex!!,
                                                onItemSelected = { viewModel.updatePowersaveMaliGov(it) }
                                            )
                                        }
                                    )
                                )
                            } else {
                                SectionLoadingIndicator()
                            }
                        }
                    }
                }              
            }
        }
    }
}

@Composable
fun GovSettingsTopAppBar(scrollBehavior: TopAppBarScrollBehavior, onBack: () -> Unit) {
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
        LargeFlexibleTopAppBar(
            title = { 
                Text(
                    text = stringResource(R.string.gov_settings),
                    fontWeight = FontWeight.Bold
                ) 
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                }
            },        
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
            scrollBehavior = scrollBehavior,
            windowInsets = WindowInsets(0, 0, 0, 0)
        )
    }
}
            