# Concept for window tiling
* Windows can be moved to different `WindowSlot`s instead of screens
* A `WindowSlot` covers a partial area of a screen
* Initially a there is one `WindowSlot` per screen in the former screen window area measurements
* `WindowSlot`s cannot stretch over multiple screens and belong only to one screen
* Windows tile by changing the size of a `WindowSlot` in half in either x or y direction and creating a new `WindowSlot` in the remaining empty space; all windows in the adjusted `WindowSlot` change their size accordingly
* Likewise `WindowSlot`s can be stretched in x or y direction to the area of another `WindowSlot` of the same size on the axis over which the stretching happens; the _targeted_ `WindowSlot` will be removed and its windows will be part of the stretched `WindowSlot`; Example: when stretching in x direction, then the height of the stretched and the targeted `WindowSlot`s must be equal
* Suggested key bindings:
  * Tiling: Alt + Win + Arrow-Key
  * Stretching: Alt + Win + Shift + Arrow-Key
* Open topics:
  * Screen removal
  * Tiling without keyboard