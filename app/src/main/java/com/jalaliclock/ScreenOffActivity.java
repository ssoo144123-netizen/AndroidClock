package com.jalaliclock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/** میان‌بر «خاموش کردن صفحه»: آیکنش را روی صفحه اصلی بگذارید و با یک لمس صفحه خاموش شود */
public class ScreenOffActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (ScreenCtl.isAdmin(this)) {
            ScreenCtl.screenOff(this);
            finish();
        } else {
            startActivityForResult(ScreenCtl.requestIntent(this), 1);
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (ScreenCtl.isAdmin(this)) ScreenCtl.screenOff(this);
        finish();
    }
}
