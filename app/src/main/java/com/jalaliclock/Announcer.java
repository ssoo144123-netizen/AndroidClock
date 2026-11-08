package com.jalaliclock;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.Calendar;

/** اعلام ساعت بر اساس «نوع اعلام»: گفتار / دینگ‌دانگ / ناقوس / صدای اعلان گوشی / زنگ + گفتار */
public class Announcer {
    public static final String[] NAMES = {
            "گفتار (ساعت سخنگو)", "زنگ ملایم (دینگ‌دانگ)", "ناقوس (سر ساعت به تعداد ساعت)",
            "صدای اعلان گوشی", "زنگ ملایم + گفتار"};

    public static void announce(final Context ctx, final Cfg c, final Speaker sp, Calendar now) {
        int m = c.announce;
        if (m == 3) {
            Chimes.notif(ctx);
            return;
        }
        if (m == 1 || m == 4) Chimes.play(ctx, 0, 1, c.tickVol + 20);
        if (m == 2) {
            int h12 = now.get(Calendar.HOUR_OF_DAY) % 12;
            if (h12 == 0) h12 = 12;
            Chimes.play(ctx, 1, now.get(Calendar.MINUTE) == 0 ? h12 : 1, c.tickVol + 20);
        }
        if (sp == null) return;
        long delay = 0;
        if (m == 4) delay = 2900;
        else if (m == 0 && c.dingBefore) {              // دینگ، بعد اعلام ساعت
            Chimes.play(ctx, 2, 1, c.tickVol + 20);
            delay = 1100;
        }
        if (delay == 0) sp.sayTime(c, now);
        else new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            public void run() {
                sp.sayTime(c, Calendar.getInstance());
            }
        }, delay);
    }
}
