#!/usr/bin/env python3
"""Composes and synthesizes EverFest's original holiday music.

    python3 tools/gen_music.py [track ...]

Every melody, chord progression and arrangement here was written for EverFest (no existing tunes are quoted), and
every sound is synthesized from sine partials, band-limited noise and a small reverb - no samples or recordings.
Requires numpy and ffmpeg with libvorbis. Output: assets/holidayseasons/sounds/music/<track>.ogg
"""
import os
import random
import subprocess
import sys
import tempfile
import wave
import zlib

import numpy as np

RATE = 44100
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "holidayseasons", "sounds",
                   "music")

NOTE = {"C": 0, "C#": 1, "Db": 1, "D": 2, "D#": 3, "Eb": 3, "E": 4, "F": 5, "F#": 6, "Gb": 6, "G": 7, "G#": 8, "Ab": 8,
        "A": 9, "A#": 10, "Bb": 10, "B": 11}


def midi(name):
    """'C#5' -> MIDI number."""
    pitch, octave = name[:-1], int(name[-1])
    return 12 * (octave + 1) + NOTE[pitch]


def freq(m):
    return 440.0 * 2 ** ((m - 69) / 12)


def tt(seconds):
    return np.arange(int(seconds * RATE)) / RATE


# ----------------------------------------------------------------------------------------------- instruments

def env_adsr(n, a, d, s, r):
    """Attack/decay/sustain/release envelope over n samples (a, d, r in seconds, s level)."""
    e = np.full(n, s, dtype=float)
    na, nd, nr = int(a * RATE), int(d * RATE), int(r * RATE)
    na = min(na, n)
    e[:na] = np.linspace(0, 1, na, endpoint=False) if na else e[:na]
    nd = min(nd, max(0, n - na))
    if nd:
        e[na:na + nd] = np.linspace(1, s, nd, endpoint=False)
    if nr and n > nr:
        e[n - nr:] *= np.linspace(1, 0, nr)
    return e


def additive(f, seconds, partials, vibrato=0.0, vib_rate=5.0):
    x = tt(seconds)
    phase = 2 * np.pi * f * x
    if vibrato:
        phase += vibrato * np.sin(2 * np.pi * vib_rate * x) * (1 - np.exp(-x * 3))
    out = np.zeros_like(x)
    for ratio, amp, decay in partials:
        out += amp * np.sin(phase * ratio) * np.exp(-x * decay)
    return out


def music_box(f, seconds):
    s = max(seconds, 1.6)
    x = additive(f, s, [(1, 1.0, 2.2), (2, 0.35, 4.0), (3, 0.12, 7.0), (4.2, 0.08, 9.0)])
    return x * env_adsr(len(x), 0.002, 0.05, 0.9, 0.05)


def celesta(f, seconds):
    s = max(seconds, 1.4)
    x = additive(f, s, [(1, 1.0, 2.8), (2, 0.5, 3.5), (3, 0.18, 6.0), (4, 0.1, 9.0)])
    return x * env_adsr(len(x), 0.002, 0.08, 0.85, 0.08)


def bell_tone(f, seconds):
    s = max(seconds, 2.5)
    x = additive(f, s, [(1, 1.0, 1.2), (2.0, 0.45, 1.6), (2.76, 0.35, 2.0), (5.4, 0.18, 3.0), (8.93, 0.08, 4.0)])
    return x * env_adsr(len(x), 0.002, 0.1, 0.9, 0.2)


def pluck(f, seconds):
    s = max(seconds, 0.8)
    x = additive(f, s, [(1, 1.0, 4.0), (2, 0.5, 6.0), (3, 0.3, 9.0), (4, 0.15, 12.0), (5, 0.08, 16.0)])
    return x * env_adsr(len(x), 0.003, 0.02, 1.0, 0.05)


def flute(f, seconds):
    x = additive(f, seconds, [(1, 1.0, 0.0), (2, 0.25, 0.0), (3, 0.08, 0.0)], vibrato=0.6, vib_rate=5.2)
    breath = band_noise(len(x), f * 0.8, f * 3.0, 77) * 0.06
    return (x + breath) * env_adsr(len(x), 0.06, 0.1, 0.85, 0.12)


