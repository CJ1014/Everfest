#!/usr/bin/env python3
"""Synthesizes EverFest's original sound effects (no samples, no recordings) and encodes them as Ogg Vorbis.

    python3 tools/gen_sounds.py

Every sound is built from sine partials, envelopes and filtered noise, so all audio is original.
Requires numpy and ffmpeg with libvorbis.
"""
import os
import subprocess
import sys
import tempfile
import wave

import numpy as np

RATE = 44100
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "holidayseasons", "sounds")
rng = np.random.default_rng(20261031)

SOUNDS = {}


def sound(name):
    def deco(fn):
        SOUNDS[name] = fn
        return fn
    return deco


def t(seconds):
    return np.arange(int(seconds * RATE)) / RATE


def env_exp(n_or_t, decay):
    tt = n_or_t if isinstance(n_or_t, np.ndarray) else t(n_or_t)
    return np.exp(-tt * decay)


def attack(x, seconds=0.005):
    n = min(len(x), int(seconds * RATE))
    x[:n] *= np.linspace(0, 1, n)
    return x


def lowpass(x, cutoff):
    """One-pole low-pass."""
    a = np.exp(-2 * np.pi * cutoff / RATE)
    y = np.empty_like(x)
    acc = 0.0
    for i, v in enumerate(x):
        acc = (1 - a) * v + a * acc
        y[i] = acc
    return y


def highpass(x, cutoff):
    return x - lowpass(x, cutoff)


def bandpass(x, lo, hi):
    return lowpass(highpass(x, lo), hi)


def reverb(x, wet=0.3, delays=(0.029, 0.037, 0.041, 0.053), feedback=0.6):
    out = np.copy(x)
    pad = int(RATE * 1.2)
    y = np.concatenate([x, np.zeros(pad)])
    acc = np.zeros_like(y)
    for d in delays:
        n = int(d * RATE)
        buf = np.copy(y)
        for i in range(n, len(buf)):
            buf[i] += feedback * buf[i - n]
        acc += buf
    acc /= len(delays)
    out = np.concatenate([out, np.zeros(pad)])
    return (1 - wet) * out + wet * acc


def bell(freq, seconds, decay=2.2, partials=((1, 1.0), (2.0, 0.45), (2.76, 0.35), (5.4, 0.2), (8.93, 0.1))):
    tt = t(seconds)
    x = np.zeros_like(tt)
    for ratio, amp in partials:
        x += amp * np.sin(2 * np.pi * freq * ratio * tt) * np.exp(-tt * decay * (0.6 + ratio * 0.25))
    return attack(x, 0.003)


def normalize(x, peak=0.85):
    m = np.max(np.abs(x))
    return x if m == 0 else x / m * peak


def mix_at(base, clip, start):
    i = int(start * RATE)
    end = i + len(clip)
    if end > len(base):
        base = np.concatenate([base, np.zeros(end - len(base))])
    base[i:end] += clip
    return base


# ----------------------------------------------------------------------------------------------- effects

@sound("doorbell")
def doorbell():
    x = np.zeros(int(RATE * 2.2))
    x = mix_at(x, bell(659.25, 1.2, decay=2.6), 0.0)    # E5
    x = mix_at(x, bell(523.25, 1.6, decay=2.0), 0.42)   # C5
    return normalize(reverb(x, 0.25))


@sound("knock")
def knock():
    x = np.zeros(int(RATE * 0.9))
    for start in (0.0, 0.21, 0.40):
        n = int(RATE * 0.09)
        tt = np.arange(n) / RATE
        thump = np.sin(2 * np.pi * (140 - 60 * tt / 0.09) * tt) * np.exp(-tt * 55)
        click = lowpass(rng.normal(0, 1, n), 1800) * np.exp(-tt * 120) * 0.8
        x = mix_at(x, (thump + click) * (0.9 + rng.uniform(-0.1, 0.1)), start)
    return normalize(reverb(x, 0.15, feedback=0.4))


@sound("candy")
def candy():
    n = int(RATE * 0.55)
    x = np.zeros(n)
    for _ in range(38):
        pos = rng.integers(0, n - 800)
        length = rng.integers(80, 600)
        burst = rng.normal(0, 1, length) * np.exp(-np.arange(length) / (length / 4))
        x[pos:pos + length] += burst * rng.uniform(0.2, 1.0)
    x = highpass(x, 2500)
    return normalize(x * np.linspace(1, 0.6, n))


