package com.jalaliclock;

import java.util.ArrayList;
import java.util.List;

/** فهرست کلمه‌هایی که باید برای «صدای خودم» ضبط شوند (کلید فایل، متن) */
public class VoiceWords {
    public static final List<String[]> LIST = new ArrayList<String[]>();
    public static final List<String> GROUP_NAMES = new ArrayList<String>();
    public static final List<Integer> GROUP_START = new ArrayList<Integer>();

    private static void group(String name) {
        GROUP_NAMES.add(name);
        GROUP_START.add(LIST.size());
    }

    private static void add(String key, String text) {
        LIST.add(new String[]{key, text});
    }

    static {
        group("عددها ۰ تا ۱۹");
        for (int i = 0; i < 20; i++) add("o" + i, SpeechText.OF[i]);
        group("دهگان‌ها (بیست تا نود)");
        for (int i = 2; i < 10; i++) add("t" + i, SpeechText.TF[i]);
        group("صدگان و هزار");
        for (int i = 1; i < 10; i++) add("h" + i, SpeechText.HF[i]);
        add("th", "هزار");
        group("ترتیبی‌ها (برای روز ماه)");
        add("ord1", "اول");
        String[] ord = {"دوم", "سوم", "چهارم", "پنجم", "ششم", "هفتم", "هشتم", "نهم", "دهم", "یازدهم", "دوازدهم",
                "سیزدهم", "چهاردهم", "پانزدهم", "شانزدهم", "هفدهم", "هجدهم", "نوزدهم"};
        for (int i = 0; i < ord.length; i++) add("ord" + (i + 2), ord[i]);
        add("ordt20", "بیستم");
        add("ordt30", "سی‌ام");
        add("ordo1", "یکم");
        group("کلمه‌های ساعت");
        add("saat", "ساعت");
        add("daq", "دقیقه");
        add("va", "و");
        add("nim", "نیم");
        add("ast", "است");
        add("dp_bamdad", "بامداد");
        add("dp_sobh", "صبح");
        add("dp_zohr", "ظهر");
        add("dp_bad", "بعد از ظهر");
        add("dp_asr", "عصر");
        add("dp_shab", "شب");
        add("emruz", "امروز");
        add("mah", "ماه");
        add("alarm", "آلارم");
        group("روزهای هفته");
        for (int i = 0; i < 7; i++) add("d" + i, Jalali.DAYS[i]);
        group("ماه‌های شمسی");
        for (int i = 0; i < 12; i++) add("m" + (i + 1), Jalali.MONTHS[i]);
    }
}
