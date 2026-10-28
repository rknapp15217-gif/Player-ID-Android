package com.playerid.app.platform

/**
 * Whether the app may leave portrait: on the Camera tab, or while a memory is open full screen.
 * The Swift app delegate reads [rotationAllowed] in `application(_:supportedInterfaceOrientationsFor:)`
 * and sets [onChange] to rotate the window when it changes.
 */
object IosOrientationLock {
    private var screenAllowsRotation = false
    private var viewerOpen = false

    val rotationAllowed: Boolean get() = screenAllowsRotation || viewerOpen
    var onChange: (() -> Unit)? = null

    fun setRotationAllowed(allowed: Boolean) = update { screenAllowsRotation = allowed }

    fun setViewerOpen(open: Boolean) = update { viewerOpen = open }

    private fun update(change: () -> Unit) {
        val before = rotationAllowed
        change()
        if (rotationAllowed != before) onChange?.invoke()
    }
}
