package com.jalaliclock;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Handler;
import android.view.View;

import java.util.Calendar;
import java.util.Random;

/** ساعت شمسی: دیجیتال / آنالوگ / ترکیبی، با کمان رنگی و جابه‌جایی ضدسوختگی */
public class ClockView extends View {
    private Cfg c;
    private final boolean preview;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Handler handler = new Handler();
    private boolean running;
    private float offX, offY;
    private long lastShiftMin = -1;
    private final Random rnd = new Random();
    private String batTxt;
    private long batAt;
    private Speaker speaker;
    private TickPlayer tickPlayer;
    private Runnable onCall;
    private int fg, bg, accent;
    private long colorSlot = -1;
    private int lastPal = -1;

    private final Runnable tick = new Runnable() {
        public void run() {
            if (!running) return;
            onSecond();
            invalidate();
            schedule();
        }
    };

    public ClockView(Context ctx, Cfg cfg, boolean preview) {
        super(ctx);
        this.c = cfg;
        this.preview = preview;
    }

    public void setCfg(Cfg cfg) {
        c = cfg;
        batAt = 0;
        colorSlot = -1;
        invalidate();
    }

    public void start() {
        running = true;
        handler.removeCallbacks(tick);
        invalidate();
        schedule();
    }

    public void stop() {
        running = false;
        handler.removeCallbacks(tick);
        Chimes.stopAll();
        if (tickPlayer != null) {
            tickPlayer.release();
            tickPlayer = null;
        }
        if (speaker != null) {
            speaker.shutdown();
            speaker = null;
        }
    }

    /** هر ثانیه: تیک‌تاک و اعلام زمان (فقط وقتی ساعت واقعاً نمایش داده می‌شود) */
    /** وقتی تماس شروع شد صدا می‌شود (برای بستن محافظ صفحه/حالت همیشه‌روشن) */
    public void setOnCall(Runnable r) {
        onCall = r;
    }

    private void onSecond() {
        if (preview) return;
        if (CallState.inCall(getContext())) {
            Chimes.stopAll();          // تماس: همه صداها قطع و ساعت کنار می‌رود
            if (tickPlayer != null) {
                tickPlayer.release();
                tickPlayer = null;
            }
            if (speaker != null) {
                speaker.shutdown();
                speaker = null;
            }
            if (onCall != null) {
                Runnable r = onCall;
                onCall = null;
                r.run();
            }
            return;
        }
        Calendar now = Calendar.getInstance();
        int S = now.get(Calendar.SECOND);
        int hr = now.get(Calendar.HOUR_OF_DAY);
        boolean active = c.activeFrom <= c.activeTo ? (hr >= c.activeFrom && hr < c.activeTo)
                : (hr >= c.activeFrom || hr < c.activeTo);
        if (!active) return;             // خارج از ساعت‌های فعال: بی‌صدا
        if (c.tick) {
            if (tickPlayer == null) tickPlayer = new TickPlayer();
            tickPlayer.play(S % 2 == 1, c.tickVol);
        }
        if (c.speak && c.speakSec > 0) {
            int sod = now.get(Calendar.HOUR_OF_DAY) * 3600 + now.get(Calendar.MINUTE) * 60 + S;
            if (sod % c.speakSec == 0) {
                Speaker sp = null;
                if (c.announce == 0 || c.announce == 4) {
                    if (speaker == null) speaker = new Speaker(getContext(), null);
                    sp = speaker;
                }
                Announcer.announce(getContext(), c, sp, now);
            }
        }
    }

