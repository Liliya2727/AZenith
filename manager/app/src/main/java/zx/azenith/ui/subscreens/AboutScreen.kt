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
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import zx.azenith.R
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.rounded.*
import zx.azenith.ui.component.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.topjohnwu.superuser.Shell
import androidx.navigation.NavController
import android.content.Context
import android.view.WindowManager
import zx.azenith.ui.util.PropertyUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberCoroutineScope
import android.widget.Toast
import androidx.compose.material3.LargeFlexibleTopAppBar
// ... (Pertahankan import yang sudah ada)
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.text.style.TextAlign
import zx.azenith.BuildConfig // Pastikan import BuildConfig ini ada untuk versi app

@Composable
fun AboutScreen(navController: NavController) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val listState = rememberLazyListState()
    
    // Fungsi pembantu untuk membuka link (GitHub/Telegram)
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
                // 1. App Header Section
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // App Icon
                        Image(
                            painter = painterResource(id = R.drawable.avatar), // Ganti dengan icon aplikasimu jika berbeda
                            contentDescription = "App Icon",
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape) // Atau RoundedCornerShape(24.dp) kalau mau bentuk kotak melengkung
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // App Name
                        Text(
                            text = stringResource(id = R.string.app_name), // Atau hardcode "AZenith"
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        // App Version
                        Text(
                            text = "Version ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // App Description
                        Text(
                            text = "Universal auto Performance module to enhance gaming experience while preserving daily usability",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }

                // 2. Maintainer Section
                item { 
                    AboutSectionTitle("Maintainer") 
                }
                item {
                    ExpressiveList(
                        content = listOf(
                            {
                                ExpressiveListItem(
                                    headlineContent = {
                                        Text(
                                            text = "Liliya",
                                            fontWeight = FontWeight.SemiBold
                                        ) 
                                    },
                                    supportingContent = { Text("Creator & Maintainer") },
                                    leadingContent = {
                                        Image(
                                            painter = painterResource(R.drawable.avatar_liliya),
                                            contentDescription = "Liliya",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
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
                                                    contentDescription = "GitHub",
                                                    modifier = Modifier.size(24.dp),
                                                    tint = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            IconButton(onClick = { openLink("https://github.com/ZeshArch") }) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_github),
                                                    contentDescription = "GitHub",
                                                    modifier = Modifier.size(24.dp),
                                                    tint = MaterialTheme.colorScheme.onSurface
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
                    AboutSectionTitle("Collaborators") 
                }
                item {
                    ExpressiveList(
                        content = listOf(
                            {
                                ExpressiveListItem(
                                    headlineContent = { 
                                        Text(text = "Rianixia", fontWeight = FontWeight.SemiBold) 
                                    },
                                    leadingContent = {
                                        Image(
                                            painter = painterResource(R.drawable.avatar_xia),
                                            contentDescription = "Xia",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                        )
                                    },
                                    trailingContent = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(onClick = { openLink("https://github.com/rianixia") }) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_github),
                                                    contentDescription = "GitHub",
                                                    modifier = Modifier.size(24.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(onClick = { openLink("https://t.me/rianixia") }) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_telegram), // Siapkan aset ic_telegram.xml
                                                    contentDescription = "Telegram",
                                                    modifier = Modifier.size(24.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                )
                            },
                            {
                                ExpressiveListItem(
                                    headlineContent = { 
                                        Text(text = "Kanaochar", fontWeight = FontWeight.SemiBold) 
                                    },
                                    leadingContent = {
                                        Image(
                                            painter = painterResource(R.drawable.avatar_kanao),
                                            contentDescription = "Kanaochar",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
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
                                                    contentDescription = "GitHub",
                                                    modifier = Modifier.size(24.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(onClick = { openLink("https://t.me/kanaochar") }) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_telegram), // Siapkan aset ic_telegram.xml
                                                    contentDescription = "Telegram",
                                                    modifier = Modifier.size(24.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        )
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
                    text = "About",
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
            