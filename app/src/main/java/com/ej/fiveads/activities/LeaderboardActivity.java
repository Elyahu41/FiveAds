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
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;

public class LeaderboardActivity extends AppCompatActivity {

    private FirebaseUser mFirebaseUser;
    private DatabaseReference mLeaderboardsDatabase;
    private final ArrayList<String > mLeaderBoardReferences = new ArrayList<>();
    private String mCurrentLeaderboardDate;
    private final String TAG = "LeaderboardActivity";
    private int mTicketsAlreadySubmitted;
    private int mCurrentLeaderboardRef = 0;
    private final ArrayList<UserData> mUserDataArrayList = new ArrayList<>();
    private final ArrayList<String> mTitleList = new ArrayList<>();
    private RecyclerView mLeaderboardRecyclerView;
    private TextView mLeaderboardCurrentUser;
    private TextView mLeaderboardCurrentUserTickets;
    private int mCurrentLeaderboardTitle;
    private String monthForMonthlyRaffles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);
        Objects.requireNonNull(getSupportActionBar()).hide();

        TextView leaderboardTitle = findViewById(R.id.leaderboardTitle);
        leaderboardTitle.setText(getString(R.string._5_raffle));

        fillTitleList();
        fillLeaderboardReferencesList();

        Calendar calendar = Calendar.getInstance();
        monthForMonthlyRaffles = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.ENGLISH);
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance();

        if (firebaseAuth.getCurrentUser() != null) {
            mFirebaseUser = firebaseAuth.getCurrentUser();
        }

        if (mFirebaseUser != null) {
            mLeaderboardsDatabase = database.getReference("Leaderboards");
            while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                calendar.add(Calendar.DATE, 1);
            }
            mCurrentLeaderboardDate = "Leaderboards" +
                    calendar.get(Calendar.YEAR) +
                    calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.ENGLISH) +
                    calendar.get(Calendar.DAY_OF_MONTH);//Final String should look like Leaderboards2021Oct20
            initializeDatabaseListener();
        }

        mLeaderboardRecyclerView = findViewById(R.id.leaderboardRV);
        mLeaderboardRecyclerView.setLayoutManager(new GridLayoutManager(this, 1));
        mLeaderboardRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        mLeaderboardRecyclerView.setAdapter(new LeaderboardAdapter(mUserDataArrayList, this));

        mLeaderboardCurrentUser = findViewById(R.id.leaderboardCurrentUser);
        String currentUserScore = "You: " + mFirebaseUser.getDisplayName();
        mLeaderboardCurrentUser.setText(currentUserScore);

        mLeaderboardCurrentUserTickets = findViewById(R.id.leaderboardCurrentUserTickets);
        mLeaderboardCurrentUserTickets.setText(String.valueOf(mTicketsAlreadySubmitted));

        TextView mLeaderboardLeft = findViewById(R.id.leaderboardTitleLeft);
        mLeaderboardLeft.setOnClickListener(v -> {
            mCurrentLeaderboardTitle--;
            if (mCurrentLeaderboardTitle < 0) {
                mCurrentLeaderboardTitle = mTitleList.size() - 1;
            }
            leaderboardTitle.setText(mTitleList.get(mCurrentLeaderboardTitle));

            if (mTitleList.get(mCurrentLeaderboardTitle).equals(getString(R.string._5_raffle))) {
                mCurrentLeaderboardDate = "Leaderboards" +
                        calendar.get(Calendar.YEAR) +
                        calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.ENGLISH) +
                        calendar.get(Calendar.DAY_OF_MONTH);//Final String should look like Leaderboards2021Oct200
            } else {
                mCurrentLeaderboardDate = "Leaderboards" +
                        calendar.get(Calendar.YEAR) +
                        monthForMonthlyRaffles;//Final String should look like Leaderboards2021Oct
            }

            mCurrentLeaderboardRef--;
            if (mCurrentLeaderboardRef < 0) {
                mCurrentLeaderboardRef = mLeaderBoardReferences.size() - 1;
            }
            initializeDatabaseListener();
        });

        TextView mLeaderboardRight = findViewById(R.id.leaderboardTitleRight);
        mLeaderboardRight.setOnClickListener(v -> {
            mCurrentLeaderboardTitle++;
            if (mCurrentLeaderboardTitle > mTitleList.size() - 1) {
                mCurrentLeaderboardTitle = 0;
            }
            leaderboardTitle.setText(mTitleList.get(mCurrentLeaderboardTitle));

            if (mTitleList.get(mCurrentLeaderboardTitle).equals(getString(R.string._5_raffle))) {
                mCurrentLeaderboardDate = "Leaderboards" +
                        calendar.get(Calendar.YEAR) +
                        calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.ENGLISH) +
                        calendar.get(Calendar.DAY_OF_MONTH);//Final String should look like Leaderboards2021Oct200
            } else {
                mCurrentLeaderboardDate = "Leaderboards" +
                        calendar.get(Calendar.YEAR) +
                        monthForMonthlyRaffles;//Final String should look like Leaderboards2021Oct
            }

            mCurrentLeaderboardRef++;
            if (mCurrentLeaderboardRef > mLeaderBoardReferences.size() - 1) {
                mCurrentLeaderboardRef = 0;
            }
            initializeDatabaseListener();
        });
    }

    private void fillTitleList() {
        mTitleList.add(getString(R.string._5_raffle));
        mTitleList.add(getString(R.string._10_raffle));
        mTitleList.add(getString(R.string._20_raffle));
        mTitleList.add(getString(R.string._25_raffle));
        mTitleList.add(getString(R.string._50_raffle));
    }

    private void fillLeaderboardReferencesList() {
        mLeaderBoardReferences.add("5Raffle");
        mLeaderBoardReferences.add("10Raffle");
        mLeaderBoardReferences.add("20Raffle");
        mLeaderBoardReferences.add("25Raffle");
        mLeaderBoardReferences.add("50Raffle");
    }

    private void rankListOfUsers() {
        Collections.sort(mUserDataArrayList, Collections.reverseOrder());
        int currentRank = 1;
        if (mUserDataArrayList.size() != 1) {//we will not get here with a size of 0
            for (int i = 0; i < mUserDataArrayList.size() - 1; i++) {
                UserData user1 = mUserDataArrayList.get(i);
                UserData user2 = mUserDataArrayList.get(i + 1);
                if (user1.compareTo(user2) > 0) {//simply put, if they have the same number of tickets, they're both first/second..., otherwise, increment the rank
                    user1.setRank(currentRank);
                    currentRank += 1;
                    user2.setRank(currentRank);
                } else if (user1.compareTo(user2) == 0) {
                    user1.setRank(currentRank);
                    user2.setRank(currentRank);
                }
            }
        } else {
            UserData user1 = mUserDataArrayList.get(0);
            user1.setRank(currentRank);
        }
    }

    private void initializeDatabaseListener() {
        ValueEventListener submittedTicketsListener = new ValueEventListener() {// Read from the database whenever there's a change
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.child(mLeaderBoardReferences.get(mCurrentLeaderboardRef)).child(mCurrentLeaderboardDate).hasChild("isInValid")) {
                    if (dataSnapshot.child(mLeaderBoardReferences.get(mCurrentLeaderboardRef))
                            .child(mCurrentLeaderboardDate)
                            .child(mFirebaseUser.getUid())
                            .child("submittedTickets")
                            .getValue(Integer.class) != null) {
                        //noinspection ConstantConditions
                        mTicketsAlreadySubmitted = dataSnapshot.child(mLeaderBoardReferences.get(mCurrentLeaderboardRef))
                                .child(mCurrentLeaderboardDate)
                                .child(mFirebaseUser.getUid())
                                .child("submittedTickets")
                                .getValue(Integer.class);//Objects.requireNonNull() did not work
                    }
                    Query query = dataSnapshot
                            .child(mLeaderBoardReferences.get(mCurrentLeaderboardRef))
                            .child(mCurrentLeaderboardDate)
                            .getRef()
                            .orderByChild("submittedTickets");
                    Task<DataSnapshot> usersSnapshot = query.get();
                    //noinspection StatementWithEmptyBody
                    while (!usersSnapshot.isComplete()) {
                    }//wait for the query to finish to get back to us
                    DataSnapshot result = usersSnapshot.getResult();
                    HashMap<?, ?> hashMapOfUIDs = (HashMap<?, ?>) (result != null ? result.getValue() : null);
                    mUserDataArrayList.clear();
                    if (hashMapOfUIDs != null) {//only null if no one submitted tickets yet for the current month
                        Collection<?> values = hashMapOfUIDs.values();
                        for (Object o : values) {
                            HashMap<?, ?> hashMap = (HashMap<?, ?>) o;
                            Collection<?> usernameAndTickets = hashMap.values();
                            Iterator<?> iterator = usernameAndTickets.iterator();
                            while (iterator.hasNext()) {
                                String displayName = "";
                                Long value = 0L;
                                if (iterator.hasNext()) {
                                    Object object = iterator.next();
                                    if (object instanceof String) {
                                        displayName = object.toString();
                                    } else if (object instanceof Long) {
                                        value = (Long) object;
                                    }
                                }
                                if (iterator.hasNext()) {
                                    Object object = iterator.next();
                                    if (object instanceof String) {
                                        displayName = object.toString();
                                    } else if (object instanceof Long) {
                                        value = (Long) object;
                                    }
                                }
                                mUserDataArrayList.add(new UserData(displayName, value.intValue()));
                            }
                        }
                        rankListOfUsers();
                    }
                    mLeaderboardRecyclerView.setAdapter(new LeaderboardAdapter(mUserDataArrayList, getApplicationContext()));
                    mLeaderboardCurrentUser = findViewById(R.id.leaderboardCurrentUser);
                    String currentUser = "You: " + mFirebaseUser.getDisplayName();
                    mLeaderboardCurrentUser.setText(currentUser);
                    mLeaderboardCurrentUserTickets.setText(String.valueOf(mTicketsAlreadySubmitted));
                } else {
                    if (mUserDataArrayList.isEmpty()) {
                        mUserDataArrayList.add(new UserData("Not Valid Raffle", 0));
                        mLeaderboardRecyclerView.setAdapter(new LeaderboardAdapter(mUserDataArrayList, getApplicationContext()));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        };
        mLeaderboardsDatabase.addValueEventListener(submittedTicketsListener);//for current leaderboard
    }//FIXME right now the leaderboards are updated whenever there is a change, which is awesome. However, it is taxing, we should change this setting to a one time event listener in the future
}