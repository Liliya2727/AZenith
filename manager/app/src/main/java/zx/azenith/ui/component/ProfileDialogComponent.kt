package zx.azenith.ui.component

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.blur.blurEffect
import zx.azenith.R

private data class ProfileOption(
    val titleRes: Int,
    val reason: String,
    val icon: ImageVector
)

@Composable
private fun getProfileOptions(): List<ProfileOption> {
    return listOf(
        ProfileOption(R.string.Profile_Balanced, "2", Icons.Outlined.Water),
        ProfileOption(R.string.Profile_Performance, "1", Icons.Outlined.OfflineBolt),
        ProfileOption(R.string.Profile_ECO_mode, "3", Icons.Outlined.EnergySavingsLeaf),
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onProfile: (String) -> Unit
) {
    val context = LocalContext.current
    val settingsPrefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val isBlurEnabled = settingsPrefs.getBoolean("expressive_blur_ui", false)
    val hazeState = LocalAppHazeState.current
    val options = getProfileOptions()

    // 1. Animasi keluar-masuk
    AnimatedVisibility(
        visible = show,
        enter = fadeIn(animationSpec = tween(250, easing = LinearOutSlowInEasing)),
        exit = fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing))
    ) {
        // Tangkap tombol back agar tidak menutup layar lain di bawahnya
        BackHandler(onBack = onDismiss)
        
        // 2. Scrim (Latar gelap) yang bisa diklik untuk menutup dialog
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(100f) 
                .background(Color.Black.copy(alpha = 0.42f)) 
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss 
                ),
            contentAlignment = Alignment.Center
        ) {
            val scale by animateFloatAsState(
                targetValue = if (show) 1f else 0.9f,
                animationSpec = tween(250, easing = LinearOutSlowInEasing),
                label = "dialog_scale"
            )

            // 3. Kotak Dialog Kustom dengan Blur (Menggantikan BasicAlertDialog)
            Box(
                modifier = Modifier
                    .widthIn(min = 320.dp, max = 400.dp) 
                    .padding(24.dp) 
                    .scale(scale)
                    .clip(RoundedCornerShape(28.dp))
                    .then(
                        if (isBlurEnabled && hazeState != null) {
                            Modifier.hazeEffect(state = hazeState) { blurEffect { blurRadius = 24.dp } }
                        } else Modifier
                    )
                    .background(
                        if (isBlurEnabled) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.35f) 
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    // Cegah klik tembus ke scrim saat mengklik area kosong di dalam dialog
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.Profile_Select),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface, // 👈 TAMBAHKAN INI
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    val content = options.map { option ->
                        @Composable {
                            ExpressiveListItem(
                                modifier = Modifier.padding(vertical = 4.dp),
                                headlineContent = {
                                    Text(
                                        text = stringResource(option.titleRes),
                                        color = MaterialTheme.colorScheme.onSurface // 👈 TAMBAHKAN INI
                                    )
                                },
                                leadingContent = { 
                                    SmallLeadingIcon(icon = option.icon) 
                                },
                                onClick = {
                                    onDismiss()
                                    onProfile(option.reason)
                                }
                            )
                        }
                    }

                    ExpressiveColumn(
                        modifier = Modifier.fillMaxWidth(),
                        content = content
                    )
                }

            }
        }
    }
}