@sound("candy_bowl_empty")
def candy_bowl_empty():
    tt = t(0.35)
    x = np.sin(2 * np.pi * 330 * tt) * np.exp(-tt * 18) + 0.4 * np.sin(2 * np.pi * 495 * tt) * np.exp(-tt * 25)
    return normalize(attack(x))


@sound("ghost_moan")
def ghost_moan():
    tt = t(2.6)
    f = 190 - 40 * tt / 2.6 + 6 * np.sin(2 * np.pi * 4.5 * tt)
    phase = 2 * np.pi * np.cumsum(f) / RATE
    tone = np.sin(phase) + 0.35 * np.sin(2 * phase) + 0.15 * np.sin(3 * phase)
    breath = bandpass(rng.normal(0, 1, len(tt)), 400, 1500) * 0.35
    amp = np.sin(np.pi * tt / 2.6) ** 1.5
    return normalize(reverb((tone * 0.6 + breath) * amp, 0.45, feedback=0.7))


@sound("ghost_hurt")
def ghost_hurt():
    tt = t(0.6)
    f = 520 * np.exp(-tt * 2.5) + 120
    phase = 2 * np.pi * np.cumsum(f) / RATE
    x = (np.sin(phase) * 0.6 + bandpass(rng.normal(0, 1, len(tt)), 600, 3000) * 0.4) * np.exp(-tt * 5)
    return normalize(reverb(attack(x), 0.3))


@sound("ghost_death")
def ghost_death():
    tt = t(2.0)
    x = np.zeros_like(tt)
    for f in (440, 554.37, 659.25, 880):
        x += np.sin(2 * np.pi * f * (1 + 0.3 * tt) * tt) * np.exp(-tt * 1.6)
    x += bandpass(rng.normal(0, 1, len(tt)), 2000, 6000) * 0.2 * np.exp(-tt * 3)
    return normalize(reverb(attack(x, 0.05), 0.5, feedback=0.75))


@sound("distant_bell")
def distant_bell():
    x = bell(196.0, 4.5, decay=0.9, partials=((0.5, 0.6), (1, 1.0), (1.19, 0.5), (1.5, 0.4), (2.0, 0.5), (2.5, 0.2), (3.0, 0.15)))
    x = lowpass(x, 1400)
    return normalize(reverb(x, 0.55, delays=(0.07, 0.11, 0.13, 0.17), feedback=0.7), 0.7)


@sound("eerie_wind")
def eerie_wind():
    tt = t(7.0)
    noise = rng.normal(0, 1, len(tt))
    a = bandpass(noise, 250, 900) * (0.5 + 0.5 * np.sin(2 * np.pi * 0.21 * tt))
    b = bandpass(noise[::-1].copy(), 900, 2200) * (0.5 + 0.5 * np.sin(2 * np.pi * 0.13 * tt + 1.0)) * 0.5
    whistle = np.sin(2 * np.pi * (700 + 120 * np.sin(2 * np.pi * 0.17 * tt)) * tt) * 0.06 * (0.5 + 0.5 * np.sin(2 * np.pi * 0.09 * tt))
    x = (a + b + whistle) * np.minimum(1, np.minimum(tt / 1.5, (7.0 - tt) / 1.5))
    return normalize(x, 0.6)


@sound("whispers")
def whispers():
    tt = t(3.0)
    x = np.zeros_like(tt)
    for _ in range(9):
        start = rng.uniform(0, 2.4)
        dur = rng.uniform(0.15, 0.45)
        n = int(dur * RATE)
        seg = bandpass(rng.normal(0, 1, n), rng.uniform(1500, 2500), rng.uniform(4000, 7000))
        seg *= np.sin(np.pi * np.arange(n) / n) ** 2
        x = mix_at(x, seg * rng.uniform(0.3, 1.0), start)
    return normalize(reverb(x[:len(tt)], 0.5), 0.5)


@sound("gourdling_hop")
def gourdling_hop():
    tt = t(0.25)
    thump = np.sin(2 * np.pi * (110 + 80 * np.exp(-tt * 30)) * tt) * np.exp(-tt * 22)
    squish = bandpass(rng.normal(0, 1, len(tt)), 300, 1200) * np.exp(-tt * 30) * 0.4
    return normalize(attack(thump + squish))


