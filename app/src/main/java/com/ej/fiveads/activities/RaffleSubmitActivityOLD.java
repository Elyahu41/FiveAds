package com.ej.fiveads.activities;

import androidx.appcompat.app.AppCompatActivity;

public class RaffleSubmitActivityOLD extends AppCompatActivity {
/**
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
    private CountDownTimer timer;
    private int raffleType;
    private boolean isNotValidRaffle = false;
    private boolean mIsTimeCorrect;
    private DatabaseReference mOffsetRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_raffle_submit);
        Objects.requireNonNull(getSupportActionBar()).hide();
        Calendar calendar = Calendar.getInstance();
        mOffsetRef = FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset");

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
                if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
                    calendar.add(Calendar.DATE, 1);
                }
                while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                    calendar.add(Calendar.DATE, 1);
                }
                mCurrentLeaderboardDate = "Leaderboards" +
                        calendar.get(Calendar.YEAR) +
                        calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.ENGLISH) +
                        calendar.get(Calendar.DAY_OF_MONTH);//Final String should look like Leaderboards2021Oct20
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
                if (!mIsTimeCorrect) {
                    new AlertDialog.Builder(this)
                            .setTitle("Your system time is incorrect!")
                            .setMessage("Your system time is off by more than a day! This will cause your tickets to go to the wrong raffle! " +
                                    "Please fix your time before submitting your tickets!")
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
                        int updatedTickets = mUserSelectedTicketsToSubmit + mTicketsAlreadySubmitted;
                        if (updatedTickets >= 0 && updatedTickets < 25) {
                            mTicketsAlreadySubmittedTV.setTextColor(getColor(R.color.red));
                            message += " Please note that you need a minimum of 25 tickets to participate in the raffle.";
                        } else {
                            mTicketsAlreadySubmittedTV.setTextColor(getColor(R.color.green));
                        }
                        new AlertDialog.Builder(this)
                                .setTitle("Tickets Submitted!")
                                .setMessage(message)
                                .setPositiveButton("Ok", (dialog, which) -> launchPlayStoreReview())
                                .create()
                                .show();
                    }
                }
            }
        });
        startCountdownTimer();
    }

    private void startCountdownTimer() {
        TextView timerView = findViewById(R.id.remainingTimeTextView);
        Calendar calendar = Calendar.getInstance();
        Calendar calendar2 = (Calendar) calendar.clone();
        if (raffleType == WEEKLY) {
            if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
                calendar.add(Calendar.DATE, 1);
            }
            while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                calendar.add(Calendar.DATE, 1);
            }
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            Log.d(TAG, "Time till end of raffle: " + (calendar.getTimeInMillis() - calendar2.getTimeInMillis()));
            timer = new CountDownTimer(calendar.getTimeInMillis() - calendar2.getTimeInMillis(),1000) {
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
                    timerView.setText(finalString);
                }
                @Override
                public void onFinish() {
                    finish();
                }
            }.start();
        }
        if (raffleType == MONTHLY) {
            calendar.add(Calendar.MONTH,1);
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            timer = new CountDownTimer(calendar.getTimeInMillis() - calendar2.getTimeInMillis(),1000) {
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
                    timerView.setText(finalString);
                }
                @Override
                public void onFinish() {
                    finish();
                }
            }.start();
        }
    }

    @Override
    protected void onPause() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        checkTimeWithFirebase();
        if (timer == null) {
            startCountdownTimer();
        }
        super.onResume();
    }

    private void checkTimeWithFirebase() {
        mOffsetRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getValue(Long.class) != null) {
                    //noinspection ConstantConditions
                    long offset = snapshot.getValue(Long.class);
                    mIsTimeCorrect = offset <= 86_400_000 && offset >= -86_400_000;
                    Log.d(TAG, "offset is: " + offset);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(TAG, "error is " + error.toString());
            }
        });
    }

    private void launchPlayStoreReview() {
        ReviewManager manager = ReviewManagerFactory.create(this);
        Task<ReviewInfo> request = manager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {// We can get the ReviewInfo object
                ReviewInfo reviewInfo = task.getResult();
                Task<Void> flow = manager.launchReviewFlow(this, reviewInfo);
                flow.addOnCompleteListener(task2 -> {
                    if (task2.isSuccessful()) {
                        Log.d(TAG, "Review Successful");//FIXME: This is not working
                    }
                });
            } else {// There was some problem, log or handle the error code.
                Log.d(TAG, "Error: " + Objects.requireNonNull(task.getException()).getMessage());
            }
        });
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
                        if (mTicketsAlreadySubmitted > 0 && mTicketsAlreadySubmitted < 25) {
                            mTicketsAlreadySubmittedTV.setTextColor(getColor(R.color.red));
                        }
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
        checkTimeWithFirebase();
    }
            **/
}