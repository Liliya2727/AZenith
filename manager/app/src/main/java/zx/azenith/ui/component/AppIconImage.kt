package zx.azenith.ui.component

import android.content.pm.PackageInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import zx.azenith.ui.viewmodel.ApplistViewmodel
@Composable
fun AppIconImage(
    packageName: String,
    size: Dp = 40.dp
) {
    val context = LocalContext.current

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(
                ApplistViewmodel.getAppIconDrawable(
                    context,
                    packageName
                )
            )
            .crossfade(false)
            .build(),
        contentDescription = null,
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
    )
}