    /** رنگ‌های فعلی؛ اگر «تغییر تصادفی رنگ» روشن باشد هر چند ثانیه (تنظیم کاربر) رنگ جدیدی انتخاب می‌شود */
    private void updateColors() {
        if (!c.colorRandom) {
            fg = c.fg;
            bg = c.bg;
            accent = c.accent;
            colorSlot = -1;
            return;
        }
        long slot = System.currentTimeMillis() / (Math.max(3, c.colorSec) * 1000L);
        if (slot == colorSlot) return;
        colorSlot = slot;
        if (c.colorMode == 0) {                     // یکی از طیف‌های رنگی تیره (بدون پس‌زمینه سفید)
            int n = Cfg.PAL.length - 1, i;
            do {
                i = rnd.nextInt(n);
            } while (i == lastPal && n > 1);
            lastPal = i;
            fg = Cfg.PAL[i][0];
            bg = Cfg.PAL[i][1];
            accent = Cfg.PAL[i][2];
        } else {                                    // رنگ‌های کاملاً تصادفی و شاد
            float hue = rnd.nextFloat() * 360f;
            fg = Color.HSVToColor(new float[]{hue, 0.12f, 1f});
            accent = Color.HSVToColor(new float[]{(hue + 40f) % 360f, 0.85f, 1f});
            bg = Color.HSVToColor(new float[]{hue, 0.6f, 0.10f});
        }
    }

    private void schedule() {
        handler.postDelayed(tick, 1000 - (System.currentTimeMillis() % 1000) + 3);
    }

    // ---------------------------------------------------------------- متن‌ها
    private static String two(int v) {
        return v < 10 ? "0" + v : String.valueOf(v);
    }

    private String timeText(int H, int M, int S) {
        int hh = c.h24 ? H : (H % 12 == 0 ? 12 : H % 12);
        String s = two(hh) + ":" + two(M);
        if (c.seconds) s += ":" + two(S);
        if (!c.h24) s += H >= 12 ? " ب.ظ" : " ق.ظ";
        return Jalali.digits(s, c.fa);
    }

