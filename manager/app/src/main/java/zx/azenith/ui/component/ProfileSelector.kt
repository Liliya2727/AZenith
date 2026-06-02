package zx.azenith.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

// BasicAlertDialog dihapus, menyisakan inner layout (Column) saja
@Composable
fun ProfileDialogContent(
    onDismiss: () -> Unit,
    onProfile: (String) -> Unit
) {
    val options = getProfileOptions()

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
                ExpressiveListItem(
                    modifier = Modifier.padding(vertical = 8.dp),
                    headlineContent = {
                        Text(stringResource(option.titleRes))
                    },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
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

@Preview(showBackground = true)
@Composable
private fun ProfileDialogContentPreview() {
    MaterialTheme {
        ProfileDialogContent(
            onDismiss = {},
            onProfile = {}
        )
    }
}
