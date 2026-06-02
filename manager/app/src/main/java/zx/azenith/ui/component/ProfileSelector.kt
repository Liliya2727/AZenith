package zx.azenith.ui.component

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.activity.compose.BackHandler
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.ProfileDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onProfile: (String) -> Unit,
    hazeState: HazeState? = null 
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val isBlurEnabled = remember { prefs.getBoolean("is_blur_enabled", false) }

    val options = getProfileOptions()
    val dialogShape = RoundedCornerShape(28.dp)
    
    val containerColor = if (isBlurEnabled) {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    if (show) {
        BackHandler { onDismiss() }
    }

    // Gunakan AnimatedVisibility untuk memicu masuknya dialog
    AnimatedVisibility(
        visible = show,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(250))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(100f)
                .background(Color.Black.copy(alpha = 0.2f)) // Scrim belakang meredup
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onDismiss() })
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = dialogShape,
                color = if (isBlurEnabled && hazeState != null) Color.Transparent else containerColor,
                modifier = Modifier
                    // LOGIKA SIZE: Sama dengan BasicAlertDialog
                    .widthIn(min = 280.dp, max = 400.dp)
                    .padding(horizontal = 24.dp)
                    .pointerInput(Unit) { detectTapGestures { /* Do nothing */ } }
                    // SHARED ELEMENT TRANSITION: Ini yang bikin animasi keluar dari card
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "profile_card_transition"),
                        animatedVisibilityScope = this@AnimatedVisibility,
                        boundsTransform = { _, _ ->
                            spring(
                                dampingRatio = 0.8f,
                                stiffness = 300f
                            )
                        }
                    )
                    // FIX BLUR NGOTAK: Shape dimasukkan ke parameter Haze
                    .then(
                        if (isBlurEnabled && hazeState != null) {
                            Modifier.hazeChild(
                                state = hazeState,
                                shape = dialogShape, // <--- Ini kuncinya
                                style = HazeStyle(
                                    backgroundColor = containerColor,
                                    blurRadius = 24.dp,
                                    tint = HazeTint(Color.Black.copy(alpha = 0.1f))
                                )
                            )
                        } else {
                            Modifier
                        }
                    )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.Profile_Select),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val content = options.map { option ->
                        @Composable
                        {
                            ExpressiveListItemHighlight(
                                modifier = Modifier.padding(vertical = 8.dp),
                                containerColor = Color.Transparent, 
                                headlineContent = { Text(stringResource(option.titleRes)) },
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.secondaryContainer.let {
                                                    if (isBlurEnabled) it.copy(alpha = 0.6f) else it
                                                },
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = option.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                },
                                onClick = {
                                    onDismiss()
                                    onProfile(option.reason)
                                }
                            )
                        }
                    }

                    ExpressiveColumn(
                        modifier = Modifier.padding(top = 20.dp),
                        content = content
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileDialogPreview() {
    MaterialTheme {
        ProfileDialog(
            show = true,
            onDismiss = {},
            onProfile = {}
        )
    }
}
