package com.jalaliclock;

import android.content.Context;
import android.content.SharedPreferences;

public class Cfg {
    public int style = 2;          // 0 دیجیتال، 1 آنالوگ، 2 آنالوگ + دیجیتال داخل صفحه
    public int theme = 0, size = 100, dim = 25, aodMin = 0;
    public int fg = 0xFFFFFFFF, bg = 0xFF000000, accent = 0xFF00D2FF;
    public boolean arc = true, rainbow = false, seconds = true, date = true, weekday = true,
            h24 = true, fa = true, alarm = true, battery = true, burn = true, watch = false;
    // صدا
    public boolean speak = false, speakDate = true, speakAlarm = true, tick = false;
    /** 0 خودکار (موتور فارسی گوشی، وگرنه صدای داخلی) · 1 صدای فارسی داخلی برنامه · 2 تلفظ لاتین با موتور گوشی */
    public int speakMode = 0;
    /** نوع اعلام ساعت: 0 گفتار · 1 دینگ‌دانگ · 2 ناقوس · 3 صدای اعلان گوشی · 4 زنگ + گفتار */
    public int announce = 0;
    /** یک «دینگ» کوتاه قبل از اعلام ساعت با گفتار */
    public boolean dingBefore = true;
    /** صدای فارسی داخلی: 0 = MBROLA (طبیعی‌تر) · 1 = eSpeak · 2 = صدای ضبط‌شده توسط کاربر */
    public int voiceSet = 0;
    // تغییر خودکار تصادفی رنگ‌ها
    public boolean colorRandom = false, screenNotif = false;
    public int colorSec = 30, colorMode = 0;
    public int speakSec = 60, tickVol = 40;
    // آلارم: فاصله تکرار خودکار صدا و مدت زنگ خوردن (ثانیه)
    public int alarmRepeatSec = 5, alarmWindowSec = 60;
    // حرکت ساعت (ضدسوختگی): دامنه به درصد صفحه، زمان یک چرخه به ثانیه
    public int burnPct = 15, burnSec = 90;
    // ساعت‌های فعال بودن ساعت سخنگو و تیک‌تاک
    public int activeFrom = 0, activeTo = 24;

    /** ۱۱ طیف رنگی؛ برای ساعت دیجیتال و آنالوگ یکسان کار می‌کنند */
    public static final String[] PALETTE_NAMES = {
            "سیاه و سفید (مناسب AMOLED)", "آبی یخی", "سبز نئونی", "قرمز آتشین",
            "طلایی", "بنفش سلطنتی", "نارنجی گرم", "روشن (پس‌زمینه سفید)",
            "سیاه و آبی نئونی", "فیروزه‌ای و طلایی", "یاسی روشن"};

    // {رنگ نوشته، پس‌زمینه، رنگ تأکید (کمان و عقربه ثانیه)}
    static final int[][] PAL = {
            {0xFFFFFFFF, 0xFF000000, 0xFF00D2FF},
            {0xFFE0F0FF, 0xFF0D1B3D, 0xFF4FC3F7},
            {0xFF39FF14, 0xFF000000, 0xFF39FF14},
            {0xFFFF3B3B, 0xFF0A0505, 0xFFFF3B3B},
            {0xFFFFD700, 0xFF000000, 0xFFFFB300},
            {0xFFF3E8FF, 0xFF1A0B33, 0xFFC084FC},
            {0xFFFFE0B2, 0xFF2B1200, 0xFFFF9800},
            {0xFF202020, 0xFFF2F2F2, 0xFFE11D48},
            {0xFFE6F0FF, 0xFF000000, 0xFF1E90FF},
            {0xFFF5F0E1, 0xFF0F6B6E, 0xFFD4A537},
            {0xFF3B1F6B, 0xFFEFE6FF, 0xFFB06BFF}};

    public void applyTheme(int i) {
        i = Math.max(0, Math.min(PAL.length - 1, i));
        theme = i;
        fg = PAL[i][0];
        bg = PAL[i][1];
        accent = PAL[i][2];
    }

