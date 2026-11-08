package com.jalaliclock;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;

import java.util.List;

public class AlarmReceiver extends BroadcastReceiver {
    /** اگر آلارم وسط تماس رسید، صفحه زنگ باز نمی‌شود؛ فقط یک اعلان می‌آید */
    private void notifyMissed(Context ctx, String label) {
        try {
            Notification n = new Notification.Builder(ctx)
                    .setContentTitle("آلارم: " + label)
                    .setContentText("هنگام تماس رسید و زنگ نخورد")
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setAutoCancel(true)
                    .setDefaults(Notification.DEFAULT_VIBRATE)
                    .build();
            ((NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE)).notify(2, n);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        SharedPreferences p = AlarmStore.sp(ctx);
        int id = p.getInt("nextId", -2);
        String label = "آلارم";
        if (id == -1) {
            label = p.getString("timerLabel", "تایمر");
            p.edit().remove("timerAt").apply();
        } else {
            List<AlarmItem> list = AlarmStore.load(ctx);
            for (AlarmItem a : list)
                if (a.id == id) {
                    if (a.label.length() > 0) label = a.label;
                    if (a.days == 0) a.on = false;          // آلارم یک‌بار: بعد از زنگ، خودش غیرفعال می‌شود
                    AlarmStore.save(ctx, list);
                    break;
                }
        }
        try {
            PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
            pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "jalaliclock:alarm").acquire(10000);
        } catch (Exception ignored) {
        }
        if (CallState.inCall(ctx)) {
            notifyMissed(ctx, label);
            AlarmStore.schedule(ctx);
            return;
        }
        Intent in = new Intent(ctx, AlarmRingActivity.class);
        in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        in.putExtra("label", label);
        ctx.startActivity(in);
        AlarmStore.schedule(ctx);
    }
}
