try:
    from status_widget import LcarswmStatusWidget
except ImportError:
    from .status_widget import LcarswmStatusWidget

from datetime import datetime
import os
import math

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


class LcarswmStatusTemperature(LcarswmStatusWidget):
    def __init__(self, width, height, css_provider):
        LcarswmStatusWidget.__init__(self, width, height, css_provider)

        self.xc = width / 2
        self.yc = height / 2

        self.drawing_area = Gtk.DrawingArea()
        self.drawing_area.set_size_request(width, height)
        self.drawing_area.connect('draw', self.draw_graph)
        self.add(self.drawing_area)

        self.update()

    def draw_graph(self, widget, context):
        max_scale = 125
        min_dimension = min(self.xc, self.yc)
        scale = min_dimension / max_scale

        self.draw_radar_cross(context, min_dimension, scale)
        self.draw_data(context, scale)

    def draw_radar_cross(self, context, min_dimension, scale):
        context.set_source_rgb(0.6, 0.6, 0.6)

        context.move_to(0, self.yc)
        context.line_to(self.width, self.yc)
        context.move_to(self.xc, 0)
        context.line_to(self.xc, self.height)
        v1, v2 = self.polar_to_cartesian(min_dimension, 135)
        (mi, ma) = (v1, v2) if v1 < v2 else (v2, v1)
        context.move_to(mi, mi)
        context.line_to(ma, ma)
        context.move_to(mi, ma)
        context.line_to(ma, mi)
        context.stroke()

        context.arc(self.xc, self.yc, 100 * scale, 0.0, 2.0 * math.pi)
        context.stroke()
        context.arc(self.xc, self.yc,  50 * scale, 0.0, 2.0 * math.pi)
        context.stroke()

    def draw_data(self, context, scale):
        temperatures = LcarswmStatusTemperature.sort_dict(LcarswmStatusTemperature.get_temperatures()).values()
        angle = 0
        points = []
        max_temp = 0
        for temp in temperatures:
            point = (self.polar_to_cartesian(temp * scale, angle))
            points .append(point)
            if temp > max_temp:
                max_temp = temp
            angle = angle + 360/len(temperatures)

        context.set_source_rgb(1.0, 0.8, 0.6)
        context.set_source_rgba(1.0, 0.8, 0.6, 0.6)
        if max_temp > 60:
            context.set_source_rgb(1.0, 0.6, 0.0)
            context.set_source_rgba(1.0, 0.6, 0.0, 0.6)
        if max_temp > 80:
            context.set_source_rgb(0.8, 0.4, 0.4)
            context.set_source_rgba(0.8, 0.4, 0.4, 0.6)

        (x, y) = points[0]
        context.move_to(x, y)
        for i in range(1, len(points)):
            (x, y) = points[i]
            context.line_to(x, y)
        context.close_path()
        context.fill_preserve()
        context.stroke()

    def update(self):
        # read the updated time
        self.drawing_area.queue_draw()

    @staticmethod
    def get_temperatures():
        # get every /sys/class/thermal/thermal_zone* directory
        # read type and temp
        # set the data
        cat = lambda file: open(file, 'r').read().strip()
        path = '/sys/class/thermal'
        files = os.listdir(path)
        temp_dict = {}
        for file in files:
            if file.startswith('thermal_zone'):
                thermal_path = os.path.join(path, file)
                name = cat(os.path.join(thermal_path, 'type'))
                temp = cat(os.path.join(thermal_path, 'temp'))
                temp_dict[name] = int(temp) / 1000
        return temp_dict

    @staticmethod
    def sort_dict(data):
        new_dict = {}
        for key in sorted(data.keys()):
            new_dict[key] = data[key]
        return new_dict

    def polar_to_cartesian(self, radius, angle):
        x = radius * math.cos(math.radians(angle))
        y = radius * math.sin(math.radians(angle))
        return self.xc+x, self.yc+y

