try:
    from status_widget import LcarswmStatusWidget
except ImportError:
    from .status_widget import LcarswmStatusWidget

from datetime import datetime, timezone
import os
import math
from random import randint
import importlib

import gi

gi.require_version("Gtk", "3.0")
gi.require_version('PangoCairo', '1.0')
from gi.repository import Gtk, Pango, PangoCairo


def read_file(file_path):
    with open(file_path, 'r') as file:
        data = file.read().strip()

    return data


class LcarswmStatusText(LcarswmStatusWidget):
    """
    LcarswmStatusText is an abstract class that acts as a frame for widgets
    that display one short line of text. This widget draws text without its
    font ascent and descent area!

    To use: extend this class and override the create_text method.
    """

    def __init__(self, width, height, css_provider, properties):
        LcarswmStatusWidget.__init__(self, width, height, css_provider, properties)

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
    """
    This widget draws the local time in a 24h format.

    preferred: width 4, height 1
    """

    def __init__(self, width, height, css_provider, properties):
        LcarswmStatusText.__init__(self, width, height, css_provider, properties)

    def create_text(self):
        now = datetime.now()
        return now.strftime("%H:%M:%S")


class LcarswmStatusDate(LcarswmStatusText):
    """
    This widget draws the current date.

    preferred: width 4, height 1
    """

    def __init__(self, width, height, css_provider, properties):
        LcarswmStatusText.__init__(self, width, height, css_provider, properties)

    def create_text(self):
        now = datetime.now()
        return now.strftime("%d.%m.%y")


class LcarswmStatusStardate(LcarswmStatusText):
    """
    This widget draws the current star date.

    Hint: I don't know anymore where I got the formula from ...
        got something better? Feel free to adjust it.

    preferred: width 4, height 1
    """

    def __init__(self, width, height, css_provider, properties):
        LcarswmStatusText.__init__(self, width, height, css_provider, properties)

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
    """
    This widget draws temperatures from thermal zones into a graph.

    preferred: width 3, height 3
    """

    def __init__(self, width, height, css_provider, properties):
        LcarswmStatusWidget.__init__(self, width, height, css_provider, properties)

        self.cx = width / 2
        self.cy = height / 2
        self.max_scale = 125
        self.min_dimension = min(self.cx, self.cy)
        self.scale = self.min_dimension / self.max_scale

        self.attention_temperature = 60
        self.warning_temperature = 80

        self.drawing_area = Gtk.DrawingArea()
        self.drawing_area.set_size_request(width, height)
        self.drawing_area.connect('draw', self.draw_graph)
        self.add(self.drawing_area)

        self.update()

    def draw_graph(self, widget, context):
        self.draw_radar_cross(context)
        self.draw_data(context)

    def draw_radar_cross(self, context):
        context.set_source_rgb(0.6, 0.6, 0.8)

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

        if not temperatures:
            # the system doesn't give us temperature sensors (maybe a virtual machine)
            return

        angle = 0
        points = []
        max_temp = 0
        for temp in temperatures:
            point = (self.polar_to_cartesian(temp * self.scale, angle))
            points.append(point)
            if temp > max_temp:
                max_temp = temp
            angle = angle + 360 / len(temperatures)

        context.set_source_rgb(1.0, 0.8, 0.6)
        context.set_source_rgba(1.0, 0.8, 0.6, 0.6)
        if max_temp > self.attention_temperature:
            context.set_source_rgb(1.0, 0.6, 0.0)
            context.set_source_rgba(1.0, 0.6, 0.0, 0.6)
        if max_temp > self.warning_temperature:
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
        path = '/sys/class/thermal'
        files = os.listdir(path)
        temp_dict = {}
        for file in files:
            if file.startswith('thermal_zone'):
                thermal_path = os.path.join(path, file)
                name = read_file(os.path.join(thermal_path, 'type'))
                temp = read_file(os.path.join(thermal_path, 'temp'))
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


