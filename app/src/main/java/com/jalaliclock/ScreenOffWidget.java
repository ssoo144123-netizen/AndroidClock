package com.jalaliclock;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/** ویجت ۱×۱ روی صفحه اصلی: با یک لمس صفحه خاموش می‌شود */
public class ScreenOffWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context ctx, AppWidgetManager m, int[] ids) {
        for (int id : ids) {
            RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_screenoff);
            PendingIntent pi = PendingIntent.getActivity(ctx, 5, new Intent(ctx, ScreenOffActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setOnClickPendingIntent(R.id.btn, pi);
            m.updateAppWidget(id, rv);
        }
    }
}
