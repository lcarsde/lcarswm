import gi
from threading import Thread
from posix_ipc import MessageQueue, BusyError

gi.require_version("Gtk", "3.0")
from gi.repository import GdkX11, Gdk, Gtk, GLib


class LcarswmAppSelector(Gtk.Window):
    def __init__(self):
        Gtk.Window.__init__(self, title="Hello World")

        self.box = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=8)
        self.add(self.box)
        self.buttons = {}

        self.set_decorated(False)
        self.override_background_color(Gtk.StateType.NORMAL, Gdk.RGBA(0, 0, 0, 1))

        self.stop_threads = False
        self.thread = Thread(target=self.read_window_list_from_queue, args=(lambda: self.stop_threads, self))
        self.thread.daemon = True

        self.connect("realize", self.on_create)
        self.connect("destroy", self.on_destroy)

    def on_create(self, window):
        # mark myself as the app menu
        self.get_property("window").set_utf8_property("LCARSWM_APP_SELECTOR", "LCARSWM_APP_SELECTOR")
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
                GLib.idle_add(window.on_list_update, window, s.decode('utf-8'))
            except BusyError:
                pass

            if stop():
                break

        mq.close()

    @staticmethod
    def on_list_update(self, list_string):
        updated_window_elements = dict((window_id, class_name) for window_id, class_name in
                                       (window_element.split("\t") for window_element in list_string.split("\n")))

        known_windows = list(self.buttons.keys())
        self.cleanup_windows(known_windows, updated_window_elements)

        self.handle_current_windows(known_windows, updated_window_elements)
        self.show_all()

    def cleanup_windows(self, known_windows, updated_window_elements):
        for known_window_id in known_windows:
            if known_window_id not in updated_window_elements.keys():
                button = self.buttons[known_window_id]
                self.box.remove(button)
                del self.buttons[known_window_id]

    def handle_current_windows(self, known_windows, updated_window_elements):
        for (window_id, class_name) in updated_window_elements.items():
            if window_id in known_windows:
                self.update_window(window_id, class_name)
            else:
                self.add_window(window_id, class_name)

    def update_window(self, window_id, class_name):
        button = self.buttons[window_id]
        button.set_label(class_name)

    def add_window(self, window_id, class_name):
        button = Gtk.Button(label=class_name)
        button.connect("clicked", self.on_button_clicked)
        self.box.pack_start(button, False, False, 0)
        self.buttons[window_id] = button

    @staticmethod
    def on_button_clicked(widget):
        print("hello world")


if __name__ == '__main__':
    win = LcarswmAppSelector()
    win.connect("destroy", Gtk.main_quit)
    win.show_all()
    Gtk.main()
