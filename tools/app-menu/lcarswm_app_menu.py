#!/usr/bin/env python3
import gi
from threading import Thread
from posix_ipc import MessageQueue, BusyError

gi.require_version("Gtk", "3.0")
from gi.repository import GdkX11, Gdk, Gtk, GLib


css = b'''
.select_button {
    font-family: 'Ubuntu Condensed', sans-serif;
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


class WindowEntry(Gtk.Box):
    """
        Window entry for selecting or closing the associated window
    """
    def __init__(self, window_id, class_name, css_provider, send_queue):
        Gtk.Box.__init__(self, spacing=8)

        self.window_id = window_id
        self.class_name = class_name
        self.sendQueue = send_queue

        self.select_button = Gtk.Button(label=class_name)
        self.select_button.set_size_request(184, 40)
        self.select_button.set_alignment(1, 1)
        self.select_button.get_style_context().add_class("select_button")
        self.select_button.get_style_context().add_provider(css_provider, Gtk.STYLE_PROVIDER_PRIORITY_USER)
        self.select_button.connect("clicked", self.on_select_clicked)
        self.pack_start(self.select_button, False, False, 0)

        close_button = Gtk.Button(label="")
        close_button.set_size_request(32, 40)
        close_button.get_style_context().add_class("close_button")
        close_button.get_style_context().add_provider(css_provider, Gtk.STYLE_PROVIDER_PRIORITY_USER)
        close_button.connect("clicked", self.on_close_clicked)
        self.pack_start(close_button, False, False, 0)

    def on_select_clicked(self, widget):
        self.sendQueue.send("select\n{0}".format(self.window_id).encode())

    def on_close_clicked(self, widget):
        self.sendQueue.send("close\n{0}".format(self.window_id).encode())

    def update_label(self, class_name):
        self.class_name = class_name
        self.select_button.set_label(class_name)


class LcarswmAppMenu(Gtk.Window):
    """
        Application menu main window
    """
    def __init__(self):
        Gtk.Window.__init__(self, title="lcarswm app menu")

        self.css_provider = Gtk.CssProvider()
        self.css_provider.load_from_data(css)

        scroll_container = Gtk.ScrolledWindow()
        scroll_container.set_policy(Gtk.PolicyType.NEVER, Gtk.PolicyType.AUTOMATIC)

        self.app_container = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=8)

        spacer = Gtk.Label(label="")
        spacer.get_style_context().add_class("spacer")
        spacer.get_style_context().add_provider(self.css_provider, Gtk.STYLE_PROVIDER_PRIORITY_USER)
        self.app_container.pack_end(spacer, True, True, 0)

        scroll_container.add(self.app_container)
        self.add(scroll_container)
        self.entries = {}

        self.set_decorated(False)
        self.get_style_context().add_class("window")
        self.get_style_context().add_provider(self.css_provider, Gtk.STYLE_PROVIDER_PRIORITY_USER)

        self.sendQueue = MessageQueue("/lcarswm-app-menu-messages")

        self.stop_threads = False
        self.thread = Thread(target=self.read_window_list_from_queue, args=(lambda: self.stop_threads, self))
        self.thread.daemon = True

        self.connect("realize", self.on_create)
        self.connect("destroy", self.on_destroy)

    def on_create(self, window):
        # mark myself as the app menu
        self.get_property("window").set_utf8_property("LCARSWM_APP_MENU", "LCARSWM_APP_MENU")
        self.thread.start()

    def on_destroy(self, window):
        self.stop_threads = True
        self.sendQueue.close()
        self.thread.join()

    @staticmethod
    def read_window_list_from_queue(stop, window):
        mq = MessageQueue("/lcarswm-active-window-list")
        while True:
            try:
                s, _ = mq.receive(.4)
                GLib.idle_add(window.on_list_update, window, s.decode("utf-8"))
            except BusyError:
                pass

            if stop():
                break

        mq.close()

    @staticmethod
    def on_list_update(self, list_string):
        data_list = list_string.splitlines()
        if data_list[0] != "list":
            return

        updated_window_elements = dict((window_id, class_name) for window_id, class_name in
                                       (window_element.split("\t") for window_element in data_list[1:]))

        known_windows = list(self.entries.keys())
        self.cleanup_windows(known_windows, updated_window_elements)

        self.handle_current_windows(known_windows, updated_window_elements)
        self.show_all()

    def cleanup_windows(self, known_windows, updated_window_elements):
        for known_window_id in known_windows:
            if known_window_id not in updated_window_elements.keys():
                entry = self.entries[known_window_id]
                self.app_container.remove(entry)
                del self.entries[known_window_id]

    def handle_current_windows(self, known_windows, updated_window_elements):
        for (window_id, class_name) in updated_window_elements.items():
            if window_id in known_windows:
                self.update_window(window_id, class_name)
            else:
                self.add_window(window_id, class_name)

    def update_window(self, window_id, class_name):
        entry = self.entries[window_id]
        entry.update_label(class_name)

    def add_window(self, window_id, class_name):
        entry = WindowEntry(window_id, class_name, self.css_provider, self.sendQueue)
        self.app_container.pack_start(entry, False, False, 0)
        self.entries[window_id] = entry


if __name__ == "__main__":
    win = LcarswmAppMenu()
    win.connect("destroy", Gtk.main_quit)
    win.show_all()
    Gtk.main()
