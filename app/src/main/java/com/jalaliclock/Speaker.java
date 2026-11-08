package com.jalaliclock;

import android.content.Context;
import android.media.AudioManager;
import android.speech.tts.TextToSpeech;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * ساعت سخنگو.
 * حالت خودکار: اگر یکی از موتورهای گفتار گوشی فارسی داشته باشد از آن استفاده می‌شود،
 * وگرنه از صدای فارسی داخلی برنامه (PersianClips). حالت‌های دیگر در Cfg.speakMode.
 */
public class Speaker {
    private final Context ctx;
    private final Runnable onReady;
    private TextToSpeech tts;
    private TextToSpeech probing;
    private final List<String> candidates = new ArrayList<String>();
    private int candIdx;
    private boolean ready, persian, closed;
    private String engineName = "";
    private Locale faLocale = new Locale("fa", "IR");
    private Cfg pendCfg;
    private Calendar pendNow;
    private String pendLabel;
    private boolean pendAlarm;

    public Speaker(Context c, Runnable onReady) {
        this.ctx = c.getApplicationContext();
        this.onReady = onReady;
        open(null);
    }

    private void open(final String pkg) {
        final TextToSpeech[] h = new TextToSpeech[1];
        TextToSpeech.OnInitListener l = new TextToSpeech.OnInitListener() {
            public void onInit(int status) {
                handle(h[0], status, pkg);
            }
        };
        h[0] = (pkg == null) ? new TextToSpeech(ctx, l) : new TextToSpeech(ctx, l, pkg);
        if (pkg != null) probing = h[0];
    }

    private void handle(TextToSpeech inst, int status, String pkg) {
        if (closed) {
            if (inst != null) inst.shutdown();
            return;
        }
        boolean fa = false;
        Locale loc = new Locale("fa");
        if (status == TextToSpeech.SUCCESS && inst != null) {
            int r = inst.isLanguageAvailable(loc);
            if (r < 0) {
                loc = new Locale("fa", "IR");
                r = inst.isLanguageAvailable(loc);
            }
            fa = r >= 0;
        }
        if (pkg == null) {
            if (status == TextToSpeech.SUCCESS) tts = inst;
            if (fa) {
                finish(loc, true, "پیش‌فرض گوشی");
                return;
            }
            if (status == TextToSpeech.SUCCESS) {
                String def = inst.getDefaultEngine();
                for (TextToSpeech.EngineInfo e : inst.getEngines())
                    if (!e.name.equals(def)) candidates.add(e.name);
            }
            probeNext();
        } else {
            probing = null;
            if (fa) {
                if (tts != null) tts.shutdown();
                tts = inst;
                finish(loc, true, pkg);
            } else {
                if (inst != null) inst.shutdown();
                probeNext();
            }
        }
    }

    private void probeNext() {
        if (candIdx < candidates.size()) open(candidates.get(candIdx++));
        else finish(faLocale, false, "");
    }

    private void finish(Locale loc, boolean fa, String name) {
        faLocale = loc;
        persian = fa;
        engineName = name;
        ready = true;
        if (pendCfg != null) {
            Cfg c = pendCfg;
            pendCfg = null;
            say(c, pendNow, pendLabel, pendAlarm);
        }
        if (onReady != null) onReady.run();
    }

    public boolean isReady() {
        return ready;
    }

    public boolean hasPersian() {
        return persian;
    }

    public String engine() {
        return engineName;
    }

    public void sayTime(Cfg c, Calendar now) {
        say(c, now, null, false);
    }

    public void sayAlarm(Cfg c, Calendar now, String label) {
        say(c, now, label, true);
    }

    private void say(Cfg c, Calendar now, String label, boolean alarm) {
        int stream = alarm ? AudioManager.STREAM_ALARM : AudioManager.STREAM_MUSIC;
        if (c.speakMode == 1) {                                   // همیشه صدای فارسی داخلی
            PersianClips.speak(ctx, PersianClips.keys(now, !alarm && c.speakDate, alarm), stream, c.voiceSet == 1 ? "" : "m_", c.voiceSet == 2);
            return;
        }
        if (!ready) {
            pendCfg = c;
            pendNow = now;
            pendLabel = label;
            pendAlarm = alarm;
            return;
        }
        if (tts == null || (c.speakMode == 0 && !persian)) {      // موتور فارسی ندارد → صدای داخلی
            PersianClips.speak(ctx, PersianClips.keys(now, !alarm && c.speakDate, alarm), stream, c.voiceSet == 1 ? "" : "m_", c.voiceSet == 2);
            return;
        }
        boolean fa = c.speakMode == 0;                            // موتور فارسی دارد (یا حالت لاتین)
        try {
            String text = SpeechText.phrase(now, fa, !alarm && c.speakDate);
            if (alarm) text = (fa ? "آلارم. " : "alarm. ") + text + (fa && label != null && label.length() > 0 ? ". " + label : "");
            tts.setLanguage(fa ? faLocale : Locale.US);
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "clk");
        } catch (Exception ignored) {
        }
    }

    public void shutdown() {
        closed = true;
        PersianClips.stopAll();
        try {
            if (tts != null) {
                tts.stop();
                tts.shutdown();
            }
            if (probing != null) probing.shutdown();
        } catch (Exception ignored) {
        }
    }
}
