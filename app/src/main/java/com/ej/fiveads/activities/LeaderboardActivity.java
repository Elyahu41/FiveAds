package com.ej.fiveads.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ej.fiveads.R;
import com.ej.fiveads.classes.LeaderboardAdapter;
import com.ej.fiveads.classes.UserData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class LeaderboardActivity extends AppCompatActivity {

    private FirebaseUser mFirebaseUser;
    private DatabaseReference mLeaderboardsDatabase;
    private String mCurrentLeaderboardDate;
    private final String TAG = "LeaderboardActivity";
    private int mTicketsAlreadySubmitted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {//.getRef().orderByValue()
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);
        Objects.requireNonNull(getSupportActionBar()).hide();

        Calendar calendar = Calendar.getInstance();
        TextView leaderboardTitle = findViewById(R.id.leaderboardTitle);
        leaderboardTitle.setText("$20");//TODO change title with added leaderboards

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance();

        if (firebaseAuth.getCurrentUser() != null) {
            mFirebaseUser = firebaseAuth.getCurrentUser();
        }

        if (mFirebaseUser != null) {
            mLeaderboardsDatabase = database.getReference("Leaderboards");
            mCurrentLeaderboardDate = "Leaderboards" +
                    calendar.get(Calendar.YEAR) +
                    calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.ENGLISH);//Final String should look like Leaderboards2021OCT
            initializeDatabaseListeners();//to see the value of total and usable tickets
        }

        RecyclerView leaderboardRecyclerView = findViewById(R.id.leaderboardRV);
        leaderboardRecyclerView.setLayoutManager(new GridLayoutManager(getApplicationContext(), 1));
        leaderboardRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        leaderboardRecyclerView.setAdapter(new LeaderboardAdapter(getListOfUsers(), getApplicationContext()));

        TextView leaderboardCurrentUser = findViewById(R.id.leaderboardCurrentUser);//TODO
        String currentUserScore = "You: " + mFirebaseUser.getDisplayName() + " " + mTicketsAlreadySubmitted;
        leaderboardCurrentUser.setText(currentUserScore);
    }

    private List<UserData> getListOfUsers() {
        ArrayList<UserData> userData = new ArrayList<>();
        userData.add(new UserData(1, "Elyahu Jacobi", 10000));
        userData.add(new UserData(3, "Elyahu Jacobi", 8000));
        userData.add(new UserData(2, "Elyahu Jacobi", 9000));
        userData.add(new UserData(5, "Elyahu Jacobi", 700));
        userData.add(new UserData(5, "Elyahu Jacobi", 700));
        userData.add(new UserData(8, "Elyahu Jacobi", 60));
        userData.add(new UserData(1, "Elyahu Jacobi", 10000));
        Collections.sort(userData);
        return userData;
    }

    private void initializeDatabaseListeners() {
        ValueEventListener submittedTicketsListener = new ValueEventListener() {// Read from the database whenever there's a change
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(mCurrentLeaderboardDate)
                        .child(mFirebaseUser.getUid())
                        .child("submittedTickets")
                        .getValue(Integer.class) != null) {
                    //noinspection ConstantConditions
                    mTicketsAlreadySubmitted = dataSnapshot.child(mCurrentLeaderboardDate)
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
    }
}