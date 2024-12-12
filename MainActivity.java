package com.example.todo;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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

public class MainActivity extends AppCompatActivity {

    private LinearLayout notesContainer;
    private DatabaseReference notesDatabaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        notesContainer = findViewById(R.id.notesContainer);
        Button addNoteButton = findViewById(R.id.buttonAddNote);
        Button signOutButton = findViewById(R.id.buttonsignOut);
        Button addTaskButton = findViewById(R.id.buttonAddTask);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in. Redirecting to login page...", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        notesDatabaseRef = FirebaseDatabase.getInstance().getReference("notes").child(userId);

        // Load notes from Firebase
        loadNotesFromFirebase();

        // Handle Add Note button click
        addNoteButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddNoteActivity.class);
            startActivity(intent);
        });

        // Handle Add Task button click
        addTaskButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TaskActivity.class);
            startActivity(intent);
        });

        // Handle Sign Out button click
        signOutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(this, "Signed out successfully!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity.this, WelcomeActivity.class));
            finish();
        });
    }

    private void loadNotesFromFirebase() {
        notesDatabaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                notesContainer.removeAllViews();
                for (DataSnapshot noteSnapshot : snapshot.getChildren()) {
                    Note note = noteSnapshot.getValue(Note.class);
                    if (note != null) {
                        addNoteToContainer(note, noteSnapshot.getKey());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MainActivity", "Failed to read notes: " + error.getMessage());
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void addNoteToContainer(Note note, String noteId) {
        View noteView = getLayoutInflater().inflate(R.layout.item_note, notesContainer, false);

        TextView titleTextView = noteView.findViewById(R.id.textViewNoteTitle);
        TextView contentTextView = noteView.findViewById(R.id.textViewNoteContent);
        TextView timestampTextView = noteView.findViewById(R.id.textViewNoteTimestamp);
        Button editButton = noteView.findViewById(R.id.buttonEditNote);
        Button deleteButton = noteView.findViewById(R.id.buttonDeleteNote);

        // Set note details
        titleTextView.setText(note.getTitle());
        contentTextView.setText(note.getContent());
        timestampTextView.setText("Created: " + note.getTimestamp());

        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddNoteActivity.class);
            intent.putExtra("NOTE_ID", noteId);
            intent.putExtra("NOTE_TITLE", note.getTitle());
            intent.putExtra("NOTE_CONTENT", note.getContent());
            startActivity(intent);
        });

        deleteButton.setOnClickListener(v -> deleteNoteFromFirebase(noteId));

        notesContainer.addView(noteView);
    }

    private void deleteNoteFromFirebase(String noteId) {
        notesDatabaseRef.child(noteId).removeValue()
                .addOnSuccessListener(aVoid -> Toast.makeText(MainActivity.this, "Note deleted!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Failed to delete note: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
