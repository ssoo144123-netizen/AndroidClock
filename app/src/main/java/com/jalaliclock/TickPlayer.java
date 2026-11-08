package com.jalaliclock;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import java.util.Random;

/** صدای تیک‌تاک؛ داخل برنامه ساخته می‌شود (بدون فایل صوتی) */
public class TickPlayer {
    private static final int SR = 22050;
    private AudioTrack tick, tock;

    private AudioTrack make(double freq) {
        int n = (int) (SR * 0.05);
        short[] p = new short[n];
        Random rnd = new Random(7);
        for (int i = 0; i < n; i++) {
            double t = i / (double) SR;
            double v = (Math.sin(2 * Math.PI * freq * t) * 0.7 + (rnd.nextDouble() * 2 - 1) * 0.3) * Math.exp(-t * 90);
            p[i] = (short) (v * 30000);
        }
        AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, SR, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, n * 2, AudioTrack.MODE_STATIC);
        at.write(p, 0, n);
        return at;
    }

    public void play(boolean isTock, int volPct) {
        try {
            if (tick == null) {
                tick = make(1900);
                tock = make(1250);
            }
            AudioTrack at = isTock ? tock : tick;
            at.setVolume(Math.max(0.01f, Math.min(1f, volPct / 100f)));
            try {
                at.stop();
            } catch (Exception ignored) {
            }
            at.reloadStaticData();
            at.play();
        } catch (Exception ignored) {
        }
    }

    public void release() {
        try {
            if (tick != null) tick.release();
            if (tock != null) tock.release();
        } catch (Exception ignored) {
        }
        tick = null;
        tock = null;
    }
}
