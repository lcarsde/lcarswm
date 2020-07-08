try:
    from . import lcarswm_status_widget
except ImportError:
    print("duh")

try:
    import lcarswm_status_widget
except ImportError:
    print("duh")

try:
    from lcarswm import lcarswm_status_widget
except ImportError:
    print("duh")

try:
    from lcarswm.lcarswm_status_widget import *
except ImportError:
    print("duh")

from datetime import datetime

try:
    import gi
    gi.require_version("Gtk", "3.0")
    from gi.repository import GdkX11, Gdk, Gtk, GLib
except ImportError:
    pass


class LcarswmStatusTime(lcarswm_status_widget.LcarswmStatusWidget):
    def __init__(self, width, height, css_provider):
        super().__init__(width, height, css_provider)

        time_label = Gtk.Label(label="12:34")
        self.add(time_label)

    def update(self):
        # read the updated time
        pass
