#!/usr/bin/env python3
from lcarswm import status_time
from threading import Thread
from time import sleep

import gi
gi.require_version("Gtk", "3.0")
from gi.repository import GdkX11, Gdk, Gtk, GLib

css = b'''
* {
    font-family: 'Ubuntu Condensed', psans-serif;
    font-weight: 600;
}
.select_button {
    font-family: 'Ubuntu Condensed', psans-serif;
    font-weight: 600;
    font-size: 15px;
    color: #000;
    text-shadow: none;
    background-color: #99F;
    background: #99F; /* for Ubuntu */
    outline-style: none;
    border-radius: 0;
    border-width: 0;
    box-shadow: none;
    padding: 2px 3px;
    margin: 0;
}
.close_button {
    background-color: #C66;
    background: #C66; /* for Ubuntu */
    outline-style: none;
    border-radius: 0 20px 20px 0;
    border-width: 0;
    box-shadow: none;
    padding: 0;
    margin: 0;
}
.spacer {
    background-color: #99C;
    outline-style: none;
    border-radius: 0;
    padding: 0;
    margin: 0 40px 0 0;
}
.window {
    background-color: #000;
}
'''


class LcarswmStatusBar(Gtk.Window):
    def __init__(self):
        Gtk.Window.__init__(self, title="lcarswm status bar")

        self.css_provider = Gtk.CssProvider()
        self.css_provider.load_from_data(css)

        self.set_decorated(False)
        self.get_style_context().add_class("window")
        self.get_style_context().add_provider(self.css_provider, Gtk.STYLE_PROVIDER_PRIORITY_USER)

        self.status_widgets = set()

        grid = Gtk.Grid()
        grid.set_column_spacing(8)
        grid.set_row_spacing(8)
        self.add(grid)

        time_widget = status_time.LcarswmStatusTime(0, 0, self.css_provider)
        grid.add(time_widget)
        self.status_widgets.add(time_widget)

        self.stop_threads = False
        self.update_thread = Thread(target=self.update_widgets, args=(lambda: self.stop_threads, self))
        self.update_thread.daemon = True

        self.connect("realize", self.on_create)
        self.connect("destroy", self.on_destroy)

    def on_create(self, window):
        # mark myself as the app menu
        self.get_property("window").set_utf8_property("LCARSWM_STATUS_BAR", "LCARSWM_STATUS_BAR")
        self.update_thread.start()

    def on_destroy(self, window):
        self.stop_threads = True
        self.update_thread.join()

    @staticmethod
    def update_widgets(stop, self):
        while True:
            for status_widget in self.status_widgets:
                GLib.idle_add(status_widget.update)

            if stop():
                break

            sleep(.3)


if __name__ == "__main__":
    win = LcarswmStatusBar()
    win.connect("destroy", Gtk.main_quit)
    win.show_all()
    Gtk.main()
