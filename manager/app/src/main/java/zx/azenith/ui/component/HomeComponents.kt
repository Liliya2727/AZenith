@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package zx.azenith.ui.component

import android.os.Build
import android.system.Os
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import dev.chrisbanes.haze.blur.blurEffect
import zx.azenith.R
import zx.azenith.ui.util.getAppVersion
import zx.azenith.ui.util.getChipsetName
import zx.azenith.ui.util.getRealDeviceName
import zx.azenith.ui.util.getHeaderImage
import zx.azenith.ui.util.getSELinuxStatus
import zx.azenith.ui.util.getBannerGradientAlpha

@Composable
fun HomeTopAppBar(scrollBehavior: TopAppBarScrollBehavior, onRebootClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val smoothGradient = Brush.verticalGradient(
        0.0f to colorScheme.surface,
        0.4f to colorScheme.surface.copy(alpha = 0.9f),
        1.0f to Color.Transparent
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(smoothGradient)
            .statusBarsPadding()
    ) {
        LargeFlexibleTopAppBar(
            navigationIcon = {
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
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
            },
            title = { Text(text = stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
            ),
            actions = {
                IconButton(onClick = onRebootClick) {
                    Icon(imageVector = Icons.Filled.PowerSettingsNew, contentDescription = stringResource(R.string.reboot))
                }
            },
            scrollBehavior = scrollBehavior,
            windowInsets = WindowInsets(0, 0, 0, 0)
        )
    }
}

@Composable
fun BannerCard(
    status: String,
    pid: String,
    isBannerEnabled: Boolean,
    isBlurEnabled: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val customBannerUri = remember { context.getHeaderImage() }
    val gradientAlpha = remember { context.getBannerGradientAlpha() }
    val isAlive = status == stringResource(R.string.status_alive)
    
    val bannerHazeState = remember { HazeState() }

    val statusBgColor = if (isAlive) {
        if (isBlurEnabled) colorScheme.secondaryContainer.copy(alpha = 0.45f) else colorScheme.secondaryContainer
    } else {
        if (isBlurEnabled) colorScheme.errorContainer.copy(alpha = 0.45f) else colorScheme.errorContainer
    }

    val statusTextColor = if (isAlive) colorScheme.onSecondaryContainer else colorScheme.onErrorContainer

    if (isBannerEnabled) {
        Card(
            modifier = modifier.clip(RoundedCornerShape(26.dp)).clickable { onClick() },
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (isBlurEnabled) Modifier.hazeSource(state = bannerHazeState) else Modifier)
                ) {
                    if (customBannerUri != null) {
                        AsyncImage(
                            model = customBannerUri, contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.banner_bg), contentDescription = null,
                            modifier = Modifier.fillMaxSize(), 
                            contentScale = ContentScale.Crop
                        )
                    }

                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, colorScheme.surfaceContainerLow.copy(alpha = gradientAlpha))
                            )
                        )
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 24.dp, bottom = 20.dp)
                        .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)), 
                    horizontalAlignment = Alignment.Start
                ) {
                    Surface(
                        color = statusBgColor,
                        shape = CircleShape,
                        modifier = Modifier
                            .clip(CircleShape)
                            .then(if (isBlurEnabled) Modifier.hazeEffect(state = bannerHazeState) { blurEffect { blurRadius = 14.dp } } else Modifier)
                    ) {
                        AnimatedContent(
                            targetState = status,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
                            },
                            label = "BannerStatusTextAnim"
                        ) { targetStatus ->
                            Text(
                                text = targetStatus, 
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), 
                                fontWeight = FontWeight.Bold,
                                color = statusTextColor
                            )
                        }
                    }

                    // Hilangkan expand/shrink, biarkan Column (pembungkusnya) yang meng-animate content size-nya
                    if (isAlive) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = if (isBlurEnabled) colorScheme.secondaryContainer.copy(alpha = 0.45f) else colorScheme.secondaryContainer, 
                            shape = CircleShape,
                            modifier = Modifier
                                .clip(CircleShape)
                                .then(if (isBlurEnabled) Modifier.hazeEffect(state = bannerHazeState) { blurEffect { blurRadius = 14.dp } } else Modifier)
                        ) {
                            AnimatedContent(
                                targetState = pid,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
                                },
                                label = "BannerPidAnim"
                            ) { targetPid ->
                                Text(
                                    text = stringResource(R.string.pid_format, targetPid),
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
    } else {
        // Mode Banner Disabled
        Surface(
            modifier = modifier
                .clip(RoundedCornerShape(26.dp))
                .clickable { onClick() }
                .animateContentSize(animationSpec = spring()), 
            color = colorScheme.secondaryContainer, 
            shape = RoundedCornerShape(26.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically, 
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AnimatedContent(
                    targetState = isAlive,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                    label = "BannerIconAnim"
                ) { alive ->
                    Icon(
                        imageVector = if (alive) Icons.Outlined.CheckCircle else Icons.Rounded.ErrorOutline,
                        contentDescription = null, 
                        tint = colorScheme.onSecondaryContainer, 
                        modifier = Modifier.size(42.dp)
                    )
                }
                
                Box(modifier = Modifier.height(42.dp).width(1.5.dp).background(colorScheme.onSecondaryContainer.copy(alpha = 0.3f)))
                
                Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.Start) {
                    AnimatedContent(
                        targetState = status,
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                        label = "BannerStatusAnimNoImage"
                    ) { targetStatus ->
                        Text(text = targetStatus, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colorScheme.onSecondaryContainer)
                    }

                    if (isAlive) {
                        Spacer(modifier = Modifier.height(2.dp))
                        AnimatedContent(
                            targetState = pid,
                            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                            label = "BannerPidAnimNoImage"
                        ) { targetPid ->
                            Text(
                                text = stringResource(R.string.pid_format, targetPid),
                                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium,
                                color = colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun InfoTile(
    modifier: Modifier, 
    icon: ImageVector, 
    label: String, 
    value: String, 
    highlight: Boolean,
    showArrow: Boolean = false, 
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    
    // Warna untuk background luar card (mengikuti gambar yang gelap/surface)
    val cardBgColor = colorScheme.surfaceColorAtElevation(1.dp)
    
    // Warna untuk Box Icon gede di atas
    val iconBoxBgColor = if (highlight) colorScheme.primaryContainer else colorScheme.surfaceVariant
    val iconColor = if (highlight) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .clickable { onClick() }
            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)),
        color = cardBgColor,
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp) // Padding dalam card keseluruhan
        ) {
            // 1. Box Icon (Top Section)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.4f) // Bikin proporsinya jadi persegi panjang (landscape)
                    .clip(RoundedCornerShape(18.dp)) // Sudut inner box sedikit lebih kecil dari outer card
                    .background(iconBoxBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon, 
                    contentDescription = null, 
                    tint = iconColor,
                    modifier = Modifier.size(36.dp) // Ukuran icon lebih besar biar seimbang
                )

                // Kalau ada arrow, taruh di pojok kanan atas box icon-nya
                if (showArrow) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight, 
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(20.dp), 
                        tint = iconColor.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            
            // 2. Teks Area (Bottom Section)
            Column(
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                // Judul (Label) -> Bold
                Text(
                    text = label, 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Deskripsi (Value) -> Dimmed & Smaller
                AnimatedContent(
                    targetState = value,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
                    },
                    label = "ValueTextAnimation"
                ) { targetValue ->
                    Text(
                        text = targetValue, 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = colorScheme.onSurfaceVariant, // Bikin teksnya agak redup
                        maxLines = 2, 
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}



@Composable
fun DeviceInfoCard() {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    var isExpanded by remember { mutableStateOf(false) }
    
    val uname = remember { Os.uname() }
    val kernelVer = remember { uname.release }
    val selinux = remember { getSELinuxStatus(context) }
    val appVer = remember { getAppVersion(context) }
    val chipsetName = remember { getChipsetName(context) }

    var realDeviceName by remember { mutableStateOf("${Build.MANUFACTURER} ${Build.MODEL}") }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            realDeviceName = getRealDeviceName(context)
        }
    }

    Surface(
        shape = RoundedCornerShape(26.dp), color = colorScheme.surfaceColorAtElevation(1.dp),
        onClick = { isExpanded = !isExpanded }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp)
                .animateContentSize(animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow))
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                SmallLeadingIcon(Icons.Outlined.Info)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.device_info), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Icon(if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
            }

            DeviceInfoRow(stringResource(R.string.kernel_version), kernelVer)
            
            DeviceInfoRow(stringResource(R.string.device_name), realDeviceName)
            
            DeviceInfoRow("Chipset", chipsetName)
            DeviceInfoRow(stringResource(R.string.azenith_version), appVer)

            if (isExpanded) {
                DeviceInfoRow(stringResource(R.string.fingerprint), Build.FINGERPRINT)
                DeviceInfoRow(stringResource(R.string.selinux_status), selinux)
                DeviceInfoRow(stringResource(R.string.instruction_sets), Build.SUPPORTED_ABIS.joinToString(", "))
                DeviceInfoRow(stringResource(R.string.android_version), "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            }
        }
    }
}


@Composable
fun DeviceInfoRow(title: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun LinkCard(icon: ImageVector, titleRes: Int, descRes: Int, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(26.dp)
    Surface(
        shape = shape, color = colorScheme.surfaceColorAtElevation(1.dp),
        modifier = Modifier.fillMaxWidth().clip(shape).clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SmallLeadingIcon(icon)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
            }
            Spacer(Modifier.height(14.dp))
            Text(stringResource(descRes), style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun RebootBottomSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    onReboot: (String) -> Unit
) {
    if (!show) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val context = LocalContext.current
    val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager?
    
    @Suppress("DEPRECATION")
    val isUserspaceSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && pm?.isRebootingUserspaceSupported == true

    val options = buildList<Triple<String, String, ImageVector>> {
        add(Triple("Reboot", "", Icons.Outlined.Refresh))
        if (isUserspaceSupported) add(Triple("Reboot to Userspace", "userspace", Icons.Outlined.RestartAlt))
        add(Triple("Soft Reboot", "soft_reboot", Icons.Outlined.Refresh))
        add(Triple("Recovery", "recovery", Icons.Outlined.SystemUpdate))
        add(Triple("Bootloader", "bootloader", Icons.Outlined.Memory))
        add(Triple("Download", "download", Icons.Outlined.Download))
        add(Triple("EDL", "edl", Icons.Outlined.DeveloperMode))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp)
        ) {
            Text(
                text = stringResource(R.string.reboot),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            ExpressiveList(
                modifier = Modifier.padding(horizontal = 16.dp),
                content = options.map { (label, reason, icon) ->
                    {
                        ExpressiveListItem(
                            headlineContent = { Text(label) },
                            leadingContent = { SmallLeadingIcon(icon) },
                            onClick = {
                                onDismiss()
                                onReboot(reason)
                            }
                        )
                    }
                }
            )
        }
    }
}
