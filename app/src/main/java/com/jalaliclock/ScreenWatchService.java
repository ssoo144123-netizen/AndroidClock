package com.jalaliclock;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;

/** با خاموش شدن صفحه، حالت همیشه‌روشن را باز می‌کند */
public class ScreenWatchService extends Service {
    public static volatile boolean armed = true;

    private final BroadcastReceiver rcv = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent i) {
            String a = i.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(a)) {
                if (armed) launch();
            } else if (Intent.ACTION_USER_PRESENT.equals(a)) {
                armed = true;
            }
        }
    };

    private final Handler h = new Handler();
    private boolean wasCall;
    private final Runnable poll = new Runnable() {
        public void run() {
            boolean call = CallState.inCall(ScreenWatchService.this);
            if (call && !wasCall) {
                wasCall = true;
                armed = false;
                AlwaysOnActivity.closeIfOpen();
            } else if (!call && wasCall) {
                wasCall = false;
                armed = true;
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                if (!pm.isInteractive()) launch();     // تماس تمام شد و صفحه خاموش است
            }
            h.postDelayed(this, 2000);
        }
    };

    private void launch() {
        if (CallState.inCall(this)) return;
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK
                    | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "jalaliclock");
            wl.acquire(4000);
            Intent in = new Intent(this, AlwaysOnActivity.class);
            in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(in);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        h.postDelayed(poll, 2000);
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_SCREEN_OFF);
        f.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(rcv, f);

        PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this, SettingsActivity.class), 0);
        Notification n = new Notification.Builder(this)
                .setContentTitle("ساعت شمسی فعال است")
                .setContentText("برای تنظیمات یا توقف، اینجا را لمس کنید")
                .setSmallIcon(android.R.drawable.ic_menu_recent_history)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
        startForeground(1, n);
    }

    @Override
    public int onStartCommand(Intent i, int flags, int id) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        h.removeCallbacks(poll);
        try {
            unregisterReceiver(rcv);
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent i) {
        return null;
    }
}
