package com.jalaliclock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** مدیریت آلارم‌ها، تایمر (ثانیه‌ای) و تنظیمات زنگ */
public class AlarmsActivity extends Activity {
    private Cfg c;
    private LinearLayout root, listBox;
    private TextView timerInfo;
    private List<AlarmItem> items;

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        c = Cfg.load(this);
        ScrollView sv = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(32));
        sv.addView(root);
        sv.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        setContentView(sv);

        label("آلارم‌ها و تایمر", 22, true);
        listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(listBox);
        Button add = new Button(this);
        add.setText("افزودن آلارم");
        add.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AlarmItem a = new AlarmItem();
                a.id = AlarmStore.newId(AlarmsActivity.this);
                Calendar n = Calendar.getInstance();
                a.h = n.get(Calendar.HOUR_OF_DAY);
                a.m = n.get(Calendar.MINUTE);
                edit(a, true);
            }
        });
        root.addView(add);

        label("تایمر سریع (ثانیه)", 16, true);
        final EditText tsec = new EditText(this);
        tsec.setInputType(InputType.TYPE_CLASS_NUMBER);
        tsec.setText("60");
        root.addView(tsec);
        LinearLayout pr = new LinearLayout(this);
        int[] presets = {10, 30, 60, 300, 600, 1800};
        for (int i = 0; i < presets.length; i++) {
            final int pv = presets[i];
            Button pb = new Button(this);
            pb.setText(String.valueOf(pv));
            pb.setTextSize(12);
            pb.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    tsec.setText(String.valueOf(pv));
                }
            });
            pr.addView(pb, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        root.addView(pr);
        Button st = new Button(this);
        st.setText("شروع تایمر");
        st.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    int s = Math.max(1, Math.min(86400, Integer.parseInt(tsec.getText().toString().trim())));
                    AlarmStore.startTimer(AlarmsActivity.this, s, "تایمر " + Jalali.digits(String.valueOf(s), true) + " ثانیه‌ای");
                    refresh();
                } catch (Exception e) {
                    Toast.makeText(AlarmsActivity.this, "یک عدد وارد کنید", Toast.LENGTH_SHORT).show();
                }
            }
        });
        root.addView(st);
        timerInfo = label("", 13, false);
        Button cancel = new Button(this);
        cancel.setText("لغو تایمر");
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AlarmStore.cancelTimer(AlarmsActivity.this);
                refresh();
            }
        });
        root.addView(cancel);

        label("تنظیمات زنگ (بدون نیاز به تأیید)", 16, true);
        TextView n = label("آلارم خودش هر چند ثانیه یک‌بار صدا را تکرار می‌کند و بعد از مدت تعیین‌شده خودکار متوقف می‌شود.", 13, false);
        n.setTextColor(0xFF888888);
        numField("فاصله تکرار خودکار صدا (ثانیه)", c.alarmRepeatSec, 1, 600, new IntSet() {
            public void set(int v) {
                c.alarmRepeatSec = v;
            }
        });
        numField("مدت زنگ خوردن تا توقف خودکار (ثانیه)", c.alarmWindowSec, 5, 3600, new IntSet() {
            public void set(int v) {
                c.alarmWindowSec = v;
            }
        });
        CheckBox cb = new CheckBox(this);
        cb.setText("هنگام آلارم، زمان با صدا گفته شود");
        cb.setChecked(c.speakAlarm);
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton v, boolean on) {
                c.speakAlarm = on;
                c.save(AlarmsActivity.this);
            }
        });
        root.addView(cb);
        refresh();
    }

    private interface IntSet {
        void set(int v);
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

    private void numField(String title, int val, final int min, final int max, final IntSet st) {
        label(title, 14, false);
        EditText et = new EditText(this);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setText(String.valueOf(val));
        et.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c2) {
            }

            public void onTextChanged(CharSequence s, int a, int b, int c2) {
            }

            public void afterTextChanged(Editable e) {
                try {
                    st.set(Math.max(min, Math.min(max, Integer.parseInt(e.toString().trim()))));
                    c.save(AlarmsActivity.this);
                } catch (Exception ignored) {
                }
            }
        });
        root.addView(et);
    }

    private void refresh() {
        items = AlarmStore.load(this);
        listBox.removeAllViews();
        if (items.isEmpty()) {
            TextView e = new TextView(this);
            e.setText("هنوز آلارمی نساخته‌اید.");
            e.setPadding(0, dp(8), 0, dp(8));
            listBox.addView(e);
        }
        for (int i = 0; i < items.size(); i++) {
            final AlarmItem a = items.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            CheckBox on = new CheckBox(this);
            on.setChecked(a.on);
            on.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton v, boolean b) {
                    a.on = b;
                    AlarmStore.save(AlarmsActivity.this, items);
                    AlarmStore.schedule(AlarmsActivity.this);
                }
            });
            row.addView(on);
            TextView t = new TextView(this);
            t.setText(a.summary());
            t.setTextSize(15);
            t.setPadding(dp(6), dp(10), dp(6), dp(10));
            t.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    edit(a, false);
                }
            });
            row.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            Button del = new Button(this);
            del.setText("حذف");
            del.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    items.remove(a);
                    AlarmStore.save(AlarmsActivity.this, items);
                    AlarmStore.schedule(AlarmsActivity.this);
                    refresh();
                }
            });
            row.addView(del);
            listBox.addView(row);
        }
        long ta = AlarmStore.timerAt(this);
        timerInfo.setText(ta > 0
                ? "تایمر فعال: " + Jalali.digits(String.valueOf((ta - System.currentTimeMillis()) / 1000), true) + " ثانیه دیگر"
                : "تایمری فعال نیست.");
    }

    private EditText num(int val, int w) {
        EditText e = new EditText(this);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setText(String.valueOf(val));
        e.setSelectAllOnFocus(true);
        e.setLayoutParams(new LinearLayout.LayoutParams(dp(w), ViewGroup.LayoutParams.WRAP_CONTENT));
        return e;
    }

    private int parse(EditText e, int min, int max) {
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(e.getText().toString().trim())));
        } catch (Exception ex) {
            return min;
        }
    }

    private void edit(final AlarmItem a, final boolean isNew) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(16), dp(8), dp(16), 0);
        l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        TextView t0 = new TextView(this);
        t0.setText("زمان آلارم (ساعت : دقیقه : ثانیه)");
        l.addView(t0);
        LinearLayout r = new LinearLayout(this);
        r.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        final EditText eh = num(a.h, 64), em = num(a.m, 64), es = num(a.s, 64);
        r.addView(eh);
        r.addView(new TextView(this) {{
            setText(" : ");
        }});
        r.addView(em);
        r.addView(new TextView(this) {{
            setText(" : ");
        }});
        r.addView(es);
        l.addView(r);
        final EditText lab = new EditText(this);
        lab.setHint("برچسب (اختیاری)");
        lab.setText(a.label);
        l.addView(lab);
        TextView t1 = new TextView(this);
        t1.setText("روزها (اگر هیچ‌کدام انتخاب نشود، فقط یک‌بار)");
        l.addView(t1);
        final List<CheckBox> boxes = new ArrayList<CheckBox>();
        final int[] order = {6, 0, 1, 2, 3, 4, 5};
        LinearLayout r1 = new LinearLayout(this), r2 = new LinearLayout(this);
        for (int i = 0; i < order.length; i++) {
            CheckBox cb = new CheckBox(this);
            cb.setText(Jalali.DAYS[order[i]]);
            cb.setTextSize(12);
            cb.setChecked((a.days & (1 << order[i])) != 0);
            boxes.add(cb);
            (i < 4 ? r1 : r2).addView(cb);
        }
        l.addView(r1);
        l.addView(r2);
        new AlertDialog.Builder(this).setTitle(isNew ? "آلارم جدید" : "ویرایش آلارم").setView(l)
                .setPositiveButton("ذخیره", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        a.h = parse(eh, 0, 23);
                        a.m = parse(em, 0, 59);
                        a.s = parse(es, 0, 59);
                        a.label = lab.getText().toString().trim();
                        int mask = 0;
                        for (int i = 0; i < order.length; i++) if (boxes.get(i).isChecked()) mask |= 1 << order[i];
                        a.days = mask;
                        a.on = true;
                        List<AlarmItem> all = AlarmStore.load(AlarmsActivity.this);
                        boolean found = false;
                        for (int i = 0; i < all.size(); i++)
                            if (all.get(i).id == a.id) {
                                all.set(i, a);
                                found = true;
                            }
                        if (!found) all.add(a);
                        AlarmStore.save(AlarmsActivity.this, all);
                        AlarmStore.schedule(AlarmsActivity.this);
                        refresh();
                    }
                }).setNegativeButton("انصراف", null).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }
}
