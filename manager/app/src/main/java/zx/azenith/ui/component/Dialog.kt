package zx.azenith.ui.component

import android.os.Parcelable
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
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
/**
 * Handle to control the custom dialog from your UI logic.
 */
interface DialogHandle {
    val isShown: Boolean
    fun show()
    fun hide()
}

/**
 * Implementation of the DialogHandle.
 */
private class CustomDialogHandleImpl(
    private val visible: MutableState<Boolean>,
    private val coroutineScope: CoroutineScope
) : DialogHandle {
    override val isShown: Boolean
        get() = visible.value

    override fun show() {
        coroutineScope.launch {
            visible.value = true
        }
    }

    override fun hide() {
        coroutineScope.launch {
            visible.value = false
        }
    }
}

/**
 * remembers a custom dialog state.
 * * @param content The composable content to show inside the dialog. 
 * Provides a `dismiss` lambda to close the dialog from within.
 */
@Composable
fun rememberCustomDialog(
    content: @Composable (dismiss: () -> Unit) -> Unit
): DialogHandle {
    // remember state across recompositions and configuration changes
    val visible = rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // If visible, execute the composable content (which should be a Dialog or Surface)
    if (visible.value) {
        content { visible.value = false }
    }

    return remember {
        CustomDialogHandleImpl(visible, coroutineScope)
    }
}
