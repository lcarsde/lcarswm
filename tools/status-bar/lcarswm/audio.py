import alsaaudio
import threading
import select
import logging


logger = logging.getLogger(__name__)


class AudioMixer:
    """
    Interface for audio mixer implementations. Implementations of this class allow audio handling in terms
    of lowering and raising the volume as well as toggling the mute status.
    """

    def __init__(self, mute_change_callback, volume_change_callback, properties):
        self.mute_change_callback = mute_change_callback
        self.volume_change_callback = volume_change_callback
        self.properties = properties

    def start(self):
        pass

    def stop(self):
        pass

    def lower_volume(self):
        pass

    def raise_volume(self):
        pass

    def toggle_mute(self):
        pass


class AlsaAudioMixer(AudioMixer):
    """
    AudioMixer implementation for ALSA.
    """
    name = "AlsaAudioMixer"

    def __init__(self, mute_change_callback, volume_change_callback, properties):
        AudioMixer.__init__(self, mute_change_callback, volume_change_callback, properties)
        self._observer = None
        self.control = self.properties.get("control", "Master")
        self.audio_step_size = int(self.properties.get("stepSize", 3))

        self.max_volume = 100
        self.min_volume = 0

        self.current_mute = None
        self.current_volume = None

    def start(self):
        self._observer = AlsaMixerObserver(
            control=self.control,
            callback=self.handle_audio_changes,
        )
        self._observer.start()

    def stop(self):
        self._observer.stop()

    @property
    def _mixer(self):
        return alsaaudio.Mixer(control=self.control)

    def lower_volume(self):
        self.change_volume(-self.audio_step_size)

    def raise_volume(self):
        self.change_volume(self.audio_step_size)

    def toggle_mute(self):
        self.set_mute(not self.current_mute)

    def get_volume(self):
        channels = self._mixer.getvolume()
        if not channels:
            return None
        else:
            # the channels might not have the same volume, use the smallest to avoid accidental loud noise
            return min(channels)

    def change_volume(self, difference):
        if self.current_volume is None:
            return

        new_volume = max(self.min_volume, min(self.current_volume + difference, self.max_volume))
        if new_volume != self.current_volume:
            self._mixer.setvolume(new_volume)

    def get_mute(self):
        """
        This method is taken from mopidy-alsamixer
        (https://github.com/mopidy/mopidy-alsamixer/blob/master/mopidy_alsamixer/mixer.py),
        on 5th September 2020. A few adjustments were added.

        The code of this class is licensed under Apache-2.0 License
        """
        try:
            channels_muted = self._mixer.getmute()
        except alsaaudio.ALSAAudioError as exc:
            logger.debug("Getting mute state failed: {}".format(exc))
            return None
        if all(channels_muted):
            return True
        elif not any(channels_muted):
            return False
        else:
            # Not all channels have the same mute state
            return None

    def set_mute(self, mute):
        """
        This method is taken from mopidy-alsamixer
        (https://github.com/mopidy/mopidy-alsamixer/blob/master/mopidy_alsamixer/mixer.py),
        on 5th September 2020. A few adjustments were added.

        The code of this class is licensed under Apache-2.0 License
        """
        try:
            self._mixer.setmute(int(mute))
            return True
        except alsaaudio.ALSAAudioError as exc:
            logger.debug("Setting mute state failed: {}".format(exc))
            return False

    def update_mute(self, new_mute):
        if self.current_mute == new_mute:
            return

        self.current_mute = new_mute
        self.mute_change_callback(self.current_mute)

    def update_volume(self, new_volume):
        if self.current_volume == new_volume:
            return

        self.current_volume = new_volume
        self.volume_change_callback(self.current_volume)

    def handle_audio_changes(self):
        self.update_mute(self.get_mute())
        self.update_volume(self.get_volume())


class AlsaMixerObserver(threading.Thread):
    """
    Deamon-thread based observer class for the ALSA audio status.

    This class is taken from mopidy-alsamixer
    (https://github.com/mopidy/mopidy-alsamixer/blob/master/mopidy_alsamixer/mixer.py),
    on 5th September 2020. A few adjustments were added.

    The code of this class is licensed under Apache-2.0 License
    """
    daemon = True
    name = "AlsaMixerObserver"

    def __init__(self, control, callback=None):
        threading.Thread.__init__(self)
        self.running = True

        # Keep the mixer instance alive for the descriptors to work
        self.mixer = alsaaudio.Mixer(control=control)
        descriptors = self.mixer.polldescriptors()
        assert len(descriptors) == 1
        self.fd = descriptors[0][0]
        self.event_mask = descriptors[0][1]

        self.callback = callback

    def stop(self):
        self.running = False

    def run(self):
        poller = select.epoll()
        poller.register(self.fd, self.event_mask | select.EPOLLET)
        while self.running:
            try:
                events = poller.poll(timeout=1)
                if events and self.callback is not None:
                    self.callback()
                    # we need to tell that we handled the events, so we get new ones
                    self.mixer.handleevents()
            except OSError as exc:
                # poller.poll() will raise an IOError because of the
                # interrupted system call when suspending the machine.
                logger.debug("Ignored IO error: {}".format(exc))
        poller.unregister(self.fd)
