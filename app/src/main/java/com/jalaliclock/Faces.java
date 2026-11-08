package com.jalaliclock;

import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

/** پنج طرح صفحه‌ی ساعت آنالوگ که با کد رسم می‌شوند؛ id از ۱ تا ۵ */
public final class Faces {
    private Faces() {}

    private static final String[] ROMAN = {"XII", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI"};

    public static void draw(Canvas cv, Paint p, float cx, float cy, float r, int H, int M, int S, int id) {
        double sec = S, min = M + sec / 60.0, hr = (H % 12) + min / 60.0;
        double hd = hr * 30, md = min * 6, sd = sec * 6;
        switch (id) {
            case 1: skmei(cv, p, cx, cy, r, hd, md, sd); break;
            case 2: grid(cv, p, cx, cy, r, hd, md, sd); break;
            case 3: marble(cv, p, cx, cy, r, hd, md, sd); break;
            case 4: neon(cv, p, cx, cy, r, hd, md, sd, H, M); break;
            case 5: floral(cv, p, cx, cy, r, hd, md, sd); break;
            default: break;
        }
    }

    // ۱: مشکی با نشانگرهای آبی و صفحه‌ی فرعی ثانیه
    private static void skmei(Canvas cv, Paint p, float cx, float cy, float r, double hd, double md, double sd) {
        gradientFill(cv, p, cx, cy, r, 0xFF1A1A1A, 0xFF000000);
        ring(cv, p, cx, cy, r - 2, Math.max(2f, r * 0.02f), 0xFF1E90FF);
        for (int i = 0; i < 60; i++) {
            boolean big = i % 5 == 0;
            tick(cv, p, cx, cy, i * 6.0, r * 0.93f, big ? r * 0.09f : r * 0.04f,
                    big ? Math.max(2f, r * 0.025f) : 1.2f, big ? 0xFF9FD0FF : 0x66FFFFFF);
        }
        float sx = cx, sy = cy + r * 0.42f, sr = r * 0.2f;
        fillCircle(cv, p, sx, sy, sr, 0xFF050505);
        ring(cv, p, sx, sy, sr, Math.max(1.5f, r * 0.012f), 0xFF1E90FF);
        line(cv, p, sx, sy, sd, sr * 0.85f, sr * 0.2f, Math.max(1.5f, r * 0.012f), 0xFF1E90FF, 0);
        line(cv, p, cx, cy, hd, r * 0.5f, r * 0.08f, r * 0.05f, 0xFF1E90FF, 0);
        line(cv, p, cx, cy, md, r * 0.8f, r * 0.1f, r * 0.035f, 0xFF2D8CFF, 0);
        line(cv, p, cx, cy, sd, r * 0.88f, r * 0.15f, Math.max(1.5f, r * 0.012f), 0xFF4FC3F7, 0);
        fillCircle(cv, p, cx, cy, r * 0.04f, 0xFF1E90FF);
    }

    // ۲: مشکی با شبکه‌ی مربع‌های رنگی و اعداد سفید درشت
    private static void grid(Canvas cv, Paint p, float cx, float cy, float r, double hd, double md, double sd) {
        gradientFill(cv, p, cx, cy, r, 0xFF101010, 0xFF000000);
        float s = r * 0.13f;
        int[] cols = {0xFF4FC3F7, 0xFF7C4DFF, 0xFF26C6DA, 0xFFFFFFFF};
        for (int gx = -7; gx <= 7; gx++) {
            for (int gy = -7; gy <= 7; gy++) {
                float x = cx + gx * s, y = cy + gy * s;
                float dist = (float) Math.hypot(x - cx, y - cy);
                if (dist > r * 0.9f || y < cy - r * 0.25f) continue;
                if ((gx * 3 + gy * 5 + 70) % 4 == 0) continue;
                p.reset();
                p.setAntiAlias(true);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(Math.max(1f, r * 0.01f));
                p.setColor((cols[(gx * gx + gy) & 3] & 0x00FFFFFF) | (0x40 << 24));
                cv.drawRect(x - s * 0.35f, y - s * 0.35f, x + s * 0.35f, y + s * 0.35f, p);
            }
        }
        ring(cv, p, cx, cy, r - 2, Math.max(2f, r * 0.02f), 0xFFFFFFFF);
        for (int n = 1; n <= 12; n++) {
            label(cv, p, String.valueOf(n), px(cx, n * 30.0, r * 0.74f), py(cy, n * 30.0, r * 0.74f),
                    r * 0.2f, 0xFFFFFFFF, true, Typeface.SANS_SERIF, 0);
        }
        line(cv, p, cx, cy, hd, r * 0.45f, r * 0.06f, r * 0.05f, 0xFFFFFFFF, 0);
        line(cv, p, cx, cy, md, r * 0.7f, r * 0.1f, r * 0.03f, 0xFFFFFFFF, 0);
        line(cv, p, cx, cy, sd, r * 0.85f, r * 0.15f, Math.max(1.5f, r * 0.012f), 0xFF4FC3F7, 0);
        fillCircle(cv, p, cx, cy, r * 0.04f, 0xFFFFFFFF);
    }

    // ۳: صفحه‌ی مرمری فیروزه‌ای، قاب طلایی با دانه‌ها و اعداد رومی
    private static void marble(Canvas cv, Paint p, float cx, float cy, float r, double hd, double md, double sd) {
        fillCircle(cv, p, cx, cy, r, 0xFFD4A537);
        gradientFill(cv, p, cx, cy, r * 0.9f, 0xFF3FB8B5, 0xFF0B4F55);
        p.reset();
        p.setAntiAlias(true);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(r * 0.02f);
        p.setColor(0x33FFFFFF);
        cv.drawArc(new RectF(cx - r * 0.7f, cy - r * 0.5f, cx + r * 0.5f, cy + r * 0.7f), 200, 120, false, p);
        cv.drawArc(new RectF(cx - r * 0.5f, cy - r * 0.8f, cx + r * 0.8f, cy + r * 0.4f), 20, 100, false, p);
        cv.drawArc(new RectF(cx - r * 0.3f, cy - r * 0.2f, cx + r * 0.9f, cy + r * 0.9f), 280, 80, false, p);
        for (int i = 0; i < 60; i++) {
            fillCircle(cv, p, px(cx, i * 6.0, r * 0.955f), py(cy, i * 6.0, r * 0.955f),
                    r * 0.018f, i % 5 == 0 ? 0xFFFFE9A0 : 0xFF9C7A1E);
        }
        ring(cv, p, cx, cy, r * 0.86f, Math.max(1f, r * 0.01f), 0xFFD4A537);
        for (int n = 1; n <= 12; n++) {
            label(cv, p, ROMAN[n % 12], px(cx, n * 30.0, r * 0.7f), py(cy, n * 30.0, r * 0.7f),
                    r * 0.16f, 0xFFF5F0E1, true, Typeface.SERIF, 0);
        }
        taper(cv, p, cx, cy, hd, r * 0.5f, r * 0.035f, 0xFFD4A537);
        taper(cv, p, cx, cy, md, r * 0.74f, r * 0.025f, 0xFFF5F0E1);
        line(cv, p, cx, cy, sd, r * 0.8f, r * 0.15f, Math.max(1.5f, r * 0.012f), 0xFFD4A537, 0);
        fillCircle(cv, p, cx, cy, r * 0.05f, 0xFFD4A537);
    }

    // ۴: ورزشی سبز نئونی با درخشش و نمایش دیجیتال کوچک
    private static void neon(Canvas cv, Paint p, float cx, float cy, float r, double hd, double md, double sd, int H, int M) {
        ring(cv, p, cx, cy, r * 0.98f, r * 0.08f, 0x2A8CFF00);
        ring(cv, p, cx, cy, r * 0.98f, r * 0.04f, 0x558CFF00);
        fillCircle(cv, p, cx, cy, r * 0.97f, 0xFF0A0F05);
        ring(cv, p, cx, cy, r * 0.93f, Math.max(1.5f, r * 0.02f), 0xFFA6FF1A);
        for (int n = 1; n <= 12; n++) {
            label(cv, p, String.valueOf(n), px(cx, n * 30.0, r * 0.76f), py(cy, n * 30.0, r * 0.76f),
                    r * 0.2f, 0xFFB6FF1A, true, Typeface.SANS_SERIF, 0xFF7CFF00);
        }
        int h12 = H % 12 == 0 ? 12 : H % 12;
        box(cv, p, cx, cy + r * 0.42f, r * 0.5f, r * 0.18f, String.format("%d:%02d", h12, M), r * 0.11f, 0xFFB6FF1A);
        line(cv, p, cx, cy, hd, r * 0.5f, r * 0.08f, r * 0.06f, 0xFFB6FF1A, 0xFF7CFF00);
        line(cv, p, cx, cy, md, r * 0.78f, r * 0.1f, r * 0.04f, 0xFFB6FF1A, 0xFF7CFF00);
        line(cv, p, cx, cy, sd, r * 0.85f, r * 0.18f, Math.max(1.5f, r * 0.015f), 0xFFD4FF3A, 0);
        fillCircle(cv, p, cx, cy, r * 0.05f, 0xFFB6FF1A);
    }

    // ۵: صفحه‌ی روشن یاسی با حاشیه‌ی نقطه‌چین بنفش و گل وسط
    private static void floral(Canvas cv, Paint p, float cx, float cy, float r, double hd, double md, double sd) {
        gradientFill(cv, p, cx, cy, r, 0xFFFFFFFF, 0xFFE7D9FF);
        p.reset();
        p.setAntiAlias(true);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(r * 0.05f);
        p.setColor(0xFFB06BFF);
        p.setPathEffect(new DashPathEffect(new float[]{r * 0.07f, r * 0.035f}, 0));
        cv.drawCircle(cx, cy, r - r * 0.04f, p);
        p.setPathEffect(null);
        ring(cv, p, cx, cy, r * 0.88f, Math.max(1.5f, r * 0.015f), 0xFF8E5CD9);
        for (int n = 1; n <= 12; n++) {
            label(cv, p, String.valueOf(n), px(cx, n * 30.0, r * 0.74f), py(cy, n * 30.0, r * 0.74f),
                    r * 0.15f, 0xFF4B2A7A, false, Typeface.SANS_SERIF, 0);
        }
        for (int k = 0; k < 5; k++) {
            fillCircle(cv, p, px(cx, k * 72.0, r * 0.14f), py(cy, k * 72.0, r * 0.14f), r * 0.07f, 0xCC7FB8FF);
        }
        fillCircle(cv, p, cx, cy, r * 0.07f, 0xFFFFD27F);
        line(cv, p, cx, cy, hd, r * 0.5f, r * 0.08f, r * 0.05f, 0xFF4B2A7A, 0);
        line(cv, p, cx, cy, md, r * 0.76f, r * 0.1f, r * 0.035f, 0xFF8E5CD9, 0);
        line(cv, p, cx, cy, sd, r * 0.82f, r * 0.18f, Math.max(1.5f, r * 0.012f), 0xFFFF6FB5, 0);
        fillCircle(cv, p, cx, cy, r * 0.035f, 0xFFFF6FB5);
    }

    // ---------- ابزارهای کمکی ----------

    private static float px(float cx, double deg, float d) {
        return cx + (float) (Math.sin(Math.toRadians(deg)) * d);
    }

    private static float py(float cy, double deg, float d) {
        return cy - (float) (Math.cos(Math.toRadians(deg)) * d);
    }

    private static void gradientFill(Canvas cv, Paint p, float x, float y, float rr, int c1, int c2) {
        p.reset();
        p.setAntiAlias(true);
        p.setShader(new RadialGradient(x, y, rr, c1, c2, Shader.TileMode.CLAMP));
        cv.drawCircle(x, y, rr, p);
        p.setShader(null);
    }

    private static void fillCircle(Canvas cv, Paint p, float x, float y, float rr, int color) {
        p.reset();
        p.setAntiAlias(true);
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        cv.drawCircle(x, y, rr, p);
    }

    private static void ring(Canvas cv, Paint p, float x, float y, float rr, float wd, int color) {
        p.reset();
        p.setAntiAlias(true);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(wd);
        p.setColor(color);
        cv.drawCircle(x, y, rr, p);
    }

    private static void tick(Canvas cv, Paint p, float cx, float cy, double deg, float outer, float len, float wd, int color) {
        p.reset();
        p.setAntiAlias(true);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(wd);
        p.setColor(color);
        cv.drawLine(px(cx, deg, outer), py(cy, deg, outer), px(cx, deg, outer - len), py(cy, deg, outer - len), p);
    }

    private static void line(Canvas cv, Paint p, float cx, float cy, double deg, float len, float tail, float wd, int color, int glow) {
        p.reset();
        p.setAntiAlias(true);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(wd);
        p.setColor(color);
        if (glow != 0) p.setShadowLayer(wd * 1.5f, 0, 0, glow);
        cv.drawLine(px(cx, deg, -tail), py(cy, deg, -tail), px(cx, deg, len), py(cy, deg, len), p);
    }

    private static void taper(Canvas cv, Paint p, float cx, float cy, double deg, float len, float halfBase, int color) {
        double a = Math.toRadians(deg);
        float sn = (float) Math.sin(a), cs = (float) Math.cos(a);
        Path path = new Path();
        path.moveTo(cx + cs * halfBase, cy + sn * halfBase);
        path.lineTo(cx + sn * len, cy - cs * len);
        path.lineTo(cx - cs * halfBase, cy - sn * halfBase);
        path.close();
        p.reset();
        p.setAntiAlias(true);
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        cv.drawPath(path, p);
    }

    private static void label(Canvas cv, Paint p, String s, float x, float y, float size, int color, boolean bold, Typeface tf, int glow) {
        p.reset();
        p.setAntiAlias(true);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(size);
        p.setColor(color);
        p.setTypeface(Typeface.create(tf, bold ? Typeface.BOLD : Typeface.NORMAL));
        if (glow != 0) p.setShadowLayer(size * 0.4f, 0, 0, glow);
        cv.drawText(s, x, y + size * 0.35f, p);
    }

    private static void box(Canvas cv, Paint p, float x, float y, float w, float h, String txt, float size, int color) {
        RectF rect = new RectF(x - w / 2, y - h / 2, x + w / 2, y + h / 2);
        p.reset();
        p.setAntiAlias(true);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF0F1A08);
        cv.drawRoundRect(rect, h * 0.3f, h * 0.3f, p);
        p.reset();
        p.setAntiAlias(true);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1f, h * 0.06f));
        p.setColor(0xFFA6FF1A);
        cv.drawRoundRect(rect, h * 0.3f, h * 0.3f, p);
        label(cv, p, txt, x, y, size, color, true, Typeface.MONOSPACE, 0);
    }
}
