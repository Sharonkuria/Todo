package com.example.todo;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddNoteActivity extends AppCompatActivity {

    private EditText titleEditText, contentEditText;

    private DatabaseReference notesDatabaseRef;
    private String noteId; // This will determine if we are editing an existing note

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_note);

        titleEditText = findViewById(R.id.editTextTitle);
        contentEditText = findViewById(R.id.editTextContent);
        TextView timestampTextView = findViewById(R.id.textViewTimestamp);
        Button saveButton = findViewById(R.id.buttonsave);

        // Initialize Firebase references
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "User not authenticated. Please log in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String userId = auth.getCurrentUser().getUid();
        notesDatabaseRef = FirebaseDatabase.getInstance().getReference("notes").child(userId);

        // Check if we're editing an existing note
        noteId = getIntent().getStringExtra("NOTE_ID");
        if (noteId != null) {
            // Populate fields for editing
            titleEditText.setText(getIntent().getStringExtra("NOTE_TITLE"));
            contentEditText.setText(getIntent().getStringExtra("NOTE_CONTENT"));
            timestampTextView.setText(getString(R.string.last_edited, getIntent().getStringExtra("NOTE_TIMESTAMP")));

        } else {
            // Show the current timestamp for new notes
            String currentTimestamp = getCurrentTimestamp();
            timestampTextView.setText(getString(R.string.created_on, currentTimestamp));

        }

        saveButton.setOnClickListener(v -> {
            String title = titleEditText.getText().toString().trim();
            String content = contentEditText.getText().toString().trim();
            String timestamp = getCurrentTimestamp();

            if (TextUtils.isEmpty(title)) {
                Toast.makeText(this, "Title is required.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(content)) {
                Toast.makeText(this, "Content is required.", Toast.LENGTH_SHORT).show();
                return;
            }

            saveNoteToFirebase(noteId, title, content);
        });
    }

    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void saveNoteToFirebase(String noteId, String title, String content) {
        // Generate current timestamp
        String timestamp = getCurrentTimestamp();
        // Generate noteId if null
        if (noteId == null) {
            // Generate a new ID for a new note
            noteId = notesDatabaseRef.push().getKey();
        }

        // Create Note object
        com.example.todo.Note note = new com.example.todo.Note(noteId, title, content, timestamp);

        // Save to Firebase
        assert noteId != null;
        // Save to Firebase
        notesDatabaseRef.child(noteId).setValue(note)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Note saved successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save note: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}