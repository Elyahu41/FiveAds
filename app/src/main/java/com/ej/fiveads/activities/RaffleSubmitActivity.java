package com.ej.fiveads.activities;

import static com.ej.fiveads.activities.MainActivity.mDeviceDefaults;
import static com.ej.fiveads.classes.RaffleData.MONTHLY;
import static com.ej.fiveads.classes.RaffleData.WEEKLY;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class RaffleSubmitActivity extends AppCompatActivity {

    private int mUserSelectedTicketsToSubmit;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mLeaderboardsDatabase;
    private DatabaseReference mTicketsDatabase;
    private int mNumberOfUsableTickets;
    private int mTicketsAlreadySubmitted;
    private TextView mTicketsTV;
    private String mCurrentLeaderboardDate;
    private Bundle mBundle;
    private final String TAG = "RaffleSubmitActivity";
    private TextView mTicketsAlreadySubmittedTV;
    private SharedPreferences mSharedPreferences;
    private CountDownTimer timer;
    private int raffleType;
    private boolean isNotValidRaffle = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_raffle_submit);
        Objects.requireNonNull(getSupportActionBar()).hide();
        Calendar calendar = Calendar.getInstance();
        mSharedPreferences = getSharedPreferences(mDeviceDefaults, MODE_PRIVATE);

        TextView title = findViewById(R.id.titleTextView);
        ImageView imageView = findViewById(R.id.submitImage);
        mTicketsTV = findViewById(R.id.numberOfTicketsTextView);
        mTicketsAlreadySubmittedTV = findViewById(R.id.numberOfTicketsSubmittedTextView);
        Button minus1 = findViewById(R.id.buttonMinus1);
        EditText editText = findViewById(R.id.editText);
        Button plus1 = findViewById(R.id.buttonPlus1);
        Button submitButton = findViewById(R.id.submitButton);

        mBundle = getIntent().getExtras();
        if (mBundle != null) {
            title.setText(mBundle.getString("Title"));
            imageView.setImageResource(mBundle.getInt("Image"));
            raffleType = mBundle.getInt("RaffleType");
        }

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance();

        if (firebaseAuth.getCurrentUser() != null) {
            mFirebaseUser = firebaseAuth.getCurrentUser();
        }

        if (mFirebaseUser != null) {
            mTicketsDatabase = database.getReference("Users");
            mLeaderboardsDatabase = database.getReference("Leaderboards");
            if (raffleType != WEEKLY) {
                mCurrentLeaderboardDate = "Leaderboards" +
                        calendar.get(Calendar.YEAR) +
                        calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.ENGLISH);//Final String should look like Leaderboards2021Oct
            } else {
                while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
                    calendar.add(Calendar.DATE, 1);
                }
                mCurrentLeaderboardDate = "Leaderboards" +
                        calendar.get(Calendar.YEAR) +
                        calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.ENGLISH) +
                        calendar.get(Calendar.DAY_OF_MONTH);//Final String should look like Leaderboards2021Oct200
            }
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
            mUserSelectedTicketsToSubmit = mNumberOfUsableTickets;
            editText.setText(String.valueOf(mUserSelectedTicketsToSubmit));
        });

        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (!editText.getText().toString().isEmpty()) {
                mUserSelectedTicketsToSubmit = Integer.parseInt(editText.getText().toString());
            }
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
            if (isNotValidRaffle) {
                new AlertDialog.Builder(this)
                        .setTitle("Invalid Raffle")
                        .setMessage("This raffle is not currently ongoing. Please update the app to see new raffles or choose another raffle.")
                        .setPositiveButton("Ok", (dialog, which) -> launchPlayStoreReview())
                        .create()
                        .show();
            } else {
                if (!editText.getText().toString().isEmpty()) {
                    mUserSelectedTicketsToSubmit = Integer.parseInt(editText.getText().toString());
                }
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
                    mTicketsTV.setText(usableTickets);
                    String message;
                    if (mUserSelectedTicketsToSubmit == 1) {
                        message = "You have submitted " + mUserSelectedTicketsToSubmit + " ticket!";
                    } else {
                        message = "You have submitted " + mUserSelectedTicketsToSubmit + " tickets!";
                    }
                    new AlertDialog.Builder(this)
                            .setTitle("Tickets Submitted!")
                            .setMessage(message)
                            .setPositiveButton("Ok", (dialog, which) -> launchPlayStoreReview())
                            .create()
                            .show();
                }
            }
        });
        startCountdownTimer();
    }

    private void startCountdownTimer() {
        TextView timer = findViewById(R.id.remainingTimeTextView);
        Calendar calendar = Calendar.getInstance();
        Calendar calendar2 = (Calendar) calendar.clone();
        if (raffleType == WEEKLY) {
            while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                calendar.add(Calendar.DATE, 1);
            }
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            this.timer = new CountDownTimer(calendar.getTimeInMillis() - calendar2.getTimeInMillis(),1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    long days = TimeUnit.MILLISECONDS.toDays(millisUntilFinished);
                    millisUntilFinished -= TimeUnit.DAYS.toMillis(days);
                    long hour = TimeUnit.MILLISECONDS.toHours(millisUntilFinished);
                    millisUntilFinished -= TimeUnit.HOURS.toMillis(hour);
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished);
                    millisUntilFinished -= TimeUnit.MINUTES.toMillis(minutes);
                    long seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished);
                    String timeTillEndOfTheWeek = hour + ":" + minutes + ":" + seconds;
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                    try {
                        timeTillEndOfTheWeek = sdf.format(Objects.requireNonNull(sdf.parse(timeTillEndOfTheWeek)));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    String str = days + "d " + timeTillEndOfTheWeek;
                    String finalString = "Raffle ends in " + str;
                    timer.setText(finalString);
                }
                @Override
                public void onFinish() {
                    startActivity(getIntent());
                }
            }.start();
        }
        if (raffleType == MONTHLY) {
            calendar.add(Calendar.MONTH,1);
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            this.timer = new CountDownTimer(calendar.getTimeInMillis() - calendar2.getTimeInMillis(),1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    long days = TimeUnit.MILLISECONDS.toDays(millisUntilFinished);
                    millisUntilFinished -= TimeUnit.DAYS.toMillis(days);
                    long hour = TimeUnit.MILLISECONDS.toHours(millisUntilFinished);
                    millisUntilFinished -= TimeUnit.HOURS.toMillis(hour);
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished);
                    millisUntilFinished -= TimeUnit.MINUTES.toMillis(minutes);
                    long seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished);
                    String timeTillEndOfTheMonth = hour + ":" + minutes + ":" + seconds;
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                    try {
                        timeTillEndOfTheMonth = sdf.format(Objects.requireNonNull(sdf.parse(timeTillEndOfTheMonth)));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    String str = days + "d " + timeTillEndOfTheMonth;
                    String finalString = "Raffle ends in " + str;
                    timer.setText(finalString);
                }
                @Override
                public void onFinish() {
                    startActivity(getIntent());
                }
            }.start();
        }
    }

    @Override
    protected void onPause() {
        if (timer != null) {
            timer.cancel();
        }
        super.onPause();
    }

    private void launchPlayStoreReview() {
       // if (!mSharedPreferences.getBoolean("hasBeenAskedToReview", false)) {
        ReviewManager manager = ReviewManagerFactory.create(this);
        Task<ReviewInfo> request = manager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {// We can get the ReviewInfo object
                ReviewInfo reviewInfo = task.getResult();
                Task<Void> flow = manager.launchReviewFlow(this, reviewInfo);
                flow.addOnCompleteListener(task2 -> {
                    if (task2.isSuccessful()) {
                        Log.d(TAG, "Review Successful");//TODO test
                    }
                });
            } else {// There was some problem, log or handle the error code.
                Log.d(TAG, "Error: " + Objects.requireNonNull(task.getException()).getMessage());
            }
        });
            //mSharedPreferences.edit().putBoolean("hasBeenAskedToReview", true).apply();
       // }
    }

    private void initializeDatabaseListeners() {
        ValueEventListener submittedTicketsListener = new ValueEventListener() {// Read from the database whenever there's a change
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.child(mBundle.getString("DatabaseRef")).child(mCurrentLeaderboardDate).hasChild("isInValid")) {
                    if (dataSnapshot.child(mBundle.getString("DatabaseRef")).child(mCurrentLeaderboardDate)
                            .child(mFirebaseUser.getUid())
                            .child("submittedTickets")
                            .getValue(Integer.class) != null) {
                        //noinspection ConstantConditions
                        mTicketsAlreadySubmitted = dataSnapshot.child(mBundle.getString("DatabaseRef")).child(mCurrentLeaderboardDate)
                                .child(mFirebaseUser.getUid())
                                .child("submittedTickets")
                                .getValue(Integer.class);//Objects.requireNonNull() did not work
                        mTicketsAlreadySubmittedTV.setText(String.format(Locale.getDefault(), "Tickets already submitted: %d", mTicketsAlreadySubmitted));
                    }
                } else {
                    isNotValidRaffle = true;
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
                    mTicketsTV.setText(usableTickets);
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