package com.example.todo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TaskActivity extends AppCompatActivity {

    private LinearLayout taskContainer;
    private DatabaseReference taskDatabaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);

        taskContainer = findViewById(R.id.taskContainer);
        Button addTaskButton = findViewById(R.id.buttonAddTask);
        Button signOutButton = findViewById(R.id.buttonsignOut);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in. Redirecting to login page...", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(TaskActivity.this, LoginActivity.class));
            finish();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        taskDatabaseRef = FirebaseDatabase.getInstance().getReference("tasks").child(userId);

        // Load tasks from Firebase
        loadTasksFromFirebase();

        // Handle Add Task button click
        addTaskButton.setOnClickListener(v -> startActivity(new Intent(TaskActivity.this, AddTaskActivity.class)));

        // Handle Sign Out button click
        signOutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(this, "Signed out successfully!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(TaskActivity.this, WelcomeActivity.class));
            finish();
        });
    }

    private void loadTasksFromFirebase() {
        taskDatabaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                taskContainer.removeAllViews();
                for (DataSnapshot taskSnapshot : snapshot.getChildren()) {
                    Task task = taskSnapshot.getValue(Task.class);
                    if (task != null) {
                        addTaskToContainer(task, taskSnapshot.getKey());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TaskActivity.this, "Failed to load tasks: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void addTaskToContainer(Task task, String taskId) {
        try {
            View taskView = getLayoutInflater().inflate(R.layout.item_task, taskContainer, false);

            EditText titleEditText = taskView.findViewById(R.id.editTextTaskTitle);
            EditText descriptionEditText = taskView.findViewById(R.id.editTextTaskDescription);
            TextView dateTextView = taskView.findViewById(R.id.textViewTaskDate);
            TextView timeTextView = taskView.findViewById(R.id.textViewTaskTime);
            Button editButton = taskView.findViewById(R.id.buttonEditTask);
            Button completeButton = taskView.findViewById(R.id.buttonCompleteTask);

            // Verify if the task data is null or invalid
            if (task.getTitle() == null || task.getDate() == null || task.getTime() == null) {
                Log.e("TaskActivity", "Invalid task data for taskId: " + taskId);
                Toast.makeText(this, "Error: Invalid task data.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Set task data
            titleEditText.setText(task.getTitle());
            descriptionEditText.setText(task.getDescription());
            dateTextView.setText(task.getDate());
            timeTextView.setText(task.getTime());

            // Additional UI setup...
            if ("completed".equals(task.getStatus())) {
                completeButton.setVisibility(View.GONE);
            } else {
                completeButton.setVisibility(View.VISIBLE);
            }

            // Handle alarm scheduling
            if ("pending".equals(task.getStatus())) {
                scheduleTaskAlarm(task, taskId);
            }
            editButton.setOnClickListener(v -> {
                titleEditText.setEnabled(true);
                descriptionEditText.setEnabled(true);
                titleEditText.requestFocus();
            });

            // Handle Mark as Completed button click
            completeButton.setOnClickListener(v -> {
                taskDatabaseRef.child(taskId).child("status").setValue("completed")
                        .addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                // Mark the task as completed in UI
                                Toast.makeText(TaskActivity.this, "Task marked as completed", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(TaskActivity.this, "Failed to mark task as completed", Toast.LENGTH_SHORT).show();
                            }
                        });
            });

            taskContainer.addView(taskView);

        } catch (Exception e) {
            Log.e("TaskActivity", "Error adding task to container", e);
            Toast.makeText(this, "An error occurred while loading the task.", Toast.LENGTH_SHORT).show();
        }
    }


    private void scheduleTaskAlarm(Task task, String taskId) {
        try {
            String dateTimeString = task.getDate() + " " + task.getTime();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Date date = sdf.parse(dateTimeString);

            if (date == null) {
                throw new IllegalArgumentException("Date parsing failed for: " + dateTimeString);
            }

            long taskTimeInMillis = date.getTime();
            Intent intent = new Intent(this, AlarmReceiver.class);
            intent.putExtra("taskId", taskId);
            intent.putExtra("taskTitle", task.getTitle());
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    taskId.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (!alarmManager.canScheduleExactAlarms()) {
                        //Intent settingsIntent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        //startActivity(settingsIntent);
                    }
                }
                alarmManager.set(AlarmManager.RTC_WAKEUP, taskTimeInMillis, pendingIntent);
            }

        } catch (ParseException e) {
            Log.e("TaskActivity", "Date parsing error for task: " + task.getTitle(), e);
            Toast.makeText(this, "Invalid date/time for task: " + task.getTitle(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("TaskActivity", "Error scheduling alarm", e);
            Toast.makeText(this, "An error occurred while scheduling the task.", Toast.LENGTH_SHORT).show();
        }
    }



}
