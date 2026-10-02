package zx.azenith.ui.component

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateDp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.blur
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.lifecycle.compose.currentStateAsState


@Composable
fun ScreenWrapper(
    navController: androidx.navigation.NavController,
    
    modifier: Modifier = Modifier,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    content: @Composable () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Lifecycle State
    val initialCurrentState = lifecycleOwner.lifecycle.currentStateAsState().value
    var isResumed by remember { mutableStateOf(initialCurrentState.isAtLeast(Lifecycle.State.RESUMED)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isResumed = true
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                isResumed = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Collect states to subscribe Compose to their updates. Every event that changes the back
    // stack (navigate / pop commit) emits currentBackStackEntry, so reading these here is what
    // triggers recomposition; the synchronous navController properties below are then re-read
    // with frame-perfect values.
    val visibleEntriesState by navController.visibleEntries.collectAsState()
    val currentBackStackEntryState by navController.currentBackStackEntryAsState()

    val syncVisibleEntries = navController.visibleEntries.collectAsState().value.also { _ -> visibleEntriesState }

    val myEntry = lifecycleOwner as? androidx.navigation.NavBackStackEntry
    val myRoute = myEntry?.destination?.route
    val isMainRootScreen = myRoute == "main" || myRoute == "get_started"
    val hasVisibleNonMainRootScreen = syncVisibleEntries.any { entry ->
        entry.destination.route?.let { route -> (route != "main" && route != "get_started") } == true
    }
    
    val transition = animatedVisibilityScope?.transition
    val isTransitionFinished = transition?.currentState == EnterExitState.Visible && transition?.targetState == EnterExitState.Visible
    val shouldRunDepthEffects = !isMainRootScreen || (hasVisibleNonMainRootScreen && !isTransitionFinished)

    // Dim/Blur Logic: the screen "behind" during any transition — forward push, committed pop,
    // or an in-progress predictive back gesture — is always the entry directly below the top of
    // the back stack, i.e. previousBackStackEntry. The incoming screen of a committed pop is
    // currentBackStackEntry (never previous), so it stays clear, and the exiting screen of a pop
    // is no longer in the back stack at all, so it stays clear too.
    //
    // We deliberately do NOT detect "behind" through visibleEntries: when a predictive back
    // gesture starts, prepareForTransition() raises the previous entry to STARTED without
    // re-emitting the visibleEntries StateFlow (NavHost composes the previous entry directly,
    // bypassing visibleEntries — see the comment inside NavHost's AnimatedContent). On a clean
    // gesture the behind screen therefore never appeared in visibleEntries, but after a
    // CANCELLED gesture the entry lingered in the transition set and the next flow emission
    // included it — which made the blur/dim work only on every other back gesture.
    //
    // previousBackStackEntry is a plain synchronous property; the currentBackStackEntryState
    // reference makes Compose re-read it on every navigate/pop commit (both change together).
    val previousEntryId = navController.previousBackStackEntry?.id.also { _ -> currentBackStackEntryState }
    val shouldDim = myEntry != null && previousEntryId == myEntry.id

    val disableBlurAllOver = false

    // Declarative Animations
    val targetRadius = if (shouldRunDepthEffects && !isResumed) 32f else 0f
    val animatedCornerRadius = if (transition != null) {
        val animatedValue by transition.animateFloat(
            transitionSpec = { tween(durationMillis = 350, easing = FastOutSlowInEasing) },
            label = "cornerRadius"
        ) { state ->
            if (shouldRunDepthEffects && (state == EnterExitState.PostExit || state == EnterExitState.PreEnter)) {
                32f
            } else {
                0f
            }
        }
        animatedValue
    } else {
        val fallbackCornerRadius = remember { Animatable(targetRadius) }
        LaunchedEffect(targetRadius) {
            fallbackCornerRadius.animateTo(targetRadius, animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing))
        }
        fallbackCornerRadius.value
    }

    // Dim: If strictly behind Top -> 0.4f (or 0.75f if blur is disabled). Else -> 0f.
    val targetDim = if (shouldRunDepthEffects && shouldDim) {
        if (disableBlurAllOver) 0.75f else 0.4f
    } else {
        0f
    }
    val animatedDimAlpha = if (transition != null) {
        val animatedValue by transition.animateFloat(
            transitionSpec = { tween(durationMillis = 350, easing = CubicBezierEasing(0.5f, 0f, 0.8f, 0.2f)) },
            label = "dimAlpha"
        ) { state ->
            if (shouldRunDepthEffects && shouldDim && (state == EnterExitState.PostExit || state == EnterExitState.PreEnter)) {
                if (disableBlurAllOver) 0.75f else 0.4f
            } else {
                0f
            }
        }
        animatedValue
    } else {
        val fallbackDimAlpha = remember { Animatable(targetDim) }
        LaunchedEffect(targetDim) {
            fallbackDimAlpha.animateTo(targetDim, animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing))
        }
        fallbackDimAlpha.value
    }

    // Blur: If strictly behind Top -> 24dp. Else -> 0dp. Disabled if disableBlurAllOver is true.
    val targetBlur = if (shouldRunDepthEffects && shouldDim && !disableBlurAllOver) 24f else 0f
    val animatedBlurRadius = if (transition != null) {
        val animatedValue by transition.animateDp(
            transitionSpec = { tween(durationMillis = 350, easing = CubicBezierEasing(0.5f, 0f, 0.8f, 0.2f)) },
            label = "blurRadius"
        ) { state ->
            if (shouldRunDepthEffects && shouldDim && !disableBlurAllOver && (state == EnterExitState.PostExit || state == EnterExitState.PreEnter)) {
                24.dp
            } else {
                0.dp
            }
        }
        animatedValue
    } else {
        val fallbackBlurRadius = remember { Animatable(targetBlur) }
        LaunchedEffect(targetBlur) {
            fallbackBlurRadius.animateTo(targetBlur, animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing))
        }
        fallbackBlurRadius.value.dp
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Keep both the graphicsLayer modifier AND its compositingStrategy stable across
            // the full lifecycle of the screen. Toggling the strategy between Auto and
            // Offscreen mid-transition (when cornerRadius crosses the threshold) causes the
            // RenderNode's rendering mode to flip for one frame, producing a subtle flash on
            // the outgoing screen right as the animation starts. Main root tab switches are
            // the exception: Home/Search/Library keep the same slide/fade transition, but skip
            // the expensive offscreen depth layer while no deeper screen is visible.
            .graphicsLayer {
                compositingStrategy = if (shouldRunDepthEffects) {
                    CompositingStrategy.Offscreen
                } else {
                    CompositingStrategy.Auto
                }
                if (shouldRunDepthEffects && animatedCornerRadius > 0.5f) {
                    this.shape = RoundedCornerShape(animatedCornerRadius.dp)
                    this.clip = true
                } else {
                    this.clip = false
                }
            }
            .blur(radius = if (shouldRunDepthEffects) animatedBlurRadius else 0.dp)
            .background(MaterialTheme.colorScheme.background)
    ) {
        content()

        // Dim Layer Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = animatedDimAlpha }
                .background(Color.Black)
        )
    }
}
