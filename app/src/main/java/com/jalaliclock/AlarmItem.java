package com.jalaliclock;

import java.util.Calendar;

public class AlarmItem {
    public int id, h, m, s, days;       // days: بیت‌ها بر اساس Calendar.DAY_OF_WEEK-1 (یکشنبه=۰)؛ صفر = فقط یک‌بار
    public boolean on = true;
    public String label = "";

    public String ser() {
        String l = label.replace("|", " ").replace(":", " ").replace("\n", " ").replace("\r", " ");
        return id + ":" + (on ? 1 : 0) + ":" + h + ":" + m + ":" + s + ":" + days + ":" + l;
    }

    public static AlarmItem parse(String x) {
        try {
            String[] p = x.split(":", 7);
            AlarmItem a = new AlarmItem();
            a.id = Integer.parseInt(p[0]);
            a.on = p[1].equals("1");
            a.h = Math.max(0, Math.min(23, Integer.parseInt(p[2])));
            a.m = Math.max(0, Math.min(59, Integer.parseInt(p[3])));
            a.s = Math.max(0, Math.min(59, Integer.parseInt(p[4])));
            a.days = Integer.parseInt(p[5]) & 127;
            a.label = p.length > 6 ? p[6] : "";
            return a;
        } catch (Exception e) {
            return null;
        }
    }

    /** زمان بعدی زنگ (میلی‌ثانیه) یا -1 */
    public long next(long now) {
        Calendar base = Calendar.getInstance();
        base.setTimeInMillis(now);
        for (int d = 0; d < 8; d++) {
            Calendar t = (Calendar) base.clone();
            t.add(Calendar.DAY_OF_YEAR, d);
            t.set(Calendar.HOUR_OF_DAY, h);
            t.set(Calendar.MINUTE, m);
            t.set(Calendar.SECOND, s);
            t.set(Calendar.MILLISECOND, 0);
            if (t.getTimeInMillis() <= now) continue;
            if (days != 0 && (days & (1 << (t.get(Calendar.DAY_OF_WEEK) - 1))) == 0) continue;
            return t.getTimeInMillis();
        }
        return -1;
    }

    public static String daysText(int mask) {
        if (mask == 0) return "یک‌بار";
        if ((mask & 127) == 127) return "هر روز";
        int[] order = {6, 0, 1, 2, 3, 4, 5};
        StringBuilder sb = new StringBuilder();
        for (int d : order)
            if ((mask & (1 << d)) != 0) {
                if (sb.length() > 0) sb.append("، ");
                sb.append(Jalali.DAYS[d]);
            }
        return sb.toString();
    }

    public String summary() {
        String t = (h < 10 ? "0" : "") + h + ":" + (m < 10 ? "0" : "") + m + ":" + (s < 10 ? "0" : "") + s;
        return Jalali.digits(t, true) + "   " + (label.length() > 0 ? label + "   " : "") + "[" + daysText(days) + "]";
    }
}
