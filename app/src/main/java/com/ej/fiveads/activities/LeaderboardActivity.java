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
    private final ArrayList<String > leaderBoardReferences = new ArrayList<>();
    private String mCurrentLeaderboardDate;
    private final String TAG = "LeaderboardActivity";
    private int mTicketsAlreadySubmitted;
    private int mCurrentLeaderboardRef = 0;
    private ArrayList<UserData> mUserDataArrayList = new ArrayList<>();
    private RecyclerView mLeaderboardRecyclerView;
    private TextView mLeaderboardCurrentUser;
    private TextView mLeaderboardCurrentUserTickets;

    @Override
    protected void onCreate(Bundle savedInstanceState) {//.getRef().orderByValue()
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);
        Objects.requireNonNull(getSupportActionBar()).hide();

        initializeLeaderboardReferences();

        Calendar calendar = Calendar.getInstance();
        TextView leaderboardTitle = findViewById(R.id.leaderboardTitle);
        leaderboardTitle.setText(getString(R.string._20_raffle));//TODO change title with added leaderboards

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance();

        if (firebaseAuth.getCurrentUser() != null) {
            mFirebaseUser = firebaseAuth.getCurrentUser();
        }

        if (mFirebaseUser != null) {
            mLeaderboardsDatabase = database.getReference("Leaderboards");
            mCurrentLeaderboardDate = "Leaderboards" +
                    calendar.get(Calendar.YEAR) +
                    calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.ENGLISH);//Final String should look like Leaderboards2021Oct
            if (mCurrentLeaderboardDate.equals("Leaderboards2021Oct")) {
                mCurrentLeaderboardDate = "Leaderboards2021Nov";//TODO remove after this month and test
            }
            initializeDatabaseListeners();//to see the value of total and usable tickets
        }

        mLeaderboardRecyclerView = findViewById(R.id.leaderboardRV);
        mLeaderboardRecyclerView.setLayoutManager(new GridLayoutManager(getApplicationContext(), 1));
        mLeaderboardRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        mLeaderboardRecyclerView.setAdapter(new LeaderboardAdapter(mUserDataArrayList, getApplicationContext()));

        mLeaderboardCurrentUser = findViewById(R.id.leaderboardCurrentUser);
        String currentUserScore = "You: " + mFirebaseUser.getDisplayName() + " " + mTicketsAlreadySubmitted;
        mLeaderboardCurrentUser.setText(currentUserScore);

        mLeaderboardCurrentUserTickets = findViewById(R.id.leaderboardCurrentUserTickets);
        mLeaderboardCurrentUserTickets.setText("0");
    }//TODO add buttons that change the leaderboard, add an int to go next / subtract to go back

    private void initializeLeaderboardReferences() {//add more leaderboards here in the future
        leaderBoardReferences.add("20Raffle");
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

    private void initializeDatabaseListeners() {
        ValueEventListener submittedTicketsListener = new ValueEventListener() {// Read from the database whenever there's a change
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(leaderBoardReferences.get(mCurrentLeaderboardRef))
                        .child(mCurrentLeaderboardDate)
                        .child(mFirebaseUser.getUid())
                        .child("submittedTickets")
                        .getValue(Integer.class) != null) {
                    //noinspection ConstantConditions
                    mTicketsAlreadySubmitted = dataSnapshot.child(leaderBoardReferences.get(mCurrentLeaderboardRef))
                            .child(mCurrentLeaderboardDate)
                            .child(mFirebaseUser.getUid())
                            .child("submittedTickets")
                            .getValue(Integer.class);//Objects.requireNonNull() did not work
                }
                Query query = dataSnapshot
                        .child(leaderBoardReferences.get(mCurrentLeaderboardRef))
                        .child(mCurrentLeaderboardDate)
                        .getRef()
                        .orderByChild("submittedTickets")
                        .limitToFirst(50);
                Task<DataSnapshot> usersSnapshot = query.get();
                //noinspection StatementWithEmptyBody
                while (!usersSnapshot.isComplete()) { }//wait for the query to finish to get back to us
                DataSnapshot result = usersSnapshot.getResult();
                HashMap<?,?> hashMapOfUIDs = (HashMap<?,?>) (result != null ? result.getValue() : null);
                if (hashMapOfUIDs != null) {//only happens if no one submitted tickets yet for the current month
                    Collection<?> values = hashMapOfUIDs.values();
                    mUserDataArrayList.clear();
                    for (Object o : values) {
                        HashMap<?,?> hashMap = (HashMap<?,?>) o;
                        Collection<?> usernameAndTickets = hashMap.values();
                        Iterator<?> iterator = usernameAndTickets.iterator();
                        while (iterator.hasNext()) {
                            String displayName = iterator.next().toString();
                            Long value = (Long) iterator.next();
                            mUserDataArrayList.add(new UserData(displayName, value.intValue()));
                        }
                    }
                    rankListOfUsers();
                    mLeaderboardRecyclerView.setAdapter(new LeaderboardAdapter(mUserDataArrayList, getApplicationContext()));
                }

                mLeaderboardCurrentUser = findViewById(R.id.leaderboardCurrentUser);
                String currentUser = "You: " + mFirebaseUser.getDisplayName();
                mLeaderboardCurrentUser.setText(currentUser);
                mLeaderboardCurrentUserTickets.setText(String.valueOf(mTicketsAlreadySubmitted));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        };
        mLeaderboardsDatabase.addValueEventListener(submittedTicketsListener);//for leaderboards and tickets submitted
    }//FIXME right now the leaderboards are updated whenever there is a change, which is awesome. However, it is taxing, we should change this setting to a one time event listener in the future
}