package de.atennert.lcarswm

import de.atennert.lcarswm.system.api.InputApi
import de.atennert.lcarswm.system.api.SystemApi
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.pointed
import xlib.*

/**
 * @return window ID of the generated root window
 */
fun setupLcarsWindow(
    system: SystemApi,
    screen: Screen,
    windowManagerState: WindowManagerState
) {
    windowManagerState.modifierKeys.addAll(getModifierKeys(system, WM_MODIFIER_KEY))

    //listOf(Button1, Button2, Button3).forEach { grabButton(display, screen.root, it) }

    grabKeys(system, screen.root, windowManagerState)

    system.sync(false)
}


/**
 * Setup keyboard handling. Keys without key code for the key sym will not be working.
 */
private fun grabKeys(inputApi: InputApi, window: Window, windowManagerState: WindowManagerState) {
    windowManagerState.modifierKeys
        .onEach { keyCode ->
            inputApi.grabKey(keyCode.convert(), AnyModifier.convert(), window, GrabModeAsync)
        }


    // get and grab all key codes for the short cut keys
    LCARS_WM_KEY_SYMS
        .map { keySym -> Pair(keySym, inputApi.keysymToKeycode(keySym.convert())) }
        .filterNot { (_, keyCode) -> keyCode.toInt() == 0 }
        .onEach { (keySym, keyCode) -> windowManagerState.keyboardKeys[keyCode.toUInt()] = keySym }
        .forEach { (_, keyCode) ->
            inputApi.grabKey(keyCode.convert(), WM_MODIFIER_KEY.convert(), window, GrabModeAsync)
        }

    LCARS_NO_MASK_KEY_SYMS
        .map { keySym -> Pair(keySym, inputApi.keysymToKeycode(keySym.convert())) }
        .filterNot { (_, keyCode) -> keyCode.toInt() == 0 }
        .onEach { (keySym, keyCode) -> windowManagerState.keyboardKeys[keyCode.toUInt()] = keySym }
        .forEach { (_, keyCode) ->
            inputApi.grabKey(keyCode.convert(), AnyModifier.convert(), window, GrabModeAsync)
        }
}

private fun getModifierKeys(inputApi: InputApi, modifierKey: Int): Collection<UByte> {
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

    val modifierKeymap = inputApi.getModifierMapping()?.pointed ?: return emptyList()

    val startPosition = modifierIndex * modifierKeymap.max_keypermod
    val endPosition = startPosition + modifierKeymap.max_keypermod
    val modKeys = ArrayList<UByte>(modifierKeymap.max_keypermod)

    for (i in startPosition until endPosition) {
        modKeys.add(modifierKeymap.modifiermap!![i])
    }

    return modKeys
}

private fun grabButton(inputApi: InputApi, window: Window, buttonId: Int) {
    inputApi.grabButton(
        buttonId.convert(), WM_MODIFIER_KEY.convert(), window, false,
        (ButtonPressMask or ButtonMotionMask or ButtonReleaseMask).convert(),
        GrabModeAsync, GrabModeAsync, None.convert(), None.convert()
    )
}
