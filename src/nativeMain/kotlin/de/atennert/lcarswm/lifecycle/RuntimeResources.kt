package de.atennert.lcarswm.lifecycle

import xlib.RROutput

data class RuntimeResources(val xEvent: XEventResources, val appMenu: AppMenuResources, val platform: PlatformResources<RROutput>)