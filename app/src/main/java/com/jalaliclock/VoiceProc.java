package com.jalaliclock;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** پردازش ضبط‌ها: کاهش نرخ نمونه‌برداری، حذف سکوت ابتدا/انتها، یکسان‌سازی بلندی و ذخیره WAV */
public class VoiceProc {
    public static final int SR = 22050;

    /** تبدیل نرخ نمونه‌برداری دلخواه به ۲۲۰۵۰ هرتز */
    static short[] resample(short[] in, int sr) {
        if (sr == SR) return in;
        double ratio = sr / (double) SR;
        int n = (int) (in.length / ratio);
        short[] out = new short[Math.max(0, n)];
        for (int i = 0; i < out.length; i++) {
            double a = i * ratio;
            if (ratio > 1.0) {                                   // کاهش: میانگین‌گیری روی بازه
                int s = (int) a, e = Math.min(in.length, (int) Math.ceil(a + ratio));
                double sum = 0;
                int cnt = 0;
                for (int k = s; k < e; k++) {
                    sum += in[k];
                    cnt++;
                }
                out[i] = (short) (cnt > 0 ? sum / cnt : 0);
            } else {                                              // افزایش: درون‌یابی خطی
                int j = (int) a;
                double fr = a - j;
                out[i] = (short) (in[j] * (1 - fr) + (j + 1 < in.length ? in[j + 1] : in[j]) * fr);
            }
        }
        return out;
    }

    private static int le(byte[] b, int o, int n) {
        int v = 0;
        for (int i = n - 1; i >= 0; i--) v = (v << 8) | (b[o + i] & 0xff);
        return v;
    }

    /** خواندن فایل WAV (PCM ۱۶ بیتی، مونو یا استریو) و تبدیل به مونو؛ نرخ نمونه در srOut[0] */
    public static short[] readWavMono(byte[] b, int[] srOut) {
        if (b.length < 44 || b[0] != 'R' || b[1] != 'I' || b[2] != 'F' || b[3] != 'F') return null;
        int ch = 1, bits = 16, fmt = 1, sr = SR;
        int pos = 12;
        while (pos + 8 <= b.length) {
            String id = new String(b, pos, 4);
            int size = le(b, pos + 4, 4);
            int body = pos + 8;
            if (id.equals("fmt ")) {
                fmt = le(b, body, 2);
                ch = Math.max(1, le(b, body + 2, 2));
                sr = le(b, body + 4, 4);
                bits = le(b, body + 14, 2);
            } else if (id.equals("data")) {
                if ((fmt != 1 && fmt != 0xFFFE) || bits != 16) return null;
                int avail = Math.min(size < 0 ? Integer.MAX_VALUE : size, b.length - body);
                int frames = avail / (2 * ch);
                short[] out = new short[frames];
                for (int i = 0; i < frames; i++) {
                    int sum = 0;
                    for (int c = 0; c < ch; c++) {
                        int o = body + (i * ch + c) * 2;
                        sum += (short) ((b[o + 1] << 8) | (b[o] & 0xff));
                    }
                    out[i] = (short) (sum / ch);
                }
                srOut[0] = sr;
                return out;
            }
            pos = body + size + (size & 1);
        }
        return null;
    }

    public static short[] process(short[] in, int srIn) {
        short[] d = resample(in, srIn);
        int peak = 0;
        for (short v : d) peak = Math.max(peak, Math.abs(v));
        if (peak < 200) return new short[0];                        // فقط سکوت
        int thr = Math.max(400, (int) (peak * 0.05));
        int s = 0;
        while (s < d.length && Math.abs(d[s]) < thr) s++;
        int e = d.length - 1;
        while (e > s && Math.abs(d[e]) < thr) e--;
        int pad = SR * 12 / 1000;
        s = Math.max(0, s - pad);
        e = Math.min(d.length - 1, e + pad);
        int n = e - s + 1;
        if (n <= 0) return new short[0];
        long mean = 0;
        for (int i = 0; i < n; i++) mean += d[s + i];
        mean /= n;                                                  // حذف DC
        int pk = 1;
        for (int i = 0; i < n; i++) pk = Math.max(pk, (int) Math.abs(d[s + i] - mean));
        double g = Math.min(26000.0 / pk, 10.0);
        short[] out = new short[n];
        for (int i = 0; i < n; i++) out[i] = (short) Math.max(-32768, Math.min(32767, (d[s + i] - mean) * g));
        int fade = Math.min(n / 4, SR * 4 / 1000);
        for (int i = 0; i < fade; i++) {
            out[i] = (short) (out[i] * i / fade);
            out[n - 1 - i] = (short) (out[n - 1 - i] * i / fade);
        }
        return out;
    }

    public static byte[] wavBytes(short[] pcm, int sr) throws IOException {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        int dl = pcm.length * 2;
        o.write(new byte[]{'R', 'I', 'F', 'F'});
        le32(o, 36 + dl);
        o.write(new byte[]{'W', 'A', 'V', 'E', 'f', 'm', 't', ' '});
        le32(o, 16);
        le16(o, 1);
        le16(o, 1);
        le32(o, sr);
        le32(o, sr * 2);
        le16(o, 2);
        le16(o, 16);
        o.write(new byte[]{'d', 'a', 't', 'a'});
        le32(o, dl);
        for (short v : pcm) le16(o, v);
        return o.toByteArray();
    }

    public static void save(File f, short[] pcm) throws IOException {
        FileOutputStream fo = new FileOutputStream(f);
        try {
            fo.write(wavBytes(pcm, SR));
        } finally {
            fo.close();
        }
    }

    private static void le16(ByteArrayOutputStream o, int v) {
        o.write(v & 0xff);
        o.write((v >> 8) & 0xff);
    }

    private static void le32(ByteArrayOutputStream o, int v) {
        le16(o, v & 0xffff);
        le16(o, (v >> 16) & 0xffff);
    }
}
