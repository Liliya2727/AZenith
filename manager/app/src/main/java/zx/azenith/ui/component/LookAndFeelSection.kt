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
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.ViewAgenda
import zx.azenith.ui.theme.NavLabelMode
import zx.azenith.ui.theme.NavShape
import zx.azenith.ui.theme.NavStyle
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height

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

        LabeledControl(Icons.Filled.ViewAgenda, stringResource(R.string.pers_navbar))
        Text(
            stringResource(R.string.pers_navbar_summary),
            modifier = Modifier.padding(start = 36.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        NavStyleSpecimen(pers)
        ExpressiveSwitchItem(
            icon = Icons.Filled.ViewAgenda,
            title = stringResource(R.string.pers_nav_floating),
            summary = stringResource(
                if (pers.navStyle == NavStyle.Floating) R.string.pers_nav_floating_on
                else R.string.pers_nav_floating_off
            ),
            checked = pers.navStyle == NavStyle.Floating,
            onCheckedChange = { floating ->
                onPersonalizationChange(
                    pers.copy(navStyle = if (floating) NavStyle.Floating else NavStyle.Pinned)
                )
            }
        )

        // A pinned bar is flush with the window edges and has no corners to round,
        // so the corner choice is only offered while the bar floats.
        if (pers.navStyle == NavStyle.Floating) {
            LabeledControl(Icons.Filled.RoundedCorner, stringResource(R.string.pers_nav_shape))
            NavShapeSpecimen(pers)
            ConnectedToggleRow(
                options = NavShape.entries,
                selected = pers.navShape,
                label = { stringResource(it.labelRes) },
                onSelect = { onPersonalizationChange(pers.copy(navShape = it)) }
            )
        }

        LabeledControl(Icons.Filled.Label, stringResource(R.string.pers_nav_labels))
        ConnectedToggleRow(
            options = NavLabelMode.entries,
            selected = pers.navLabels,
            label = { stringResource(it.labelRes) },
            onSelect = { onPersonalizationChange(pers.copy(navLabels = it)) }
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

/**
 * Silhouettes of the three bar layouts at true relative width, so the choice is
 * made by shape rather than by reading three labels.
 */
@Composable
private fun NavStyleSpecimen(pers: Personalization, modifier: Modifier = Modifier) {
    val fill = MaterialTheme.colorScheme.surfaceContainerHighest
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NavStyle.entries.forEach { style ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .width(86.dp)
                        .height(34.dp)
                        .clip(
                            RoundedCornerShape(
                                percent = when {
                                    style == NavStyle.Pinned -> 0
                                    else -> (pers.navShape.radiusFraction * 100).toInt()
                                }
                            )
                        )
                        .background(fill)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(
                                percent = when {
                                    style == NavStyle.Pinned -> 0
                                    else -> (pers.navShape.radiusFraction * 100).toInt()
                                }
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Pinned shows four evenly spread icon dots edge to edge; the
                    // others show one selected pill centred among them.
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(if (style == NavStyle.Pinned) 6.dp else 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(4) { i ->
                            val on = if (style == NavStyle.Pinned) i == 0 else i == 1
                            Box(
                                Modifier
                                    .size(9.dp)
                                    .clip(RoundedCornerShape(percent = 50))
                                    .background(if (on) accent else MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(style.labelRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** The four corner treatments at the real bar proportions. */
@Composable
private fun NavShapeSpecimen(pers: Personalization, modifier: Modifier = Modifier) {
    val fill = MaterialTheme.colorScheme.surfaceContainerHighest
    Row(
        modifier = modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        NavShape.entries.forEach { shape ->
            Box(
                Modifier
                    .size(width = 52.dp, height = 30.dp)
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
