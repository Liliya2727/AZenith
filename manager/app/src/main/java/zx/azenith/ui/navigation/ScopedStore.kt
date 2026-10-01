package zx.azenith.ui.navigation

/**
 * The route-selection rule for a sub-screen's ViewModel store, as a pure
 * function.
 *
 * Separate from the composable so it can be exercised on the JVM. The crash it
 * fixes cannot be: `getBackStackEntry` needs a live `NavController`, and the
 * failure was an `IllegalArgumentException` thrown during composition on the
 * main thread, which a unit test cannot reach. What *is* reachable — and is the
 * actual defect — is the decision about which owner to use.
 */
internal object ScopedStore {

    /** Sentinel meaning "use the composition's own owner". */
    const val AMBIENT = "<ambient>"

    /**
     * @param requestedRoute the parent whose store the screen wants to share
     * @param currentRoute the route actually showing, or `null` with no stack
     * @param isOnStack membership test against the live back stack
     * @return the route whose store should be used, or [AMBIENT]
     */
    fun resolve(
        requestedRoute: String,
        currentRoute: String?,
        isOnStack: (String) -> Boolean
    ): String = when {
        // The normal case: opened from the parent, so share its store.
        isOnStack(requestedRoute) -> requestedRoute

        // Entered directly — a Quick Profile tile opens any route in its
        // rotation. The requested parent was never pushed; use the screen's
        // own entry so it still gets a ViewModel and still works.
        //
        // The isNotBlank guard matters: a NavBackStackEntry reports its route
        // as "" when there is no stack at all, and returning that empty string
        // as a "route" would send the caller back into findBackStackEntryOrNull
        // for something that is not a route.
        currentRoute != null && currentRoute.isNotBlank() -> currentRoute

        // Deep link with no stack at all: nothing to share with.
        else -> AMBIENT
    }
}
