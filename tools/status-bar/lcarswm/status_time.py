try:
    from status_widget import LcarswmStatusWidget
except ImportError:
    from .status_widget import LcarswmStatusWidget

from datetime import datetime

import gi
gi.require_version("Gtk", "3.0")
from gi.repository import GdkX11, Gdk, Gtk, GLib


class LcarswmStatusTime(LcarswmStatusWidget):
    def __init__(self, width, height, css_provider):
        LcarswmStatusWidget.__init__(self, width, height, css_provider)

        time_label = Gtk.Label(label="12:34")
        self.add(time_label)

    def update(self):
        # read the updated time
        pass
