package com.example.todo;

public class Task {
    private String title;
    private String description;
    private String date;
    private String time;
    private String status;  // Status of the task (e.g., "pending", "completed")
    private long scheduledTime;  // Time in milliseconds for scheduling the alarm

    // Default constructor (required for Firebase)
    public Task() {}

    // Constructor
    public Task(String title, String description, String date, String time, String status, long scheduledTime) {
        this.title = title;
        this.description = description;
        this.date = date;
        this.time = time;
        this.status = status;
        this.scheduledTime = scheduledTime;
    }

    // Getters and setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(long scheduledTime) {
        this.scheduledTime = scheduledTime;
    }
}
