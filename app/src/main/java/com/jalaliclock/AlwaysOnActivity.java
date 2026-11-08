package com.jalaliclock;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

/** ساعت تمام‌صفحه با نور کم؛ با لمس صفحه بسته می‌شود */
public class AlwaysOnActivity extends Activity {
    private ClockView cv;
    private static AlwaysOnActivity instance;
    private final Handler h = new Handler();
    private final Runnable callPoll = new Runnable() {
        public void run() {
            if (CallState.inCall(AlwaysOnActivity.this)) {
                ScreenWatchService.armed = false;   // تا پایان تماس دوباره باز نشود
                finish();
                return;
            }
            h.postDelayed(this, 1000);
        }
    };

    /** از سرویس صدا زده می‌شود: اگر ساعت باز است ببندد */
    public static void closeIfOpen() {
        if (instance != null) instance.finish();
    }

    private final Runnable stop = new Runnable() {
        public void run() {
            ScreenWatchService.armed = false;
            ScreenCtl.screenOff(AlwaysOnActivity.this);   // اگر دسترسی فعال باشد، صفحه کاملاً خاموش می‌شود
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Cfg c = Cfg.load(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = Math.max(0.01f, c.dim / 100f);
        getWindow().setAttributes(lp);

        cv = new ClockView(this, c, false);
        cv.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ScreenWatchService.armed = false;   // تا باز شدن قفل بعدی، دوباره خودکار باز نشود
                finish();
            }
        });
        cv.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                if (ScreenCtl.isAdmin(AlwaysOnActivity.this)) {
                    ScreenWatchService.armed = false;
                    ScreenCtl.screenOff(AlwaysOnActivity.this);     // نگه‌داشتن انگشت = خاموش کردن صفحه
                    finish();
                } else {
                    Toast.makeText(AlwaysOnActivity.this, "ابتدا در تنظیمات، دسترسی خاموش کردن صفحه را فعال کنید", Toast.LENGTH_LONG).show();
                }
                return true;
            }
        });
        setContentView(cv);
        hideUi();
        instance = this;
        h.postDelayed(callPoll, 1000);
        if (c.aodMin > 0) h.postDelayed(stop, c.aodMin * 60000L);
    }

    private void hideUi() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cv.start();
    }

    @Override
    protected void onPause() {
        cv.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        h.removeCallbacks(stop);
        h.removeCallbacks(callPoll);
        if (instance == this) instance = null;
        super.onDestroy();
    }
}
