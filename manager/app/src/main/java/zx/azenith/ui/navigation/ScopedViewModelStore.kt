package zx.azenith.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavController

/**
 * The ViewModel store a sub-screen should scope its `viewModel()` call to.
 *
 * A sub-screen normally wants its *parent's* store, so the parent and every
 * child below it share one ViewModel instance and one snapshot of state:
 * editing a resolution on the child and reading it back on the parent cannot
 * disagree.
 *
 * But the parent is not guaranteed to be on the stack. The dashboard's Quick
 * Profiles row navigates straight to any route in its rotation, so opening Game
 * Props Spoof from a tile lands on `integrity_gamespoof` with no
 * `integrity_spoofing` beneath it, and the unguarded
 * `getBackStackEntry("integrity_spoofing")` threw an IllegalArgumentException
 * on the main thread during composition — killing the process the moment that
 * screen opened.
 *
 * So: the requested parent when it is present, otherwise the screen's own
 * entry, otherwise the composition's owner. The decision itself is
 * [ScopedStore.resolve]; this only performs the lookups.
 */
@Composable
fun rememberScopedStoreOwner(
    navController: NavController,
    route: String
): ViewModelStoreOwner {
    // Non-null by contract: a NavHost is always hosted, so the composition
    // always has an owner. Failing loudly beats returning null and crashing
    // later inside viewModel() with a far less obvious message.
    val ambient = checkNotNull(LocalViewModelStoreOwner.current) {
        "no ViewModelStoreOwner in the composition"
    }
    val currentEntry = navController.currentBackStackEntry
    val currentRoute = currentEntry?.destination?.route

    val resolved = ScopedStore.resolve(route, currentRoute) { candidate ->
        navController.findBackStackEntryOrNull(candidate) != null
    }

    return when (resolved) {
        ScopedStore.AMBIENT -> ambient
        // Already proven present by the membership test above.
        else -> navController.findBackStackEntryOrNull(resolved) ?: ambient
    }
}

/**
 * The back-stack entry for [route], or `null` if it is not on the stack.
 *
 * `NavController.getBackStackEntry` throws `IllegalArgumentException` when the
 * route is absent. That is the wrong default for every caller here: they are
 * asking "is my parent up there, so I can share its ViewModel?", and the honest
 * answer is frequently *no* — not an error.
 */
fun NavController.findBackStackEntryOrNull(route: String) =
    try {
        getBackStackEntry(route)
    } catch (_: IllegalArgumentException) {
        null
    }
