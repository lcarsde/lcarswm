package de.atennert.lcarswm

import de.atennert.lcarswm.system.xEventApi
import de.atennert.lcarswm.system.xInputApi
import kotlinx.cinterop.*
import xlib.*

/**
 * @return window ID of the generated root window
 */
fun setupLcarsWindow(
    display: CPointer<Display>,
    screen: Screen,
    windowManagerState: WindowManagerState
) {
    windowManagerState.modifierKeys.addAll(getModifierKeys(display, WM_MODIFIER_KEY))

    //listOf(Button1, Button2, Button3).forEach { grabButton(display, screen.root, it) }

    grabKeys(display, screen.root, windowManagerState)

    xEventApi().sync(display, false)
}


/**
 * Setup keyboard handling. Keys without key code for the key sym will not be working.
 */
private fun grabKeys(display: CPointer<Display>, window: Window, windowManagerState: WindowManagerState) {
    windowManagerState.modifierKeys
        .onEach { keyCode ->
            xInputApi().grabKey(
                display, keyCode.convert(), AnyModifier.convert(), window, false, GrabModeAsync, GrabModeAsync
            )
        }


    // get and grab all key codes for the short cut keys
    LCARS_WM_KEY_SYMS
        .map { keySym -> Pair(keySym, xInputApi().keysymToKeycode(display, keySym.convert())) }
        .filterNot { (_, keyCode) -> keyCode.toInt() == 0 }
        .onEach { (keySym, keyCode) -> windowManagerState.keyboardKeys[keyCode.toUInt()] = keySym }
        .forEach { (_, keyCode) ->
            xInputApi().grabKey(
                display, keyCode.convert(), WM_MODIFIER_KEY.convert(), window,
                false, GrabModeAsync, GrabModeAsync
            )
        }

    LCARS_NO_MASK_KEY_SYMS
        .map { keySym -> Pair(keySym, xInputApi().keysymToKeycode(display, keySym.convert())) }
        .filterNot { (_, keyCode) -> keyCode.toInt() == 0 }
        .onEach { (keySym, keyCode) -> windowManagerState.keyboardKeys[keyCode.toUInt()] = keySym }
        .forEach { (_, keyCode) ->
            xInputApi().grabKey(display, keyCode.convert(), AnyModifier.convert(), window, false, GrabModeAsync, GrabModeAsync)
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

    val modifierKeymap = xInputApi().getModifierMapping(display)?.pointed ?: return emptyList()

    val startPosition = modifierIndex * modifierKeymap.max_keypermod
    val endPosition = startPosition + modifierKeymap.max_keypermod
    val modKeys = ArrayList<UByte>(modifierKeymap.max_keypermod)

    for (i in startPosition until endPosition) {
        modKeys.add(modifierKeymap.modifiermap!![i])
    }

    return modKeys
}

private fun grabButton(display: CPointer<Display>, window: Window, buttonId: Int) {
    xInputApi().grabButton(
        display, buttonId.convert(), WM_MODIFIER_KEY.convert(), window, false,
        (ButtonPressMask or ButtonMotionMask or ButtonReleaseMask).convert(),
        GrabModeAsync, GrabModeAsync, None.convert(), None.convert()
    )
}