    private String dateText(Calendar now) {
        int[] j = Jalali.fromGregorian(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH));
        String s = j[2] + " " + Jalali.MONTHS[j[1] - 1] + " " + j[0];
        if (c.weekday) s = Jalali.DAYS[now.get(Calendar.DAY_OF_WEEK) - 1] + " " + s;
        return Jalali.digits(s, c.fa);
    }

    private String alarmText() {
        try {
            AlarmManager am = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
            AlarmManager.AlarmClockInfo ai = am.getNextAlarmClock();
            if (ai == null) return null;
            Calendar t = Calendar.getInstance();
            t.setTimeInMillis(ai.getTriggerTime());
            return Jalali.digits("آلارم: " + two(t.get(Calendar.HOUR_OF_DAY)) + ":" + two(t.get(Calendar.MINUTE)), c.fa);
        } catch (Exception e) {
            return null;
        }
    }

    private String batteryText() {
        long now = System.currentTimeMillis();
        if (now - batAt < 30000 && batTxt != null) return batTxt;
        batAt = now;
        try {
            Intent b = getContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (b == null) return null;
            int level = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = b.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            boolean plugged = b.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
            if (level < 0) return null;
            int pct = level * 100 / Math.max(1, scale);
            batTxt = Jalali.digits("باتری " + pct + "٪" + (plugged ? " (در حال شارژ)" : ""), c.fa);
        } catch (Exception e) {
            batTxt = null;
        }
        return batTxt;
    }

    // ---------------------------------------------------------------- رسم
    private void text(Canvas cv, String s, float cx, float top, float blockH, float size, int color, boolean bold) {
        p.reset();
        p.setAntiAlias(true);
        p.setTextSize(size);
        p.setColor(color);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        Paint.FontMetrics fm = p.getFontMetrics();
        float base = top + (blockH - (fm.descent - fm.ascent)) / 2f - fm.ascent;
        cv.drawText(s, cx, base, p);
    }

    @Override
    protected void onDraw(Canvas cv) {
        int w = getWidth(), h = getHeight();
        updateColors();
        cv.drawColor(bg);
        Calendar now = Calendar.getInstance();
        int H = now.get(Calendar.HOUR_OF_DAY), M = now.get(Calendar.MINUTE), S = now.get(Calendar.SECOND);
        float u = Math.min(w, h) * c.size / 100f;

        float cx = w / 2f, cy = h / 2f;

        boolean analog = c.style != 0, digitalOnly = c.style == 0;
        float D = analog ? u * 0.80f : 0, tsz = u * 0.2f, dsz = u * 0.07f, isz = u * 0.05f, gap = u * 0.04f;
        String timeTxt = timeText(H, M, S);
        String dateTxt = c.date ? dateText(now) : null;
        String alarmTxt = c.alarm ? alarmText() : null;
        String batT = c.battery ? batteryText() : null;

        float total = 0;
        if (analog) total += D + gap;
        if (digitalOnly) total += tsz * 1.2f + gap;
        if (dateTxt != null) total += dsz * 1.5f;
        if (alarmTxt != null) total += isz * 1.6f;
        if (batT != null) total += isz * 1.6f;
        // حرکت آهسته و نرم ساعت (ضدسوختگی): دامنه و سرعت در تنظیمات؛ از لبه صفحه بیرون نمی‌زند
        if (c.burn && !preview && c.burnPct > 0) {
            float amp = Math.min(w, h) * c.burnPct / 100f;
            float ampX = Math.max(0f, Math.min(amp, (w - u * 0.86f) / 2f));
            float ampY = Math.max(0f, Math.min(amp, (h - total) / 2f));
            double ph = 2 * Math.PI * (now.getTimeInMillis() / 1000.0) / Math.max(10, c.burnSec);
            cx += (float) (ampX * Math.sin(ph));
            cy += (float) (ampY * Math.sin(ph * 0.73 + 1.3));
        }
        float y = cy - total / 2f;

        if (analog) {
            if (c.face > 0) Faces.draw(cv, p, cx, y + D / 2f, D / 2f, H, M, S, c.face);
            else drawDial(cv, cx, y + D / 2f, D / 2f, H, M, S, c.style == 2 ? timeTxt : null);
            y += D + gap;
        }
        if (digitalOnly) {
            text(cv, timeTxt, cx, y, tsz * 1.2f, tsz, fg, true);
            y += tsz * 1.2f + gap;
        }
        if (dateTxt != null) {
            text(cv, dateTxt, cx, y, dsz * 1.5f, dsz, fg, false);
            y += dsz * 1.5f;
        }
        if (alarmTxt != null) {
            text(cv, alarmTxt, cx, y, isz * 1.6f, isz, accent, false);
            y += isz * 1.6f;
        }
        if (batT != null) {
            text(cv, batT, cx, y, isz * 1.6f, isz, (fg & 0x00FFFFFF) | (170 << 24), false);
        }
    }

    private int shifted(int color, double shift) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[0] = (float) ((hsv[0] + shift) % 360);
        hsv[1] = Math.max(0.6f, hsv[1]);
        hsv[2] = 1f;
        return Color.HSVToColor(hsv);
    }

    private void ring(Canvas cv, float cx, float cy, float rad, float thick, double sweep, double shift) {
        RectF o = new RectF(cx - rad, cy - rad, cx + rad, cy + rad);
        p.reset();
        p.setAntiAlias(true);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(thick);
        p.setColor((fg & 0x00FFFFFF) | (55 << 24));
        cv.drawOval(o, p);
        if (sweep < 1) return;
        if (!c.rainbow) {
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setColor(shift == 0 ? accent : shifted(accent, shift));
            cv.drawArc(o, -90, (float) sweep, false, p);
        } else {
            p.setStrokeCap(Paint.Cap.BUTT);
            float[] hsv = new float[3];
            for (double a = 0; a < sweep; a += 3) {
                hsv[0] = (float) ((a + shift) % 360);
                hsv[1] = 0.9f;
                hsv[2] = 1f;
                p.setColor(Color.HSVToColor(hsv));
                cv.drawArc(o, (float) (a - 90), (float) Math.min(4.0, sweep - a + 0.5), false, p);
            }
        }
    }

    private void hand(Canvas cv, float cx, float cy, double deg, float len, float wd, float tail, int color) {
        double a = Math.toRadians(deg);
        float sn = (float) Math.sin(a), cs = (float) Math.cos(a);
        p.reset();
        p.setAntiAlias(true);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(wd);
        p.setColor(color);
        cv.drawLine(cx - sn * tail, cy + cs * tail, cx + sn * len, cy - cs * len, p);
    }

    private void drawFace(Canvas cv, float cx, float cy, float r) {
        if (faceBmp == null || faceBmpId != c.face) {
            int[] ids = {R.drawable.face_1, R.drawable.face_2, R.drawable.face_3, R.drawable.face_4, R.drawable.face_5};
            faceBmp = BitmapFactory.decodeResource(getResources(), ids[c.face - 1]);
            faceBmpId = c.face;
        }
        if (faceBmp == null) return;
        int bw = faceBmp.getWidth(), bh = faceBmp.getHeight(), s = Math.min(bw, bh);
        Rect src = new Rect((bw - s) / 2, (bh - s) / 2, (bw + s) / 2, (bh + s) / 2);
        Path clip = new Path();
        clip.addCircle(cx, cy, r, Path.Direction.CW);
        cv.save();
        cv.clipPath(clip);
        Paint bp = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        cv.drawBitmap(faceBmp, src, new RectF(cx - r, cy - r, cx + r, cy + r), bp);
        cv.restore();
    }

    private void drawDial(Canvas cv, float cx, float cy, float r, int H, int M, int S, String inText) {
        float thick = Math.max(4f, r * 0.07f), edge = r - 2;
        double sec = S, min = M + sec / 60.0, hr = (H % 12) + min / 60.0;

        if (c.arc) {
            float r1 = r - thick / 2f - 2, r2 = r1 - thick * 1.35f;
            ring(cv, cx, cy, r1, thick, sec * 6, 0);      // کمان ثانیه
            ring(cv, cx, cy, r2, thick, min * 6, 150);    // کمان دقیقه
            edge = r2 - thick / 2f - 4;
        }

        for (int i = 0; i < 60; i++) {
            boolean big = i % 5 == 0;
            float len = big ? r * 0.1f : r * 0.05f;
            double a = i * Math.PI / 30;
            float sn = (float) Math.sin(a), cs = (float) Math.cos(a);
            p.reset();
            p.setAntiAlias(true);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(big ? Math.max(2f, r * 0.03f) : 1.5f);
            p.setColor(big ? fg : ((fg & 0x00FFFFFF) | (110 << 24)));
            cv.drawLine(cx + sn * edge, cy - cs * edge, cx + sn * (edge - len), cy - cs * (edge - len), p);
        }

        if (r > 140) {
            float fs = r * 0.13f, rn = edge - r * 0.1f - fs * 0.85f;
            for (int n = 1; n <= 12; n++) {
                double a = n * Math.PI / 6;
                float nx = cx + (float) Math.sin(a) * rn, ny = cy - (float) Math.cos(a) * rn;
                text(cv, Jalali.digits(String.valueOf(n), c.fa), nx, ny - fs * 0.65f, fs * 1.3f, fs, fg, true);
            }
        }

        if (inText != null) {   // ساعت دیجیتال داخل صفحه آنالوگ
            float sz = edge * 0.19f;
            text(cv, inText, cx, cy + edge * 0.30f, sz * 1.3f, sz, fg, true);
        }

        hand(cv, cx, cy, hr * 30, edge * 0.5f, Math.max(4f, r * 0.055f), 0, fg);
        hand(cv, cx, cy, min * 6, edge * 0.78f, Math.max(3f, r * 0.035f), 0, fg);
        hand(cv, cx, cy, sec * 6, edge * 0.88f, Math.max(2f, r * 0.018f), edge * 0.15f, accent);
        p.reset();
        p.setAntiAlias(true);
        p.setColor(accent);
        cv.drawCircle(cx, cy, Math.max(4f, r * 0.045f), p);
    }
}
