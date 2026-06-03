/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package zx.azenith.ui.component

import android.content.Context
import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.blur.blurEffect
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.parcelize.Parcelize
import kotlin.coroutines.resume
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.scale



private const val TAG = "DialogComponent"

val LocalAppHazeState = compositionLocalOf<HazeState?> { null }

interface ConfirmDialogVisuals : Parcelable {
    val title: String
    val content: String?
    val confirm: String?
    val dismiss: String?
}

@Parcelize
private data class ConfirmDialogVisualsImpl(
    override val title: String,
    override val content: String?,
    override val confirm: String?,
    override val dismiss: String?,
) : ConfirmDialogVisuals {
    companion object {
        val Empty: ConfirmDialogVisuals = ConfirmDialogVisualsImpl("", "", null, null)
    }
}

interface DialogHandle {
    val isShown: Boolean
    val dialogType: String
    fun show()
    fun hide()
}

interface LoadingDialogHandle : DialogHandle {
    suspend fun <R> withLoading(block: suspend () -> R): R
    fun showLoading()
}

sealed interface ConfirmResult {
    object Confirmed : ConfirmResult
    object Canceled : ConfirmResult
}

// Tambahan fungsi accept() dan cancel() untuk dipanggil oleh Host
interface ConfirmDialogHandle : DialogHandle {
    val visuals: ConfirmDialogVisuals
    fun showConfirm(title: String, content: String? = null, confirm: String? = null, dismiss: String? = null)
    suspend fun awaitConfirm(title: String, content: String? = null, confirm: String? = null, dismiss: String? = null): ConfirmResult
    fun accept() 
    fun cancel() 
}

private abstract class DialogHandleBase(
    val visible: MutableState<Boolean>,
    val coroutineScope: CoroutineScope
) : DialogHandle {
    override val isShown: Boolean get() = visible.value
    override fun show() { coroutineScope.launch { visible.value = true } }
    final override fun hide() { coroutineScope.launch { visible.value = false } }
}

private class LoadingDialogHandleImpl(
    visible: MutableState<Boolean>,
    coroutineScope: CoroutineScope
) : LoadingDialogHandle, DialogHandleBase(visible, coroutineScope) {
    override suspend fun <R> withLoading(block: suspend () -> R): R {
        return coroutineScope.async {
            try {
                visible.value = true
                block()
            } finally {
                visible.value = false
            }
        }.await()
    }
    override fun showLoading() { show() }
    override val dialogType: String get() = "LoadingDialog"
}

typealias NullableCallback = (() -> Unit)?

interface ConfirmCallback {
    val onConfirm: NullableCallback
    val onDismiss: NullableCallback
    val isEmpty: Boolean get() = onConfirm == null && onDismiss == null
    companion object {
        operator fun invoke(onConfirmProvider: () -> NullableCallback, onDismissProvider: () -> NullableCallback) =
            object : ConfirmCallback {
                override val onConfirm: NullableCallback get() = onConfirmProvider()
                override val onDismiss: NullableCallback get() = onDismissProvider()
            }
    }
}

private class ConfirmDialogHandleImpl(
    visible: MutableState<Boolean>,
    coroutineScope: CoroutineScope,
    private val callback: ConfirmCallback,
    initialVisuals: ConfirmDialogVisuals,
    private val resultChannel: Channel<ConfirmResult>
) : ConfirmDialogHandle, DialogHandleBase(visible, coroutineScope) {

    // Visuals sekarang berupa state agar Host tahu saat ada update teks
    private val _visuals = mutableStateOf(initialVisuals)
    override var visuals: ConfirmDialogVisuals
        get() = _visuals.value
        set(value) { _visuals.value = value }

    init {
        coroutineScope.launch {
            resultChannel.consumeAsFlow()
                .onEach { result ->
                    awaitContinuation?.let {
                        awaitContinuation = null
                        if (it.isActive) it.resume(result)
                    }
                    when (result) {
                        ConfirmResult.Confirmed -> callback.onConfirm?.invoke()
                        ConfirmResult.Canceled -> callback.onDismiss?.invoke()
                    }
                }
                .onEach { hide() }
                .collect {}
        }
    }

    private var awaitContinuation: CancellableContinuation<ConfirmResult>? = null

    private suspend fun awaitResult(): ConfirmResult {
        return suspendCancellableCoroutine {
            awaitContinuation = it.apply {
                if (callback.isEmpty) invokeOnCancellation { visible.value = false }
            }
        }
    }

    override fun show() { if (visuals !== ConfirmDialogVisualsImpl.Empty) super.show() }

    override fun showConfirm(title: String, content: String?, confirm: String?, dismiss: String?) {
        coroutineScope.launch {
            visuals = ConfirmDialogVisualsImpl(title, content, confirm, dismiss)
            show()
        }
    }

    override suspend fun awaitConfirm(title: String, content: String?, confirm: String?, dismiss: String?): ConfirmResult {
        coroutineScope.launch {
            visuals = ConfirmDialogVisualsImpl(title, content, confirm, dismiss)
            show()
        }
        return awaitResult()
    }

    override fun accept() { coroutineScope.launch { resultChannel.send(ConfirmResult.Confirmed) } }
    override fun cancel() { coroutineScope.launch { resultChannel.send(ConfirmResult.Canceled) } }
    override val dialogType: String get() = "ConfirmDialog"

    companion object {
        fun Saver(visible: MutableState<Boolean>, coroutineScope: CoroutineScope, callback: ConfirmCallback, resultChannel: Channel<ConfirmResult>) =
            Saver<ConfirmDialogHandle, ConfirmDialogVisuals>(
                save = { it.visuals },
                restore = { ConfirmDialogHandleImpl(visible, coroutineScope, callback, it, resultChannel) }
            )
    }
}

