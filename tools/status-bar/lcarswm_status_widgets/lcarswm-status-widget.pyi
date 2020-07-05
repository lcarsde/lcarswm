import gi

gi.require_version("Gtk", "3.0")
from gi.repository import GdkX11, Gdk, Gtk, GLib

class LcarswmStatusWidget(Gtk.Widget):
    def __init__(self, width, height, css_provider):
        self.width = width
        self.heigth = height

        self.get_style_context().add_class("status_bar_widget")
        self.get_style_context().add_provider(css_provider, Gtk.STYLE_PROVIDER_PRIORITY_USER)