class LcarswmStatusAudio(LcarswmStatusWidget):
    """
    This widget has a volume level display and basic audio controls (mute, quieter, louder)

    preferred: width 4, height 1
    """
    def __init__(self, width, height, css_provider, properties):
        LcarswmStatusWidget.__init__(self, width, height, css_provider, properties)

        audio_handler_name = properties["handler"]
        audio_handler_module = properties["handlerModule"]
        audio_handler_package = properties.get("handlerPackage")
        audio_handler_class = getattr(importlib.import_module(audio_handler_module, audio_handler_package),
                                      audio_handler_name)

        self.audio_mixer = audio_handler_class(self.update_mute, self.update_volume, properties)
        self.current_volume = 0
        self.current_mute = False

        box = Gtk.Box(spacing=8)

        lower_audio_button = self.create_button(css_provider, {"button--left", "button--f90"}, self.draw_lower)
        lower_audio_button.connect("clicked", self.lower_volume)
        box.pack_start(lower_audio_button, False, False, 0)

        self.mute_audio_button = self.create_button(css_provider, {"button--middle", "button--99f"}, self.draw_mute)
        self.mute_audio_button.connect("clicked", self.toggle_mute)
        box.pack_start(self.mute_audio_button, False, False, 0)

        self.drawing_area = Gtk.DrawingArea()
        self.drawing_area.set_size_request(40, 40)
        self.drawing_area.connect('draw', self.draw_volume)
        box.pack_start(self.drawing_area, False, False, 0)

        raise_audio_button = self.create_button(css_provider, {"button--right", "button--f90"}, self.draw_raise)
        raise_audio_button.connect("clicked", self.raise_volume)
        box.pack_start(raise_audio_button, False, False, 0)

        self.add(box)

    def start(self):
        self.audio_mixer.start()

    def stop(self):
        self.audio_mixer.stop()

    def lower_volume(self, widget):
        self.audio_mixer.lower_volume()

    def raise_volume(self, widget):
        self.audio_mixer.raise_volume()

    def toggle_mute(self, widget):
        self.audio_mixer.toggle_mute()

    @staticmethod
    def create_button(css_provider, style_classes, icon_draw_handle):
        button = Gtk.Button()
        button.set_size_request(40, 40)
        for style_class in style_classes:
            button.get_style_context().add_class(style_class)
        button.get_style_context().add_provider(css_provider, Gtk.STYLE_PROVIDER_PRIORITY_USER)
        button.set_alignment(.5, .5)

        icon_area = Gtk.DrawingArea()
        icon_area.connect("draw", icon_draw_handle)
        button.add(icon_area)
        return button

    def draw_speaker(self, context):
        context.move_to(28, 10)
        context.line_to(28, 30)
        context.line_to(20, 25)
        context.line_to(12, 25)
        context.line_to(12, 15)
        context.line_to(20, 15)
        context.close_path()
        context.stroke()

    def draw_mute(self, widget, context):
        context.set_source_rgb(0.0, 0.0, 0.0)
        self.draw_speaker(context)

        context.move_to(10, 28)
        context.line_to(30, 14)
        context.stroke()

    def draw_raise(self, widget, context):
        context.set_source_rgb(0.0, 0.0, 0.0)
        self.draw_speaker(context)

        context.move_to(14, 20)
        context.line_to(20, 20)
        context.move_to(17, 17)
        context.line_to(17, 23)
        context.stroke()

    def draw_lower(self, widget, context):
        context.set_source_rgb(0.0, 0.0, 0.0)
        self.draw_speaker(context)

        context.move_to(14, 20)
        context.line_to(20, 20)
        context.stroke()

    def update_mute(self, new_mute):
        self.current_mute = new_mute
        self.drawing_area.queue_draw()
        if new_mute:
            self.mute_audio_button.get_style_context().add_class("button--c66")
            self.mute_audio_button.get_style_context().remove_class("button--99c")
        else:
            self.mute_audio_button.get_style_context().add_class("button--99c")
            self.mute_audio_button.get_style_context().remove_class("button--c66")

    def update_volume(self, new_volume):
        self.current_volume = new_volume
        self.drawing_area.queue_draw()

    def draw_volume(self, widget, context):
        if self.current_mute or (self.current_volume == 0):
            context.set_source_rgb(0.6, 0.6, 0.8)
        else:
            context.set_source_rgb(1.0, 0.8, 0.6)

        # draw sound triangle border
        context.move_to(0, 39)
        context.line_to(39, 39)
        context.line_to(39, 0)
        context.close_path()
        context.stroke()

        # draw volume level
        display_volume = int(self.current_volume * 40 / 100)
        context.rectangle(0, 0, display_volume, 39)
        context.fill()

        # clear volume level drawn above triangle
        context.set_source_rgb(0.0, 0.0, 0.0)
        context.move_to(0, 0)
        context.line_to(38, 0)
        context.line_to(0, 38)
        context.close_path()
        context.fill()


class LcarswmNetworkStatus(LcarswmStatusWidget):
    """
    """
    def __init__(self, width, height, css_provider, properties):
        LcarswmStatusWidget.__init__(self, width, height, css_provider, properties)
        # /proc/net/wireless -> link
        # /sys/class/net -> e* / w*


