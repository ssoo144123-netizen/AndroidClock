package com.jalaliclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/** ذخیره آلارم‌ها و زمان‌بندی دقیق با AlarmManager (حتی در حالت خواب گوشی) */
public class AlarmStore {
    static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("alarms", Context.MODE_PRIVATE);
    }

    public static List<AlarmItem> load(Context c) {
        List<AlarmItem> l = new ArrayList<AlarmItem>();
        String s = sp(c).getString("list", "");
        if (s.length() == 0) return l;
        for (String part : s.split("\\|")) {
            AlarmItem a = AlarmItem.parse(part);
            if (a != null) l.add(a);
        }
        return l;
    }

    public static void save(Context c, List<AlarmItem> l) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < l.size(); i++) {
            if (i > 0) sb.append('|');
            sb.append(l.get(i).ser());
        }
        sp(c).edit().putString("list", sb.toString()).apply();
    }

    public static int newId(Context c) {
        int n = sp(c).getInt("idc", 1);
        sp(c).edit().putInt("idc", n + 1).apply();
        return n;
    }

    public static void startTimer(Context c, int secs, String label) {
        sp(c).edit().putLong("timerAt", System.currentTimeMillis() + secs * 1000L).putString("timerLabel", label).apply();
        schedule(c);
    }

    public static void cancelTimer(Context c) {
        sp(c).edit().remove("timerAt").apply();
        schedule(c);
    }

    public static long timerAt(Context c) {
        long t = sp(c).getLong("timerAt", 0);
        return t > System.currentTimeMillis() ? t : 0;
    }

    public static void schedule(Context c) {
        long now = System.currentTimeMillis(), best = Long.MAX_VALUE;
        int bestId = -2;
        for (AlarmItem a : load(c)) {
            if (!a.on) continue;
            long t = a.next(now);
            if (t >= 0 && t < best) {
                best = t;
                bestId = a.id;
            }
        }
        long tAt = sp(c).getLong("timerAt", 0);
        if (tAt > now && tAt < best) {
            best = tAt;
            bestId = -1;
        }
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent fire = PendingIntent.getBroadcast(c, 0, new Intent(c, AlarmReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
        if (bestId == -2) {
            am.cancel(fire);
            sp(c).edit().remove("nextId").apply();
            return;
        }
        PendingIntent show = PendingIntent.getActivity(c, 1, new Intent(c, AlarmsActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        am.setAlarmClock(new AlarmManager.AlarmClockInfo(best, show), fire);
        sp(c).edit().putInt("nextId", bestId).putLong("nextWhen", best).apply();
    }
}