// Hanya menyimpan handle, TIDAK me-render UI di sini
@Composable
fun rememberLoadingDialog(): LoadingDialogHandle {
    val visible = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    return remember { LoadingDialogHandleImpl(visible, coroutineScope) }
}

// Hanya menyimpan handle, TIDAK me-render UI di sini
@Composable
fun rememberConfirmDialog(onConfirm: NullableCallback = null, onDismiss: NullableCallback = null): ConfirmDialogHandle {
    val currentOnConfirm by rememberUpdatedState(onConfirm)
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val callback = remember { ConfirmCallback({ currentOnConfirm }, { currentOnDismiss }) }
    val visible = rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val resultChannel = remember { Channel<ConfirmResult>() }

    return rememberSaveable(
        saver = ConfirmDialogHandleImpl.Saver(visible, coroutineScope, callback, resultChannel),
        init = { ConfirmDialogHandleImpl(visible, coroutineScope, callback, ConfirmDialogVisualsImpl.Empty, resultChannel) }
    )
}

// --- 👇 HOST COMPONENTS: Panggil ini di akhir hirarki layout layar kamu 👇 --- //

@Composable
fun LoadingDialogHost(handle: LoadingDialogHandle) {
    LoadingDialog(visible = handle.isShown)
}

@Composable
fun ConfirmDialogHost(handle: ConfirmDialogHandle) {
    ConfirmDialog(
        visible = handle.isShown,
        visuals = handle.visuals,
        confirm = { handle.accept() },
        dismiss = { handle.cancel() }
    )
}

// --- INTERNAL UI COMPONENTS --- //

@Composable
private fun LoadingDialog(visible: Boolean) {
    val context = LocalContext.current
    val settingsPrefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val isBlurEnabled = settingsPrefs.getBoolean("expressive_blur_ui", false)
    val hazeState = LocalAppHazeState.current

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(250, easing = LinearOutSlowInEasing)),
        exit = fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing))
    ) {
        BackHandler(onBack = { })
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(100f) 
                .background(Color.Black.copy(alpha = 0.42f)) // Scrim gelap di belakang
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} 
                ),
            contentAlignment = Alignment.Center
        ) {
            // Gunakan animateFloatAsState agar tidak perlu nested AnimatedVisibility
            val scale by animateFloatAsState(
                targetValue = if (visible) 1f else 0.9f,
                animationSpec = tween(250, easing = LinearOutSlowInEasing),
                label = "dialog_scale"
            )

            // Ganti Surface dengan Box untuk rendering Haze yang lebih stabil
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale)
                    // 1. Wajib di-clip ke bentuk dialognya dulu
                    .clip(RoundedCornerShape(24.dp))
                    // 2. Terapkan Haze Effect untuk me-render blur
                    .then(
                        if (isBlurEnabled && hazeState != null) {
                            Modifier.hazeEffect(state = hazeState) { blurEffect { blurRadius = 24.dp } }
                        } else Modifier
                    )
                    // 3. Terapkan warna transparan (Tint) DI ATAS blur
                    .background(
                        if (isBlurEnabled) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.35f) // Alpha diturunkan agar blur tembus
                        else MaterialTheme.colorScheme.surface
                    ),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}


@Composable
private fun ConfirmDialog(
    visible: Boolean, 
    visuals: ConfirmDialogVisuals, 
    confirm: () -> Unit, 
    dismiss: () -> Unit
) {
    val context = LocalContext.current
    val settingsPrefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val isBlurEnabled = settingsPrefs.getBoolean("expressive_blur_ui", false)
    val hazeState = LocalAppHazeState.current

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(250, easing = LinearOutSlowInEasing)),
        exit = fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing))
    ) {
        BackHandler(onBack = dismiss)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(100f) 
                .background(Color.Black.copy(alpha = 0.42f)) 
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = dismiss 
                ),
            contentAlignment = Alignment.Center
        ) {
            val scale by animateFloatAsState(
                targetValue = if (visible) 1f else 0.9f,
                animationSpec = tween(250, easing = LinearOutSlowInEasing),
                label = "dialog_scale"
            )

            // Ganti Surface dengan Box
            Box(
                modifier = Modifier
                    .widthIn(min = 350.dp, max = 500.dp) 
                    .padding(24.dp) 
                    .scale(scale)
                    // 1. Clip bounds
                    .clip(RoundedCornerShape(28.dp))
                    // 2. Render blur dari background
                    .then(
                        if (isBlurEnabled && hazeState != null) {
                            Modifier.hazeEffect(state = hazeState) { blurEffect { blurRadius = 24.dp } }
                        } else Modifier
                    )
                    // 3. Tambahkan semi-transparent tint
                    .background(
                        if (isBlurEnabled) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.35f) 
                        else AlertDialogDefaults.containerColor
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Blokir sentuhan agar tidak merembes ke dismiss
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = visuals.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    visuals.content?.let { contentText ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = contentText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = dismiss) {
                            Text(text = visuals.dismiss ?: stringResource(id = android.R.string.cancel))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = confirm) {
                            Text(text = visuals.confirm ?: stringResource(id = android.R.string.ok))
                        }
                    }
                }
            }
        }
    }
}
