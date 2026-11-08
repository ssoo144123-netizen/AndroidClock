package com.jalaliclock;

import android.service.dreams.DreamService;

/** محافظ صفحه (Daydream): هنگام شارژ/داک، ساعت شمسی نمایش داده می‌شود */
public class JalaliDream extends DreamService {
    private ClockView cv;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Cfg c = Cfg.load(this);
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(c.dim >= 60);
        cv = new ClockView(this, c, false);
        cv.setOnCall(new Runnable() {
            public void run() {
                JalaliDream.this.finish();      // با تماس، محافظ صفحه بسته می‌شود
            }
        });
        setContentView(cv);
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        if (cv != null) cv.start();
    }

    @Override
    public void onDreamingStopped() {
        if (cv != null) cv.stop();
        super.onDreamingStopped();
    }
}
