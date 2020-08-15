try:
    from status_widget import LcarswmStatusWidget
except ImportError:
    from .status_widget import LcarswmStatusWidget

from datetime import datetime, timezone
import os
import math
from random import randint

import gi
gi.require_version("Gtk", "3.0")
gi.require_version('PangoCairo', '1.0')
from gi.repository import Gtk, Pango, PangoCairo


class LcarswmStatusText(LcarswmStatusWidget):
    """
    LcarswmStatusText is an abstract class that acts as a frame for widgets
    that display one short line of text.

    To use: extend this class and override the create_text method.
    """
    def __init__(self, width, height, css_provider):
        LcarswmStatusWidget.__init__(self, width, height, css_provider)

        self.drawing_area = Gtk.DrawingArea()
        self.drawing_area.set_size_request(width, height)
        self.drawing_area.connect('draw', self.draw_text)
        self.add(self.drawing_area)

        self.update()

    def draw_text(self, widget, context):
        context.set_source_rgb(1.0, 0.6, 0.0)
        layout = PangoCairo.create_layout(context)
        layout.set_text(self.create_text(), -1)
        description = Pango.FontDescription('Ubuntu Condensed, 40')
        layout.set_font_description(description)
        width, height = layout.get_size()
        context.move_to((self.width - (float(width) / 1024.)), -11)
        PangoCairo.show_layout(context, layout)

    def create_text(self):
        """
        :return: short line of text that will be displayed in widget
        """
        pass

    def update(self):
        # update the text
        self.drawing_area.queue_draw()


class LcarswmStatusTime(LcarswmStatusText):
    def __init__(self, width, height, css_provider):
        LcarswmStatusText.__init__(self, width, height, css_provider)

    def create_text(self):
        now = datetime.now()
        return now.strftime("%H:%M:%S")


class LcarswmStatusDate(LcarswmStatusText):
    def __init__(self, width, height, css_provider):
        LcarswmStatusText.__init__(self, width, height, css_provider)

    def create_text(self):
        now = datetime.now()
        return now.strftime("%d.%m.%y")


class LcarswmStatusStardate(LcarswmStatusText):
    def __init__(self, width, height, css_provider):
        LcarswmStatusText.__init__(self, width, height, css_provider)

    @staticmethod
    def days_per_month(days_per_year):
        return {
            1: 31,
            2: 29 if days_per_year == 366 else 28,
            3: 31,
            4: 30,
            5: 31,
            6: 30,
            7: 31,
            8: 31,
            9: 30,
            10: 31,
            11: 30
        }

    @staticmethod
    def passed_month_days(current_month, days_per_year):
        days = 0
        for (month, month_days) in LcarswmStatusStardate.days_per_month(days_per_year).items():
            if current_month > month:
                days += month_days
        return days

    @staticmethod
    def calculate_star_date():
        now = datetime.now(timezone.utc)
        years = now.year
        hours = now.hour
        minutes = now.minute
        days_in_year = 366 if years % 4 == 0 and (years % 100 != 0 or years % 400 == 0) else 365
        day = now.day + LcarswmStatusStardate.passed_month_days(now.month, days_in_year)

        earth_time = years + (day - 1 + hours / 24 + minutes / 1440) / days_in_year
        star_date = 1000 * (earth_time - 2323)
        return star_date

    def create_text(self):
        star_date = self.calculate_star_date()
        return f"{star_date:.2f}"[:-1]


class LcarswmStatusTemperature(LcarswmStatusWidget):
    def __init__(self, width, height, css_provider):
        LcarswmStatusWidget.__init__(self, width, height, css_provider)

        self.cx = width / 2
        self.cy = height / 2
        self.max_scale = 125
        self.min_dimension = min(self.cx, self.cy)
        self.scale = self.min_dimension / self.max_scale

        self.drawing_area = Gtk.DrawingArea()
        self.drawing_area.set_size_request(width, height)
        self.drawing_area.connect('draw', self.draw_graph)
        self.add(self.drawing_area)

        self.update()

    def draw_graph(self, widget, context):
        self.draw_radar_cross(context)
        self.draw_data(context)

    def draw_radar_cross(self, context):
        context.set_source_rgb(0.6, 0.6, 0.6)

        context.move_to(0, self.cy)
        context.line_to(self.width, self.cy)
        context.move_to(self.cx, 0)
        context.line_to(self.cx, self.height)
        v1, v2 = self.polar_to_cartesian(self.min_dimension, 135)
        (mi, ma) = (v1, v2) if v1 < v2 else (v2, v1)
        context.move_to(mi, mi)
        context.line_to(ma, ma)
        context.move_to(mi, ma)
        context.line_to(ma, mi)
        context.stroke()

        context.arc(self.cx, self.cy, 100 * self.scale, 0.0, 2.0 * math.pi)
        context.stroke()
        context.arc(self.cx, self.cy, 50 * self.scale, 0.0, 2.0 * math.pi)
        context.stroke()

    def draw_data(self, context):
        temperatures = LcarswmStatusTemperature.sort_dict(LcarswmStatusTemperature.get_temperatures()).values()
        angle = 0
        points = []
        max_temp = 0
        for temp in temperatures:
            point = (self.polar_to_cartesian(temp * self.scale, angle))
            points.append(point)
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
        return self.cx + x, self.cy + y


class LcarswmStatusFiller(LcarswmStatusWidget):
    def __init__(self, width, height, css_provider):
        LcarswmStatusWidget.__init__(self, width, height, css_provider)

        self.label = Gtk.Label()
        self.label.set_size_request(width, height)
        self.label.set_alignment(1, 1)
        self.add(self.label)

        self.label.get_style_context().add_class("button--99c")
        self.label.get_style_context().add_provider(css_provider, Gtk.STYLE_PROVIDER_PRIORITY_USER)

        self.update()

    def update(self):
        # text = str(randint(0, 9999)).zfill(4)
        # self.label.set_label(text)
        pass
