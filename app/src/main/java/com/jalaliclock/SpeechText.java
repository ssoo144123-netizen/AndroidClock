package com.jalaliclock;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** ساخت جمله «ساعت هفت و نیم صبح است…» به فارسی (یا تلفظ لاتین برای صدای انگلیسی) */
public class SpeechText {
    static final String[] OF = {"صفر", "یک", "دو", "سه", "چهار", "پنج", "شش", "هفت", "هشت", "نه", "ده", "یازده", "دوازده", "سیزده", "چهارده", "پانزده", "شانزده", "هفده", "هجده", "نوزده"};
    static final String[] TF = {"", "", "بیست", "سی", "چهل", "پنجاه", "شصت", "هفتاد", "هشتاد", "نود"};
    static final String[] HF = {"", "صد", "دویست", "سیصد", "چهارصد", "پانصد", "ششصد", "هفتصد", "هشتصد", "نهصد"};
    static final String[] OL = {"sefr", "yek", "do", "se", "chahar", "panj", "shesh", "haft", "hasht", "noh", "dah", "yazdah", "davazdah", "sizdah", "chahardah", "panzdah", "shanzdah", "hefdah", "hejdah", "nuzdah"};
    static final String[] TL = {"", "", "bist", "si", "chehel", "panjah", "shast", "haftad", "hashtad", "navad"};
    static final String[] HL = {"", "sad", "divist", "sisad", "chaharsad", "pansad", "sheshsad", "haftsad", "hashtsad", "nohsad"};
    static final String[] MON_L = {"Farvardin", "Ordibehesht", "Khordad", "Tir", "Mordad", "Shahrivar", "Mehr", "Aban", "Azar", "Dey", "Bahman", "Esfand"};
    static final String[] DAY_L = {"Yekshanbeh", "Doshanbeh", "Seshanbeh", "Chaharshanbeh", "Panjshanbeh", "Jomeh", "Shanbeh"};

    static String w(int n, boolean fa) {
        String[] ones = fa ? OF : OL, tens = fa ? TF : TL, hund = fa ? HF : HL;
        String thousand = fa ? "هزار" : "hezar", and = fa ? " و " : " o ";
        if (n == 0) return ones[0];
        List<String> parts = new ArrayList<String>();
        if (n >= 1000) {
            int th = n / 1000;
            parts.add(th == 1 ? thousand : w(th, fa) + " " + thousand);
            n %= 1000;
        }
        if (n >= 100) {
            parts.add(hund[n / 100]);
            n %= 100;
        }
        if (n >= 20) {
            parts.add(tens[n / 10]);
            n %= 10;
            if (n > 0) parts.add(ones[n]);
        } else if (n > 0) parts.add(ones[n]);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) sb.append(and);
            sb.append(parts.get(i));
        }
        return sb.toString();
    }

    static String ord(int n, boolean fa) {
        if (n == 1) return fa ? "اول" : "avval";
        String s = w(n, fa);
        if (fa) {
            if (s.endsWith("سه")) return s.substring(0, s.length() - 2) + "سوم";
            if (s.endsWith("ی")) return s + "\u200Cام";
            return s + "م";
        }
        if (s.endsWith("se")) return s.substring(0, s.length() - 2) + "sevom";
        if (s.endsWith("si")) return s + "yom";
        return s + "om";
    }

    static String dayPart(int h, boolean fa) {
        if (h < 5) return fa ? "بامداد" : "bamdad";
        if (h < 12) return fa ? "صبح" : "sobh";
        if (h < 13) return fa ? "ظهر" : "zohr";
        if (h < 17) return fa ? "بعد از ظهر" : "bad az zohr";
        if (h < 20) return fa ? "عصر" : "asr";
        return fa ? "شب" : "shab";
    }

    public static String phrase(Calendar now, boolean fa, boolean date) {
        int H = now.get(Calendar.HOUR_OF_DAY), M = now.get(Calendar.MINUTE);
        int h12 = H % 12 == 0 ? 12 : H % 12;
        String s = (fa ? "ساعت " : "saat ") + w(h12, fa);
        if (M == 30) s += fa ? " و نیم" : " o nim";
        else if (M > 0) s += (fa ? " و " : " o ") + w(M, fa) + (fa ? " دقیقه" : " daghigheh");
        s += " " + dayPart(H, fa) + (fa ? " است" : " ast");
        if (date) {
            int[] j = Jalali.fromGregorian(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH));
            int dow = now.get(Calendar.DAY_OF_WEEK) - 1;
            if (fa)
                s += ". امروز " + Jalali.DAYS[dow] + " " + ord(j[2], true) + " " + Jalali.MONTHS[j[1] - 1] + " ماه " + w(j[0], true);
            else
                s += ". emrooz " + DAY_L[dow] + " " + ord(j[2], false) + " " + MON_L[j[1] - 1] + " mah " + w(j[0], false);
        }
        return s;
    }
}
