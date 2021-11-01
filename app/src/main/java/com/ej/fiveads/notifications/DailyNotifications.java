package com.ej.fiveads.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.ej.fiveads.R;
import com.ej.fiveads.activities.MainActivity;

public class DailyNotifications extends BroadcastReceiver {

    private static int MID = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "Daily Reminder",
                    "Daily Notifications",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("This notification will remind you to earn your tickets everyday!");
            channel.enableLights(true);
            channel.enableVibration(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                channel.setAllowBubbles(true);
            }
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setLightColor(Color.BLUE);
            notificationManager.createNotificationChannel(channel);
        }

        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(context, "Daily Reminder")
                .setSmallIcon(R.drawable.ic_baseline_play_arrow_24)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher_foreground))
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText("Don't forget to earn your tickets today! The more tickets you enter, the greater your chance to win!")
                .setStyle(new NotificationCompat
                        .BigTextStyle()
                        .setBigContentTitle("5 Ads")
                        .setSummaryText("Do Your Dailies!")
                        .bigText("Don't forget to earn your tickets today! The more tickets you enter, the greater your chance to win!"))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(alarmSound)
                .setColor(context.getColor(R.color.purple_200))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        notificationManager.notify(MID, mNotifyBuilder.build());
        MID++;
    }
}