def fiddle(f, seconds):
    parts = [(k, 1.0 / k ** 1.3, 0.0) for k in range(1, 9)]
    x = additive(f, seconds, parts, vibrato=0.9, vib_rate=5.8)
    return x * env_adsr(len(x), 0.03, 0.08, 0.8, 0.06) * 0.6


def brass(f, seconds):
    parts = [(k, 1.0 / k ** 1.1, 0.0) for k in range(1, 8)]
    x = additive(f, seconds, parts, vibrato=0.3, vib_rate=4.5)
    return x * env_adsr(len(x), 0.04, 0.12, 0.75, 0.1) * 0.5


def pad(f, seconds):
    x = tt(seconds)
    out = np.zeros_like(x)
    for detune in (-0.12, 0.0, 0.11):
        g = f * 2 ** (detune / 12)
        for k, amp in ((1, 1.0), (2, 0.35), (3, 0.12)):
            out += amp * np.sin(2 * np.pi * g * k * x + detune * 7)
    return out / 3 * env_adsr(len(x), min(0.8, seconds / 3), 0.3, 0.8, min(1.0, seconds / 3))


def band_noise(n, lo, hi, seed):
    rng = np.random.default_rng(seed)
    spec = np.fft.rfft(rng.normal(0, 1, n))
    fr = np.fft.rfftfreq(n, 1 / RATE)
    spec[(fr < lo) | (fr > hi)] = 0
    out = np.fft.irfft(spec, n)
    m = np.max(np.abs(out))
    return out / m if m else out


_noise_cache = {}


def _noise(kind, n, lo, hi):
    key = (kind, n)
    if key not in _noise_cache:
        _noise_cache[key] = band_noise(n, lo, hi, zlib.crc32(kind.encode()) % 1000)
    return _noise_cache[key]


def sleigh_bells(seconds=0.35):
    n = int(seconds * RATE)
    x = _noise("sleigh", n, 5000, 12000) * np.exp(-tt(seconds) * 14)
    jingle = sum(np.sin(2 * np.pi * f0 * tt(seconds)) for f0 in (6200, 7400, 8900)) * np.exp(-tt(seconds) * 20) * 0.15
    return (x + jingle) * 0.35


def shaker(seconds=0.12):
    n = int(seconds * RATE)
    return _noise("shaker", n, 4000, 10000) * np.exp(-tt(seconds) * 40) * 0.25


def kick(seconds=0.35):
    x = tt(seconds)
    f = 50 + 70 * np.exp(-x * 30)
    return np.sin(2 * np.pi * np.cumsum(f) / RATE) * np.exp(-x * 9)


def snare(seconds=0.2):
    n = int(seconds * RATE)
    return (_noise("snare", n, 1500, 7000) * 0.7 + np.sin(2 * np.pi * 190 * tt(seconds)) * 0.3) * np.exp(-tt(seconds) * 18) * 0.6


def frame_drum(seconds=0.4):
    x = tt(seconds)
    return np.sin(2 * np.pi * (90 + 30 * np.exp(-x * 20)) * x) * np.exp(-x * 7) * 0.8


INSTRUMENTS = {"music_box": music_box, "celesta": celesta, "bell": bell_tone, "pluck": pluck, "flute": flute,
               "fiddle": fiddle, "brass": brass, "pad": pad}


# ----------------------------------------------------------------------------------------------- arrangement

class Track:
    def __init__(self, bpm, beats):
        self.spb = 60.0 / bpm
        self.buf = np.zeros(int((beats * self.spb + 4) * RATE))

    def add(self, clip, beat, gain, pan=0.0):
        i = int(beat * self.spb * RATE)
        end = min(len(self.buf), i + len(clip))
        if end > i:
            self.buf[i:end] += clip[:end - i] * gain

    def note(self, instrument, name_or_midi, beat, beats, gain):
        m = midi(name_or_midi) if isinstance(name_or_midi, str) else name_or_midi
        self.add(INSTRUMENTS[instrument](freq(m), beats * self.spb), beat, gain)

    def line(self, instrument, notes, start, gain):
        """notes: list of (name, beats) - 'R' rests."""
        b = start
        for name, beats in notes:
            if name != "R":
                self.note(instrument, name, b, beats, gain)
            b += beats
        return b

    def hit(self, clip, beat, gain):
        self.add(clip, beat, gain)


