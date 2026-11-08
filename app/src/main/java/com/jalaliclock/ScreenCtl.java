package com.jalaliclock;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

/** خاموش کردن (با دسترسی مدیر دستگاه) و روشن کردن صفحه */
public class ScreenCtl {
    public static ComponentName admin(Context c) {
        return new ComponentName(c, AdminReceiver.class);
    }

    private static DevicePolicyManager dpm(Context c) {
        return (DevicePolicyManager) c.getSystemService(Context.DEVICE_POLICY_SERVICE);
    }

    public static boolean isAdmin(Context c) {
        try {
            return dpm(c).isAdminActive(admin(c));
        } catch (Exception e) {
            return false;
        }
    }

    public static Intent requestIntent(Context c) {
        Intent i = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        i.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin(c));
        i.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "فقط برای اینکه برنامه بتواند صفحه را خاموش (قفل) کند. به هیچ اطلاعات یا تنظیم دیگری دسترسی نمی‌گیرد.");
        return i;
    }

    /** صفحه را خاموش و گوشی را قفل می‌کند؛ اگر دسترسی فعال نباشد false برمی‌گرداند */
    public static boolean screenOff(Context c) {
        if (!isAdmin(c)) return false;
        try {
            ScreenWatchService.armed = false;   // حالت همیشه‌روشن بلافاصله دوباره باز نشود
            dpm(c).lockNow();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** صفحه را برای مدتی روشن می‌کند (نیازی به دسترسی ویژه ندارد) */
    @SuppressWarnings("deprecation")
    public static void screenOn(Context c, long ms) {
        try {
            PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
            pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP
                    | PowerManager.ON_AFTER_RELEASE, "jalaliclock:on").acquire(ms);
        } catch (Exception ignored) {
        }
    }

    public static void removeAdmin(Context c) {
        try {
            dpm(c).removeActiveAdmin(admin(c));
        } catch (Exception ignored) {
        }
    }
}
