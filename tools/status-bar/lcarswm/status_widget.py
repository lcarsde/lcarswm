import gi

gi.require_version("Gtk", "3.0")
from gi.repository import Gtk


class LcarswmStatusWidget(Gtk.Bin):
    """
    General widget frame for lcarswm status widgets
    """
    def __init__(self, width, height, css_provider, properties):
        Gtk.Bin.__init__(self)
        self.width = width
        self.height = height
        self.set_size_request(width, height)
        self.properties = properties

        self.get_style_context().add_class("status_bar_widget")
        self.get_style_context().add_provider(css_provider, Gtk.STYLE_PROVIDER_PRIORITY_USER)

    def update(self):
        """
        Called for triggering an update of the widget data.
        """
        pass

    def start(self):
        """
        Called on creation of status bar. Use to initialize threads and things.
        """
        pass

    def stop(self):
        """
        Called on shutdown of the status bar. Use to stop threads and cleanup things.
        """
        pass
