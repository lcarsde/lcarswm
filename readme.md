# LCARSWM
It's a window manager written in Kotlin that is designed to look like an LCARS interface.

LCARSWM has two kinds of monitors, one primary monitor (which maps to the X primary monitor) and "other" monitors. Only the primary screen has the upper (currently empty) information area in normal mode. The other screens normal mode looks just like the maximized mode. If there's no primary screen defined in the X server, then LCARSWM will use the first monitor it finds as primary.

LCARSWM displays one window at a time per screen although that might change in the future (maybe I'll make it tiling). Windows always open on the primary monitor and can be moved to other monitors with keyboard short cuts. At some point, I'd love to have that managable via touch though, because what would an LCARS interface be without touchscreen usability.

## Acknowledgements
Thank you very much to the creators of the following resources:
* [Chuan Ji - How X Window Managers Work, And How To Write One](https://jichu4n.com/posts/how-x-window-managers-work-and-how-to-write-one-part-i/)
* [openbox on github](https://github.com/danakj/openbox)
* [mcwm on GitHub](https://github.com/mchackorg/mcwm)
* [XCB implementation of TinyWM on GitHub](https://github.com/stefanbidi/tinywm-xcb)
* [XCB tutorial](https://www.x.org/releases/X11R7.7/doc/libxcb/tutorial/index.html)
* [How to read XPM files and draw them in a window](http://www.dis.uniroma1.it/~liberato/screensaver/image.html)
* [libXpm API](https://fossies.org/dox/libXpm-4.7/files.html)

## Functionality
* Windows-key + Q closes the window manager
* Windows-key + Tab toggles through the windows
* Windows-key + Up/Down moves the active window to other monitors
* Windows-key + M toggles the display mode between normal, maximized and fullscreen
* Windows are resized when the screen resolution is changed and the size depends on the window manager mode.
* Other key combinations can be connected to commands via a key configuration file

## Pictures
Yay, it's starting to get usable. So here are some pictures from the VirtualBox test environment.

Normal mode
![lcarswm in normal mode](doc/images/screen-normal-mode.png)

Maximized mode
![lcarswm in normal mode](doc/images/screen-maximized-mode.png)

Fullscreen mode
![lcarswm in normal mode](doc/images/screen-fullscreen-mode.png)

## Required
* ncurses5-compat-libs: apparently libtinfo.so.5 is used by the compiler, which is part of this package.
* header files and libraries for XLib, randr and xpm. Check the travis yaml file to find the build dependencies in the install routines for Ubuntu.

## Known issues
* Video players crash displaylink driver when using lcarswm - if you know or guess something that could help, then write me ... seriously, I'm out of ideas on this one :-(
* The frame window is drawing over the background and not scaling with popups
* If screens lay over one another they are not "merged" but draw over one another

## To Do
### ICCCM
* Client properties (stuff to use)
  * WM_NAME
  * WM_ICON_NAME ?
  * WM_NORMAL_HINTS
  * WM_HINTS
  * WM_CLASS ?
  * WM_TRANSIENT_FOR
  * WM_PROTOCOLS
    * WM_TAKE_FOCUS
    * ...
  * WM_COLORMAP_WINDOWS
* Window manager properties (stuff to set)
  * WM_ICON_SIZE ?
* State transitions
  * Thinks about handling iconic states (ClientMessage)
* Configuration of clients
  * Fix the handling of ConfigureRequests according to defined window manager responses
* Input handling
  * adjust handling according to WM_HINTS.inputField and WM_TAKE_FOCUS
* Handle ColormapChanges ?
* Icon handling ?
* Popup handling
  * window groups (WM_HINTS)
  * WM_TRANSIENT_FOR
  * override redirect

### EWMH
TODO

### Other
* Signal handling
* Activate windows my clicking on them
* Application selector
* Content for the data bar (empty upper area in normal mode)
  * Time
  * Master volume
  * Heat signature
  * Processor usage
  * Memory usage
  * Connection state of network interfaces
  * Data throughput of network interfaces
  * ...
* GTK-Theme (if I can't find one)
* Configuration for the colors
* Monitors
  * Merge overlaying screens of same size
  * If screens have different sizes, the higher one draws

## Some definitions
* Height of top and bottom bar: 40px
* Width of height and bottom bar ends: 32px
* Width of bar gaps: 8px
* Corner pieces have an inner corner with radius: 10px
* Random data area height: 100px
* Width of normal mode side bar: 184px

## Logo
The logo is a xpm file. It needs to be located in /usr/share/pixmaps and must be named lcarswm.xpm. It can be exchanged. The only restriction is that the logo height needs to be 40px. The bars will adjust to the width.

## Automated testing
Automated tests are set up using Travis CI. It's working well except for the fact, that the whole environment is downloaded again for every test run. That means downloads of over 500MB, which takes time. However, it's working and that's good enough for me for now.

System tests are created using a mocked system facade, that covers all calls to Xlib, POSIX and other things. The difficulty lies within guessing the behavior of the mocked system though, in cases where calls would be dependent on each other when using the actual libraries.

## Manuel testing / running the wm
To manually test the functionality, I've set up a virtual Linux machine in VirtualBox with a shared directory to the generated executables. In this virtual environment, I run the window manager like this:

```
export XDG_CONFIG_HOME="$HOME/.config"
startx /path/to/executable/lcarswm.kexe
```

That runs the X window manager with the lcarswm window manager. The .config folder in the home directory should contain an lcarswm folder with a key-config.properties file with the configuration for the key bindings (see the example file in `src/nativeMain/resources/homedir/.config/lcarswm`).

LCARSWM creates a log file at the path `/var/log/lcarswm.log`.

Probably one of the most interesting features in using a VM and RANDR is screen resizing. For me this works with using VBoxVGA as graphics controller and `VBoxClient-all` needs to be called after the activation of X to allow for resizing detection and all other things VBoxClient offers. Alternatively, VBoxClient can be called with specific flags.