    public static Cfg load(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences("cfg", Context.MODE_PRIVATE);
        Cfg c = new Cfg();
        c.style = p.getInt("style", c.style);
        c.theme = Math.max(0, Math.min(PAL.length - 1, p.getInt("theme", c.theme)));
        c.size = p.getInt("size", c.size);
        c.dim = p.getInt("dim", c.dim);
        c.aodMin = p.getInt("aodMin", c.aodMin);
        c.fg = p.getInt("fg", c.fg);
        c.bg = p.getInt("bg", c.bg);
        c.accent = p.getInt("accent", c.accent);
        c.arc = p.getBoolean("arc", c.arc);
        c.rainbow = p.getBoolean("rainbow", c.rainbow);
        c.seconds = p.getBoolean("seconds", c.seconds);
        c.date = p.getBoolean("date", c.date);
        c.weekday = p.getBoolean("weekday", c.weekday);
        c.h24 = p.getBoolean("h24", c.h24);
        c.fa = p.getBoolean("fa", c.fa);
        c.alarm = p.getBoolean("alarm", c.alarm);
        c.battery = p.getBoolean("battery", c.battery);
        c.burn = p.getBoolean("burn", c.burn);
        c.watch = p.getBoolean("watch", c.watch);
        c.speak = p.getBoolean("speak", c.speak);
        c.speakDate = p.getBoolean("speakDate", c.speakDate);
        c.speakMode = p.getInt("speakMode", p.getBoolean("speakLatin", false) ? 2 : 0);
        c.speakAlarm = p.getBoolean("speakAlarm", c.speakAlarm);
        c.announce = Math.max(0, Math.min(4, p.getInt("announce", c.announce)));
        c.dingBefore = p.getBoolean("dingBefore", c.dingBefore);
        c.voiceSet = Math.max(0, Math.min(2, p.getInt("voiceSet", c.voiceSet)));
        c.colorRandom = p.getBoolean("colorRandom", c.colorRandom);
        c.screenNotif = p.getBoolean("screenNotif", c.screenNotif);
        c.colorSec = Math.max(3, Math.min(86400, p.getInt("colorSec", c.colorSec)));
        c.colorMode = p.getInt("colorMode", c.colorMode);
        c.alarmRepeatSec = Math.max(1, Math.min(600, p.getInt("alarmRepeatSec", c.alarmRepeatSec)));
        c.alarmWindowSec = Math.max(5, Math.min(3600, p.getInt("alarmWindowSec", c.alarmWindowSec)));
        c.burnPct = Math.max(0, Math.min(30, p.getInt("burnPct", c.burnPct)));
        c.burnSec = Math.max(10, Math.min(600, p.getInt("burnSec", c.burnSec)));
        c.activeFrom = Math.max(0, Math.min(24, p.getInt("activeFrom", c.activeFrom)));
        c.activeTo = Math.max(0, Math.min(24, p.getInt("activeTo", c.activeTo)));
        c.tick = p.getBoolean("tick", c.tick);
        c.speakSec = Math.max(3, Math.min(86400, p.getInt("speakSec", c.speakSec)));
        c.tickVol = Math.max(1, Math.min(100, p.getInt("tickVol", c.tickVol)));
        return c;
    }

    public void save(Context ctx) {
        ctx.getSharedPreferences("cfg", Context.MODE_PRIVATE).edit()
                .putInt("style", style).putInt("theme", theme).putInt("size", size).putInt("dim", dim)
                .putInt("aodMin", aodMin).putInt("fg", fg).putInt("bg", bg).putInt("accent", accent)
                .putBoolean("arc", arc).putBoolean("rainbow", rainbow).putBoolean("seconds", seconds)
                .putBoolean("date", date).putBoolean("weekday", weekday).putBoolean("h24", h24)
                .putBoolean("fa", fa).putBoolean("alarm", alarm).putBoolean("battery", battery)
                .putBoolean("burn", burn).putBoolean("watch", watch)
                .putBoolean("speak", speak).putBoolean("speakDate", speakDate).putInt("speakMode", speakMode).putBoolean("speakAlarm", speakAlarm).putInt("alarmRepeatSec", alarmRepeatSec)
                .putInt("alarmWindowSec", alarmWindowSec).putInt("burnPct", burnPct).putInt("burnSec", burnSec)
                .putInt("activeFrom", activeFrom).putInt("activeTo", activeTo)
                .putInt("announce", announce).putBoolean("dingBefore", dingBefore).putInt("voiceSet", voiceSet).putBoolean("colorRandom", colorRandom).putBoolean("screenNotif", screenNotif)
                .putInt("colorSec", colorSec).putInt("colorMode", colorMode)
                .putBoolean("tick", tick).putInt("speakSec", speakSec).putInt("tickVol", tickVol).apply();
    }
}