def chord_notes(root, quality, octave):
    base = midi(root + str(octave))
    intervals = {"maj": (0, 4, 7), "min": (0, 3, 7), "dim": (0, 3, 6), "sus": (0, 5, 7)}[quality]
    return [base + i for i in intervals]


def parse_chord(c):
    if c.endswith("m"):
        return c[:-1], "min"
    return c, "maj"


def comb_block(x, d, g):
    """y[n] = x[n] + g*y[n-d], vectorized one delay-length block at a time."""
    y = np.copy(x)
    for s in range(d, len(y), d):
        e = min(len(y), s + d)
        y[s:e] += g * y[s - d:e - d]
    return y


def allpass_block(x, d, g):
    """y[n] = -g*x[n] + x[n-d] + g*y[n-d]."""
    y = -g * x
    y[d:] += x[:-d]
    for s in range(d, len(y), d):
        e = min(len(y), s + d)
        y[s:e] += g * y[s - d:e - d]
    return y


def reverb(x, wet=0.25, size=1.0):
    combs = [int(RATE * t0 * size) for t0 in (0.0297, 0.0371, 0.0411, 0.0437)]
    acc = sum(comb_block(x, d, 0.78) for d in combs) / len(combs)
    acc = allpass_block(acc, int(RATE * 0.005), 0.7)
    acc = allpass_block(acc, int(RATE * 0.0017), 0.7)
    return (1 - wet) * x + wet * acc


