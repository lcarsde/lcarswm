try:
    from status_widget import LcarswmStatusWidget
except ImportError:
    from .status_widget import LcarswmStatusWidget

from datetime import datetime

import gi
gi.require_version("Gtk", "3.0")
gi.require_version('PangoCairo', '1.0')
from gi.repository import Gtk, Pango, PangoCairo


class LcarswmStatusTime(LcarswmStatusWidget):
    def __init__(self, width, height, css_provider):
        LcarswmStatusWidget.__init__(self, width, height, css_provider)

        self.drawing_area = Gtk.DrawingArea()
        self.drawing_area.set_size_request(width, height)
        self.drawing_area.connect('draw', self.draw_time)
        self.add(self.drawing_area)

        self.update()

    def draw_time(self, widget, context):
        now = datetime.now()

        context.set_source_rgb(1.0, 0.6, 0.0)
        layout = PangoCairo.create_layout(context)
        layout.set_text(now.strftime("%H:%M:%S"), -1)
        description = Pango.FontDescription('Ubuntu Condensed, 40')
        layout.set_font_description(description)
        width, height = layout.get_size()
        context.move_to(((self.width - (float(width) / 1024.)) / 2), -11)
        PangoCairo.show_layout(context, layout)

    def update(self):
        # read the updated time
        self.drawing_area.queue_draw()
