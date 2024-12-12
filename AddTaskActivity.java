package com.example.todo;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class AddTaskActivity extends AppCompatActivity {

    private EditText editTextTitle, editTextDescription, editTextDate, editTextTime;
    private DatabaseReference tasksDatabaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        // Initialize Firebase Auth and Database
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in to add tasks", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        tasksDatabaseRef = FirebaseDatabase.getInstance().getReference("tasks").child(userId);

        // Initialize Views
        editTextTitle = findViewById(R.id.editTextTaskTitle);
        editTextDescription = findViewById(R.id.editTextTaskDescription);
        editTextDate = findViewById(R.id.editTextTaskDate);
        editTextTime = findViewById(R.id.editTextTaskTime);
        Button buttonSaveTask = findViewById(R.id.buttonSaveTask);

        // Set listeners for Date and Time EditText
        editTextDate.setOnClickListener(v -> showDatePicker());
        editTextTime.setOnClickListener(v -> showTimePicker());

        // Handle Save Task button click
        buttonSaveTask.setOnClickListener(v -> saveTaskToFirebase());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                AddTaskActivity.this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = String.format(Locale.getDefault(), "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
                    editTextDate.setText(date);
                },
                year, month, day);
        datePickerDialog.show();
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                AddTaskActivity.this,
                (view, selectedHour, selectedMinute) -> {
                    String time = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
                    editTextTime.setText(time);
                },
                hour, minute, true);
        timePickerDialog.show();
    }

    private void saveTaskToFirebase() {
        String title = editTextTitle.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();
        String date = editTextDate.getText().toString().trim();
        String time = editTextTime.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(title)) {
            editTextTitle.setError("Title is required");
            return;
        }
        if (TextUtils.isEmpty(date)) {
            editTextDate.setError("Date is required");
            return;
        }
        if (TextUtils.isEmpty(time)) {
            editTextTime.setError("Time is required");
            return;
        }

        // Convert date and time into a long value (milliseconds)
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        try {
            String dateTime = date + " " + time;
            calendar.setTime(dateFormat.parse(dateTime));  // Parse the date and time
        } catch (Exception e) {
            Toast.makeText(this, "Invalid date/time format", Toast.LENGTH_SHORT).show();
            return;
        }
        long taskTimeInMillis = calendar.getTimeInMillis();

        // Generate unique ID for the task
        String taskId = tasksDatabaseRef.push().getKey();
        if (taskId == null) {
            Toast.makeText(this, "Error generating task ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a HashMap for task details
        HashMap<String, String> taskMap = new HashMap<>();
        taskMap.put("title", title);
        taskMap.put("description", description);
        taskMap.put("date", date);
        taskMap.put("time", time);
        taskMap.put("status", "pending");  // Set default status to "pending"

        // Save to Firebase
        tasksDatabaseRef.child(taskId).setValue(taskMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AddTaskActivity.this, "Task added successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // Close AddTaskActivity
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddTaskActivity.this, "Failed to add task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
