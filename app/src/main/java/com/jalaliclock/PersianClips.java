package com.jalaliclock;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioTrack;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * گفتار فارسی داخلی: کلمه‌ها از کلیپ‌های صوتی پشت هم پخش می‌شوند؛ به موتور گفتار گوشی نیازی نیست.
 * مجموعه‌ها: MBROLA («m_»)، eSpeak (بدون پیشوند) یا صدای ضبط‌شده توسط خود کاربر (پوشه voice).
 */
public class PersianClips {
    private static final int SR = 22050;
    private static final Map<String, short[]> cache = new HashMap<String, short[]>();
    private static final Object lock = new Object();
    private static AudioTrack current;

    private static List<String> one(String k) {
        List<String> l = new ArrayList<String>();
        l.add(k);
        return l;
    }

    /** عدد ۰ تا ۹۹۹۹ به صورت کلید کلیپ‌ها */
    static void num(int n, List<String> k) {
        if (n == 0) {
            k.add("o0");
            return;
        }
        List<List<String>> parts = new ArrayList<List<String>>();
        if (n >= 1000) {
            int th = n / 1000;
            List<String> p = new ArrayList<String>();
            if (th != 1) num(th, p);
            p.add("th");
            parts.add(p);
            n %= 1000;
        }
        if (n >= 100) {
            parts.add(one("h" + (n / 100)));
            n %= 100;
        }
        if (n >= 20) {
            parts.add(one("t" + (n / 10)));
            n %= 10;
            if (n > 0) parts.add(one("o" + n));
        } else if (n > 0) {
            parts.add(one("o" + n));
        }
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) k.add("va");
            k.addAll(parts.get(i));
        }
    }

    /** عدد ترتیبی روز: اول، دوم، ... بیست و یکم، سی‌ام، سی و یکم */
    static void ord(int n, List<String> k) {
        if (n <= 19) k.add("ord" + n);
        else if (n == 20) k.add("ordt20");
        else if (n == 30) k.add("ordt30");
        else {
            k.add("t" + (n / 10));
            k.add("va");
            k.add(n % 10 == 1 ? "ordo1" : "ord" + (n % 10));
        }
    }

    static String dayPart(int h) {
        if (h < 5) return "dp_bamdad";
        if (h < 12) return "dp_sobh";
        if (h < 13) return "dp_zohr";
        if (h < 17) return "dp_bad";
        if (h < 20) return "dp_asr";
        return "dp_shab";
    }

    public static List<String> keys(Calendar now, boolean date, boolean alarm) {
        List<String> k = new ArrayList<String>();
        if (alarm) k.add("alarm");
        int H = now.get(Calendar.HOUR_OF_DAY), M = now.get(Calendar.MINUTE);
        k.add("saat");
        num(H % 12 == 0 ? 12 : H % 12, k);
        if (M == 30) {
            k.add("va");
            k.add("nim");
        } else if (M > 0) {
            k.add("va");
            num(M, k);
            k.add("daq");
        }
        k.add(dayPart(H));
        k.add("ast");
        if (date) {
            int[] j = Jalali.fromGregorian(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH));
            k.add("emruz");
            k.add("d" + (now.get(Calendar.DAY_OF_WEEK) - 1));
            ord(j[2], k);
            k.add("m" + j[1]);
            k.add("mah");
            num(j[0], k);
        }
        return k;
    }

    public static File voiceDir(Context ctx) {
        return new File(ctx.getFilesDir(), "voice");
    }

    public static void clearCache() {
        synchronized (cache) {
            cache.clear();
        }
    }

    static short[] decode(byte[] b) {
        int cnt = Math.max(0, (b.length - 44) / 2);
        short[] s = new short[cnt];
        for (int i = 0; i < cnt; i++) s[i] = (short) ((b[44 + 2 * i + 1] << 8) | (b[44 + 2 * i] & 0xff));
        return s;
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
        in.close();
        return bo.toByteArray();
    }

    /** اگر custom باشد اول ضبط خود کاربر؛ اگر نبود، کلیپ آماده با پیشوند prefix */
    private static short[] load(Context ctx, String key, String prefix, boolean custom) {
        String ck = (custom ? "u:" : "") + prefix + key;
        synchronized (cache) {
            short[] c = cache.get(ck);
            if (c != null) return c;
        }
        try {
            byte[] b = null;
            if (custom) {
                File f = new File(voiceDir(ctx), key + ".wav");
                if (f.exists()) b = readAll(new FileInputStream(f));
            }
            if (b == null) {
                int id = ctx.getResources().getIdentifier(prefix + key, "raw", ctx.getPackageName());
                if (id == 0) return null;
                b = readAll(ctx.getResources().openRawResource(id));
            }
            short[] s = decode(b);
            synchronized (cache) {
                cache.put(ck, s);
            }
            return s;
        } catch (Exception e) {
            return null;
        }
    }

    /** پخش کلیدها پشت هم (روی یک ترد جدا) */
    public static void speak(final Context ctx, final List<String> keys, final int stream, final String prefix, final boolean custom) {
        final Context app = ctx.getApplicationContext();
        new Thread(new Runnable() {
            public void run() {
                try {
                    int gap = (int) (SR * 0.05);
                    List<short[]> clips = new ArrayList<short[]>();
                    int total = 0;
                    for (String key : keys) {
                        short[] s = load(app, key, prefix, custom);
                        if (s == null) continue;
                        clips.add(s);
                        total += s.length + gap;
                    }
                    if (total == 0) return;
                    short[] pcm = new short[total];
                    int pos = 0;
                    for (short[] s : clips) {
                        System.arraycopy(s, 0, pcm, pos, s.length);
                        pos += s.length + gap;
                    }
                    AudioTrack at = new AudioTrack(stream, SR, AudioFormat.CHANNEL_OUT_MONO,
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
    }
}
