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
 
import android.app.Activity
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.edit
import com.materialkolor.rememberDynamicColorScheme
import androidx.navigation.NavController
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import coil.compose.AsyncImage
import zx.azenith.R
import zx.azenith.ui.theme.ColorMode
import zx.azenith.ui.theme.ThemeController
import zx.azenith.ui.util.PropertyUtils
import com.topjohnwu.superuser.CallbackList
import kotlinx.coroutines.launch
import com.topjohnwu.superuser.Shell
import com.fox2code.androidansi.ktx.parseAsAnsiAnnotatedString
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// Definisikan data class untuk log di luar atau di dalam fungsi
data class ShellOutput(
    val text: String,
    val isCompleted: Boolean = false
)

// ... (Imports tetap seperti sebelumnya, pastikan items dari lazy di-import)

@Composable
fun BypassChargeCheckScreen(navController: NavController) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val colorScheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    val logs = remember { mutableStateListOf<ShellOutput>() }
    var isRunning by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val ansiRegex = Regex("\u001B\\[[;\\d]*m")
    // Hapus regex-nya, kita butuh kode ANSI-nya buat warna

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val callbackList = object : CallbackList<String>() {
                override fun onAddElement(line: String) {
                    // Masukkan raw string (termasuk kode warna ASCII/ANSI)
                    scope.launch(Dispatchers.Main.immediate) {
                        logs.add(ShellOutput(line, true))
                    }
                }
            }

            val binaryPath = "/data/adb/modules/AZenith/system/bin/sys.azenith-service"
            // Tambahkan flag agar binary dipaksa keluarin warna kalau dia support (biasanya --color)
            // 2>&1 supaya pesan error juga masuk dan berwarna merah kalau binary-nya pinter
            val command = "$binaryPath -cbc 2>&1"

            Shell.cmd(command).to(callbackList).submit { result ->
                isRunning = false
                logs.add(ShellOutput("\n\u001B[33m------------------------------------------\u001B[0m", true))
                val status = if (result.isSuccess) "\u001B[32mSUCCESS\u001B[0m" else "\u001B[31mFAILED\u001B[0m"
                logs.add(ShellOutput("Status: $status", true))
            }
        }
    }

    // Auto-scroll yang responsif
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.lastIndex)
        }
    }

    MaterialExpressiveTheme {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = { 
                BypassChgCheckTopAppBar(
                    scrollBehavior = scrollBehavior, 
                    onBack = { navController.popBackStack() }
                ) 
            },
            floatingActionButton = {
                // FAB hanya muncul saat SELESAI untuk navigasi balik (onBack)
                AnimatedVisibility(
                    visible = !isRunning,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut()
                ) {
                    ExtendedFloatingActionButton(
                        onClick = { navController.popBackStack() },
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary,
                        icon = { Icon(Icons.Rounded.Check, null) },
                        text = { Text("Done") }
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Header Status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isRunning) "Diagnostic in progress" else "Diagnostic complete",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isRunning) "Please wait..." else "All checks finished",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }

                    if (isRunning) {
                        LoadingIndicator(modifier = Modifier.size(36.dp))
                    }
                }

                // Log View
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorScheme.surface
                    )
                ) {
                    SelectionContainer {
                        // Box dengan horizontal scroll agar baris panjang tidak wrap (berantakan)
                        Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(0.dp),
                                contentPadding = PaddingValues(bottom = 120.dp)
                            ) {
                                // ... di dalam LazyColumn ...
                                items(logs) { line ->
                                    Text(
                                        // INI KUNCINYA: Mengubah kode ANSI jadi AnnotatedString
                                        text = line.text.parseAsAnsiAnnotatedString(), 
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            // Kasih line height biar nggak terlalu nempel antar baris
                                            lineHeight = 16.sp 
                                        ),
                                        color = Color.Unspecified, // Biarkan library yang tentuin warna dari ANSI
                                        softWrap = false
                                    )
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
fun BypassChgCheckTopAppBar(scrollBehavior: TopAppBarScrollBehavior, onBack: () -> Unit) {
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
            title = { 
                Text(
                    text = stringResource(R.string.CompatibilityCheck),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                ) 
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
