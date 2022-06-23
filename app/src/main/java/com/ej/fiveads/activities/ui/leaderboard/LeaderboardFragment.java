package com.ej.fiveads.activities.ui.leaderboard;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ej.fiveads.R;
import com.ej.fiveads.classes.LeaderboardAdapter;
import com.ej.fiveads.classes.UserData;
import com.ej.fiveads.databinding.FragmentLeaderboardBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Objects;

public class LeaderboardFragment extends Fragment {

    private FirebaseUser mFirebaseUser;
    private DatabaseReference mLeaderboardsDatabase;
    private final ArrayList<String > mLeaderBoardReferences = new ArrayList<>();
    private final String TAG = "LeaderboardFragment";
    private int mCurrentLeaderboardRef = 0;
    private final ArrayList<UserData> mUserDataArrayList = new ArrayList<>();
    private final ArrayList<String> mTitleList = new ArrayList<>();
    private RecyclerView mLeaderboardRecyclerView;
    private int mCurrentLeaderboardTitle;
    private Context mContext;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //SpendViewModel spendViewModel = new ViewModelProvider(this).get(SpendViewModel.class);
        com.ej.fiveads.databinding.FragmentLeaderboardBinding binding = FragmentLeaderboardBinding.inflate(inflater, container, false);

        mContext = getContext();

        TextView leaderboardTitle = binding.leaderboardTitle;
        leaderboardTitle.setText(getString(R.string._5_raffle));

        fillTitleList();
        fillLeaderboardReferencesList();

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance();

        if (firebaseAuth.getCurrentUser() != null) {
            mFirebaseUser = firebaseAuth.getCurrentUser();
        }

        if (mFirebaseUser != null) {
            mLeaderboardsDatabase = database.getReference("Leaderboards");
            initializeDatabaseListener();
        }

        mLeaderboardRecyclerView = binding.leaderboardRV;
        mLeaderboardRecyclerView.setLayoutManager(new GridLayoutManager(mContext, 1));
        mLeaderboardRecyclerView.addItemDecoration(new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL));
        mLeaderboardRecyclerView.setAdapter(new LeaderboardAdapter(mUserDataArrayList, mContext));

        TextView mLeaderboardLeft = binding.leaderboardTitleLeft;
        mLeaderboardLeft.setOnClickListener(v -> {
            mCurrentLeaderboardTitle--;
            if (mCurrentLeaderboardTitle < 0) {
                mCurrentLeaderboardTitle = mTitleList.size() - 1;//loop to front
            }
            leaderboardTitle.setText(mTitleList.get(mCurrentLeaderboardTitle));
            mCurrentLeaderboardRef--;
            if (mCurrentLeaderboardRef < 0) {
                mCurrentLeaderboardRef = mLeaderBoardReferences.size() - 1;
            }
            initializeDatabaseListener();
        });

        TextView mLeaderboardRight = binding.leaderboardTitleRight;
        mLeaderboardRight.setOnClickListener(v -> {
            mCurrentLeaderboardTitle++;
            if (mCurrentLeaderboardTitle > mTitleList.size() - 1) {
                mCurrentLeaderboardTitle = 0;
            }
            leaderboardTitle.setText(mTitleList.get(mCurrentLeaderboardTitle));

            mCurrentLeaderboardRef++;
            if (mCurrentLeaderboardRef > mLeaderBoardReferences.size() - 1) {
                mCurrentLeaderboardRef = 0;
            }
            initializeDatabaseListener();
        });
        return binding.getRoot();
    }

    private void fillTitleList() {
        mTitleList.add(getString(R.string._5_raffle));
        mTitleList.add(getString(R.string._10_raffle));
        mTitleList.add(getString(R.string._15_raffle));
        mTitleList.add(getString(R.string._20_raffle));
    }

    private void fillLeaderboardReferencesList() {
        mLeaderBoardReferences.add("5Raffle");
        mLeaderBoardReferences.add("10Raffle");
        mLeaderBoardReferences.add("15Raffle");
        mLeaderBoardReferences.add("20Raffle");
    }

    private void rankListOfUsers() {
        int currentRank = 1;
        if (mUserDataArrayList.size() != 0) {
            for (int i = 0; i < mUserDataArrayList.size(); i++) {
                mUserDataArrayList.get(i).setRank(currentRank);
            }
        }
    }

    private void initializeDatabaseListener() {
        ValueEventListener winnersListener = new ValueEventListener() {// Read from the database whenever there's a change to find the winners
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.child(mLeaderBoardReferences.get(mCurrentLeaderboardRef)).hasChild("isInValid")) {
                    DataSnapshot dr = dataSnapshot.child(mLeaderBoardReferences.get(mCurrentLeaderboardRef));
                    mUserDataArrayList.clear();
                    for (DataSnapshot ds : dr.getChildren()) {
                        if (!Objects.requireNonNull(ds.getKey()).startsWith("Leaderboards")) {
                            String winner = ds.getKey();
                            mUserDataArrayList.add(new UserData(winner, mTitleList.get(mCurrentLeaderboardTitle)));
                        }
                    }
                    rankListOfUsers();
                    if (mUserDataArrayList.isEmpty()) {
                        mUserDataArrayList.add(new UserData("No Winners Yet", "NA"));
                    }
                    mLeaderboardRecyclerView.setAdapter(new LeaderboardAdapter(mUserDataArrayList, mContext));
                } else {
                    if (mUserDataArrayList.isEmpty()) {
                        mUserDataArrayList.add(new UserData("Not Valid Winning", "NA"));
                        mLeaderboardRecyclerView.setAdapter(new LeaderboardAdapter(mUserDataArrayList, mContext));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        };
        mLeaderboardsDatabase.addListenerForSingleValueEvent(winnersListener);//for current leaderboard
    }
}
