import gi

gi.require_version("Gtk", "3.0")
from gi.repository import GdkX11, Gtk


class LcarswmAppSelector(Gtk.Window):
    def __init__(self):
        Gtk.Window.__init__(self, title="Hello World")

        self.button = Gtk.Button(label="Click Me")
        self.button.connect("clicked", self.on_button_clicked)
        self.add(self.button)
        self.set_decorated(False)
        self.connect("realize", self.on_create)

    def on_create(self, window):
        self.get_property("window").set_utf8_property("LCARSWM_APP_SELECTOR", "LCARSWM_APP_SELECTOR")
        print("realized", self.get_property("window").get_xid())

    def on_button_clicked(self, widget):
        print("hello world")


if __name__ == '__main__':
    win = LcarswmAppSelector()
    win.connect("destroy", Gtk.main_quit)
    win.show_all()
    Gtk.main()