@sound("gourdling_hurt")
def gourdling_hurt():
    tt = t(0.3)
    f = 900 + 400 * np.sin(2 * np.pi * 12 * tt)
    x = np.sin(2 * np.pi * np.cumsum(f) / RATE) * np.exp(-tt * 9)
    return normalize(attack(x))


@sound("gourdling_death")
def gourdling_death():
    tt = t(0.5)
    pop = bandpass(rng.normal(0, 1, len(tt)), 200, 2500) * np.exp(-tt * 25)
    seeds = np.zeros_like(tt)
    for _ in range(14):
        p = rng.integers(int(0.05 * RATE), len(tt) - 400)
        seeds[p:p + 300] += np.sin(2 * np.pi * rng.uniform(1500, 3000) * np.arange(300) / RATE) * np.exp(-np.arange(300) / 60)
    return normalize(pop + seeds * 0.4)


@sound("scarecrow_rustle")
def scarecrow_rustle():
    n = int(RATE * 0.8)
    x = np.zeros(n)
    for _ in range(60):
        p = rng.integers(0, n - 1500)
        length = rng.integers(300, 1500)
        x[p:p + length] += rng.normal(0, 1, length) * np.hanning(length) * rng.uniform(0.2, 1)
    x = bandpass(x, 1200, 5000)
    return normalize(x * np.sin(np.pi * np.arange(n) / n))


@sound("scarecrow_creak")
def scarecrow_creak():
    tt = t(0.9)
    f = 80 + 30 * np.sin(2 * np.pi * 1.3 * tt)
    pulses = np.sin(2 * np.pi * np.cumsum(f) / RATE)
    grain = (np.sin(2 * np.pi * 35 * tt) > 0.6).astype(float)
    x = bandpass(pulses * grain + rng.normal(0, 0.2, len(tt)) * grain, 200, 1800)
    return normalize(x * np.sin(np.pi * tt / 0.9))


@sound("trick_poof")
def trick_poof():
    tt = t(0.7)
    x = bandpass(rng.normal(0, 1, len(tt)), 300, 4000) * np.exp(-tt * 6)
    sparkle = np.zeros_like(tt)
    for k in range(6):
        s = int((0.05 + k * 0.08) * RATE)
        n = int(0.12 * RATE)
        sparkle[s:s + n] += np.sin(2 * np.pi * (1200 + 300 * k) * np.arange(n) / RATE) * np.exp(-np.arange(n) / (0.03 * RATE))
    return normalize(x * 0.7 + sparkle * 0.5)



@sound("turkey_gobble")
def turkey_gobble():
    """A tom's gobble: a quick run of throaty, pitch-bent warble syllables."""
    out = np.zeros(int(RATE * 0.95))
    start = 0.0
    for i, dur in enumerate((0.07, 0.055, 0.05, 0.05, 0.05, 0.05, 0.06, 0.08)):
        tt = t(dur)
        f0 = 720 - 35 * i + 160 * np.sin(np.pi * tt / dur)
        phase = 2 * np.pi * np.cumsum(f0) / RATE
        voice = sum(np.sin(k * phase) / k for k in range(1, 9))
        syl = bandpass(voice, 350, 3200) * np.sin(np.pi * tt / dur) ** 0.6
        out = mix_at(out, syl * (1.0 - 0.05 * i), start)
        start += dur * 0.92
    return normalize(reverb(out, 0.15))


def write_ogg(name, data):
    os.makedirs(OUT, exist_ok=True)
    pcm = np.clip(data, -1, 1)
    pcm16 = (pcm * 32767).astype(np.int16)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        wav_path = tmp.name
    with wave.open(wav_path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(pcm16.tobytes())
    out = os.path.join(OUT, name + ".ogg")
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav_path, "-c:a", "libvorbis", "-q:a", "4", out], check=True)
    os.unlink(wav_path)
    print("wrote", os.path.relpath(out), f"{len(data) / RATE:.2f}s")


def main():
    only = set(sys.argv[1:])
    for name, fn in SOUNDS.items():
        if only and name not in only:
            continue
        write_ogg(name, fn())


if __name__ == "__main__":
    main()
