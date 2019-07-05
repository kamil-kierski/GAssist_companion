package com.cybernetic87.GAssist;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.Objects;

public class Service extends android.app.Service {
    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";
//    private SAAgentV2.RequestAgentCallback mAgentCallback = new SAAgentV2.RequestAgentCallback() {
//        @Override
//        public void onAgentAvailable(SAAgent agent) {
//            mProviderService = (ProviderService) agent;
//        }
//
//
//        @Override
//        public void onError(int errorCode, String message) {
//            Log.e("SERVICE", "Agent initialization error: " + errorCode + ". ErrorMsg: " + message);
//        }
//    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();

            if (ACTION_START_FOREGROUND_SERVICE.equals(action)) {
                startForegroundService();
                Toast.makeText(getApplicationContext(), "GAssist.Net service started", Toast.LENGTH_LONG).show();


                Intent notificationIntent = new Intent(this, MainActivity.class);
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

                NotificationManager notificationManager =
                        (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationChannel channel = new NotificationChannel("gassist", "GAssist", NotificationManager.IMPORTANCE_LOW);
                channel.setDescription("Channel description");

                Objects.requireNonNull(notificationManager).createNotificationChannel(channel);

                Notification notification = new NotificationCompat.Builder(this, channel.getId())
                        .setContentTitle("title")
                        .setContentText("content")
                        //.setSmallIcon(R.mipmap.pro_icon)
                        .setPriority(5)
                        .setContentIntent(pendingIntent)
                        .build();

                startForeground(1, notification);
            }
        }
        return START_STICKY;
    }

    private void startForegroundService() {

        //SAAgentV2.requestAgent(this, ProviderService.class.getName(), mAgentCallback);
    }
}
