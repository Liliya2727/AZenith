package zx.azenith.ui.screens

import android.os.Build
import android.system.Os
import android.app.Activity
import androidx.compose.ui.platform.LocalUriHandler
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import zx.azenith.R
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import zx.azenith.ui.util.*
import androidx.compose.material.icons.rounded.*
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val view = LocalView.current
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    val uriHandler = LocalUriHandler.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    // --- LOGIKA SISTEM MODULAR ---
    val serviceInfo by produceState(initialValue = "Suspended" to "") {
        value = RootUtils.getServiceStatus()
    }
    val currentProfile by produceState(initialValue = "Initializing") {
        value = RootUtils.getCurrentProfile()
    }
    val rootStatus by produceState(initialValue = false) {
        value = RootUtils.isRootGranted()
    }

    val listState = rememberLazyListState()

    // Setiap kali masuk ke screen ini, scroll ke index 0
    LaunchedEffect(Unit) {
        listState.scrollToItem(0)
    }
    
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { HomeTopAppBar(scrollBehavior = scrollBehavior) },
        containerColor = MaterialTheme.colorScheme.surface 
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = 16.dp, start = 16.dp, end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mengirimkan status dan pid asli ke Banner
            item { 
                BannerCard(status = serviceInfo.first, pid = serviceInfo.second) { } 
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoTile(
                        Modifier.weight(1f), 
                        Icons.Default.Bolt, 
                        "Current Profile", 
                        currentProfile, 
                        highlight = (currentProfile != "Unknown" && currentProfile != "Initializing")
                    ) {}
                    
                    InfoTile(
                        Modifier.weight(1f), 
                        Icons.Default.Security, 
                        "Root Access", 
                        if (rootStatus) "Granted" else "Not Granted", 
                        highlight = false
                    ) {}
                }
            }
            item { DeviceInfoCard() }
            item { SupportCard { uriHandler.openUri("https://t.me/ZeshArch") } }
            item { LearnMoreCard { uriHandler.openUri("https://github.com/Liliya2727/AZenith") } }
        }
    }
}


@Composable
fun BannerCard(status: String, pid: String, onClick: () -> Unit) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    
    // Ambil URI custom banner jika ada
    val customBannerUri = remember { context.getHeaderImage() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(20 / 9f)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box {
            // LOGIKA GAMBAR: Jika ada custom URI, gunakan AsyncImage. Jika tidak, pakai Default.
            if (customBannerUri != null) {
                AsyncImage(
                    model = customBannerUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.banner_bg),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
                        

            // Overlay Gradient (Agar teks tetap terbaca jelas)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                colorScheme.surfaceColorAtElevation(3.dp).copy(alpha = 0.85f)
                            )
                        )
                    )
            )            
                        
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Perbaikan Sintaksis Surface di sini
                Surface(
                    color = if (status == "Alive") colorScheme.secondaryContainer 
                            else colorScheme.errorContainer, // Gunakan errorContainer agar lebih aman
                    shape = CircleShape
                ) {
                    Text(
                        text = status,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (status == "Alive") colorScheme.onSecondaryContainer 
                                else colorScheme.onErrorContainer
                    )
                }

                if (status == "Alive") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = colorScheme.secondaryContainer,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "PID: $pid",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopAppBar(scrollBehavior: TopAppBarScrollBehavior) {
    // TopAppBar bawaan otomatis menangani transisi warna kontainer saat scroll
    TopAppBar(
        title = {
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
                    "AZenith火",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        // Ini kuncinya: Warna status bar akan mengikuti warna yang diatur di sini
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface, // Warna awal
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer // Warna saat scroll
        ),
        scrollBehavior = scrollBehavior,
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    )
}

@Composable
fun DeviceInfoCard() {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    var isExpanded by remember { mutableStateOf(false) }
    val uname = Os.uname()

    // State untuk menyimpan data agar tidak di-load berulang kali saat recomposition
    val kernelVer = remember { uname.release }
    val selinux = remember { getSELinuxStatus() }
    val appVer = remember { getAppVersion(context) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colorScheme.surfaceColorAtElevation(1.dp),
        onClick = { isExpanded = !isExpanded }
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .animateContentSize(animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow))
        ) {
            // HEADER
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmallLeadingIcon(Icons.Outlined.Info) // Gunakan Outlined ala KSU
                Spacer(Modifier.width(12.dp))
                Text("Device Info", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
            }

            Spacer(Modifier.height(8.dp))

            // INFO UTAMA (Real Data)
            DeviceInfoRow("Kernel Version", kernelVer)
            DeviceInfoRow("Device Name", "${Build.MANUFACTURER} ${Build.MODEL}")
            DeviceInfoRow("AZenith Version", appVer)

            // INFO TAMBAHAN (Sembunyi)
            if (isExpanded) {
                DeviceInfoRow("Fingerprint", Build.FINGERPRINT)
                DeviceInfoRow("SELinux Status", selinux)
                DeviceInfoRow("Instruction Sets", Build.SUPPORTED_ABIS.joinToString(", "))
                DeviceInfoRow("Android Version", "${Build.VERSION.RELEASE} API${Build.VERSION.SDK_INT}")
            }
        }
    }
}



@Composable
fun DeviceInfoRow(title: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp) // LEBIH MEPET
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
        Text(
            value,
            
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
} 

@Composable
fun InfoTile(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    highlight: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        color = if (highlight)
            colorScheme.secondaryContainer
        else
            colorScheme.surfaceColorAtElevation(1.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp) // ✅ padding dikurangi
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(20.dp) // icon sedikit lebih kecil
            )
            Spacer(Modifier.height(4.dp)) // ✅ jarak antara icon & label dikurangi
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
        }
    }
}

@Composable
fun LearnMoreCard(onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)

    Surface(
        shape = shape,
        color = colorScheme.surfaceColorAtElevation(1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape) // ⬅️ WAJIB
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SmallLeadingIcon(Icons.Default.Info)

                Spacer(Modifier.width(12.dp))

                Text(
                    "Learn More",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                "Documentation, source code, changelogs, and detailed explanations about how AZenith works under the hood. Learn how features are implemented and how to customize them safely.",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SupportCard(onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)

    Surface(
        shape = shape,
        color = colorScheme.surfaceColorAtElevation(1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SmallLeadingIcon(Icons.Default.Favorite)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Support Us",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp)) // jarak teks dikurangi

            Text(
                "Help support development and keep AZenith improving.",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingRow(title: String, subtitle: String) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, null, tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun SmallLeadingIcon(icon: ImageVector) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = cs.primary.copy(alpha = 0.12f),
        modifier = Modifier.size(32.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = cs.primary,
            modifier = Modifier
                .padding(7.dp)
                .size(18.dp)
        )
    }
}
