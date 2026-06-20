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
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import zx.azenith.BuildConfig
import zx.azenith.R
import zx.azenith.ui.component.*
import zx.azenith.ui.util.PropertyUtils


@Composable
fun AboutScreen(navController: NavController) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val listState = rememberLazyListState()
    

    val openLink = { url: String ->
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    MaterialExpressiveTheme {        
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = { 
                AboutTopAppBar(
                    scrollBehavior = scrollBehavior,
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
                )
            ) {

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.avatar),
                            contentDescription = stringResource(R.string.app_name),
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Text(
                            text = stringResource(R.string.str_version_buildconfig_version_na, BuildConfig.VERSION_NAME),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = { openLink("https://t.me/ArchHavenDisc") }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_telegram), 
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.str_support_group))
                            }
                            
                            OutlinedButton(
                                onClick = { openLink("https://t.me/ZeshArch") }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_telegram), 
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.str_channel))
                            }
                        }
                    }
                }


                item { 
                    AboutSectionTitle(stringResource(R.string.str_maintainer)) 
                }
                item {
                    ExpressiveList(
                        content = listOf(
                            {
                                ExpressiveListItem(
                                    headlineContent = { Text(text = stringResource(R.string.str_liliya), fontWeight = FontWeight.SemiBold) },
                                    supportingContent = { Text(stringResource(R.string.str_creator_maintainer)) },
                                    leadingContent = {
                                        Image(
                                            painter = painterResource(R.drawable.avatar_liliya),
                                            contentDescription = stringResource(R.string.str_liliya),
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(48.dp).clip(CircleShape)
                                        )
                                    },
                                    trailingContent = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(onClick = { openLink("https://github.com/Liliya2727") }) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_github),
                                                    contentDescription = stringResource(R.string.cd_github),
                                                    modifier = Modifier.size(29.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            IconButton(onClick = { openLink("https://t.me/Zexshia") }) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_telegram),
                                                    contentDescription = stringResource(R.string.cd_telegram),
                                                    modifier = Modifier.size(26.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        )
                    )
                }


                item { 
                    AboutSectionTitle(stringResource(R.string.str_collaborators)) 
                }
                item {
                    ExpressiveList(
                        content = listOf(
                            {
                                ExpressiveListItem(
                                    headlineContent = { Text(text = stringResource(R.string.str_rianixia), fontWeight = FontWeight.SemiBold) },
                                    supportingContent = { Text(stringResource(R.string.str_co_maintainer)) },
                                    leadingContent = {
                                        Image(
                                            painter = painterResource(R.drawable.avatar_xia),
                                            contentDescription = stringResource(R.string.str_rianixia),
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(48.dp).clip(CircleShape)
                                        )
                                    },
                                    trailingContent = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(onClick = { openLink("https://github.com/ryanistr") }) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_github),
                                                    contentDescription = stringResource(R.string.cd_github),
                                                    modifier = Modifier.size(29.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            IconButton(onClick = { openLink("https://t.me/rianixia") }) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_telegram),
                                                    contentDescription = stringResource(R.string.cd_telegram),
                                                    modifier = Modifier.size(26.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                )
                            },
                            {
                                ExpressiveListItem(
                                    headlineContent = { Text(text = stringResource(R.string.str_kanaochar), fontWeight = FontWeight.SemiBold) },
                                    supportingContent = { Text(stringResource(R.string.str_co_maintainer)) },
                                    leadingContent = {
                                        Image(
                                            painter = painterResource(R.drawable.avatar_kanao),
                                            contentDescription = stringResource(R.string.str_kanaochar),
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(48.dp).clip(CircleShape)
                                        )
                                    },
                                    trailingContent = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(onClick = { openLink("https://github.com/kanaodnd") }) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_github),
                                                    contentDescription = stringResource(R.string.cd_github),
                                                    modifier = Modifier.size(29.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            IconButton(onClick = { openLink("https://t.me/kanaochar") }) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_telegram),
                                                    contentDescription = stringResource(R.string.cd_telegram),
                                                    modifier = Modifier.size(26.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        )
                    )
                }
                
                item { 
                    AboutSectionTitle(stringResource(R.string.str_open_source)) 
                }
                item {
                    ExpressiveList(
                        content = listOf {
                            ExpressiveListItem(
                                onClick = { openLink("https://github.com/Liliya2727/AZenith") },
                                headlineContent = { Text(stringResource(R.string.str_source_code)) },
                                supportingContent = { Text(stringResource(R.string.str_view_the_source_code_on_github)) },
                                leadingContent = { LeadingIcon(icon = Icons.Rounded.Code) },
                                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AboutSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = 12.dp,
            end = 12.dp,
            top = 16.dp,
            bottom = 8.dp
        )
    )
}

@Composable
fun AboutTopAppBar(scrollBehavior: TopAppBarScrollBehavior, onBack: () -> Unit) {
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
                    text = stringResource(R.string.section_about),
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
 