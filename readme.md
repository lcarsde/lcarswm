# lcarswm
It's a window manager written in Kotlin. The goal is to finish it and if that happens to happen, it will look like an LCARS interface.

## Acknowledgements
Thank you very much to the creators of the following resources:
* [Chuan Ji - How X Window Managers Work, And How To Write One](https://jichu4n.com/posts/how-x-window-managers-work-and-how-to-write-one-part-i/)
* [mcwm on GitHub](https://github.com/mchackorg/mcwm)
* [XCB implementation of TinyWM on GitHub](https://github.com/stefanbidi/tinywm-xcb)

## Functionality
* The window manager can be started.
* The window manager can be closed using the left or right mouse button.
* Using the middle mouse button opens a terminal.

## UI?
Not yet, but there are some concept pictures in the doc folder.

## Required
* ncurses5-compat-libs: apparently libtinfo.so.5 is used by the compiler, which is part of this package.
* header files and libraries for xcb and xcb-util

## Notes to myself :-)
* The main method needs to be outside of any packages so that the compiler can find it.

## Sources
There are common* and native* source directories under `src`. Everything that interacts with XCB or other native resources is in native. Native types and type conversions are hereby also restricted to native*. Everything that has no native dependency is in the common* directories. The goal is to keep the native code as small as possible and put as much as possible in the common part.

## Automated testing
Automated tests are set up using Travis CI. It's working well except for the fact, that the whole environment is downloaded again for every test run. That means downloads of over 500MB, which takes time. However, it's working and that's good enough for me for now.

## Manuel testing / running the wm
To manually test the functionality, I've set up a virtual Linux machine in VirtualBox with a shared directory to the generated executables. In this virtual environment, I run the window manager like this:

```
startx /path/to/executable/lcarswm.kexe > logfile 2>$1
```

That runs the X window manager with the lcarswm window manager and writes all standard and error log output into the file "logfile". The window manager seems to keep hanging in initialization. I think it's the output forwarding that causes the problems, but so far I didn't check what exactly is happening. Running the window manager once without the output forwarding seems to help:

```
startx /path/to/executable/lcarswm.kexe
```

Probably one of the most interesting features in using a VM and RANDR is screen resizing. For me this works with using VBoxVGA as graphics controller and `VBoxClient-all` needs to be called after the activation of X to allow for resizing detection and all other things VBoxClient offers. Alternatively, VBoxClient can be called with specific flags.

## Some definitions
* Height of top and bottom bar: 40px
* Width of height and bottom bar ends: 32px
* Width of bar gaps: 8px
* Corner pieces have an inner corner with radius: 10px
* Random data area height: 100px
