#!/usr/bin/env python3
from threading import Thread
from time import sleep
import importlib
import numpy as np
from lcarswm import internal_widgets as iw
import xml.etree.ElementTree as ET
import os.path

import gi
gi.require_version("Gtk", "3.0")
from gi.repository import GdkX11, Gdk, Gtk, GLib

css = b'''
* {
    font-family: 'Ubuntu Condensed', sans-serif;
    font-weight: 600;
    letter-spacing: 0px;
    text-shadow: none;
    background-color: #000;
    background: #000; /* for Ubuntu */
    border-width: 0;
    box-shadow: none;
    padding: 0;
    margin: 0;
    outline-style: none;
}
.button--f90 {
    font-size: 16px;
    color: #000;
    background-color: #f90;
    background: #f90; /* for Ubuntu */
}
.button--c9c {
    font-size: 16px;
    color: #000;
    background-color: #c9c;
    background: #c9c; /* for Ubuntu */
}
.button--99c {
    font-size: 16px;
    color: #000;
    background-color: #99c;
    background: #99c; /* for Ubuntu */
}
.button--c66 {
    font-size: 16px;
    color: #000;
    background-color: #c66;
    background: #c66; /* for Ubuntu */
}
.button--fc9 {
    font-size: 16px;
    color: #000;
    background-color: #fc9;
    background: #fc9; /* for Ubuntu */
}
.button--99f {
    font-size: 16px;
    color: #000;
    background-color: #99f;
    background: #99f; /* for Ubuntu */
}
.button--f96 {
    font-size: 16px;
    color: #000;
    background-color: #f96;
    background: #f96; /* for Ubuntu */
}
.button--c69 {
    font-size: 16px;
    color: #000;
    background-color: #f96;
    background: #f96; /* for Ubuntu */
}
.button--long {
    border-radius: 20px;
    padding: 2px 16px;
}
.button--left {
    border-radius: 20px 0 0 20px;
}
.button--middle {
    border-radius: 0;
}
.button--right {
    border-radius: 0 20px 20px 0;
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
    def __init__(self, package, module, widget, x, y, width, height, properties):
        self.package = package
        self.module = module
        self.widget = widget
        self.x = x
        self.y = y
        self.width = width
        self.height = height
        self.properties = properties


def get_configuration_path():
    path = "{0}/.config/lcarswm/status-config.xml".format(os.environ.get('HOME'))
    if not os.path.isfile(path):
        print("no local config -> falling back to global config")
        path = "/etc/lcarswm/status-config.xml"
    return path


def load_widget_configuration(path):
    root = ET.ElementTree(file=path).getroot()
    widgets = set()

    for configuration in root:
        package = None if "package" not in configuration.attrib else configuration.attrib["package"]
        module = configuration.attrib["module"]
        name = configuration.attrib["name"]
        x = y = width = height = None
        properties = {}
        for elem in configuration:
            if elem.tag == "position":
                x = int(elem.attrib["x"])
                y = int(elem.attrib["y"])
                width = int(elem.attrib["width"])
                height = int(elem.attrib["height"])
            elif elem.tag == "properties":
                for property in elem:
                    properties[property.attrib["key"]] = property.attrib["value"]
        widgets.add(WidgetConfiguration(package, module, name, x, y, width, height, properties))

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

        self.width = 40
        self.height = LcarswmStatusBar.get_pixels_for_cells(3)  # remains constant by design

        self.grid = Gtk.Grid()
        self.grid.set_column_spacing(GAP_SIZE)
        self.grid.set_row_spacing(GAP_SIZE)
        self.grid.get_style_context().add_provider(self.css_provider, Gtk.STYLE_PROVIDER_PRIORITY_USER)
        self.add(self.grid)

        self.widget_dict = self.import_widgets()

        self.stop_threads = False
        self.update_thread = Thread(target=self.update_widgets, args=(lambda: self.stop_threads, self))
        self.update_thread.daemon = True

        self.connect("size-allocate", self.on_size_allocate)
        self.connect("realize", self.on_create)
        self.connect("destroy", self.on_destroy)

    def on_create(self, window):
        """
        Called when the window is created.
        - Set the atom that tells lcarswm that this is the status bar tool window.
        - Start the widget update threading

        :param window: the window (same as self)
        """
        # mark myself as the app menu
        self.get_property("window").set_utf8_property("LCARSWM_STATUS_BAR", "LCARSWM_STATUS_BAR")
        self.update_thread.start()
        for widget in self.widget_dict.keys():
            widget.start()

    def on_destroy(self, window):
        """
        Clean up on shutdown -> stop the update threads.

        :param window: the window (same as self)
        """
        self.stop_threads = True
        for widget in self.widget_dict.keys():
            widget.stop()
        self.update_thread.join()

    def on_size_allocate(self, window, event):
        """
        Trigger an update of the status bar grid layout when the status bar size changes.

        :param window: the window (same as self)
        :param event: the size allocation event
        """
        new_width = self.get_allocation().width
        if new_width != self.width:
            self.width = new_width
            self.update_layout()

    def import_widgets(self):
        """
        Import the configured widgets.

        :return: dictionary of the widget instances and their configurations
        """
        widget_dict = {}
        configuration_path = get_configuration_path()
        for widget_config in load_widget_configuration(configuration_path):
            widget = getattr(importlib.import_module(widget_config.module, widget_config.package), widget_config.widget)
            widget_instance = widget(LcarswmStatusBar.get_pixels_for_cells(widget_config.width),
                                     LcarswmStatusBar.get_pixels_for_cells(widget_config.height),
                                     self.css_provider,
                                     widget_config.properties)
            widget_dict[widget_instance] = widget_config
        return widget_dict

    @staticmethod
    def get_pixels_for_cells(cells):
        """
        Convert cells into pixels.

        :param cells: number of cells
        :return: pixels correspond to the given cell amount
        """
        return CELL_SIZE * cells + GAP_SIZE * (cells - 1)

    def update_layout(self):
        """
        Update the status bar grid layout.
        """
        horizontal_cells, left_over_pixels = self.get_cells_and_overflow()
        self.cleanup_grid()
        self.fill_status_bar(horizontal_cells, left_over_pixels)

    def get_cells_and_overflow(self):
        """
        :return: the number of currently available horizontal cells and the rest of the pixels that don't fit
        """
        horizontal_cells = int(self.width / (CELL_SIZE + GAP_SIZE))
        left_over_pixels = self.width % (CELL_SIZE + GAP_SIZE)
        if left_over_pixels >= CELL_SIZE:
            horizontal_cells += 1
            left_over_pixels -= CELL_SIZE
        else:
            left_over_pixels += GAP_SIZE
        return horizontal_cells, left_over_pixels

    def cleanup_grid(self):
        """
        Cleanup the current grid a.k.a. remove all the child widgets from it.
        """
        for widget in self.grid.get_children():
            self.grid.remove(widget)

    def fill_status_bar(self, horizontal_cells, left_over_pixels):
        """
        Fill the status bar with the configured widgets and fillers for the empty space.

        :param horizontal_cells: number of available horizontal cells
        :param left_over_pixels: rest of the pixels that don't fit the cells
        """
        widget_map = self.fill_configured_widgets(horizontal_cells)

        self.fill_empty_space(widget_map, left_over_pixels)

        self.grid.show_all()

    def fill_configured_widgets(self, horizontal_cells):
        """
        Fill the configured widgets into the grid.
        
        :param horizontal_cells: number of available horizontal cells
        :return: 2D array (map) that shows where widgets are configured to be in the grid
        """
        widget_map = np.zeros((3, horizontal_cells), dtype=int)
        for widget, config in self.widget_dict.items():
            x, y = horizontal_cells - config.x - config.width, config.y
            x_end, y_end = horizontal_cells - config.x, config.y + config.height
            if (config.x + config.width) > horizontal_cells:
                continue
            widget_map[y:y_end, x:x_end] = 1
            self.grid.attach(widget, x, y, config.width, config.height)
        return widget_map

    def fill_empty_space(self, widget_map, left_over_pixels):
        """
        Fill unused status bar space.

        :param widget_map: 2D array (map) that shows where widgets are configured to be in the grid
        :param left_over_pixels: rest of the pixels that don't fit the cells
        """
        first_non_empty_index = np.min((widget_map != 0).argmax(axis=1))

        pixels_to_add = left_over_pixels
        if first_non_empty_index % 2 != 0:
            pixels_to_add += CELL_SIZE + GAP_SIZE

        filler_count = int(first_non_empty_index / 2)
        if filler_count == 0:
            self.fill_with_empty_space(pixels_to_add)
        else:
            self.fill_with_filler_widgets(filler_count, pixels_to_add)

    def fill_with_empty_space(self, pixels_to_add):
        """
        Fill the empty status bar space with an empty widget (necessary if the filler is to big for the empty space)

        :param pixels_to_add: pixels to fill
        """
        if pixels_to_add <= 0:
            return

        col_index = -1 if pixels_to_add == GAP_SIZE else 0

        self.grid.attach(iw.LcarswmStatusWidget(pixels_to_add - GAP_SIZE, CELL_SIZE, self.css_provider, {}),
                         col_index, 0, 1, 1)

    def fill_with_filler_widgets(self, filler_count, pixels_to_add):
        """
        Fill the empty status bar space with filler widgets.

        :param filler_count: number of fillers
        :param pixels_to_add: amount of pixels to distribute over fillers (not fitting into used cells)
        """
        pixels_per_filler = LcarswmStatusBar.additional_pixels_per_filler(filler_count, pixels_to_add)

        for col in range(filler_count):
            width = CELL_SIZE * 2 + GAP_SIZE + pixels_per_filler[col]
            for row in range(3):
                self.grid.attach(iw.LcarswmStatusFiller(width, CELL_SIZE, self.css_provider, {}), col*2, row, 2, 1)

    @staticmethod
    def additional_pixels_per_filler(filler_count, pixels_to_add):
        """
        Calculate the distribution of additional pixels over all fillers.

        :param filler_count: number of filler widgets
        :param pixels_to_add: amount of pixels to distribute
        :return: array with pixels per filler
        """
        pixels_per_filler = np.empty(filler_count, dtype=int)
        if filler_count == 0:
            return []

        full_pixels = int(np.floor(pixels_to_add / filler_count))
        rest_pixels = pixels_to_add % filler_count

        pixels_per_filler.fill(full_pixels)
        pixels_per_filler[0:rest_pixels] = pixels_per_filler[0:rest_pixels] + 1

        return pixels_per_filler

    @staticmethod
    def update_widgets(stop, self):
        """
        Trigger a widget update (called in daemon thread).
        """
        while True:
            for widget in self.widget_dict:
                GLib.idle_add(widget.update)

            if stop():
                break

            sleep(.3)


if __name__ == "__main__":
    win = LcarswmStatusBar()
    win.connect("destroy", Gtk.main_quit)
    win.show_all()
    Gtk.main()
