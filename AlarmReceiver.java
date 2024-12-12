package com.example.todo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String taskId = intent.getStringExtra("taskId");
        String taskTitle = intent.getStringExtra("taskTitle");

        // Check and handle notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                // Log or handle missing notification permission
                return;
            }
        }

        // Create a notification channel for Android 8.0 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(context);
        }

        // Send the notification
        try {
            sendNotification(context, taskId, taskTitle);
        } catch (SecurityException e) {
            e.printStackTrace();
            // Optionally, log or notify about the failure
        }

        // Mark the task as non-editable in Firebase
        markTaskAsNonEditable(taskId);
    }

    private void createNotificationChannel(Context context) {
        String channelId = "TaskChannel";
        CharSequence name = "Task Notifications";
        String description = "Notifications for task reminders";
        int importance = NotificationManager.IMPORTANCE_HIGH;

        NotificationChannel channel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel(channelId, name, importance);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel.setDescription(description);
        }

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void sendNotification(Context context, String taskId, String taskTitle) {
        // Notification sound
        Uri tone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "TaskChannel")
                .setSmallIcon(R.drawable.ic_alarm) // Replace with your app's icon
                .setContentTitle("Task Reminder")
                .setContentText("Time for task: " + taskTitle)
                .setSound(tone)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        // Create an intent to open AddTaskActivity when the notification is clicked
        Intent notifyIntent = new Intent(context, AddTaskActivity.class);
        notifyIntent.putExtra("taskId", taskId);

        // Wrap the intent in a PendingIntent
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                taskId.hashCode(), // Unique ID for each notification
                notifyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        builder.setContentIntent(pendingIntent);

        // Display the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(taskId.hashCode(), builder.build());
    }

    private void markTaskAsNonEditable(String taskId) {
        DatabaseReference taskRef = FirebaseDatabase.getInstance().getReference("tasks")
                .child(FirebaseAuth.getInstance().getUid()).child(taskId);

        // Mark the task as non-editable in Firebase
        taskRef.child("editable").setValue(false)
                .addOnFailureListener(e -> {
                    // Handle potential errors while updating Firebase
                    e.printStackTrace(); // Log the error for debugging
                });
    }
}
