package de.atennert.lcarswm

import kotlinx.cinterop.*
import xlib.*

/**
 * @return window ID of the generated root window
 */
fun setupLcarsWindow(
    display: CPointer<Display>,
    screen: Screen,
    windowManagerState: WindowManagerState
): ULong {
    val lcarsWindow = XCreateSimpleWindow(display, screen.root, 0, 0, screen.width.convert(), screen.height.convert(), 0.convert(), screen.black_pixel, screen.black_pixel)

    XSelectInput(display, lcarsWindow, SubstructureRedirectMask or SubstructureNotifyMask)

    XMapWindow(display, lcarsWindow)

    listOf(Button1, Button2, Button3).forEach { grabButton(display, lcarsWindow, it) }

    grabKeys(display, lcarsWindow, windowManagerState)

    XSync(display, X_FALSE)

    return lcarsWindow
}


/**
 * Setup keyboard handling. Keys without key code for the key sym will not be working.
 */
private fun grabKeys(display: CPointer<Display>, window: ULong, windowManagerState: WindowManagerState) {

    windowManagerState.modifierKeys.addAll(getModifierKeys(display, WM_MODIFIER_KEY))
    windowManagerState.modifierKeys
        .onEach { keyCode ->
            XGrabKey(
                display, keyCode.convert(), AnyModifier.convert(), window,
                X_FALSE, GrabModeAsync, GrabModeAsync
            )
        }


    // get and grab all key codes for the short cut keys
    LCARS_WM_KEY_SYMS
        .map { keySym -> Pair(keySym, XKeysymToKeycode(display, keySym.convert())) }
        .filterNot { (_, keyCode) -> keyCode.toInt() == 0 }
        .onEach { (keySym, keyCode) -> windowManagerState.keyboardKeys[keyCode.toUInt()] = keySym }
        .forEach { (_, keyCode) ->
            XGrabKey(
                display, keyCode.convert(), WM_MODIFIER_KEY.convert(), window,
                X_FALSE, GrabModeAsync, GrabModeAsync
            )
        }
}

private fun getModifierKeys(display: CValuesRef<Display>, modifierKey: Int): Collection<UByte> {
    val modifierIndex = arrayOf(
        ShiftMask,
        LockMask,
        ControlMask,
        Mod1Mask,
        Mod2Mask,
        Mod3Mask,
        Mod4Mask,
        Mod5Mask
    ).indexOf(modifierKey)

    val modifierKeymap = XGetModifierMapping(display)?.pointed ?: return emptyList()

    val startPosition = modifierIndex * modifierKeymap.max_keypermod
    val endPosition = startPosition + modifierKeymap.max_keypermod
    val modKeys = ArrayList<UByte>(modifierKeymap.max_keypermod)

    for (i in startPosition until endPosition) {
        modKeys.add(modifierKeymap.modifiermap!![i])
    }

    return modKeys
}

private fun grabButton(display: CPointer<Display>, window: ULong, buttonId: Int) {
    XGrabButton(
        display, buttonId.convert(), 0.convert(), window, X_FALSE,
        (ButtonPressMask or ButtonMotionMask or ButtonReleaseMask).convert(),
        GrabModeAsync, GrabModeAsync, None.convert(), None.convert()
    )
}
