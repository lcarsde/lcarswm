# lcarswm
It's a window manager written in Kotlin. The goal is to finish it and if that happens to happen, it will look like an LCARS interface.

## Required
* ncurses5-compat-libs: apparently libtinfo.so.5 is used by the compiler, which is part of this package.

## stuff
* The main methods needs to be outside of any packages so that the compiler can find it.