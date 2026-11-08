package com.jalaliclock;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/** اعلان دائمی «خاموش کردن صفحه» در نوار اعلان */
public class NotifHelper {
    public static void update(Context c, boolean on) {
        try {
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (!on) {
                nm.cancel(3);
                return;
            }
            PendingIntent pi = PendingIntent.getActivity(c, 3, new Intent(c, ScreenOffActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
            Notification n = new Notification.Builder(c)
                    .setContentTitle("خاموش کردن صفحه")
                    .setContentText("برای خاموش‌کردن صفحه اینجا را لمس کنید")
                    .setSmallIcon(android.R.drawable.ic_lock_power_off)
                    .setContentIntent(pi)
                    .setOngoing(true)
                    .setPriority(Notification.PRIORITY_LOW)
                    .build();
            nm.notify(3, n);
        } catch (Exception ignored) {
        }
    }
}
