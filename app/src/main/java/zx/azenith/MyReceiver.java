package zx.azenith;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class ZenithReceiver extends BroadcastReceiver {
    private static final String CH_PROFILE = "az_profile";
    private static final String CH_SYSTEM = "az_system";
    private static final int PROFILE_ID = 1001;

    // Actions
    public static final String ACTION_MANAGE = "zx.azenith.ACTION_MANAGE";
    private static final String ACTION_RESHOW = "zx.azenith.ACTION_RESHOW";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (ACTION_MANAGE.equals(action)) {
            // 1. Clear All
            if (intent.getBooleanExtra("clearall", false) || "true".equals(intent.getStringExtra("clearall"))) {
                manager.cancelAll();
            }

            // 2. Toast
            String toastMsg = intent.getStringExtra("toasttext");
            if (toastMsg != null) {
                Toast.makeText(context, toastMsg, Toast.LENGTH_LONG).show();
            }

            // 3. Notification
            String notifyMsg = intent.getStringExtra("notifytext");
            if (notifyMsg != null) {
                handleNotification(context, intent, manager);
            }
        } 
        else if (ACTION_RESHOW.equals(action)) {
            // Logika Auto-Reshow (Tiru MyReceiver lama)
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent reshow = new Intent(context, ZenithReceiver.class);
                reshow.setAction(ACTION_MANAGE);
                // Copy extras dari intent lama ke intent baru
                reshow.putExtras(intent.getExtras());
                context.sendBroadcast(reshow);
            }, 3000);
        }
    }

    private void handleNotification(Context context, Intent intent, NotificationManager manager) {
        String title = intent.getStringExtra("notifytitle");
        if (title == null) title = "AZenith";
        
        String message = intent.getStringExtra("notifytext");
        boolean chrono = intent.getBooleanExtra("chrono_bool", "true".equals(intent.getStringExtra("chrono")));
        
        long timeout = 0;
        String timeoutStr = intent.getStringExtra("timeout");
        if (timeoutStr != null) timeout = Long.parseLong(timeoutStr);

        boolean isProfile = title.toLowerCase().contains("profile") || 
                           title.toLowerCase().contains("mode") || 
                           title.toLowerCase().contains("initializing...");

        String channelId = isProfile ? CH_PROFILE : CH_SYSTEM;

        // Create Channel (Android O+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                channelId,
                isProfile ? "AZenith Profiles" : "AZenith System",
                NotificationManager.IMPORTANCE_LOW
            );
            manager.createNotificationChannel(channel);
        }

        Notification.Builder builder = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
            ? new Notification.Builder(context, channelId) 
            : new Notification.Builder(context);

        builder.setSmallIcon(context.getApplicationInfo().icon)
               .setContentTitle(title)
               .setContentText(message)
               .setUsesChronometer(chrono)
               .setOngoing(isProfile) // Profile notif biasanya persisten
               .setAutoCancel(!isProfile);

        // Jika Profile, pasang DeleteIntent untuk trigger ACTION_RESHOW
        if (isProfile) {
            Intent reshowIntent = new Intent(context, ZenithReceiver.class);
            reshowIntent.setAction(ACTION_RESHOW);
            reshowIntent.putExtra("notifytitle", title);
            reshowIntent.putExtra("notifytext", message);
            reshowIntent.putExtra("chrono_bool", chrono);
            
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;

            PendingIntent deletePI = PendingIntent.getBroadcast(context, title.hashCode(), reshowIntent, flags);
            builder.setDeleteIntent(deletePI);
        }

        if (timeout > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setTimeoutAfter(timeout);
        }

        manager.notify(isProfile ? PROFILE_ID : title.hashCode(), builder.build());
    }
}
