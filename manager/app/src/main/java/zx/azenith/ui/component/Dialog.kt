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

import android.os.Parcelable
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.parcelize.Parcelize
import kotlin.coroutines.resume

private const val TAG = "DialogComponent"

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

interface ConfirmDialogHandle : DialogHandle {
    val visuals: ConfirmDialogVisuals
    fun showConfirm(
        title: String,
        content: String? = null,
        confirm: String? = null,
        dismiss: String? = null
    )
    suspend fun awaitConfirm(
        title: String,
        content: String? = null,
        confirm: String? = null,
        dismiss: String? = null
    ): ConfirmResult
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
    private val callback: ConfirmCallback, // FIX: Ditambah 'private val' agar bisa diakses di awaitResult
    override var visuals: ConfirmDialogVisuals = ConfirmDialogVisualsImpl.Empty,
    private val resultFlow: ReceiveChannel<ConfirmResult>
) : ConfirmDialogHandle, DialogHandleBase(visible, coroutineScope) {

    init {
        coroutineScope.launch {
            resultFlow.consumeAsFlow()
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
                if (callback.isEmpty) {
                    invokeOnCancellation { visible.value = false }
                }
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

    override val dialogType: String get() = "ConfirmDialog"

    companion object {
        fun Saver(visible: MutableState<Boolean>, coroutineScope: CoroutineScope, callback: ConfirmCallback, resultChannel: ReceiveChannel<ConfirmResult>) =
            Saver<ConfirmDialogHandle, ConfirmDialogVisuals>(
                save = { it.visuals },
                restore = { ConfirmDialogHandleImpl(visible, coroutineScope, callback, it, resultChannel) }
            )
    }
}

@Composable
fun rememberLoadingDialog(): LoadingDialogHandle {
    val visible = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    if (visible.value) LoadingDialog()
    return remember { LoadingDialogHandleImpl(visible, coroutineScope) }
}

@Composable
fun rememberConfirmDialog(onConfirm: NullableCallback = null, onDismiss: NullableCallback = null): ConfirmDialogHandle {
    val currentOnConfirm by rememberUpdatedState(onConfirm)
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val callback = remember { ConfirmCallback({ currentOnConfirm }, { currentOnDismiss }) }
    
    val visible = rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val resultChannel = remember { Channel<ConfirmResult>() }

    val handle = rememberSaveable(
        saver = ConfirmDialogHandleImpl.Saver(visible, coroutineScope, callback, resultChannel),
        init = { ConfirmDialogHandleImpl(visible, coroutineScope, callback, ConfirmDialogVisualsImpl.Empty, resultChannel) }
    )

    if (visible.value) {
        ConfirmDialog(
            handle.visuals,
            confirm = { coroutineScope.launch { resultChannel.send(ConfirmResult.Confirmed) } },
            dismiss = { coroutineScope.launch { resultChannel.send(ConfirmResult.Canceled) } }
        )
    }
    return handle
}

@Composable
fun rememberCustomDialog(composable: @Composable (dismiss: () -> Unit) -> Unit): DialogHandle {
    val visible = rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    if (visible.value) composable { visible.value = false }
    return remember { 
        object : DialogHandleBase(visible, coroutineScope) { 
            override val dialogType: String get() = "CustomDialog" 
        } 
    }
}

@Composable
private fun LoadingDialog() {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = false)
    ) {
        MaterialExpressiveTheme {
            Surface(modifier = Modifier.size(100.dp), shape = RoundedCornerShape(8.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            }
        }
    }
}

@Composable
private fun ConfirmDialog(visuals: ConfirmDialogVisuals, confirm: () -> Unit, dismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(text = visuals.title) },
        text = { visuals.content?.let { Text(text = it) } },
        confirmButton = {
            TextButton(onClick = confirm) {
                Text(text = visuals.confirm ?: stringResource(id = android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = dismiss) {
                Text(text = visuals.dismiss ?: stringResource(id = android.R.string.cancel))
            }
        },
    )
}
