package com.jalaliclock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import java.util.Calendar;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {
    private Cfg c;
    private ClockView pv;
    private LinearLayout root;
    private Speaker testSpeaker;
    private TickPlayer testTick;
    private TextView ttsStatus, adminStatus;
    private final Handler hd = new Handler();

    private interface IntSet { void set(int v); }
    private interface BoolSet { void set(boolean v); }

    private static final String[] COLOR_NAMES = {"سفید", "زرد طلایی", "سبز نئونی", "قرمز", "آبی آسمانی", "بنفش", "نارنجی", "صورتی", "خاکستری", "مشکی", "کد دلخواه..."};
    private static final int[] COLOR_VALUES = {0xFFFFFFFF, 0xFFFFD700, 0xFF39FF14, 0xFFFF3B3B, 0xFF00D2FF, 0xFFC084FC, 0xFFFF9800, 0xFFFF69B4, 0xFF9E9E9E, 0xFF000000};

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        c = Cfg.load(this);
        testSpeaker = new Speaker(this, new Runnable() {
            public void run() {
                updateTtsStatus();
            }
        });
        ScrollView sv = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(32));
        sv.addView(root);
        sv.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        setContentView(sv);
        build();
    }

    private void save() {
        c.save(this);
        pv.setCfg(c);
    }

    private TextView label(String s, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        t.setPadding(0, dp(10), 0, dp(4));
        root.addView(t);
        return t;
    }

    private Button button(String s, View.OnClickListener l) {
        Button bt = new Button(this);
        bt.setText(s);
        bt.setOnClickListener(l);
        root.addView(bt, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return bt;
    }

    private void check(String s, boolean val, final BoolSet st) {
        CheckBox cb = new CheckBox(this);
        cb.setText(s);
        cb.setChecked(val);
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton v, boolean on) {
                st.set(on);
                save();
            }
        });
        root.addView(cb);
    }

    private void spin(String title, String[] items, int sel, final IntSet st, final boolean recreate) {
        label(title, 14, false);
        Spinner sp = new Spinner(this);
        sp.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, items));
        sp.setSelection(sel);
        final int[] last = {sel};
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> a, View v, int pos, long id) {
                if (pos == last[0]) return;
                last[0] = pos;
                st.set(pos);
                save();
                if (recreate) recreate();
            }

            public void onNothingSelected(AdapterView<?> a) {
            }
        });
        root.addView(sp);
    }

    private void seek(String title, int max, int val, final IntSet st) {
        label(title, 14, false);
        SeekBar sb = new SeekBar(this);
        sb.setMax(max);
        sb.setProgress(val);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean user) {
                if (user) {
                    st.set(p);
                    save();
                }
            }

            public void onStartTrackingTouch(SeekBar s) {
            }

            public void onStopTrackingTouch(SeekBar s) {
            }
        });
        root.addView(sb);
    }

    private void seekV(String title, final int min, int max, int val, final String unit, final IntSet st) {
        final TextView t = label(title + ": " + Jalali.digits(String.valueOf(val), true) + " " + unit, 14, false);
        final String base = title;
        SeekBar sb = new SeekBar(this);
        sb.setMax(max - min);
        sb.setProgress(val - min);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean user) {
                t.setText(base + ": " + Jalali.digits(String.valueOf(p + min), true) + " " + unit);
                if (user) {
                    st.set(p + min);
                    save();
                }
            }

            public void onStartTrackingTouch(SeekBar s) {
            }

            public void onStopTrackingTouch(SeekBar s) {
            }
        });
        root.addView(sb);
    }

    private void colorBtn(String s, final int which) {
        button(s, new View.OnClickListener() {
            public void onClick(View v) {
                new AlertDialog.Builder(SettingsActivity.this).setTitle("انتخاب رنگ")
                        .setItems(COLOR_NAMES, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int i) {
                                if (i < COLOR_VALUES.length) setColor(which, COLOR_VALUES[i]);
                                else customColor(which);
                            }
                        }).show();
            }
        });
    }

    private void setColor(int which, int col) {
        if (which == 0) c.fg = col;
        else if (which == 1) c.bg = col;
        else c.accent = col;
        save();
    }

    private void customColor(final int which) {
        final EditText et = new EditText(this);
        et.setHint("#RRGGBB");
        new AlertDialog.Builder(this).setTitle("کد رنگ").setView(et)
                .setPositiveButton("تأیید", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int i) {
                        try {
                            setColor(which, Color.parseColor(et.getText().toString().trim()) | 0xFF000000);
                        } catch (Exception e) {
                            Toast.makeText(SettingsActivity.this, "کد رنگ نامعتبر است", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).setNegativeButton("انصراف", null).show();
    }

    private void build() {
        label("ساعت شمسی برای صفحه خاموش", 22, true);
        pv = new ClockView(this, c, true);
        root.addView(pv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(260)));

        button("مدیریت آلارم‌ها و تایمر", new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(SettingsActivity.this, AlarmsActivity.class));
            }
        });
        label("روش نمایش وقتی صفحه خاموش است", 16, true);
        TextView n1 = label("۱) محافظ صفحه (Daydream): هنگام شارژ، ساعت روی صفحه دیده می‌شود. در تنظیمات باز‌شده «ساعت شمسی» را انتخاب و زمان شروع را «هنگام شارژ» بگذارید.", 13, false);
        n1.setTextColor(0xFF888888);
        button("باز کردن تنظیمات محافظ صفحه", new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Settings.ACTION_DREAM_SETTINGS));
                } catch (Exception e) {
                    Toast.makeText(SettingsActivity.this, "در تنظیمات ← نمایش ← محافظ صفحه (Daydream) را باز کنید", Toast.LENGTH_LONG).show();
                }
            }
        });
        TextView n2 = label("۲) حالت همیشه‌روشن: با زدن دکمه خاموش، صفحه با نور کم و ساعت روشن می‌ماند. با لمس صفحه بسته می‌شود و با نگه‌داشتن انگشت روی آن، خود صفحه هم خاموش می‌شود (پس از فعال‌سازی «کنترل صفحه» پایین‌تر). باتری بیشتری مصرف می‌کند.", 13, false);
        n2.setTextColor(0xFF888888);
        check("نمایش خودکار ساعت وقتی صفحه خاموش می‌شود", c.watch, new BoolSet() {
            public void set(boolean on) {
                c.watch = on;
                Intent i = new Intent(SettingsActivity.this, ScreenWatchService.class);
                if (on) startService(i);
                else stopService(i);
            }
        });
        button("شروع حالت همیشه‌روشن همین حالا", new View.OnClickListener() {
            public void onClick(View v) {
                ScreenWatchService.armed = true;
                startActivity(new Intent(SettingsActivity.this, AlwaysOnActivity.class));
            }
        });
        final int[] aodVals = {0, 30, 60, 180, 480};
        int sel = 0;
        for (int i = 0; i < aodVals.length; i++) if (aodVals[i] == c.aodMin) sel = i;
        spin("خاموش‌شدن خودکار صفحه در حالت همیشه‌روشن بعد از", new String[]{"بدون محدودیت", "۳۰ دقیقه", "۱ ساعت", "۳ ساعت", "۸ ساعت"}, sel,
                new IntSet() {
                    public void set(int v) {
                        c.aodMin = aodVals[v];
                    }
                }, false);

        label("کنترل صفحه (خاموش و روشن کردن)", 16, true);
        TextView n5 = label("اندروید اجازه خاموش‌کردن صفحه را فقط از راه «مدیر دستگاه» می‌دهد؛ برنامه فقط دسترسی «قفل صفحه» را می‌گیرد و به چیز دیگری دسترسی ندارد. روشن‌کردن صفحه دسترسی لازم ندارد. یک آیکن جدا هم به نام «خاموش کردن صفحه» ساخته شده که می‌توانید روی صفحه اصلی بگذارید.", 13, false);
        n5.setTextColor(0xFF888888);
        adminStatus = label("", 13, true);
        button("فعال‌سازی دسترسی خاموش کردن صفحه", new View.OnClickListener() {
            public void onClick(View v) {
                startActivityForResult(ScreenCtl.requestIntent(SettingsActivity.this), 7);
            }
        });
        button("خاموش کردن صفحه (۳ ثانیه دیگر)", new View.OnClickListener() {
            public void onClick(View v) {
                if (!ScreenCtl.isAdmin(SettingsActivity.this)) {
                    Toast.makeText(SettingsActivity.this, "اول دسترسی را فعال کنید", Toast.LENGTH_SHORT).show();
                    return;
                }
                hd.postDelayed(new Runnable() {
                    public void run() {
                        ScreenCtl.screenOff(SettingsActivity.this);
                    }
                }, 3000);
            }
        });
        button("آزمایش: خاموش شدن و ۵ ثانیه بعد روشن شدن", new View.OnClickListener() {
            public void onClick(View v) {
                if (!ScreenCtl.isAdmin(SettingsActivity.this)) {
                    Toast.makeText(SettingsActivity.this, "اول دسترسی را فعال کنید", Toast.LENGTH_SHORT).show();
                    return;
                }
                hd.postDelayed(new Runnable() {
                    public void run() {
                        ScreenCtl.screenOff(SettingsActivity.this);
                        hd.postDelayed(new Runnable() {
                            public void run() {
                                ScreenCtl.screenOn(SettingsActivity.this, 10000);
                            }
                        }, 5000);
                    }
                }, 2000);
            }
        });
        button("روشن کردن صفحه (۱۰ ثانیه)", new View.OnClickListener() {
            public void onClick(View v) {
                ScreenCtl.screenOn(SettingsActivity.this, 10000);
            }
        });
        TextView n6 = label("اگر آیکن «خاموش کردن صفحه» را در لیست برنامه‌ها نمی‌بینید (بعضی لانچرها آیکن دوم را نشان نمی‌دهند)، از یکی از این سه راه استفاده کنید:", 13, false);
        n6.setTextColor(0xFF888888);
        button("افزودن میان‌بر «خاموش کردن صفحه» به صفحه اصلی", new View.OnClickListener() {
            public void onClick(View v) {
                Intent target = new Intent(SettingsActivity.this, ScreenOffActivity.class);
                target.setAction(Intent.ACTION_MAIN);
                Intent i = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
                i.putExtra(Intent.EXTRA_SHORTCUT_INTENT, target);
                i.putExtra(Intent.EXTRA_SHORTCUT_NAME, "خاموش کردن صفحه");
                i.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(SettingsActivity.this, R.mipmap.ic_screenoff));
                sendBroadcast(i);
                Toast.makeText(SettingsActivity.this, "میان‌بر به صفحه اصلی اضافه شد (اگر دیده نشد، از روش‌های بعدی استفاده کنید)", Toast.LENGTH_LONG).show();
            }
        });
        TextView n7 = label("ویجت: روی صفحه اصلی انگشت را نگه دارید ← «ویجت‌ها» ← «خاموش کردن صفحه» را بکشید و رها کنید.", 13, false);
        n7.setTextColor(0xFF888888);
        check("دکمه «خاموش کردن صفحه» در نوار اعلان (همیشه در دسترس)", c.screenNotif, new BoolSet() {
            public void set(boolean v) {
                c.screenNotif = v;
                NotifHelper.update(SettingsActivity.this, v);
            }
        });
        button("غیرفعال کردن دسترسی (لازم برای حذف برنامه)", new View.OnClickListener() {
            public void onClick(View v) {
                ScreenCtl.removeAdmin(SettingsActivity.this);
                updateAdmin();
            }
        });

        label("ظاهر", 16, true);
        spin("نوع ساعت", new String[]{"دیجیتال", "آنالوگ", "آنالوگ + دیجیتال داخل صفحه"}, c.style, new IntSet() {
            public void set(int v) {
                c.style = v;
            }
        }, false);
        spin("طیف رنگی (برای دیجیتال و آنالوگ)", Cfg.PALETTE_NAMES, c.theme, new IntSet() {
            public void set(int v) {
                c.applyTheme(v);
            }
        }, false);
        spin("تصویر صفحه ساعت (آنالوگ)", new String[]{"بدون تصویر (فقط طیف رنگی)", "تصویر ۱", "تصویر ۲", "تصویر ۳", "تصویر ۴", "تصویر ۵"}, c.face, new IntSet() {
            public void set(int v) {
                c.face = v;
            }
        }, false);
        check("کمان رنگی دور ساعت آنالوگ (ثانیه و دقیقه)", c.arc, new BoolSet() {
            public void set(boolean v) {
                c.arc = v;
            }
        });
        check("کمان رنگین‌کمانی", c.rainbow, new BoolSet() {
            public void set(boolean v) {
                c.rainbow = v;
            }
        });
        check("نمایش ثانیه", c.seconds, new BoolSet() {
            public void set(boolean v) {
                c.seconds = v;
            }
        });
        check("نمایش تاریخ شمسی", c.date, new BoolSet() {
            public void set(boolean v) {
                c.date = v;
            }
        });
        check("نمایش نام روز هفته", c.weekday, new BoolSet() {
            public void set(boolean v) {
                c.weekday = v;
            }
        });
        check("ساعت ۲۴ ساعته", c.h24, new BoolSet() {
            public void set(boolean v) {
                c.h24 = v;
            }
        });
        check("اعداد فارسی", c.fa, new BoolSet() {
            public void set(boolean v) {
                c.fa = v;
            }
        });
        check("نمایش زمان آلارم بعدی", c.alarm, new BoolSet() {
            public void set(boolean v) {
                c.alarm = v;
            }
        });
        check("نمایش درصد باتری", c.battery, new BoolSet() {
            public void set(boolean v) {
                c.battery = v;
            }
        });
        check("جابه‌جایی آهسته ساعت (جلوگیری از سوختگی صفحه AMOLED)", c.burn, new BoolSet() {
            public void set(boolean v) {
                c.burn = v;
            }
        });
        seek("اندازه ساعت", 50, c.size - 50, new IntSet() {
            public void set(int v) {
                c.size = v + 50;
            }
        });
        seek("روشنایی صفحه در حالت ساعت", 100, c.dim, new IntSet() {
            public void set(int v) {
                c.dim = Math.max(1, v);
            }
        });
        seekV("دامنه حرکت ساعت روی صفحه (درصد، ۰ = ثابت)", 0, 30, c.burnPct, "٪", new IntSet() {
            public void set(int v) {
                c.burnPct = v;
            }
        });
        seekV("زمان یک چرخه حرکت (ثانیه)", 10, 600, c.burnSec, "ثانیه", new IntSet() {
            public void set(int v) {
                c.burnSec = v;
            }
        });
        TextView n4 = label("حرکت ساعت در پیش‌نمایش دیده نمی‌شود؛ فقط در محافظ صفحه و حالت همیشه‌روشن اجرا می‌شود. دامنه به فضای خالی صفحه محدود است؛ برای حرکت بیشتر، اندازه ساعت را کمتر کنید.", 12, false);
        n4.setTextColor(0xFF888888);
        colorBtn("رنگ نوشته...", 0);
        colorBtn("رنگ پس‌زمینه...", 1);
        colorBtn("رنگ کمان و عقربه ثانیه...", 2);
        check("تغییر خودکار و تصادفی رنگ‌ها", c.colorRandom, new BoolSet() {
            public void set(boolean v) {
                c.colorRandom = v;
            }
        });
        numberField("هر چند ثانیه رنگ‌ها عوض شوند", c.colorSec, new IntSet() {
            public void set(int v) {
                c.colorSec = v;
            }
        });
        spin("نوع رنگ تصادفی", new String[]{"از بین طیف‌های رنگی آماده (تیره)", "رنگ‌های کاملاً تصادفی"}, c.colorMode, new IntSet() {
            public void set(int v) {
                c.colorMode = v;
            }
        }, false);

        label("صدا و گفتار", 16, true);
        TextView n3 = label("ساعت سخنگو و تیک‌تاک وقتی کار می‌کنند که ساعت روی صفحه باشد (محافظ صفحه یا حالت همیشه‌روشن).", 13, false);
        n3.setTextColor(0xFF888888);
        check("اعلام خودکار زمان با صدا (گفتار یا زنگ)", c.speak, new BoolSet() {
            public void set(boolean v) {
                c.speak = v;
            }
        });
        numberField("فاصله اعلام زمان (ثانیه)", c.speakSec, new IntSet() {
            public void set(int v) {
                c.speakSec = v;
            }
        });
        spin("نوع اعلام ساعت (اگر صدای گفتار دلچسب نیست، زنگ ملایم یا ناقوس را انتخاب کنید)", Announcer.NAMES, c.announce, new IntSet() {
            public void set(int v) {
                c.announce = v;
            }
        }, false);
        check("یک «دینگ» کوتاه قبل از اعلام ساعت (و قبل از گفتار آلارم) پخش شود", c.dingBefore, new BoolSet() {
            public void set(boolean v) {
                c.dingBefore = v;
            }
        });
        check("همراه ساعت، تاریخ شمسی هم گفته شود", c.speakDate, new BoolSet() {
            public void set(boolean v) {
                c.speakDate = v;
            }
        });
        spin("صدای فارسی داخلی برنامه", new String[]{"صدای ۱ (MBROLA، طبیعی‌تر)", "صدای ۲ (eSpeak، رباتی‌تر)", "صدای خودم (ضبط‌شده)"}, c.voiceSet, new IntSet() {
            public void set(int v) {
                c.voiceSet = v;
            }
        }, false);
        button("ضبط صدای خودم (کلمه به کلمه)", new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(SettingsActivity.this, VoiceRecordActivity.class));
            }
        });
        spin("صدای گفتار", new String[]{"خودکار (موتور فارسی گوشی، وگرنه صدای فارسی داخلی برنامه)", "همیشه صدای فارسی داخلی برنامه", "تلفظ لاتین با موتور گفتار گوشی"},
                c.speakMode, new IntSet() {
                    public void set(int v) {
                        c.speakMode = v;
                    }
                }, false);
        ttsStatus = label("در حال بررسی موتور گفتار...", 13, false);
        ttsStatus.setTextColor(0xFF888888);
        button("آزمایش اعلام زمان", new View.OnClickListener() {
            public void onClick(View v) {
                Announcer.announce(SettingsActivity.this, c, testSpeaker, Calendar.getInstance());
            }
        });
        button("تنظیمات موتور گفتار (نصب صدای فارسی)", new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    startActivity(new Intent("com.android.settings.TTS_SETTINGS"));
                } catch (Exception e) {
                    try {
                        startActivity(new Intent(Settings.ACTION_SETTINGS));
                    } catch (Exception ignored) {
                    }
                }
            }
        });
        seekV("ساعت سخنگو و تیک‌تاک فعال باشند: از ساعت", 0, 24, c.activeFrom, "", new IntSet() {
            public void set(int v) {
                c.activeFrom = v;
            }
        });
        seekV("تا ساعت (۰ تا ۲۴ = همیشه فعال؛ بازه شبانه مثل ۲۲ تا ۶ هم کار می‌کند)", 0, 24, c.activeTo, "", new IntSet() {
            public void set(int v) {
                c.activeTo = v;
            }
        });
        check("صدای تیک‌تاک ساعت", c.tick, new BoolSet() {
            public void set(boolean v) {
                c.tick = v;
            }
        });
        seek("بلندی تیک‌تاک و زنگ", 99, c.tickVol - 1, new IntSet() {
            public void set(int v) {
                c.tickVol = v + 1;
            }
        });
        button("آزمایش تیک‌تاک", new View.OnClickListener() {
            public void onClick(View v) {
                if (testTick == null) testTick = new TickPlayer();
                testTick.play(false, c.tickVol);
            }
        });
    }

    /** فیلد عددی + دکمه‌های آماده (۳۰، ۶۰، ۳۰۰ ... ثانیه) */
    private void numberField(String title, int val, final IntSet st) {
        label(title, 14, false);
        final EditText et = new EditText(this);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setText(String.valueOf(val));
        et.setSelectAllOnFocus(true);
        et.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c2) {
            }

            public void onTextChanged(CharSequence s, int a, int b, int c2) {
            }

            public void afterTextChanged(Editable e) {
                try {
                    int v = Integer.parseInt(e.toString().trim());
                    st.set(Math.max(3, Math.min(86400, v)));
                    save();
                } catch (Exception ignored) {
                }
            }
        });
        root.addView(et);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        int[] presets = {30, 60, 300, 900, 1800, 3600};
        for (int i = 0; i < presets.length; i++) {
            final int pv2 = presets[i];
            Button bt = new Button(this);
            bt.setText(String.valueOf(pv2));
            bt.setTextSize(12);
            bt.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    et.setText(String.valueOf(pv2));
                }
            });
            row.addView(bt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        root.addView(row);
    }

    private void updateTtsStatus() {
        runOnUiThread(new Runnable() {
            public void run() {
                if (ttsStatus == null || testSpeaker == null) return;
                if (testSpeaker.hasPersian()) {
                    ttsStatus.setText("موتور گفتار گوشی فارسی دارد (" + testSpeaker.engine() + ")؛ در حالت «خودکار» از آن استفاده می‌شود.");
                } else {
                    ttsStatus.setText("موتور گفتار گوشی فارسی ندارد؛ در حالت «خودکار» از صدای فارسی داخلی برنامه استفاده می‌شود (نیازی به نصب چیزی نیست).");
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (testSpeaker != null) testSpeaker.shutdown();
        if (testTick != null) testTick.release();
        super.onDestroy();
    }

    private void updateAdmin() {
        if (adminStatus != null)
            adminStatus.setText(ScreenCtl.isAdmin(this) ? "وضعیت: دسترسی خاموش کردن صفحه فعال است ✓" : "وضعیت: دسترسی فعال نیست");
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        updateAdmin();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAdmin();
        pv.start();
    }

    @Override
    protected void onPause() {
        pv.stop();
        super.onPause();
    }
}
