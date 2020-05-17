import gi
from threading import Thread
from posix_ipc import MessageQueue, BusyError

gi.require_version("Gtk", "3.0")
from gi.repository import GdkX11, Gtk, GObject


class LcarswmAppSelector(Gtk.Window):
    def __init__(self):
        Gtk.Window.__init__(self, title="Hello World")

        self.button = Gtk.Button(label="Click Me")
        self.button.connect("clicked", self.on_button_clicked)
        self.add(self.button)
        self.set_decorated(False)

        GObject.signal_new("list-update-signal", self, GObject.SIGNAL_RUN_LAST, GObject.TYPE_NONE, (GObject.TYPE_STRING,))
        self.connect("list-update-signal", self.on_list_update)

        self.stop_threads = False
        self.thread = Thread(target=self.read_window_list_from_queue, args=(lambda: self.stop_threads, self))
        self.thread.daemon = True

        self.connect("realize", self.on_create)
        self.connect("destroy", self.on_destroy)

    def on_create(self, window):
        self.get_property("window").set_utf8_property("LCARSWM_APP_SELECTOR", "LCARSWM_APP_SELECTOR")
        print("realized", self.get_property("window").get_xid())
        self.thread.start()

    def on_destroy(self, window):
        self.stop_threads = True
        self.thread.join()

    @staticmethod
    def read_window_list_from_queue(stop, window):
        mq = MessageQueue("/lcarswm-active-window-list")
        while True:
            try:
                s, _ = mq.receive(.4)
                window.emit("list-update-signal", s.decode('utf-8'))
            except BusyError:
                pass

            if stop():
                break

        mq.close()

    @staticmethod
    def on_list_update(self, list_string):
        print("Received:", list_string)

    @staticmethod
    def on_button_clicked(widget):
        print("hello world")


if __name__ == '__main__':
    win = LcarswmAppSelector()
    win.connect("destroy", Gtk.main_quit)
    win.show_all()
    Gtk.main()
