package com.jalaliclock;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

/** صداهای ملایم برای اعلام ساعت: دینگ‌دانگ، ناقوس (به تعداد ساعت) و صدای اعلان گوشی */
public class Chimes {
    private static final int SR = 22050;
    private static final Object lock = new Object();
    private static AudioTrack current;
    private static Ringtone notif;

    private static void tone(double[] buf, double start, double f, double dur, double amp, boolean bell) {
        int i0 = (int) (start * SR), n = (int) (dur * SR);
        double[] ratio = bell ? new double[]{1, 2.01, 2.76, 4.07, 5.43} : new double[]{1, 2, 3};
        double[] a = bell ? new double[]{1, 0.5, 0.35, 0.2, 0.1} : new double[]{0.65, 0.25, 0.1};
        double[] dec = bell ? new double[]{1.5, 2.2, 3.0, 4.2, 5.5} : new double[]{3.2, 3.2, 3.2};
        for (int i = 0; i < n && i0 + i < buf.length; i++) {
            double t = i / (double) SR, v = 0;
            for (int k = 0; k < ratio.length; k++) v += a[k] * Math.exp(-dec[k] * t) * Math.sin(2 * Math.PI * f * ratio[k] * t);
            buf[i0 + i] += v * (t < 0.005 ? t / 0.005 : 1.0) * amp;
        }
    }

    /** kind 0: دینگ‌دانگ ملایم · kind 1: ناقوس با تعداد ضربه (strikes) */
    static short[] synth(int kind, int strikes, int volPct) {
        strikes = Math.max(1, Math.min(12, strikes));
        double len = kind == 0 ? 2.8 : kind == 2 ? 1.5 : (strikes - 1) * 1.15 + 3.2;
        double[] buf = new double[(int) (SR * len)];
        if (kind == 0) {
            tone(buf, 0.0, 880, 1.6, 0.6, false);
            tone(buf, 0.75, 659.25, 2.0, 0.6, false);
        } else if (kind == 2) {                       // یک «دینگ» کوتاه و روشن
            tone(buf, 0.0, 1318.5, 1.5, 0.5, true);
        } else {
            for (int s = 0; s < strikes; s++) tone(buf, s * 1.15, 440, 3.0, 0.45, true);
        }
        double vol = Math.max(1, Math.min(100, volPct)) / 100.0;
        short[] p = new short[buf.length];
        for (int i = 0; i < p.length; i++) p[i] = (short) (Math.max(-1, Math.min(1, buf[i])) * vol * 30000);
        int fade = Math.min(p.length / 4, SR / 30);          // محو کوتاه انتهای صدا
        for (int i = 0; i < fade; i++) p[p.length - 1 - i] = (short) (p[p.length - 1 - i] * i / fade);
        return p;
    }

    public static void play(Context ctx, int kind, int strikes, int volPct) {
        final short[] pcm = synth(kind, strikes, volPct);
        new Thread(new Runnable() {
            public void run() {
                try {
                    AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, SR, AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT, pcm.length * 2, AudioTrack.MODE_STATIC);
                    at.write(pcm, 0, pcm.length);
                    synchronized (lock) {
                        if (current != null) {
                            try {
                                current.stop();
                                current.release();
                            } catch (Exception ignored) {
                            }
                        }
                        current = at;
                    }
                    at.play();
                    Thread.sleep(pcm.length * 1000L / SR + 300);
                    synchronized (lock) {
                        if (current == at) {
                            try {
                                at.release();
                            } catch (Exception ignored) {
                            }
                            current = null;
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }).start();
    }

    /** صدای اعلان پیش‌فرض گوشی (همان صدای آشنای پیام‌ها) */
    public static void notif(Context ctx) {
        try {
            Uri u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            notif = RingtoneManager.getRingtone(ctx.getApplicationContext(), u);
            if (notif != null) notif.play();
        } catch (Exception ignored) {
        }
    }

    public static void stopAll() {
        synchronized (lock) {
            if (current != null) {
                try {
                    current.stop();
                    current.release();
                } catch (Exception ignored) {
                }
                current = null;
            }
        }
        try {
            if (notif != null) notif.stop();
        } catch (Exception ignored) {
        }
    }
}
