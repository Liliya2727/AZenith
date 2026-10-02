package zx.azenith.ui.navigation

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController

fun NavController.safePopBackStack() {
    if (this.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        this.popBackStack()
    }
}
