package com.ej.fiveads.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.ej.fiveads.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

public class RaffleSubmitActivity extends AppCompatActivity {

    private int mUserSelectedTicketsToSubmit;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mLeaderboardsDatabase;
    private DatabaseReference mTicketsDatabase;
    private int mTicketsAlreadySubmitted;
    private TextView mTickets;
    private int mNumberOfUsableTickets;
    private String mCurrentLeaderboardDate;
    private Bundle mBundle;
    private final String TAG = "RaffleActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_raffle_submit);
        Objects.requireNonNull(getSupportActionBar()).hide();
        Calendar calendar = Calendar.getInstance();

        TextView title = findViewById(R.id.titleTextView);
        ImageView imageView = findViewById(R.id.submitImage);
        mTickets = findViewById(R.id.numberOfTicketsTextView);
        Button minus1 = findViewById(R.id.buttonMinus1);
        EditText editText = findViewById(R.id.editText);
        Button plus1 = findViewById(R.id.buttonPlus1);
        Button submitButton = findViewById(R.id.submitButton);

        mBundle = getIntent().getExtras();
        if (mBundle != null) {
            title.setText(mBundle.getString("Title"));
            imageView.setImageResource(mBundle.getInt("Image"));
        }

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance();

        if (firebaseAuth.getCurrentUser() != null) {
            mFirebaseUser = firebaseAuth.getCurrentUser();
        }

        if (mFirebaseUser != null) {
            mTicketsDatabase = database.getReference("Users");
            mLeaderboardsDatabase = database.getReference("Leaderboards");
            mCurrentLeaderboardDate = "Leaderboards" +
                    calendar.get(Calendar.YEAR) +
                    calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.ENGLISH);//Final String should look like Leaderboards2021OCT
            initializeDatabaseListeners();//to see the value of total and usable tickets
        }

        minus1.setOnClickListener(v -> {
            if (!editText.getText().toString().isEmpty()) {
                mUserSelectedTicketsToSubmit = Integer.parseInt(editText.getText().toString());
            }
            if (mUserSelectedTicketsToSubmit > 0) {
                mUserSelectedTicketsToSubmit--;
                editText.setText(String.valueOf(mUserSelectedTicketsToSubmit));
            }
        });

        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                mUserSelectedTicketsToSubmit = mNumberOfUsableTickets;
                editText.setText(String.valueOf(mUserSelectedTicketsToSubmit));
            }
        });

        editText.setOnEditorActionListener((v, actionId, event) -> {
            mUserSelectedTicketsToSubmit = Integer.parseInt(v.getText().toString());
            return false;
        });

        plus1.setOnClickListener(v -> {
            if (!editText.getText().toString().isEmpty()) {
                mUserSelectedTicketsToSubmit = Integer.parseInt(editText.getText().toString());
            }
            mUserSelectedTicketsToSubmit++;
            editText.setText(String.valueOf(mUserSelectedTicketsToSubmit));
        });

        submitButton.setOnClickListener(v -> {
            if (mUserSelectedTicketsToSubmit == 0) {
                Toast.makeText(this, R.string.you_cant_submit_nothing, Toast.LENGTH_SHORT).show();
            } else if (mUserSelectedTicketsToSubmit > mNumberOfUsableTickets) {
                Toast.makeText(this, R.string.You_do_not_have_that_many_tickets, Toast.LENGTH_SHORT).show();
            } else {
                if (mBundle != null) {
                    mLeaderboardsDatabase.child(mBundle.getString("DatabaseRef").trim())
                            .child(mCurrentLeaderboardDate)
                            .child(mFirebaseUser.getUid())
                            .child("displayName")
                            .setValue(mFirebaseUser.getDisplayName());
                    mLeaderboardsDatabase.child(mBundle.getString("DatabaseRef"))
                            .child(mCurrentLeaderboardDate)
                            .child(mFirebaseUser.getUid())
                            .child("submittedTickets").setValue(
                            mUserSelectedTicketsToSubmit + mTicketsAlreadySubmitted
                    );
                }
                mTicketsDatabase.child(mFirebaseUser.getUid())
                        .child("usableTickets")
                        .setValue(
                        mNumberOfUsableTickets - mUserSelectedTicketsToSubmit
                );
                String updatedUsableTickets = String.format(Locale.ENGLISH, "%,d", mNumberOfUsableTickets - mUserSelectedTicketsToSubmit);
                String usableTickets = "Usable tickets: " + updatedUsableTickets;
                mTickets.setText(usableTickets);
                new AlertDialog.Builder(this)
                        .setTitle("Tickets Submitted!")
                        .setMessage("You have submitted " + mUserSelectedTicketsToSubmit + " ticket(s)!")
                        .setPositiveButton("Ok", (dialog, which) -> { })
                        .create()
                        .show();
            }
        });
    }

    private void initializeDatabaseListeners() {
        ValueEventListener submittedTicketsListener = new ValueEventListener() {// Read from the database whenever there's a change
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(mBundle.getString("DatabaseRef")).child(mCurrentLeaderboardDate)
                        .child(mFirebaseUser.getUid())
                        .child("submittedTickets")
                        .getValue(Integer.class) != null) {
                    //noinspection ConstantConditions
                    mTicketsAlreadySubmitted = dataSnapshot.child(mBundle.getString("DatabaseRef")).child(mCurrentLeaderboardDate)
                            .child(mFirebaseUser.getUid())
                            .child("submittedTickets")
                            .getValue(Integer.class);//Objects.requireNonNull() did not work
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        };
        mLeaderboardsDatabase.addValueEventListener(submittedTicketsListener);//for leaderboards and tickets submitted

        ValueEventListener ticketListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(mFirebaseUser.getUid()).child("usableTickets").getValue(Integer.class) != null) {
                    //noinspection ConstantConditions
                    mNumberOfUsableTickets = dataSnapshot
                            .child(mFirebaseUser.getUid())
                            .child("usableTickets")
                            .getValue(Integer.class);//Objects.requireNonNull() did not work
                    String numberOfUsableTickets = String.format(Locale.ENGLISH, "%,d", mNumberOfUsableTickets);
                    String usableTickets = "Usable tickets: " + numberOfUsableTickets;
                    mTickets.setText(usableTickets);
                }
                Log.d(TAG, "Usable Tickets Value is: " + mNumberOfUsableTickets);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        };
        mTicketsDatabase.addValueEventListener(ticketListener);//for users and tickets earned
    }
}