package com.brawl.modmenu;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import java.util.Random;

public class StatusActivity extends Activity {

    private Handler handler = new Handler(Looper.getMainLooper());
    private Random rnd = new Random();
    private int seconds = 0;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.status);

        TextView status = findViewById(R.id.statusTitle);
        TextView speed = findViewById(R.id.speedText);
        TextView blocked = findViewById(R.id.blockedText);
        TextView uptime = findViewById(R.id.uptimeText);

        status.setText("🛡 VPN АКТИВЕН");

        handler.post(new Runnable() {
            @Override
            public void run() {
                seconds++;
                int mins = seconds / 60;
                int secs = seconds % 60;

                speed.setText("Скорость: " + (80 + rnd.nextInt(40)) + " Мбит/с");
                blocked.setText("Заблокировано угроз: " + (5 + rnd.nextInt(30)));
                uptime.setText(String.format("Время работы: %02d:%02d", mins, secs));

                handler.postDelayed(this, 1000);
            }
        });
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
