#!/usr/bin/env python3
import gi

gi.require_version("Gtk", "3.0")
from gi.repository import GdkX11, Gdk, Gtk, GLib


class LcarswmStatusBar(Gtk.Window):
    def __init__(self):
        Gtk.Window.__init__(self, title="lcarswm status bar")

        self.connect("realize", self.on_create)

    def on_create(self, window):
        # mark myself as the app menu
        self.get_property("window").set_utf8_property("LCARSWM_STATUS_BAR", "LCARSWM_STATUS_BAR")


if __name__ == "__main__":
    win = LcarswmStatusBar()
    win.connect("destroy", Gtk.main_quit)
    win.show_all()
    Gtk.main()
