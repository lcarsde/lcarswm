#!/usr/bin/env python3
from threading import Thread
from time import sleep
import importlib

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


CELL_SIZE = 40
GAP_SIZE = 8


class WidgetConfiguration:
    """
    Container for widget definition
    """
    def __init__(self, package, module, widget, x, y, width, height):
        self.package = package
        self.module = module
        self.widget = widget
        self.x = x
        self.y = y
        self.width = width
        self.height = height


def get_widgets():
    widgets = set()

    widgets.add(WidgetConfiguration(None, "lcarswm.internal_widgets", "LcarswmStatusTime", 0, 0, 4, 1))
    widgets.add(WidgetConfiguration(None, "lcarswm.internal_widgets", "LcarswmStatusDate", 0, 1, 4, 1))
    widgets.add(WidgetConfiguration(None, "lcarswm.internal_widgets", "LcarswmStatusStardate", 0, 2, 4, 1))
    widgets.add(WidgetConfiguration(None, "lcarswm.internal_widgets", "LcarswmStatusTemperature", 4, 0, 3, 3))
    widgets.add(WidgetConfiguration(None, "lcarswm.internal_widgets", "LcarswmStatusFiller", 7, 0, 2, 1))

    return widgets


class LcarswmStatusBar(Gtk.Window):
    """
    The status bar window implementation
    """
    def __init__(self):
        Gtk.Window.__init__(self, title="lcarswm status bar")

        self.css_provider = Gtk.CssProvider()
        self.css_provider.load_from_data(css)

        self.set_decorated(False)
        self.get_style_context().add_class("window")
        self.get_style_context().add_provider(self.css_provider, Gtk.STYLE_PROVIDER_PRIORITY_USER)

        self.width = 640
        self.height = LcarswmStatusBar.get_pixels_for_cells(3)  # remains constant by design

        grid = Gtk.Grid()
        grid.set_column_spacing(GAP_SIZE)
        grid.set_row_spacing(GAP_SIZE)
        grid.get_style_context().add_provider(self.css_provider, Gtk.STYLE_PROVIDER_PRIORITY_USER)
        self.add(grid)

        self.widget_dict = self.import_widgets(grid)

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

    def import_widgets(self, grid):
        widget_dict = {}
        for widget_config in get_widgets():
            widget = getattr(importlib.import_module(widget_config.module, widget_config.package), widget_config.widget)
            widget_instance = widget(LcarswmStatusBar.get_pixels_for_cells(widget_config.width),
                                     LcarswmStatusBar.get_pixels_for_cells(widget_config.height),
                                     self.css_provider)
            widget_dict[widget_instance] = widget_config
            # TODO remove this
            grid.attach(widget_instance, widget_config.x, widget_config.y, widget_config.width, widget_config.height)
        return widget_dict

    @staticmethod
    def get_pixels_for_cells(cells):
        return CELL_SIZE * cells + GAP_SIZE * (cells - 1)

    def update_layout(self):
        # TODO update layout
        horizontal_cells = int(self.width / CELL_SIZE)
        left_over_pixels = self.width % CELL_SIZE
        print(self.width, horizontal_cells, left_over_pixels)

    @staticmethod
    def update_widgets(stop, self):
        while True:
            for status_widget in self.widget_dict:
                GLib.idle_add(status_widget.update)

            if stop():
                break

            sleep(.3)


if __name__ == "__main__":
    win = LcarswmStatusBar()
    win.connect("destroy", Gtk.main_quit)
    win.show_all()
    Gtk.main()
