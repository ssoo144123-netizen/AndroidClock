package com.jalaliclock;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** ضبط راهنمایی‌شده صدای خود کاربر برای ساعت سخنگو (کلمه به کلمه) */
public class VoiceRecordActivity extends Activity {
    private static final int SR_IN = 44100;
    private int idx = 0;
    private TextView progTv, wordTv, keyTv, statusTv;
    private Button recBtn;
    private CheckBox autoNext;
    private Spinner jump;
    private File dir;
    private final Handler h = new Handler();

    private AudioRecord rec;
    private Thread recThread;
    private volatile boolean recording;
    private ByteArrayOutputStream bos;

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        dir = PersianClips.voiceDir(this);
        dir.mkdirs();
        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(32));
        sv.addView(root);
        sv.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        setContentView(sv);

        add(root, "ضبط صدای خودم", 22, true);
        TextView n = add(root, "در جای ساکت، هر کلمه را جدا، واضح و با لحن عادی بگویید. بعد از ضبط، کلمه خودکار پخش می‌شود. لازم نیست همه را ضبط کنید؛ کلمه‌های ضبط‌نشده با «صدای ۱» پر می‌شوند.", 13, false);
        n.setTextColor(0xFF888888);

        jump = new Spinner(this);
        jump.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, VoiceWords.GROUP_NAMES));
        jump.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> a, View v, int pos, long id) {
                int start = VoiceWords.GROUP_START.get(pos);
                if (curGroup() != pos) show(start);
            }

            public void onNothingSelected(AdapterView<?> a) {
            }
        });
        root.addView(jump);
        progTv = add(root, "", 13, false);
        wordTv = add(root, "", 44, true);
        wordTv.setGravity(Gravity.CENTER);
        keyTv = add(root, "", 11, false);
        keyTv.setGravity(Gravity.CENTER);
        keyTv.setTextColor(0xFF888888);
        statusTv = add(root, "", 14, true);
        statusTv.setGravity(Gravity.CENTER);

        recBtn = new Button(this);
        recBtn.setTextSize(20);
        recBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (recording) stopRec();
                else startRec();
            }
        });
        root.addView(recBtn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(72)));

        LinearLayout row = new LinearLayout(this);
        row.addView(btn("قبلی", new View.OnClickListener() {
            public void onClick(View v) {
                show(idx - 1);
            }
        }), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(btn("پخش", new View.OnClickListener() {
            public void onClick(View v) {
                playCurrent();
            }
        }), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(btn("بعدی", new View.OnClickListener() {
            public void onClick(View v) {
                show(idx + 1);
            }
        }), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(row);

        autoNext = new CheckBox(this);
        autoNext.setText("بعد از ضبط، خودکار به کلمه بعدی برو");
        autoNext.setChecked(true);
        root.addView(autoNext);

        root.addView(btn("رفتن به اولین کلمه ضبط‌نشده", new View.OnClickListener() {
            public void onClick(View v) {
                for (int i = 0; i < VoiceWords.LIST.size(); i++)
                    if (!has(i)) {
                        show(i);
                        return;
                    }
                Toast.makeText(VoiceRecordActivity.this, "همه کلمه‌ها ضبط شده‌اند ✓", Toast.LENGTH_SHORT).show();
            }
        }));
        root.addView(btn("پخش جمله نمونه (ساعت فعلی) با صدای من", new View.OnClickListener() {
            public void onClick(View v) {
                PersianClips.clearCache();
                PersianClips.speak(VoiceRecordActivity.this, PersianClips.keys(Calendar.getInstance(), true, false),
                        AudioManager.STREAM_MUSIC, "m_", true);
            }
        }));
        root.addView(btn("استفاده از صدای من در برنامه", new View.OnClickListener() {
            public void onClick(View v) {
                Cfg c = Cfg.load(VoiceRecordActivity.this);
                c.voiceSet = 2;
                c.save(VoiceRecordActivity.this);
                Toast.makeText(VoiceRecordActivity.this, "فعال شد: از این پس ساعت سخنگو با صدای شما می‌خواند", Toast.LENGTH_LONG).show();
            }
        }));
        root.addView(btn("وارد کردن بسته صدا از پوشه JalaliClockVoice", new View.OnClickListener() {
            public void onClick(View v) {
                importFolder();
            }
        }));
        TextView n2 = add(root, "اگر صدای طبیعی را با ابزار دیگری (مثلاً یک موتور گفتار عصبی یا ضبط حرفه‌ای) ساخته‌اید: فایل‌های WAV با نام‌های کلید (مثل o5.wav و saat.wav) را در پوشه JalaliClockVoice در حافظه داخلی گوشی بگذارید و این دکمه را بزنید.", 12, false);
        n2.setTextColor(0xFF888888);
        root.addView(btn("حذف همه ضبط‌ها", new View.OnClickListener() {
            public void onClick(View v) {
                new AlertDialog.Builder(VoiceRecordActivity.this).setTitle("حذف همه ضبط‌ها؟")
                        .setPositiveButton("حذف", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) {
                                File[] fs = dir.listFiles();
                                if (fs != null) for (File f : fs) f.delete();
                                PersianClips.clearCache();
                                show(idx);
                            }
                        }).setNegativeButton("انصراف", null).show();
            }
        }));
        show(0);
    }

    private TextView add(LinearLayout root, String s, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, dp(8), 0, dp(4));
        root.addView(t);
        return t;
    }

    private Button btn(String s, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(s);
        b.setOnClickListener(l);
        return b;
    }

    private File file(int i) {
        return new File(dir, VoiceWords.LIST.get(i)[0] + ".wav");
    }

    private boolean has(int i) {
        return file(i).exists();
    }

    private int curGroup() {
        int g = 0;
        for (int i = 0; i < VoiceWords.GROUP_START.size(); i++) if (idx >= VoiceWords.GROUP_START.get(i)) g = i;
        return g;
    }

    private void show(int i) {
        if (recording) return;
        int n = VoiceWords.LIST.size();
        idx = Math.max(0, Math.min(n - 1, i));
        int done = 0;
        for (int k = 0; k < n; k++) if (has(k)) done++;
        String[] w = VoiceWords.LIST.get(idx);
        progTv.setText("کلمه " + Jalali.digits(String.valueOf(idx + 1), true) + " از " + Jalali.digits(String.valueOf(n), true)
                + " — ضبط‌شده: " + Jalali.digits(String.valueOf(done), true));
        wordTv.setText(w[1]);
        keyTv.setText(w[0]);
        boolean ok = has(idx);
        statusTv.setText(ok ? "ضبط شده ✓" : "هنوز ضبط نشده");
        statusTv.setTextColor(ok ? 0xFF2E7D32 : 0xFFB71C1C);
        recBtn.setText("● ضبط");
        int g = curGroup();
        if (jump.getSelectedItemPosition() != g) jump.setSelection(g);
    }

    private void playCurrent() {
        if (!has(idx)) {
            Toast.makeText(this, "این کلمه هنوز ضبط نشده", Toast.LENGTH_SHORT).show();
            return;
        }
        PersianClips.clearCache();
        List<String> k = new ArrayList<String>();
        k.add(VoiceWords.LIST.get(idx)[0]);
        PersianClips.speak(this, k, AudioManager.STREAM_MUSIC, "m_", true);
    }

    private void startRec() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 11);
            return;
        }
        int min = AudioRecord.getMinBufferSize(SR_IN, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        try {
            rec = new AudioRecord(MediaRecorder.AudioSource.MIC, SR_IN, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, Math.max(min, SR_IN));
        } catch (Exception e) {
            rec = null;
        }
        if (rec == null || rec.getState() != AudioRecord.STATE_INITIALIZED) {
            Toast.makeText(this, "میکروفون در دسترس نیست", Toast.LENGTH_LONG).show();
            return;
        }
        PersianClips.stopAll();
        bos = new ByteArrayOutputStream();
        recording = true;
        rec.startRecording();
        recThread = new Thread(new Runnable() {
            public void run() {
                byte[] buf = new byte[4096];
                while (recording) {
                    int n = rec.read(buf, 0, buf.length);
                    if (n > 0) synchronized (bos) {
                        bos.write(buf, 0, n);
                    }
                }
            }
        });
        recThread.start();
        recBtn.setText("■ توقف");
        statusTv.setText("در حال ضبط... کلمه را بگویید و «توقف» را بزنید");
        statusTv.setTextColor(0xFFB71C1C);
    }

    private void stopRec() {
        recording = false;
        try {
            recThread.join(800);
        } catch (InterruptedException ignored) {
        }
        try {
            rec.stop();
        } catch (Exception ignored) {
        }
        rec.release();
        rec = null;
        byte[] raw;
        synchronized (bos) {
            raw = bos.toByteArray();
        }
        short[] in = new short[raw.length / 2];
        for (int i = 0; i < in.length; i++) in[i] = (short) ((raw[2 * i + 1] << 8) | (raw[2 * i] & 0xff));
        short[] out = VoiceProc.process(in, SR_IN);
        if (out.length < VoiceProc.SR / 20) {
            Toast.makeText(this, "صدایی ضبط نشد؛ نزدیک‌تر و بلندتر بگویید", Toast.LENGTH_LONG).show();
            show(idx);
            return;
        }
        try {
            VoiceProc.save(file(idx), out);
        } catch (Exception e) {
            Toast.makeText(this, "ذخیره نشد", Toast.LENGTH_SHORT).show();
            show(idx);
            return;
        }
        PersianClips.clearCache();
        show(idx);
        playCurrent();
        if (autoNext.isChecked() && idx < VoiceWords.LIST.size() - 1) {
            h.postDelayed(new Runnable() {
                public void run() {
                    if (!recording) show(idx + 1);
                }
            }, 1400);
        }
    }

    private void importFolder() {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 12);
            return;
        }
        File src = new File(Environment.getExternalStorageDirectory(), "JalaliClockVoice");
        if (!src.isDirectory()) {
            Toast.makeText(this, "پوشه پیدا نشد: " + src.getPath(), Toast.LENGTH_LONG).show();
            return;
        }
        int ok = 0, bad = 0;
        for (String[] w : VoiceWords.LIST) {
            File f = new File(src, w[0] + ".wav");
            if (!f.exists()) continue;
            try {
                FileInputStream in = new FileInputStream(f);
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
                in.close();
                int[] sr = new int[1];
                short[] pcm = VoiceProc.readWavMono(bo.toByteArray(), sr);
                short[] out = pcm == null ? null : VoiceProc.process(pcm, sr[0]);
                if (out == null || out.length < VoiceProc.SR / 20) {
                    bad++;
                    continue;
                }
                VoiceProc.save(new File(dir, w[0] + ".wav"), out);
                ok++;
            } catch (Exception e) {
                bad++;
            }
        }
        PersianClips.clearCache();
        show(idx);
        Toast.makeText(this, "وارد شد: " + Jalali.digits(String.valueOf(ok), true) + " کلمه"
                + (bad > 0 ? " — ناموفق: " + Jalali.digits(String.valueOf(bad), true) + " (فقط WAV ۱۶بیتی پشتیبانی می‌شود)" : ""), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] res) {
        if (req == 12) {
            if (res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) importFolder();
            else Toast.makeText(this, "بدون اجازه خواندن حافظه نمی‌شود وارد کرد", Toast.LENGTH_LONG).show();
            return;
        }
        if (res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED)
            Toast.makeText(this, "اجازه داده شد؛ دوباره «ضبط» را بزنید", Toast.LENGTH_SHORT).show();
        else Toast.makeText(this, "بدون اجازه میکروفون نمی‌شود ضبط کرد", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        recording = false;
        h.removeCallbacksAndMessages(null);
        try {
            if (rec != null) {
                rec.stop();
                rec.release();
            }
        } catch (Exception ignored) {
        }
        PersianClips.stopAll();
        super.onDestroy();
    }
}
