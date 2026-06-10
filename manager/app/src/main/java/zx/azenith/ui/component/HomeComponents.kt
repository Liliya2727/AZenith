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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import zx.azenith.R
import zx.azenith.ui.util.getAppVersion
import zx.azenith.ui.util.getChipsetName
import zx.azenith.ui.util.getRealDeviceName
import zx.azenith.ui.util.getHeaderImage
import zx.azenith.ui.util.getSELinuxStatus
import zx.azenith.ui.util.getBannerGradientAlpha
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.viewinterop.AndroidView
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import android.graphics.SurfaceTexture
import android.graphics.Matrix
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import android.content.ContentResolver
import android.net.Uri
import android.webkit.MimeTypeMap
import android.view.ViewGroup


@Composable
fun MediaBannerRenderer(
    uriString: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (uriString == null) {
        Image(
            painter = painterResource(id = R.drawable.banner_bg),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
        return
    }

    val uri = Uri.parse(uriString)
    val mimeType = remember(uriString) {
        val extension = MimeTypeMap.getFileExtensionFromUrl(uriString)
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) 
            ?: context.contentResolver.getType(uri) ?: ""
    }

    val isVideo = mimeType.startsWith("video/")

    if (isVideo) {
        AndroidView(
            factory = { ctx ->
                val textureView = TextureView(ctx)
                
                textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    var mediaPlayer: MediaPlayer? = null

                    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                        val surface = Surface(surfaceTexture)
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(ctx, uri)
                            setSurface(surface)
                            isLooping = true
                            setVolume(0f, 0f)

                            setOnVideoSizeChangedListener { _, videoWidth, videoHeight ->
                                val viewWidth = textureView.width.toFloat()
                                val viewHeight = textureView.height.toFloat()
                                
                                if (viewWidth == 0f || viewHeight == 0f || videoWidth == 0 || videoHeight == 0) return@setOnVideoSizeChangedListener

                                val videoRatio = videoWidth.toFloat() / videoHeight.toFloat()
                                val viewRatio = viewWidth / viewHeight

                                val scaleX: Float
                                val scaleY: Float

                                if (videoRatio > viewRatio) {
                                    scaleX = (viewHeight * videoRatio) / viewWidth
                                    scaleY = 1f
                                } else {
                                    scaleX = 1f
                                    scaleY = (viewWidth / videoRatio) / viewHeight
                                }

                                val matrix = Matrix()
                                matrix.setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
                                // Pasang matrix-nya ke textureView
                                textureView.setTransform(matrix)
                            }

                            prepareAsync() 
                            setOnPreparedListener { start() } 
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                    
                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        mediaPlayer?.release()
                        mediaPlayer = null
                        return true
                    }
                    
                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                }
                
                // Return TextureView-nya
                textureView
            },
            modifier = modifier
        )
    } else {
        val imageLoader = remember {
            ImageLoader.Builder(context)
                .components {
                    if (Build.VERSION.SDK_INT >= 28) {
                        add(ImageDecoderDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                }
                .build()
        }

        AsyncImage(
            model = uri,
            imageLoader = imageLoader,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}



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
                    MediaBannerRenderer(
                        uriString = customBannerUri,
                        modifier = Modifier.fillMaxSize()
                    )
                
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

    val cardBgColor = colorScheme.surfaceColorAtElevation(1.dp)

    val iconBoxBgColor by animateColorAsState(
        targetValue = if (highlight) colorScheme.primaryContainer else colorScheme.surfaceVariant.copy(alpha = 0.5f),
        animationSpec = tween(400), 
        label = "iconBoxBgColorAnim"
    )

    val iconColor by animateColorAsState(
        targetValue = if (highlight) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
        animationSpec = tween(400),
        label = "iconColorAnim"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .clickable { onClick() }
            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)),
        color = cardBgColor,
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp) 
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp) 
                    .clip(RoundedCornerShape(18.dp)) 
                    .background(iconBoxBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon, 
                    contentDescription = null, 
                    tint = iconColor,
                    modifier = Modifier.size(36.dp) 
                )

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
            
            Column(
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = label, 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                AnimatedContent(
                    targetState = value,
                    transitionSpec = {
                        // Kombinasi fade in + sedikit scale up untuk masuk
                        // dan fade out + sedikit scale down untuk keluar (bikin efek "luntur/ngeblur")
                        (fadeIn(animationSpec = tween(300, delayMillis = 100)) +
                         scaleIn(initialScale = 0.95f, animationSpec = tween(300, delayMillis = 100)))
                            .togetherWith(
                                fadeOut(animationSpec = tween(200)) +
                                scaleOut(targetScale = 1.05f, animationSpec = tween(200))
                            )
                    },
                    label = "ValueTextAnimation"
                ) { targetValue ->
                    Text(
                        text = targetValue, 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = colorScheme.onSurfaceVariant, 
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

    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "expandArrowRotation"
    )

    Surface(
        shape = RoundedCornerShape(26.dp), 
        color = colorScheme.surfaceColorAtElevation(1.dp),
        onClick = { isExpanded = !isExpanded }
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .animateContentSize(animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow))
        ) {
            // Header Info Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp), 
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmallLeadingIcon(Icons.Outlined.Info)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.device_info), 
                    modifier = Modifier.weight(1f), 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Rounded.ExpandMore, 
                    contentDescription = null,
                    modifier = Modifier.graphicsLayer { 
                        rotationZ = rotationAngle
                    }
                )
            }

            Spacer(Modifier.height(12.dp))

            // Grid Container
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp) 
            ) {
                // Baris 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp) 
                ) {
                    DeviceInfoGridItem(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.kernel_version), 
                        value = kernelVer
                    )
                    DeviceInfoGridItem(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.device_name), 
                        value = realDeviceName
                    )
                }

                // Baris 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DeviceInfoGridItem(
                        modifier = Modifier.weight(1f),
                        title = "Chipset", 
                        value = chipsetName
                    )
                    DeviceInfoGridItem(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.azenith_version), 
                        value = appVer
                    )
                }

                if (isExpanded) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp) // Jaraknya langsung diatur di sini
                    ) {
                        // ❌ HAPUS Spacer yang ada di sini!
                        
                        // Baris 3
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DeviceInfoGridItem(
                                modifier = Modifier.weight(1f),
                                title = stringResource(R.string.fingerprint), 
                                value = Build.FINGERPRINT
                            )
                            DeviceInfoGridItem(
                                modifier = Modifier.weight(1f),
                                title = stringResource(R.string.selinux_status), 
                                value = selinux
                            )
                        }

                        // Baris 4
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DeviceInfoGridItem(
                                modifier = Modifier.weight(1f),
                                title = stringResource(R.string.instruction_sets), 
                                value = Build.SUPPORTED_ABIS.joinToString(", ")
                            )
                            DeviceInfoGridItem(
                                modifier = Modifier.weight(1f),
                                title = stringResource(R.string.android_version), 
                                value = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun DeviceInfoGridItem(modifier: Modifier = Modifier, title: String, value: String) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        // Cukup pakai modifier yang di-passing (karena sudah ada weight(1f) dari luar),
        // ditambah aspectRatio untuk bentuk konsisten.
        modifier = modifier.height(86.dp),
        color = colorScheme.surfaceVariant.copy(alpha = 0.5f), 
        shape = RoundedCornerShape(18.dp) 
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(), // Pastikan Column memenuhi seluruh ruang dalam Surface
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start // Pastikan teks rata kiri
        ) {
            Text(
                text = title, 
                style = MaterialTheme.typography.labelSmall, // Balikin ke labelMedium agar proporsional
                color = colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp)) // Jarak title dan value dibesarkan dikit
            
            // Box untuk membungkus Value, agar dia ngambil semua sisa space ke bawah
            Box(
                modifier = Modifier.weight(1f), 
                contentAlignment = Alignment.TopStart
            ) {
                Text(
                    text = value, 
                    style = MaterialTheme.typography.titleSmall, // Balikin ke titleSmall
                    fontWeight = FontWeight.Bold, 
                    color = colorScheme.onSurface,
                    maxLines = 2, 
                    // Kalau ruangannya lega tapi sering kepotong "...",
                    // hapus saja overflow = TextOverflow.Ellipsis
                    // Biarkan Compose yang motong secara alami
                    lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified // Bebaskan line height
                )
            }
        }
    }
}



