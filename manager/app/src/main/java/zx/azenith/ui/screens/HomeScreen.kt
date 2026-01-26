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
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val view = LocalView.current
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    val uriHandler = LocalUriHandler.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    val serviceInfo by produceState(initialValue = "Suspended" to "") {
        value = RootUtils.getServiceStatus()
    }
    val currentProfile by produceState(initialValue = "Initializing") {
        value = RootUtils.getCurrentProfile()
    }
    val rootStatus by produceState(initialValue = false) {
        value = RootUtils.isRootGranted()
    }
    
    val moduleInstalled by produceState(initialValue = false) {
        value = RootUtils.isModuleInstalled()
    }

    val listState = rememberLazyListState()

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
            item {
                BannerCard(
                    status = if (!moduleInstalled)
                        stringResource(R.string.module_not_installed)
                    else
                        serviceInfo.first,
                    pid = serviceInfo.second
                ) { }
            }
            
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoTile(
                        Modifier.weight(1f),
                        Icons.Default.Bolt,
                        stringResource(R.string.current_profile),
                        currentProfile,
                        highlight = (currentProfile != stringResource(R.string.status_initializing))
                    ) {}
                    
                    InfoTile(
                        Modifier.weight(1f),
                        Icons.Default.Security,
                        stringResource(R.string.root_access),
                        if (rootStatus)
                            stringResource(R.string.root_granted)
                        else
                            stringResource(R.string.root_not_granted),
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
                Surface(
                    color = when {
                        status == stringResource(R.string.module_not_installed) ->
                            colorScheme.errorContainer
                        status == stringResource(R.string.status_alive) ->
                            colorScheme.secondaryContainer
                        else ->
                            colorScheme.errorContainer
                    },
                    shape = CircleShape
                ) {
                    Text(
                        text = status,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }

                if (status == stringResource(R.string.status_alive)) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = colorScheme.secondaryContainer,
                        shape = CircleShape
                    ) {
                        Text(
                            text = stringResource(R.string.pid_format, pid),
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
                    text = stringResource(R.string.app_name_styled),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
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
                SmallLeadingIcon(Icons.Outlined.Info)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.device_info), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
            }

            Spacer(Modifier.height(8.dp))

            DeviceInfoRow(
                stringResource(R.string.kernel_version),
                kernelVer
            )
            
            DeviceInfoRow(
                stringResource(R.string.device_name),
                "${Build.MANUFACTURER} ${Build.MODEL}"
            )
            
            DeviceInfoRow(
                stringResource(R.string.azenith_version),
                appVer
            )

            if (isExpanded) {
                DeviceInfoRow(stringResource(R.string.fingerprint), Build.FINGERPRINT)
                DeviceInfoRow(stringResource(R.string.selinux_status), selinux)
                DeviceInfoRow(stringResource(R.string.instruction_sets), Build.SUPPORTED_ABIS.joinToString(", "))
                DeviceInfoRow(
                    stringResource(R.string.android_version),
                    "${Build.VERSION.RELEASE} API${Build.VERSION.SDK_INT}"
                )
            }
        }
    }
}



@Composable
fun DeviceInfoRow(title: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
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
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(4.dp))
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
                    stringResource(R.string.learn_more),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                stringResource(R.string.learn_more_desc),
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
                    stringResource(R.string.support_us),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                stringResource(R.string.support_us_desc),
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
