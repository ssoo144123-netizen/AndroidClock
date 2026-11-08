package com.jalaliclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent i) {
        Cfg c = Cfg.load(ctx);
        AlarmStore.schedule(ctx);                 // بعد از روشن شدن گوشی، آلارم‌ها دوباره زمان‌بندی می‌شوند
        NotifHelper.update(ctx, c.screenNotif);
        if (c.watch) ctx.startService(new Intent(ctx, ScreenWatchService.class));
    }
}
