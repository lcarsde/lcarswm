#!/usr/bin/env python3
from lcarswm import internal_widgets
from threading import Thread
from time import sleep

import gi
gi.require_version("Gtk", "3.0")
from gi.repository import GdkX11, Gdk, Gtk, GLib

css = b'''
* {
    font-family: 'Ubuntu Condensed', sans-serif;
    font-weight: 600;
    text-shadow: none;
    background-color: #000;
    background: #000; /* for Ubuntu */
    border-width: 0;
    box-shadow: none;
    padding: 0;
    margin: 0;
    outline-style: none;
}
.button--99c {
    font-family: 'Ubuntu Condensed', sans-serif;
    font-weight: 600;
    font-size: 18px;
    letter-spacing: 0px;
    color: #000;
    background-color: #99c;
    background: #99c; /* for Ubuntu */
    border-radius: 20px;
    padding: 2px 16px;
}
.window {
    background-color: #000;
    background: #000; /* for Ubuntu */
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
        self.width = 640
        self.height = 136  # remains constant by designs

        grid = Gtk.Grid()
        grid.set_column_spacing(8)
        grid.set_row_spacing(8)
        grid.get_style_context().add_provider(self.css_provider, Gtk.STYLE_PROVIDER_PRIORITY_USER)
        self.add(grid)

        time_widget = internal_widgets.LcarswmStatusTime(184, 40, self.css_provider)
        grid.attach(time_widget, 0, 0, 4, 1)
        self.status_widgets.add(time_widget)

        date = internal_widgets.LcarswmStatusDate(184, 40, self.css_provider)
        grid.attach(date, 0, 1, 4, 1)
        self.status_widgets.add(date)

        star_date = internal_widgets.LcarswmStatusStardate(184, 40, self.css_provider)
        grid.attach(star_date, 0, 2, 4, 1)
        self.status_widgets.add(star_date)

        temperature_widget = internal_widgets.LcarswmStatusTemperature(136, 136, self.css_provider)
        grid.attach(temperature_widget, 4, 0, 3, 3)
        self.status_widgets.add(temperature_widget)

        filler_test = internal_widgets.LcarswmStatusFiller(88, 40, self.css_provider)
        grid.attach(filler_test, 7, 0, 2, 1)
        self.status_widgets.add(filler_test)

        self.stop_threads = False
        self.update_thread = Thread(target=self.update_widgets, args=(lambda: self.stop_threads, self))
        self.update_thread.daemon = True

        self.connect("size-allocate", self.on_size_allocate)
        self.connect("realize", self.on_create)
        self.connect("destroy", self.on_destroy)

    def on_create(self, window):
        # mark myself as the app menu
        self.get_property("window").set_utf8_property("LCARSWM_STATUS_BAR", "LCARSWM_STATUS_BAR")
        self.update_thread.start()

    def on_destroy(self, window):
        self.stop_threads = True
        self.update_thread.join()

    def on_size_allocate(self, widget, event):
        new_width = self.get_allocation().width
        if new_width != self.width:
            self.width = new_width
            self.update_layout()

    def update_layout(self):
        # TODO update layout
        print(self.width)

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
