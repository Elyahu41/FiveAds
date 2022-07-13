package com.ej.fiveads.activities;

import static com.ej.fiveads.activities.MainActivity.mDeviceDefaults;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.ej.fiveads.R;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import rubikstudio.library.LuckyWheelView;
import rubikstudio.library.model.LuckyItem;

public class RaffleSubmitActivity extends AppCompatActivity {

    private FirebaseUser mFirebaseUser;
    private DatabaseReference mLeaderboardsDatabase;
    private DatabaseReference mUsersDatabase;
    private int mNumberOfUsableTickets;
    private TextView mTicketsTV;
    private TextView mMoneyTV;
    private Bundle mBundle;
    private final String TAG = "RaffleSubmitActivity";
    private String mGameRisk;
    private boolean isNotValidRaffle = false;
    private List<LuckyItem> mLuckyItems;
    private final String[] mGameResults = new String[12];
    boolean lastWasWhite = false;
    private rubikstudio.library.DistributedRandomNumberGenerator mNumberGenerator;
    private Activity mActivity;
    private double mMoneyUserHas;
    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_raffle_submit);
        Objects.requireNonNull(getSupportActionBar()).hide();
        mActivity = this;

        mAdView = findViewById(R.id.adViewRS);//RS = Raffle Submit (activity)
        mAdView.loadAd(new AdRequest.Builder().build());

        TextView title = findViewById(R.id.titleTextView);
        mTicketsTV = findViewById(R.id.numberOfTicketsTextView);
        mMoneyTV = findViewById(R.id.moneyTextView);

        LuckyWheelView luckyWheelView = findViewById(R.id.luckyWheel);
        mLuckyItems = new ArrayList<>();


        mBundle = getIntent().getExtras();
        if (mBundle != null) {
            title.setText(mBundle.getString("Title"));
            mGameRisk = mBundle.getString("DatabaseRef");
        }

        mNumberGenerator = new rubikstudio.library.DistributedRandomNumberGenerator();

        switch (mGameRisk) {
            case "20Raffle":
                fill20dollarList();
                break;
            case "15Raffle":
                fill15dollarList();
                break;
            case "10Raffle":
                fill10dollarList();
                break;
            case "5Raffle":
                fill5dollarList();
                break;
        }

        luckyWheelView.getPielView().setGenerator(mNumberGenerator);
        luckyWheelView.setData(mLuckyItems);
        luckyWheelView.setRound(10);

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance();

        if (firebaseAuth.getCurrentUser() != null) {
            mFirebaseUser = firebaseAuth.getCurrentUser();
        }

        if (mFirebaseUser != null) {
            mUsersDatabase = database.getReference("Users");
            mLeaderboardsDatabase = database.getReference("Leaderboards");
            initializeDatabaseListeners();//to see the value of total and usable tickets
        }

        Button playButton = findViewById(R.id.play_button);
        playButton.setOnClickListener(v -> {
            if (mNumberOfUsableTickets > 0) {
                luckyWheelView.startLuckyWheelWithTargetIndex(mNumberGenerator.getDistributedRandomNumber());
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(RaffleSubmitActivity.this);
                builder.setMessage("You don't have enough tickets to play this game. Please get more tickets to play this game.")
                        .setCancelable(false)
                        .setPositiveButton("OK", (dialog, id) -> {
                            dialog.cancel();
                            finish();
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });

        luckyWheelView.setLuckyRoundItemSelectedListener(index -> {// listener after finish lucky wheel
            if (isNotValidRaffle) {
                new AlertDialog.Builder(getApplicationContext())
                        .setTitle("Invalid Raffle")
                        .setMessage("This game is not currently ongoing. Please update the app to see new games or choose another game.")
                        .setPositiveButton("Ok", (dialog, which) -> launchPlayStoreReview())
                        .create()
                        .show();
            } else {
                String result = mGameResults[index];
                switch (result) {
                    case "$0.01":
                        subtractOneTicketFromDB();
                        addToUserBalance(0.01);
                        break;
                    case "$0.02":
                        subtractOneTicketFromDB();
                        addToUserBalance(0.02);
                        break;
                    case "$0.03":
                        subtractOneTicketFromDB();
                        addToUserBalance(0.03);
                        break;
                    case "$5":
                        subtractOneTicketFromDB();
                        addToWinnerListInDB(5);
                        break;
                    case "$10":
                        subtractOneTicketFromDB();
                        addToWinnerListInDB(10);
                        break;
                    case "$15":
                        subtractOneTicketFromDB();
                        addToWinnerListInDB(15);
                        break;
                    case "$20":
                        subtractOneTicketFromDB();
                        addToWinnerListInDB(20);
                        break;
                    case "Loss":
                        subtractOneTicketFromDB();
                        new AlertDialog.Builder(this)
                                .setTitle("Loss!")
                                .setMessage("Please try again!")
                                .setPositiveButton("Ok", (dialog, which) -> dialog.dismiss())
                                .create()
                                .show();
                        break;
                    case "Retry":
                        new AlertDialog.Builder(this)
                                .setTitle("Retry!")
                                .setMessage("No tickets were used. Please try again!")
                                .setPositiveButton("Ok", (dialog, which) -> dialog.dismiss())
                                .create()
                                .show();
                        break;
                }
                launchPlayStoreReview();
            }
            if (mNumberOfUsableTickets == 0) {
                playButton.setEnabled(false);
                luckyWheelView.setTouchEnabled(false);
            }
        });
    }

    private void addToWinnerListInDB(int moneyWon) {
        Map<String, String> userData = new HashMap<>();
        userData.put("MoneyWon", String.valueOf(moneyWon));
        userData.put("displayName", mFirebaseUser.getDisplayName());
        userData.put("UID", mFirebaseUser.getUid());
        userData.put("email", mFirebaseUser.getEmail());
        userData.put("NotFromLeftInCode", "true");
        mLeaderboardsDatabase.child("WinnersUnpaid").push().setValue(userData);

        mLeaderboardsDatabase
                .child(mGameRisk)
                .child(Objects.requireNonNull(mFirebaseUser.getDisplayName()))
                .setValue("");//for Leaderboard fragment

        new AlertDialog.Builder(this)
                .setTitle("Congratulations!")
                .setMessage("You won $" + moneyWon + "!!!" + "\n\n" + "You can see your winnings in the leaderboards and an email will be sent to you shortly.")
                .setPositiveButton("Ok", (dialog, which) -> launchPlayStoreReview())
                .create()
                .show();
    }

    private void addToUserBalance(double value) {
        mUsersDatabase.child(mFirebaseUser.getUid()).child("totalMoneyEarned").setValue(mMoneyUserHas + value);
        new AlertDialog.Builder(this)
                .setTitle("Congratulations!")
                .setMessage("You won $" + value + "!!!")
                .setPositiveButton("Ok", (dialog, which) -> launchPlayStoreReview())
                .create()
                .show();
    }

    private void subtractOneTicketFromDB() {
        mUsersDatabase.child(mFirebaseUser.getUid())
                .child("usableTickets")
                .setValue(mNumberOfUsableTickets - 1);
        String updatedUsableTickets = String.format(Locale.ENGLISH, "%,d", mNumberOfUsableTickets - 1);
        String usableTickets = "Usable tickets: " + updatedUsableTickets;
        mTicketsTV.setText(usableTickets);
    }

    private void fill20dollarList() {
        for (int i = 0; i < 12; i++) {
            LuckyItem luckyItem = new LuckyItem();
            if (i == 0 || i == 3 || i == 6 || i == 9) {//loser pie slices
                luckyItem.topText = "Loss";
                luckyItem.color = getColor(R.color.red);
                luckyItem.icon = R.drawable.red_x;
                mGameResults[i] = "Loss";
                mNumberGenerator.addNumber(i, 0.6d / 4.0d);//60% chance of getting a loss split between 4 pie slices
            } else if (i == 1) {//winner pie slice
                luckyItem.topText = "Win $0.01";
                luckyItem.color = getColor(R.color.light_green);
                luckyItem.icon = R.drawable.money_48;
                mGameResults[i] = "$0.01";
                mNumberGenerator.addNumber(i, 0.05d);//5% chance
            } else if (i == 10) {//winner pie slice
                luckyItem.topText = "Win $20";
                luckyItem.color = getColor(R.color.green);
                luckyItem.icon = R.drawable.money_48;
                mGameResults[i] = "$20";
                mNumberGenerator.addNumber(i, 0.000001d);//.00001% chance to win $20
            } else {//ticket pie slices
                luckyItem.topText = "Retry";
                if (lastWasWhite) {
                    luckyItem.color = 0xffFFF3E0;
                    lastWasWhite = false;
                } else {
                    lastWasWhite = true;
                }
                luckyItem.icon = R.drawable.ticket;
                mGameResults[i] = "Retry";
                mNumberGenerator.addNumber(i, 0.05d);//5% * 6 = 30% chance
            }
            mLuckyItems.add(luckyItem);
        }
    }

    private void fill15dollarList() {
        for (int i = 0; i < 12; i++) {
            LuckyItem luckyItem = new LuckyItem();
            if (i == 0 || i == 3 || i == 6) {//loser pie slices
                luckyItem.topText = "Loss";
                luckyItem.color = getColor(R.color.red);
                luckyItem.icon = R.drawable.red_x;
                mGameResults[i] = "Loss";
                mNumberGenerator.addNumber(i, 0.5d / 3.0d);//50% chance of getting a loss split between 3 pie slices
            } else if (i == 1) {//winner pie slice
                luckyItem.topText = "Win $0.01";
                luckyItem.color = getColor(R.color.light_green);
                luckyItem.icon = R.drawable.money_48;
                mGameResults[i] = "$0.01";
                mNumberGenerator.addNumber(i, 0.05d);//5% chance
            } else if (i == 10) {//winner pie slice
                luckyItem.topText = "Win $15";
                luckyItem.color = getColor(R.color.green);
                luckyItem.icon = R.drawable.money_48;
                mGameResults[i] = "$15";
                mNumberGenerator.addNumber(i, 0.000001d);//.00001% chance to win $15
            } else {//ticket pie slices
                luckyItem.topText = "Retry";
                if (lastWasWhite) {
                    luckyItem.color = 0xffFFF3E0;
                    lastWasWhite = false;
                } else {
                    lastWasWhite = true;
                }
                luckyItem.icon = R.drawable.ticket;
                mGameResults[i] = "Retry";
                mNumberGenerator.addNumber(i, 0.05d);//5% * 7 = 35% chance
            }
            mLuckyItems.add(luckyItem);
        }
    }

    private void fill10dollarList() {
        for (int i = 0; i < 12; i++) {
            LuckyItem luckyItem = new LuckyItem();
            if (i == 0 || i == 3) {//loser pie slices
                luckyItem.topText = "Loss";
                luckyItem.color = getColor(R.color.red);
                luckyItem.icon = R.drawable.red_x;
                mGameResults[i] = "Loss";
                mNumberGenerator.addNumber(i, 0.6d / 2.0d);//60% chance of getting a loss split between 3 pie slices
            } else if (i == 1) {//winner pie slice
                luckyItem.topText = "Win $0.01";
                luckyItem.color = getColor(R.color.light_green);
                luckyItem.icon = R.drawable.money_48;
                mGameResults[i] = "$0.01";
                mNumberGenerator.addNumber(i, 0.05d);//5% chance
            } else if (i == 4) {//winner pie slice
                luckyItem.topText = "Win $0.02";
                luckyItem.color = getColor(R.color.light_green);
                luckyItem.icon = R.drawable.money_48;
                mGameResults[i] = "$0.02";
                mNumberGenerator.addNumber(i, 0.05d);//5% chance
            } else if (i == 10) {//winner pie slice
                luckyItem.topText = "Win $10";
                luckyItem.color = getColor(R.color.green);
                luckyItem.icon = R.drawable.money_48;
                mGameResults[i] = "$10";
                mNumberGenerator.addNumber(i, 0.000001d);//.00001% chance to win $10
            } else {//ticket pie slices
                luckyItem.topText = "Retry";
                if (lastWasWhite) {
                    luckyItem.color = 0xffFFF3E0;
                    lastWasWhite = false;
                } else {
                    lastWasWhite = true;
                }
                luckyItem.icon = R.drawable.ticket;
                mGameResults[i] = "Retry";
                mNumberGenerator.addNumber(i, 0.05d);//5% * 7 = 35% chance
            }
            mLuckyItems.add(luckyItem);
        }
    }

    private void fill5dollarList() {
        for (int i = 0; i < 12; i++) {
            LuckyItem luckyItem = new LuckyItem();
            if (i == 0) {//loser pie slices
                luckyItem.topText = "Loss";
                luckyItem.color = getColor(R.color.red);
                luckyItem.icon = R.drawable.red_x;
                mGameResults[i] = "Loss";
                mNumberGenerator.addNumber(i, 0.5d);//50% chance of getting a loss
            } else if (i == 1) {//winner pie slice
                luckyItem.topText = "Win $0.01";
                luckyItem.color = getColor(R.color.light_green);
                luckyItem.icon = R.drawable.money_48;
                mGameResults[i] = "$0.01";
                mNumberGenerator.addNumber(i, 0.4d);//40% chance
            } else if (i == 4) {//winner pie slice
                luckyItem.topText = "Win $0.02";
                luckyItem.color = getColor(R.color.light_green);
                luckyItem.icon = R.drawable.money_48;
                mGameResults[i] = "$0.02";
                mNumberGenerator.addNumber(i, 0.05d);//5% chance
            } else if (i == 7) {//winner pie slice
                luckyItem.topText = "Win $0.03";
                luckyItem.color = getColor(R.color.light_green);
                luckyItem.icon = R.drawable.money_48;
                mGameResults[i] = "$0.03";
                mNumberGenerator.addNumber(i, 0.05d);//5% chance
            } else if (i == 10) {//winner pie slice
                luckyItem.topText = "Win $5";
                luckyItem.color = getColor(R.color.green);
                luckyItem.icon = R.drawable.money_48;
                mGameResults[i] = "$5";
                mNumberGenerator.addNumber(i, 0.000001d);//.00001% chance to win $10
            } else {//ticket pie slices
                luckyItem.topText = "Retry";
                if (lastWasWhite) {
                    luckyItem.color = 0xffFFF3E0;
                    lastWasWhite = false;
                } else {
                    lastWasWhite = true;
                }
                luckyItem.icon = R.drawable.ticket;
                mGameResults[i] = "Retry";
                mNumberGenerator.addNumber(i, 0.05d);//5% * 7 = 35% chance
            }
            mLuckyItems.add(luckyItem);
        }
    }

    private void launchPlayStoreReview() {
        if (getSharedPreferences(mDeviceDefaults, MODE_PRIVATE).getBoolean("PREF_REVIEW_LAUNCHED", false)) {
            return;
        }
        ReviewManager manager = ReviewManagerFactory.create(this);
        manager.requestReviewFlow()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // We can get the ReviewInfo object
                        ReviewInfo reviewInfo = task.getResult();
                        // and use it to request a review.
                        Task<Void> flow = manager.launchReviewFlow(mActivity, reviewInfo);
                        flow.addOnCompleteListener(task1 -> {
                            // The flow has finished. The API does not indicate whether the user
                            // reviewed or not, or even whether the review dialog was shown. Thus, no
                            // matter the result, we continue our app flow.
                            // We log the result.
                            Log.d(TAG, "Review flow result: " + task1.isSuccessful());
                            getSharedPreferences(mDeviceDefaults, MODE_PRIVATE).edit().putBoolean("PREF_REVIEW_LAUNCHED", true).apply();
                        });
                    }
                });
    }

    private void initializeDatabaseListeners() {
        ValueEventListener raffleValidListener = new ValueEventListener() {// Read from the database whenever there's a change
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                isNotValidRaffle = dataSnapshot.child(mBundle.getString("DatabaseRef")).hasChild("isInValid");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        };
        mLeaderboardsDatabase.addValueEventListener(raffleValidListener);//to check if raffle is valid

        ValueEventListener userListener = new ValueEventListener() {
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
                    mTicketsTV.setText(usableTickets);
                }

                if (dataSnapshot.child(mFirebaseUser.getUid()).child("totalMoneyEarned").getValue(Double.class) != null) {
                    //noinspection ConstantConditions
                    mMoneyUserHas = dataSnapshot
                            .child(mFirebaseUser.getUid())
                            .child("totalMoneyEarned")
                            .getValue(Double.class);//Objects.requireNonNull() did not work
                    String moneyUserHas = String.format(Locale.ENGLISH, "%,.2f", mMoneyUserHas);
                    String totalMoneyEarned = "Total money earned: $" + moneyUserHas;
                    mMoneyTV.setText(totalMoneyEarned);
                }
                Log.d(TAG, "Usable Tickets Value is: " + mNumberOfUsableTickets);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        };
        mUsersDatabase.addValueEventListener(userListener);//for users and tickets earned
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAdView != null) {
            mAdView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAdView != null) {
            mAdView.destroy();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mAdView != null) {
            mAdView.pause();
        }
    }
}