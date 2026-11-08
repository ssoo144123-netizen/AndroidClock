package com.jalaliclock;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;

/**
 * صفحه زنگ آلارم. بدون نیاز به تأیید: صدا هر «فاصله تکرار» ثانیه خودکار تکرار می‌شود
 * و بعد از «مدت زنگ خوردن» ثانیه، آلارم خودش متوقف و بسته می‌شود.
 */
public class AlarmRingActivity extends Activity {
    private final Handler h = new Handler();
    private Cfg c;
    private long start;
    private int lastSlot = -1;
    private TextView timeTv, info;
    private Ringtone ring;
    private Speaker speaker;
    private String label = "آلارم";
    private boolean done;

    private final Runnable loop = new Runnable() {
        public void run() {
            step();
        }
    };
    private final Runnable stopRing = new Runnable() {
        public void run() {
            stopRingtone();
        }
    };
    private final Runnable sayIt = new Runnable() {
        public void run() {
            if (done) return;
            if (speaker == null) speaker = new Speaker(AlarmRingActivity.this, null);
            speaker.sayAlarm(c, Calendar.getInstance(), label);
        }
    };
    private final Runnable speakIt = new Runnable() {
        public void run() {
            if (done) return;
            if (c.dingBefore) {                       // دینگ قبل از اعلام
                Chimes.play(AlarmRingActivity.this, 2, 1, c.tickVol + 20);
                h.postDelayed(sayIt, 1100);
            } else {
                sayIt.run();
            }
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        c = Cfg.load(this);
        if (getIntent().getStringExtra("label") != null) label = getIntent().getStringExtra("label");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(c.bg);
        root.setPadding(40, 40, 40, 40);
        timeTv = tv(root, "", 56, c.fg, true);
        tv(root, label, 28, c.accent, true);
        info = tv(root, "", 16, c.fg, false);
        Button stop = new Button(this);
        stop.setText("توقف");
        stop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finishRing();
            }
        });
        root.addView(stop, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(root);
        start = System.currentTimeMillis();
        h.post(loop);
    }

    private TextView tv(LinearLayout root, String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, 16, 0, 16);
        root.addView(t);
        return t;
    }

    private void step() {
        if (done) return;
        if (CallState.inCall(this)) {      // تماس شروع شد: آلارم کنار می‌رود
            finishRing();
            return;
        }
        double el = (System.currentTimeMillis() - start) / 1000.0;
        int rep = Math.max(1, c.alarmRepeatSec);
        int slot = (int) (el / rep);
        if (slot != lastSlot) {
            lastSlot = slot;
            playSlot(slot, rep);
        }
        int rem = (int) Math.ceil(c.alarmWindowSec - el);
        if (rem <= 0) {
            finishRing();
            return;
        }
        Calendar now = Calendar.getInstance();
        String t = String.format("%02d:%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), now.get(Calendar.SECOND));
        timeTv.setText(Jalali.digits(t, c.fa));
        info.setText("توقف خودکار تا " + Jalali.digits(String.valueOf(rem), c.fa) + " ثانیه دیگر");
        h.postDelayed(loop, 250);
    }

    private void playSlot(int slot, int rep) {
        stopRingtone();
        try {
            Uri u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (u == null) u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            ring = RingtoneManager.getRingtone(this, u);
            if (ring != null) {
                ring.setStreamType(AudioManager.STREAM_ALARM);
                ring.play();
            }
        } catch (Exception ignored) {
        }
        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) v.vibrate(new long[]{0, 400, 200, 400}, -1);
        } catch (Exception ignored) {
        }
        h.postDelayed(stopRing, Math.max(500, Math.min(3000, rep * 1000 - 200)));
        if (slot == 0 && c.speakAlarm) h.postDelayed(speakIt, Math.max(800, Math.min(3300, rep * 1000 - 100)));
    }

    private void stopRingtone() {
        try {
            if (ring != null) ring.stop();
        } catch (Exception ignored) {
        }
    }

    private void finishRing() {
        if (done) return;
        done = true;
        h.removeCallbacksAndMessages(null);
        stopRingtone();
        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) v.cancel();
        } catch (Exception ignored) {
        }
        if (speaker != null) {
            speaker.shutdown();
            speaker = null;
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        finishRing();
        super.onDestroy();
    }
}
