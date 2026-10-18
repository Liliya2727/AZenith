package zx.azenith.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Android16BatteryIcon(
    level: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier,
    trackColor: Color = Color(0xFF8E8E93),
    chargingColor: Color = Color(0xFF22C55E),
    normalColor: Color = Color.White,
    lowColor: Color = Color(0xFFEF4444)
) {
    val clampedLevel = level.coerceIn(0, 100)
    val isLow = clampedLevel <= 20 && !isCharging

    val fillColor = when {
        isCharging -> chargingColor
        isLow -> lowColor
        else -> normalColor
    }

    val textColor = when {
        isCharging -> Color(0xFF003803)
        isLow -> Color.White
        else -> Color(0xFF161616)
    }

    val bodyWidth = 35.dp
    val bodyHeight = 17.5.dp

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (isCharging) 0.dp else 1.2.dp)
    ) {
        // Battery Body
        Box(
            modifier = Modifier
                .width(bodyWidth)
                .height(bodyHeight)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cornerRadius = CornerRadius(size.height * 0.28f, size.height * 0.28f)

                // 1. Draw track
                drawRoundRect(
                    color = trackColor,
                    size = size,
                    cornerRadius = cornerRadius
                )

                // 2. Draw fill
                val fillWidth = size.width * (clampedLevel / 100f)
                if (fillWidth > 0f) {
                    val clipPath = Path().apply {
                        addRoundRect(
                            RoundRect(
                                rect = Rect(Offset.Zero, size),
                                cornerRadius = cornerRadius
                            )
                        )
                    }
                    clipPath(clipPath) {
                        drawRect(
                            color = fillColor,
                            topLeft = Offset.Zero,
                            size = Size(fillWidth, size.height)
                        )
                    }
                }
            }

            // Numeric percentage text inside the pill
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val (fontSize, letterSpacing) = when {
                    clampedLevel == 100 -> 11.5.sp to (-0.5).sp
                    clampedLevel >= 10 -> 12.5.sp to (-0.2).sp
                    else -> 12.5.sp to 0.sp
                }
                Text(
                    text = "$clampedLevel",
                    style = TextStyle(
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeight = fontSize,
                        letterSpacing = letterSpacing
                    ),
                    color = textColor,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Terminal Cap or Charging Lightning Bolt
        if (isCharging) {
            Icon(
                imageVector = Icons.Rounded.Bolt,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(16.dp)
                    .offset(x = (-1.5).dp)
            )
        } else {
            Surface(
                shape = RoundedCornerShape(
                    topEnd = 1.2.dp,
                    bottomEnd = 1.2.dp,
                    topStart = 0.5.dp,
                    bottomStart = 0.5.dp
                ),
                color = trackColor,
                modifier = Modifier
                    .width(2.dp)
                    .height(7.dp)
            ) {}
        }
    }
}