@Composable
fun LinkCard(icon: ImageVector, titleRes: Int, descRes: Int, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(26.dp)
    
    Surface(
        shape = shape, 
        color = colorScheme.surfaceColorAtElevation(1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp), // Padding disesuaikan biar proporsional
            verticalAlignment = Alignment.CenterVertically // Bikin icon dan teks sejajar di tengah
        ) {
            // 1. Icon Sebelah Kiri
            SmallLeadingIcon(icon)
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 2. Teks Judul & Deskripsi di Tengah (weight = 1f biar dorong icon kanan ke ujung)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(titleRes), 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.SemiBold, 
                    color = colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = stringResource(descRes), 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 3. Icon panah/open link kecil di Kanan
            Icon(
                imageVector = Icons.Rounded.OpenInNew, // Bisa diganti Icons.Rounded.ChevronRight kalau lebih suka panah
                contentDescription = "Open Link",
                modifier = Modifier.size(22.dp),
                tint = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RebootBottomSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    onReboot: (String) -> Unit
) {

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

    // Gunakan CustomBottomSheet yang baru kita buat
    CustomBottomSheet(
        visible = show,
        onDismiss = onDismiss
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
                // 👇 UBAH BAGIAN INI (Gunakan start, end, bottom)
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
            )


            ExpressiveList(
                modifier = Modifier.padding(horizontal = 16.dp),
                content = options.map { (label, reason, icon) ->
                    {
                        ExpressiveListItem(
                            // 👇 Jangan lupa suntik warna biar gak hitam di Dark Mode
                            headlineContent = { 
                                Text(text = label, color = MaterialTheme.colorScheme.onSurface) 
                            },
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

@Composable
fun RunningGameCard(
    pkgName: String,
    startTimeStr: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val density = LocalDensity.current
    
    // 1. Cek apakah ini mode CLI (Tidak ada Foreground App)
    val isNoApp = pkgName == "(null)" || pkgName.equals("null", ignoreCase = true) || pkgName.isEmpty()
    
    // 2. Ambil AppInfo & Nama
    val appInfo = remember(pkgName, isNoApp) {
        if (isNoApp) null else try { pm.getApplicationInfo(pkgName, 0) } catch (e: Exception) { null }
    }
    val appName = remember(appInfo, isNoApp) {
        if (isNoApp) "Performance profile active" else appInfo?.loadLabel(pm)?.toString() ?: pkgName
    }

    // 3. Siapkan State untuk Icon Bitmap
    var appIconBitmap by remember(pkgName) { 
        mutableStateOf(if (isNoApp) null else AppIconCache.get(pkgName)) 
    }
    
    val targetSizePx = remember(density) { 
        with(density) { 54.dp.roundToPx() } 
    }

    LaunchedEffect(pkgName, isNoApp) {
        if (!isNoApp && appIconBitmap == null && appInfo != null) {
            try {
                appIconBitmap = AppIconCache.loadIcon(pm, appInfo, targetSizePx)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 4. Logika Timer
    var elapsedTime by remember { mutableStateOf("00:00:00") }
    
    LaunchedEffect(startTimeStr) {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        val startParsed = try { sdf.parse(startTimeStr) } catch(e: Exception) { null }
        
        if (startParsed != null) {
            while (true) {
                val now = java.util.Calendar.getInstance()
                val start = java.util.Calendar.getInstance().apply {
                    time = startParsed
                    set(java.util.Calendar.YEAR, now.get(java.util.Calendar.YEAR))
                    set(java.util.Calendar.MONTH, now.get(java.util.Calendar.MONTH))
                    set(java.util.Calendar.DAY_OF_MONTH, now.get(java.util.Calendar.DAY_OF_MONTH))
                }
                
                var diff = now.timeInMillis - start.timeInMillis
                if (diff < 0) diff += 24 * 60 * 60 * 1000
                
                val h = diff / 3600000
                val m = (diff / 60000) % 60
                val s = (diff / 1000) % 60
                
                elapsedTime = String.format("%02d:%02d:%02d", h, m, s)
                kotlinx.coroutines.delay(1000)
            }
        } else {
            elapsedTime = "--:--:--"
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .then(
                // Hilangkan interaksi "Clickable" jika tidak ada game
                if (!isNoApp) {
                    Modifier.clickable {
                        val intent = pm.getLaunchIntentForPackage(pkgName)
                        if (intent != null) {
                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        }
                    }
                } else Modifier
            ),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(26.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            
            Box(
                modifier = Modifier.size(64.dp), // Ukuran diperbesar dikit buat spasi Wavy Indicator
                contentAlignment = Alignment.Center
            ) {
                if (!isNoApp) {
                    // Wavy border mengelilingi icon
                    CircularWavyProgressIndicator(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primary // Sesuaikan warnanya kalau perlu
                    )
                    
                    // Render Icon Game di tengah Wavy Indicator
                    Crossfade(
                        targetState = appIconBitmap,
                        animationSpec = tween(durationMillis = 200),
                        label = "GameIconFade"
                    ) { icon ->
                        if (icon == null) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp) // Ukuran icon lebih kecil dari container supaya Wavy-nya kelihatan
                                    .clip(CircleShape) // Bikin bulat
                                    .background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f))
                            )
                        } else {
                            Image(
                                bitmap = icon,
                                contentDescription = appName,
                                contentScale = ContentScale.Crop, // Pastikan nge-crop jadi lingkaran sempurna
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape) // Bikin bulat
                            )
                        }
                    }
                } else {
                    ContainedLoadingIndicator()
                    
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            // Text Detail Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Running for $elapsedTime",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
            
            // Tombol Return to Game HANYA jika ada game
            if (!isNoApp) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Return",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}
