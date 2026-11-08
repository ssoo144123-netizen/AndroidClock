package com.jalaliclock;

import android.content.Context;
import android.media.AudioManager;

/** تشخیص تماس (تلفن یا تماس‌های اینترنتی) بدون نیاز به هیچ مجوزی، از روی حالت صدای سیستم */
public class CallState {
    public static boolean inCall(Context c) {
        try {
            AudioManager am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
            int m = am.getMode();
            return m == AudioManager.MODE_RINGTONE || m == AudioManager.MODE_IN_CALL
                    || m == AudioManager.MODE_IN_COMMUNICATION;
        } catch (Exception e) {
            return false;
        }
    }
}