class LcarswmBatteryStatus(LcarswmStatusWidget):
    """
    This widget displays the status of the configured battery.

    The battery can be set with key "device" in the status-config.xml.

    preferred: width 1, height 1
    """

    def __init__(self, width, height, css_provider, properties):
        LcarswmStatusWidget.__init__(self, width, height, css_provider, properties)

        self.warning_capacity = int(properties.get("warningCapacity", "10"))

        self.drawing_area = Gtk.DrawingArea()
        self.drawing_area.set_size_request(width, height)
        self.drawing_area.connect('draw', self.draw_status)
        self.add(self.drawing_area)

        self.update()

    def draw_status(self, widget, context):
        capacity, status = self.read_battery_status()
        battery_missing = capacity is None

        self.draw_battery(context, battery_missing)
        if not battery_missing:
            self.draw_battery_status(context, capacity, status)

    def read_battery_status(self):
        # status -> Discharging, Charging, Full
        # capacity -> 0 .. 100

        path = '/sys/class/power_supply'
        battery_path = os.path.join(path, self.properties["device"])

        if os.path.isdir(battery_path):
            capacity = int(read_file(os.path.join(battery_path, 'capacity')))
            status = read_file(os.path.join(battery_path, 'status'))
            return capacity, status
        else:
            return None, None

    def draw_battery(self, context, battery_missing):
        if battery_missing:
            context.set_source_rgba(0.8, 0.4, 0.4)
        else:
            context.set_source_rgb(1.0, 0.8, 0.6)

        context.move_to(15, 6)
        context.line_to(15, 0)
        context.line_to(25, 0)
        context.line_to(25, 6)
        context.line_to(29, 6)
        context.line_to(29, 40)
        context.line_to(11, 40)
        context.line_to(11, 6)
        context.line_to(15, 6)
        context.stroke()

    def draw_battery_status(self, context, capacity, status):
        context.set_source_rgba(1.0, 0.8, 0.6, 0.6)
        if status == "Charging":
            context.set_source_rgba(0.6, 0.6, 1.0, 0.6)
        if status == "Discharging":
            context.set_source_rgba(1.0, 0.6, 0.4, 0.6)
        if status == "Discharging" and capacity <= self.warning_capacity:
            context.set_source_rgba(0.8, 0.4, 0.4, 0.6)

        capacity_display = int(capacity * 38 / 100)
        context.rectangle(12, 39, 16, max(-capacity_display, -33))
        context.rectangle(16, 39, 8, -capacity_display)
        context.fill()

    def update(self):
        # read the updated time
        self.drawing_area.queue_draw()


class LcarswmStatusButton(LcarswmStatusWidget):
    """
    This widget is used show a button for executing commands.

    preferred: width 2+, height 1
    """

    COLORS = [
        "f90",
        "c9c",
        "99c",
        "c66",
        "99f",
        "f96",
        "c69"]

    def __init__(self, width, height, css_provider, properties):
        LcarswmStatusWidget.__init__(self, width, height, css_provider, properties)

        text = properties["text"].upper()
        self.button = Gtk.Button(label=text)
        self.button.set_size_request(width, height)
        self.button.set_alignment(1, 1)
        self.button.connect("clicked", self.on_click)
        self.add(self.button)

        color = properties.get("color", "99f")
        if color not in self.COLORS:
            color = "99f"

        self.button.get_style_context().add_class("button--{}".format(color))
        self.button.get_style_context().add_class("button--long")
        self.button.get_style_context().add_provider(css_provider, Gtk.STYLE_PROVIDER_PRIORITY_USER)

    def on_click(self, widget):
        os.system(self.properties["command"])


class LcarswmStatusFiller(LcarswmStatusWidget):
    """
    This widget is used to fill empty space in the status bar.

    preferred: width 2+, height 1
    """

    COLORS = [
        "c9c",
        "99c",
        "f96"]

    def __init__(self, width, height, css_provider, properties):
        LcarswmStatusWidget.__init__(self, width, height, css_provider, properties)

        text = str(randint(0, 9999)).zfill(4)
        self.label = Gtk.Label(label=text)
        self.label.set_size_request(width, height)
        self.label.set_alignment(1, 1)
        self.add(self.label)

        color_index = randint(0, len(self.COLORS) - 1)
        color = self.COLORS[color_index]

        self.label.get_style_context().add_class("button--{}".format(color))
        self.label.get_style_context().add_class("button--long")
        self.label.get_style_context().add_provider(css_provider, Gtk.STYLE_PROVIDER_PRIORITY_USER)
