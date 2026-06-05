package zx.azenith.ui.component

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.blur.blurEffect
import zx.azenith.R

private data class ProfileOption(
    val titleRes: Int,
    val reason: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
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

    // Cek show di sini, sisanya biarkan HomeScreen yang mengatur animasi dan posisinya
    if (show) {
        // Tangkap tombol back agar tidak menutup layar lain di bawahnya
        BackHandler(onBack = onDismiss)

        // Kotak Dialog Kustom (Langsung ke komponen inti)
        Box(
            modifier = Modifier
                .widthIn(min = 320.dp, max = 400.dp) 
                // Catatan: padding luar dihapus agar animasi SharedBounds pas dengan tepi card
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
                // Cegah klik tembus ke bawah
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp) // Padding dalam untuk konten tetap dipertahankan
            ) {
                Text(
                    text = stringResource(R.string.RefreshRatePicker_Select), // Atau pakai R.string.Profile_Select jika ada
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val content = options.map { option ->
                    @Composable {
                        ExpressiveListItem(
                            modifier = Modifier.padding(vertical = 4.dp),
                            headlineContent = {
                                Text(
                                    text = stringResource(option.titleRes),
                                    color = MaterialTheme.colorScheme.onSurface 
                                )
                            },
                            leadingContent = { 
                                SmallLeadingIcon(icon = option.icon) 
                            },
                            onClick = {
                                onDismiss() // Tutup via state di HomeScreen
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