def finish(track, wet=0.25):
    x = reverb(track.buf, wet)
    nz = np.nonzero(np.abs(x) > 1e-4)[0]
    x = x[: (nz[-1] + RATE // 2) if len(nz) else len(x)]
    fade = int(RATE * 1.5)
    x[-fade:] *= np.linspace(1, 0, fade)
    x[: int(RATE * 0.02)] *= np.linspace(0, 1, int(RATE * 0.02))
    # Level every track to the same loudness (RMS), never above a 0.8 peak.
    peak = np.max(np.abs(x))
    rms = np.sqrt(np.mean(x ** 2))
    if peak == 0:
        return x
    return x * min(0.8 / peak, 0.12 / rms)


# ----------------------------------------------------------------------------------------------- compositions

def melody_from_chords(progression, beats_per_bar, rng, octave, chord_octave_bias=0, rhythm=(1,)):
    """An original line that leans on chord tones on strong beats and steps between them."""
    out = []
    prev = None
    for chord in progression:
        root, q = parse_chord(chord)
        tones = chord_notes(root, q, octave)
        tones += [t0 + 12 for t0 in tones]
        b = 0.0
        while b < beats_per_bar - 1e-6:
            dur = rng.choice(rhythm)
            dur = min(dur, beats_per_bar - b)
            if prev is None or b == 0:
                m = rng.choice(tones[:4])
            else:
                step = rng.choice([-2, -1, 1, 2, 0, 3, -3])
                cand = prev + step
                m = min(tones, key=lambda t0: abs(t0 - cand)) if b % 1 == 0 else cand
            m = max(midi("C4") + chord_octave_bias, min(midi("A6"), m))
            out.append((m, dur))
            prev = m
            b += dur
    return out


def play_line(track, instrument, line, start, gain):
    b = start
    for m, d in line:
        track.note(instrument, m, b, d, gain)
        b += d
    return b


def lantern_hollow():
    """Halloween: a lonely music-box waltz in D minor over a drone, with distant bells."""
    bpm, bar = 96, 3
    prog_a = ["Dm", "Bb", "Gm", "A", "Dm", "F", "Gm", "Dm"]
    prog_b = ["Bb", "C", "Dm", "A", "Gm", "Bb", "A", "A"]
    form = ["intro"] * 4 + prog_a + prog_a + prog_b + prog_a + ["outro"] * 4
    tr = Track(bpm, len(form) * bar)
    motif = [("A4", 1), ("D5", 1), ("F5", 1), ("E5", 2), ("D5", 1), ("Bb4", 1), ("D5", 1), ("G5", 1), ("F5", 1), ("E5", 1), ("C#5", 1),
             ("D5", 1), ("F5", 1), ("A5", 1), ("C6", 2), ("A5", 1), ("G5", 1), ("Bb5", 1), ("E5", 1), ("D5", 3)]
    answer = [("F5", 1), ("E5", 1), ("D5", 1), ("C5", 2), ("E5", 1), ("F5", 1), ("G5", 1), ("A5", 1), ("E5", 3),
              ("D5", 1), ("C5", 1), ("Bb4", 1), ("D5", 2), ("F5", 1), ("E5", 1), ("C#5", 1), ("E5", 1), ("A4", 3)]
    beat = 0
    for i, c in enumerate(form):
        b = i * bar
        if c in ("intro", "outro"):
            tr.note("pad", "D3", b, bar, 0.22)
            tr.note("pad", "A3", b, bar, 0.12)
            continue
        root, q = parse_chord(c)
        notes = chord_notes(root, q, 3)
        tr.note("pad", notes[0] - 12, b, bar, 0.2)
        tr.note("pad", notes[1], b, bar, 0.08)
        tr.note("pluck", notes[0] - 12, b, 1, 0.35)
        tr.note("pluck", notes[1], b + 1, 1, 0.18)
        tr.note("pluck", notes[2], b + 2, 1, 0.18)
        if i % 8 == 4:
            tr.note("bell", "D4", b, 3, 0.25)
    a1 = 4 * bar
    tr.line("music_box", motif, a1, 0.5)
    a2 = a1 + 8 * bar
    tr.line("music_box", [(n[:-1] + str(int(n[-1]) - 1) if n != "R" else n, d) for n, d in motif], a2, 0.45)
    tr.line("celesta", motif, a2 + 0.0, 0.15)
    b1 = a2 + 8 * bar
    tr.line("music_box", answer, b1, 0.5)
    a3 = b1 + 8 * bar
    tr.line("music_box", motif, a3, 0.5)
    tr.note("bell", "A4", a3 + 8 * bar, 6, 0.3)
    return finish(tr, 0.32)


def hearthlight():
    """Christmas: a warm celesta tune in G major with sleigh bells and a soft pad."""
    bpm, bar = 104, 4
    prog_a = ["G", "Em", "C", "D", "G", "Bm", "C", "G"]
    prog_b = ["C", "G", "Am", "D", "Em", "C", "Am", "D"]
    form = prog_a + prog_a + prog_b + prog_a
    tr = Track(bpm, len(form) * bar + 4)
    motif = [("D5", 1), ("B4", 1), ("G4", 1), ("B4", 1), ("E5", 2), ("D5", 1), ("B4", 1), ("C5", 1), ("E5", 1), ("G5", 1), ("E5", 1),
             ("F#5", 3), ("A4", 1), ("B4", 1), ("D5", 1), ("G5", 1), ("F#5", 1), ("E5", 1), ("D5", 1), ("B4", 1), ("F#4", 1),
             ("A4", 1), ("C5", 1), ("E5", 1), ("D5", 1), ("G5", 4)]
    bridge = [("E5", 2), ("G5", 1), ("E5", 1), ("D5", 3), ("B4", 1), ("C5", 1), ("E5", 1), ("A5", 1), ("G5", 1), ("F#5", 4),
              ("G5", 1), ("F#5", 1), ("E5", 1), ("B4", 1), ("C5", 2), ("E5", 2), ("A4", 1), ("C5", 1), ("E5", 1), ("G5", 1),
              ("F#5", 2), ("D5", 2)]
    for i, c in enumerate(form):
        b = i * bar
        root, q = parse_chord(c)
        notes = chord_notes(root, q, 3)
        tr.note("pad", notes[0], b, bar, 0.12)
        tr.note("pad", notes[1], b, bar, 0.08)
        tr.note("pad", notes[2], b, bar, 0.08)
        tr.note("pluck", notes[0] - 12, b, 1, 0.4)
        tr.note("pluck", notes[2] - 12, b + 2, 1, 0.3)
        for k in range(bar):
            tr.hit(sleigh_bells(), b + k, 0.2 if k % 2 == 0 else 0.12)
            tr.hit(shaker(), b + k + 0.5, 0.25)
    tr.line("celesta", motif, 0, 0.45)
    tr.line("celesta", motif, 8 * bar, 0.45)
    tr.line("flute", [(n[:-1] + str(int(n[-1]) - 1), d) for n, d in motif], 8 * bar, 0.12)
    tr.line("celesta", bridge, 16 * bar, 0.45)
    tr.line("celesta", motif, 24 * bar, 0.45)
    tr.note("bell", "G5", 32 * bar, 4, 0.35)
    tr.note("bell", "D5", 32 * bar, 4, 0.25)
    return finish(tr, 0.28)


def meadow_morning():
    """Spring (Easter, Valentine's): a light flute melody in F major over rippling plucked arpeggios."""
    bpm, bar = 92, 4
    prog = ["F", "C", "Dm", "Bb", "F", "Am", "Bb", "C"]
    form = prog * 3
    tr = Track(bpm, len(form) * bar + 4)
    rng = random.Random(3105)
    for i, c in enumerate(form):
        b = i * bar
        root, q = parse_chord(c)
        notes = chord_notes(root, q, 4)
        arp = [notes[0], notes[1], notes[2], notes[1] + 12, notes[2], notes[1], notes[0] + 12, notes[2]]
        for k, m in enumerate(arp):
            tr.note("pluck", m - 12, b + k * 0.5, 0.5, 0.18)
        tr.note("pad", notes[0] - 12, b, bar, 0.1)
    theme = [("C5", 1), ("F5", 1), ("A5", 1), ("G5", 1), ("F5", 2), ("C5", 2), ("D5", 1), ("F5", 1), ("E5", 1), ("D5", 1),
             ("C5", 4), ("A4", 1), ("C5", 1), ("F5", 1), ("A5", 1), ("G5", 1.5), ("F5", 0.5), ("E5", 1), ("C5", 1),
             ("D5", 1), ("F5", 1), ("Bb5", 1), ("A5", 1), ("G5", 4)]
    tr.line("flute", theme, 0, 0.4)
    play_line(tr, "flute", melody_from_chords(prog, bar, rng, 5, rhythm=(1, 1, 0.5, 2)), 8 * bar, 0.35)
    tr.line("flute", theme, 16 * bar, 0.4)
    tr.note("celesta", "F5", 24 * bar, 4, 0.3)
    return finish(tr, 0.3)


def harvest_table():
    """Thanksgiving: a lively fiddle tune in A mixolydian over a frame drum and plucked bass."""
    bpm, bar = 132, 3
    prog = ["A", "G", "A", "E", "A", "D", "G", "A"]
    form = prog * 5
    tr = Track(bpm, len(form) * bar + 4)
    rng = random.Random(1123)
    for i, c in enumerate(form):
        b = i * bar
        root, q = parse_chord(c)
        notes = chord_notes(root, q, 3)
        tr.note("pluck", notes[0] - 12, b, 1, 0.45)
        tr.note("pluck", notes[2] - 12, b + 1, 1, 0.3)
        tr.note("pluck", notes[1], b + 2, 1, 0.22)
        tr.hit(frame_drum(), b, 0.5)
        tr.hit(frame_drum(0.2), b + 1.5, 0.25)
        tr.hit(shaker(), b + 1, 0.2)
        tr.hit(shaker(), b + 2, 0.2)
    lines = [melody_from_chords(prog, bar, rng, 4, rhythm=(0.5, 0.5, 0.5, 1)) for _ in range(2)]
    starts = [0, 8, 16, 24, 32]
    pick = [0, 0, 1, 1, 0]
    for s, p in zip(starts, pick):
        play_line(tr, "fiddle", lines[p], s * bar, 0.45)
    tr.note("fiddle", "A5", 40 * bar, 3, 0.4)
    return finish(tr, 0.2)


def midnight_fireworks():
    """New Year's and Independence Day: a bright brass fanfare-march in C major with sparkling bells."""
    bpm, bar = 120, 4
    verse = ["C", "Am", "F", "G"] * 4
    chorus = ["F", "G", "Em", "Am", "F", "G", "C", "C"]
    form = verse + chorus + ["C", "Am", "F", "G", "C", "C"]
    tr = Track(bpm, len(form) * bar + 4)
    rng = random.Random(1231)
    for i, c in enumerate(form):
        b = i * bar
        root, q = parse_chord(c)
        notes = chord_notes(root, q, 3)
        tr.note("pad", notes[1], b, bar, 0.08)
        tr.note("pluck", notes[0] - 12, b, 1, 0.4)
        tr.note("pluck", notes[0] - 12, b + 2, 1, 0.35)
        tr.hit(kick(), b, 0.6)
        tr.hit(kick(), b + 2, 0.5)
        tr.hit(snare(), b + 1, 0.4)
        tr.hit(snare(), b + 3, 0.4)
        for k in range(8):
            tr.hit(shaker(0.06), b + k * 0.5, 0.15)
        if i >= 16:
            for k, m in enumerate([notes[0] + 24, notes[1] + 24, notes[2] + 24, notes[1] + 24]):
                tr.note("celesta", m, b + k, 1, 0.12)
    fanfare = [("G4", 0.5), ("C5", 0.5), ("E5", 1), ("G5", 2), ("E5", 1), ("C5", 1), ("A4", 2), ("C5", 1), ("F5", 1), ("A5", 2),
               ("G5", 1), ("F5", 1), ("E5", 1), ("D5", 1), ("G5", 2)]
    b = tr.line("brass", fanfare, 0, 0.4)
    play_line(tr, "brass", melody_from_chords(["C", "Am", "F", "G"] * 3, bar, rng, 4, rhythm=(1, 1, 0.5, 2)), 4 * bar, 0.38)
    chorus_line = [("A5", 2), ("G5", 1), ("F5", 1), ("G5", 2), ("D5", 2), ("E5", 1), ("G5", 1), ("B5", 1), ("G5", 1), ("A5", 4),
                   ("F5", 1), ("A5", 1), ("C6", 2), ("B5", 1), ("G5", 1), ("D5", 2), ("C5", 1), ("E5", 1), ("G5", 2), ("C6", 4)]
    tr.line("brass", chorus_line, 16 * bar, 0.42)
    tr.line("brass", fanfare, 24 * bar, 0.4)
    tr.note("bell", "C6", 30 * bar, 4, 0.3)
    return finish(tr, 0.22)


TRACKS = {
    "lantern_hollow": lantern_hollow,
    "hearthlight": hearthlight,
    "meadow_morning": meadow_morning,
    "harvest_table": harvest_table,
    "midnight_fireworks": midnight_fireworks,
}


def write_ogg(name, data):
    os.makedirs(OUT, exist_ok=True)
    pcm16 = (np.clip(data, -1, 1) * 32767).astype(np.int16)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        wav_path = tmp.name
    with wave.open(wav_path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(pcm16.tobytes())
    out = os.path.join(OUT, name + ".ogg")
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav_path, "-c:a", "libvorbis", "-q:a", "3", out], check=True)
    os.unlink(wav_path)
    print("wrote", os.path.relpath(out), f"{len(data) / RATE:.1f}s", f"{os.path.getsize(out) // 1024} KiB")


def main():
    only = set(sys.argv[1:])
    for name, fn in TRACKS.items():
        if only and name not in only:
            continue
        write_ogg(name, fn())


if __name__ == "__main__":
    main()
