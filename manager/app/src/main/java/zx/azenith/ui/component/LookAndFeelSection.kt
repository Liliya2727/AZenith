package zx.azenith.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brightness1
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.CropOriginal
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import zx.azenith.R
import zx.azenith.ui.theme.AccentIntensity
import zx.azenith.ui.theme.BannerShape
import zx.azenith.ui.theme.MotionScale
import zx.azenith.ui.theme.Personalization
import zx.azenith.ui.theme.ShapeScale
import zx.azenith.ui.theme.withAccentIntensity

/**
 * Shape, text size, motion and content contrast.
 *
 * Split out of CustomThemeScreen because that file already carries the whole
 * settings list and this section is independently reusable by the quick-settings
 * tile. Every control reports through [onPersonalizationChange] instead of
 * touching SharedPreferences, so the screen stays the owner of persistence.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LookAndFeelSection(
    pers: Personalization,
    onPersonalizationChange: (Personalization) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel(R.string.pers_look)
        SectionCaption(R.string.pers_look_summary)

        LabeledControl(Icons.Filled.Tune, stringResource(R.string.pers_shape))
        ConnectedToggleRow(
            options = ShapeScale.entries,
            selected = pers.shapeScale,
            label = { stringResource(it.labelRes) },
            onSelect = { onPersonalizationChange(pers.copy(shapeScale = it)) }
        )
        ShapeSpecimen(pers)

        LabeledSlider(
            icon = Icons.Filled.TextFields,
            label = stringResource(R.string.pers_text_scale),
            value = pers.textScale,
            valueText = "${(pers.textScale * 100).toInt()}%",
            valueRange = 0.85f..1.3f,
            steps = 8,
            onValueChange = { onPersonalizationChange(pers.copy(textScale = it)) },
            onValueChangeFinished = { snapped ->
                onPersonalizationChange(pers.copy(textScale = (snapped * 20).toInt() / 20f))
            }
        )

        LabeledSlider(
            icon = Icons.Filled.Brightness1,
            label = stringResource(R.string.pers_corners),
            value = pers.cornerBoost,
            valueText = "${(pers.cornerBoost * 100).toInt()}%",
            valueRange = 0f..0.75f,
            steps = 14,
            onValueChange = { onPersonalizationChange(pers.copy(cornerBoost = it)) },
            onValueChangeFinished = { snapped ->
                onPersonalizationChange(pers.copy(cornerBoost = (snapped * 20).toInt() / 20f))
            }
        )

        LabeledControl(Icons.Filled.Bolt, stringResource(R.string.pers_motion))
        ConnectedToggleRow(
            options = MotionScale.entries,
            selected = pers.motionScale,
            label = { stringResource(it.labelRes) },
            onSelect = { onPersonalizationChange(pers.copy(motionScale = it)) }
        )

        LabeledControl(Icons.Filled.Palette, stringResource(R.string.pers_accent_intensity))
        Text(
            stringResource(R.string.pers_accent_intensity_summary),
            modifier = Modifier.padding(start = 36.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AccentSpecimen(pers)
        ConnectedToggleRow(
            options = AccentIntensity.entries,
            selected = pers.accentIntensity,
            label = { stringResource(it.labelRes) },
            onSelect = { onPersonalizationChange(pers.copy(accentIntensity = it)) }
        )

        LabeledControl(Icons.Filled.CropOriginal, stringResource(R.string.pers_banner_shape))
        ConnectedToggleRow(
            options = BannerShape.entries,
            selected = pers.bannerShape,
            label = { stringResource(it.labelRes) },
            onSelect = { onPersonalizationChange(pers.copy(bannerShape = it)) }
        )
        BannerShapeSpecimen(pers)

        ExpressiveSwitchItem(
            icon = Icons.Filled.Contrast,
            title = stringResource(R.string.pers_contrast),
            summary = stringResource(R.string.pers_contrast_summary),
            checked = pers.contentContrast,
            onCheckedChange = { onPersonalizationChange(pers.copy(contentContrast = it)) }
        )
    }
}

/**
 * Swatches of the accent roles at the chosen intensity, computed with the same
 * transform the theme applies. This is what makes the control trustworthy: the
 * chips are the live values, not a mock-up of them.
 */
@Composable
private fun AccentSpecimen(pers: Personalization, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme.withAccentIntensity(pers.accentIntensity)
    Row(
        modifier = modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        listOf(
            scheme.primary,
            scheme.secondary,
            scheme.tertiary,
            scheme.primaryContainer
        ).forEach { c ->
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(c)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            )
        }
    }
}

/** Three banner silhouettes at the chosen corner treatment, widest ratio. */
@Composable
private fun BannerShapeSpecimen(pers: Personalization, modifier: Modifier = Modifier) {
    val fill = MaterialTheme.colorScheme.secondaryContainer
    Row(
        modifier = modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BannerShape.entries.forEach { shape ->
            Box(
                Modifier
                    .size(width = 56.dp, height = 34.dp)
                    .clip(RoundedCornerShape(percent = (shape.radiusFraction * 100).toInt()))
                    .background(fill)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(percent = (shape.radiusFraction * 100).toInt())
                    )
            )
        }
    }
}

@Composable
private fun SectionLabel(res: Int) {
    Text(
        stringResource(res),
        modifier = Modifier.padding(horizontal = 12.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun SectionCaption(res: Int) {
    Text(
        stringResource(res),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun LabeledControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun <T> ConnectedToggleRow(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        options.forEachIndexed { index, option ->
            ToggleButton(
                checked = selected == option,
                onCheckedChange = { checked -> if (checked) onSelect(option) },
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = ToggleButtonDefaults.toggleButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Text(text = label(option), maxLines = 1)
            }
        }
    }
}

/**
 * Live specimen of the actual shape ramp. Renders [Personalization.shapes] rather
 * than a re-derived approximation, so what the user sees here is exactly what the
 * rest of the app will do -- the controls change it under the finger.
 */
@Composable
private fun ShapeSpecimen(pers: Personalization, modifier: Modifier = Modifier) {
    val shapes = pers.shapes
    val fill by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.primaryContainer,
        animationSpec = spring(),
        label = "specimenFill"
    )
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        listOf(shapes.medium, shapes.large, shapes.extraLarge).forEachIndexed { i, shape ->
            Box(
                Modifier
                    .size(48.dp)
                    .clip(shape)
                    .background(fill.copy(alpha = 1f - i * 0.25f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            )
        }
    }
}

/**
 * Icon + label + live value readout over the slider. The readout matters: these
 * two controls are continuous, and a bare track gives no sense of where you are
 * between stops.
 */
@Composable
private fun LabeledSlider(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: Float,
    valueText: String,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                valueText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        ZenithSlider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = { onValueChangeFinished(value) },
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
