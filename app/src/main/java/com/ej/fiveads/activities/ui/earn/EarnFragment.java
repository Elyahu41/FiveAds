package com.ej.fiveads.activities.ui.earn;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.MODE_PRIVATE;
import static com.ej.fiveads.activities.MainActivity.mDeviceDefaults;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ej.fiveads.R;
import com.ej.fiveads.classes.BadWordList;
import com.ej.fiveads.classes.Message;
import com.ej.fiveads.classes.MessageAdapter;
import com.ej.fiveads.databinding.FragmentEarnBinding;
import com.ej.fiveads.notifications.DailyNotifications;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class EarnFragment extends Fragment {

    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new FirebaseAuthUIActivityResultContract(), this::onSignInResult);
    public static final int DEFAULT_MAX_ENERGY = 5;
    private static int mNumberOfUsableTickets = 0;
    private int mEnergyAmountAsInt;
    private int mNumberOfTotalTickets = 0;
    private FragmentEarnBinding binding;
    private FirebaseUser mFirebaseUser;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private ValueEventListener mUserListener;
    private FirebaseDatabase mDatabase;
    private DatabaseReference mUsersDatabase;
    private DatabaseReference mMessageDatabase;
    private TextView mTicketAmount;
    private TextView mEnergyAmount;
    private TextView mCountDown;
    private static RewardedAd mRewardedAd;
    private SharedPreferences mSharedPreferences;
    private final String TAG = "EarnFragment";
    private final String mUsersPath = "Users";
    private final String mMessagesPath = "Messages";
    private String mCurrentKeyForTheDay;
    private boolean isAddingTickets = false;
    private CountDownTimer mCountDownTimer;
    private Context mContext;
    private EditText mEditMessage;
    private RecyclerView mRecyclerView;
    private Double mMoneyUserEarned;
    private TextView mMoneyAmount;
    private boolean mIsTimeCorrect;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //EarnViewModel earnViewModel = new ViewModelProvider(this).get(EarnViewModel.class);
        binding = FragmentEarnBinding.inflate(inflater, container, false);
        mContext = getContext();
        initializeAds();
        mSharedPreferences = mContext.getSharedPreferences(mDeviceDefaults, MODE_PRIVATE);

        mFirebaseAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();

        if (mFirebaseAuth.getCurrentUser() != null) {
            mFirebaseUser = mFirebaseAuth.getCurrentUser();
        }

        initializeStateListener();//for user login

        if (mFirebaseUser != null) {
            mUsersDatabase = mDatabase.getReference(mUsersPath);//Users/UID we use the UID for the number of USABLE tickets the user has
            mMessageDatabase = mDatabase.getReference(mMessagesPath);//Messages
            initializeDatabaseListener();//to see the value of total and usable tickets
        }

        mCountDown = binding.countdown;
        mCountDown.setVisibility(View.INVISIBLE);
        mEnergyAmount = binding.amountOfEnergyRemaining;
        mTicketAmount = binding.ticketAmount;
        mMoneyAmount = binding.moneyAmount;

        mRecyclerView = binding.messageRV;
        mRecyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mContext);
        linearLayoutManager.setStackFromEnd(true);
        mRecyclerView.setLayoutManager(linearLayoutManager);

        mEditMessage = binding.editTextMessage;
        FloatingActionButton floatingActionButton = binding.sendMessage;

        updateCurrentEnergy();

        floatingActionButton.setOnClickListener(v -> {

            if (!mEditMessage.getText().toString().isEmpty()) {
                sendMessageToDatabase();

                if (mRewardedAd != null && mEnergyAmountAsInt > 0) {
                    mRewardedAd.show(requireActivity(), rewardItem -> {// show ad and handle the reward.
                        recreateTimer(rewardItem);
                        Log.d(TAG, "The user earned the reward.");
                        int rewardAmount = rewardItem.getAmount();//should be only 1
                        if (rewardItem.getType().equals("Ticket")) {
                            mUsersDatabase.child(mFirebaseUser.getUid()).child("usableTickets").setValue(mNumberOfUsableTickets + rewardAmount);
                            mUsersDatabase.child(mFirebaseUser.getUid()).child("totalTicketsEarned").setValue(mNumberOfTotalTickets + rewardAmount);
                            if (mEnergyAmountAsInt > 0) {
                                mEnergyAmountAsInt--;
                            }
                            String updatedEnergy = "Energy: " + mEnergyAmountAsInt;
                            mEnergyAmount.setText(updatedEnergy);
                            Toast.makeText(mContext, "-1 Energy / +1 Ticket", Toast.LENGTH_SHORT).show();
                            if (mEnergyAmountAsInt == 0) {
                                Toast.makeText(mContext, "You have finished all your dailies! Don't forget to use your tickets in the spend tab!", Toast.LENGTH_SHORT).show();
                            }
                            mSharedPreferences.edit().putInt(mCurrentKeyForTheDay, mEnergyAmountAsInt).apply();
                        }
                        initializeAds();
                    });
                } else {
                    initializeAds();
                    Log.d(TAG, "The rewarded ad wasn't ready yet.");
                }
            }
        });

        checkTimeWithFirebase();

        setNotifications();
        return binding.getRoot();
    }

    private void checkTimeWithFirebase() {
        DatabaseReference mOffsetRef = FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset");
        mOffsetRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getValue(Long.class) != null) {
                    //noinspection ConstantConditions
                    long offset = snapshot.getValue(Long.class);
                    mIsTimeCorrect = offset <= 7_200_000 && offset >= -7_200_000;//two hours
                    showDailyLoginDialog();
                    //Log.d(TAG, "offset is: " + offset + " and isTimeCorrect is: " + mIsTimeCorrect);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(TAG, "error is " + error.getMessage());
            }
        });
    }

    private void showDailyLoginDialog() {
        if (mIsTimeCorrect) {
            int sum = 0;
            Calendar calendar = Calendar.getInstance();
            for (int i = 0; i < 7; i++) {
                calendar.add(Calendar.DATE, -1);
                String key = "DL" + calendar.get(Calendar.DAY_OF_YEAR) + "" + calendar.get(Calendar.YEAR);//DL is for daily login
                if (mSharedPreferences.getInt(key, 0) == 1) {//if the user has logged in that day, add 1 to the sum
                    sum++;
                }
            }
            if (mSharedPreferences.getInt("DL" + mCurrentKeyForTheDay, 0) != 1) {//show the dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle("Daily Login Reward!");
                if (sum == 7) {
                    builder.setMessage("You can earn a reward for every day you login!\n\n" +
                            "Day 1: 1 Ticket\n" +
                            "Day 2: 1 Ticket\n" +
                            "Day 3: 1 Ticket\n" +
                            "Day 4: 1 Ticket\n" +
                            "Day 5: 1 Ticket\n" +
                            "Day 6: 1 Ticket\n" +
                            "Day 7 and onward: $0.01 and 1 Ticket\n\n" +
                            "Collect your reward by clicking the button below!\n\n" +
                            "Your reward for today is $0.01 and 1 Ticket!");
                    builder.setIcon(R.drawable.money_48);
                    builder.setPositiveButton("Collect Reward", (dialog, which) -> {
                        mUsersDatabase.child(mFirebaseUser.getUid()).child("money").setValue(mMoneyUserEarned + 0.01);
                        mUsersDatabase.child(mFirebaseUser.getUid()).child("usableTickets").setValue(mNumberOfUsableTickets + 1);
                        mUsersDatabase.child(mFirebaseUser.getUid()).child("totalTicketsEarned").setValue(mNumberOfTotalTickets + 1);
                        Toast.makeText(mContext, "You have earned $0.01 and 1 Ticket!", Toast.LENGTH_SHORT).show();
                        mSharedPreferences.edit().putInt("DL" + mCurrentKeyForTheDay, 1).apply();
                    });
                } else {
                    builder.setMessage("You can earn a reward for every day you login!\n\n" +
                            "Day 1: 1 Ticket\n" +
                            "Day 2: 1 Ticket\n" +
                            "Day 3: 1 Ticket\n" +
                            "Day 4: 1 Ticket\n" +
                            "Day 5: 1 Ticket\n" +
                            "Day 6: 1 Ticket\n" +
                            "Day 7 and onward: $0.01 and 1 Ticket\n\n" +
                            "Collect your reward by clicking the button below!\n\n" +
                            "Your reward for today is 1 ticket!");
                    builder.setIcon(R.drawable.ticket);
                    builder.setPositiveButton("Collect Reward", (dialog, which) -> {
                        mUsersDatabase.child(mFirebaseUser.getUid()).child("usableTickets").setValue(mNumberOfUsableTickets + 1);
                        mUsersDatabase.child(mFirebaseUser.getUid()).child("totalTicketsEarned").setValue(mNumberOfTotalTickets + 1);
                        Toast.makeText(mContext, "You have earned 1 ticket!", Toast.LENGTH_SHORT).show();
                        mSharedPreferences.edit().putInt("DL" + mCurrentKeyForTheDay, 1).apply();
                    });
                }
                builder.setNeutralButton("No Thanks", (dialog, which) -> dialog.dismiss());
                builder.show();
            }
        }
    }

    private void sendMessageToDatabase() {
        String messageValue = mEditMessage.getText().toString();
        if (!messageValue.isEmpty()) {
            for (String word : BadWordList.words) {
                Pattern rx = Pattern.compile("\\b" + word + "\\b", Pattern.CASE_INSENSITIVE);
                messageValue = rx.matcher(messageValue).replaceAll(new String(new char[word.length()]).replace('\0', '*'));
            }
            DatabaseReference newPost = mMessageDatabase.push();
            newPost.setValue(new Message(mFirebaseUser.getDisplayName(), messageValue, mFirebaseUser.getUid(), ServerValue.TIMESTAMP));
            mEditMessage.setText("");
        }
    }

    private void recreateTimer(RewardItem rewardItem) {
        mCountDownTimer = new CountDownTimer(1000, 1000) {

            int currentTickets = mNumberOfUsableTickets;
            final int FINAL_TICKETS = currentTickets + rewardItem.getAmount();
            final int resetTextColor = mTicketAmount.getTextColors().getDefaultColor();

            @Override
            public void onTick(long millisUntilFinished) {
                isAddingTickets = true;
                mTicketAmount.setTextColor(mContext.getResources().getColor(R.color.green, mContext.getTheme()));
                currentTickets += 1;
                mTicketAmount.setText(String.format("Tickets: %s", String.format(Locale.getDefault(), "%,d", currentTickets)));
            }

            @Override
            public void onFinish() {
                mTicketAmount.setTextColor(resetTextColor);
                mTicketAmount.setText(String.format("Tickets: %s", String.format(Locale.getDefault(), "%,d", FINAL_TICKETS)));
                isAddingTickets = false;
            }
        };
    }

    private void initializeAds() {//TODO add resizable ads
        MobileAds.initialize(mContext, initializationStatus -> {
                    RewardedAd.load(mContext,
                            mContext.getString(R.string.admob_main_ad_id),// replace with "ca-app-pub-3940256099942544/5224354917" for development
                            new AdRequest.Builder().build(),
                            new RewardedAdLoadCallback() {
                                @Override
                                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {// Handle the error.
                                    Log.d(TAG, loadAdError.getMessage());
                                    mRewardedAd = null;
                                }

                                @Override
                                public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                                    mRewardedAd = rewardedAd;
                                    mRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                                        @Override
                                        public void onAdShowedFullScreenContent() {// Called when ad is shown.
                                            Log.d(TAG, "Ad was shown.");
                                        }

                                        @Override
                                        public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) { // Called when ad fails to show.
                                            Log.d(TAG, "Ad failed to show.");
                                        }

                                        @Override
                                        public void onAdDismissedFullScreenContent() {// Called when ad is dismissed.
                                            Log.d(TAG, "Ad was dismissed.");
                                            mRewardedAd = null;// Set the ad reference to null so you don't show the ad a second time.
                                            if (mEnergyAmountAsInt != 0) {
                                                initializeAds();
                                            }
                                            mCountDownTimer.start();
                                        }
                                    });
                                    Log.d(TAG, "Rewarded Ad was loaded.");
                                }
                            });
        });
    }

    private void updateCurrentEnergy() {
        Calendar calendar = Calendar.getInstance();
        mCurrentKeyForTheDay = "" + calendar.get(Calendar.DAY_OF_YEAR) + "" + calendar.get(Calendar.YEAR);
        mEnergyAmountAsInt = mSharedPreferences.getInt(mCurrentKeyForTheDay, DEFAULT_MAX_ENERGY);//probably should save in server or somewhere else
        String currentEnergy = "Energy: " + mEnergyAmountAsInt;
        mEnergyAmount.setText(currentEnergy);
    }

    private void startCountdownTimer() {
        mCountDown.setVisibility(View.VISIBLE);
        Calendar calendar = Calendar.getInstance();
        Calendar calendar2 = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY,0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.add(Calendar.DATE,1);
        new CountDownTimer(calendar.getTimeInMillis() - calendar2.getTimeInMillis(),1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long hour = TimeUnit.MILLISECONDS.toHours(millisUntilFinished);
                millisUntilFinished -= TimeUnit.HOURS.toMillis(hour);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished);
                millisUntilFinished -= TimeUnit.MINUTES.toMillis(minutes);
                long seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished);
                String timeTillMidnight = hour + ":" + minutes + ":" + seconds;
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                try {
                    timeTillMidnight = sdf.format(Objects.requireNonNull(sdf.parse(timeTillMidnight)));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                String s = "Reset: " + timeTillMidnight;
                mCountDown.setText(s);
            }
            @Override
            public void onFinish() {
                startActivity(requireActivity().getIntent());
            }
        }.start();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateCurrentEnergy();
        if (mEnergyAmountAsInt <= 0) {//should never be less than 0, but it covers edge cases
            startCountdownTimer();
        }
        if (!isAddingTickets) {
            mTicketAmount.setText(String.format("Tickets: %s", String.format(Locale.getDefault(), "%,d", mNumberOfUsableTickets)));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
        if (mUsersDatabase != null) {
            mUsersDatabase.addValueEventListener(mUserListener);//not sure if needed
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        if (mUsersDatabase != null) {
            mUsersDatabase.removeEventListener(mUserListener);//not sure if needed
        }
    }

    private void initializeDatabaseListener() {
        mUserListener = new ValueEventListener() {// Read from the database whenever there's a change
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(mFirebaseUser.getUid()).child("usableTickets").getValue(Integer.class) != null) {
                    //noinspection ConstantConditions
                    mNumberOfUsableTickets = dataSnapshot.child(mFirebaseUser.getUid()).child("usableTickets").getValue(Integer.class);//Objects.requireNonNull() did not work
                }

                if (dataSnapshot.child(mFirebaseUser.getUid()).child("totalTicketsEarned").getValue(Integer.class) != null) {
                    //noinspection ConstantConditions
                    mNumberOfTotalTickets = dataSnapshot.child(mFirebaseUser.getUid()).child("totalTicketsEarned").getValue(Integer.class);
                }

                if (dataSnapshot.child(mFirebaseUser.getUid()).child("totalMoneyEarned").getValue(Double.class) != null) {
                    mMoneyUserEarned = dataSnapshot.child(mFirebaseUser.getUid()).child("totalMoneyEarned").getValue(Double.class);
                }

                if (!isAddingTickets) {
                    mTicketAmount.setText(String.format("Tickets: %s", String.format(Locale.getDefault(), "%,d", mNumberOfUsableTickets)));
                }
                mMoneyAmount.setText(String.format("Money: $%s", String.format(Locale.getDefault(), "%,.2f", mMoneyUserEarned)));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        };
        mUsersDatabase.addValueEventListener(mUserListener);

        //get all messages from database and display them in the recycler view
        ValueEventListener messageListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    ArrayList<Message> messages = new ArrayList<>();
                    for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                        try {
                            String username = (String) messageSnapshot.child("a").getValue();//accidentally minified these values
                            String content = (String) messageSnapshot.child("b").getValue();
                            String userId = (String) messageSnapshot.child("c").getValue();
                            Long timestamp = (Long) messageSnapshot.child("d").getValue();
                            messages.add(new Message(username, content, userId, timestamp));
                        } catch (Exception e) {
                            try {
                                String username = (String) messageSnapshot.child("username").getValue();
                                String content = (String) messageSnapshot.child("content").getValue();
                                String userId = (String) messageSnapshot.child("uid").getValue();
                                Long timestamp = (Long) messageSnapshot.child("timestamp").getValue();
                                messages.add(new Message(username, content, userId, timestamp));
                            } catch (Exception e1) {
                                e.printStackTrace();
                                e1.printStackTrace();
                                messages.add(new Message("Error", "Error reading message", "0", System.currentTimeMillis()));
                            }
                        }
                    }
                    new Thread(() -> {
                        Long smallestTimestamp = 0L;
                        for (Message message : messages) {
                            if (message.getTimestampLong() == null) {
                                continue;
                            }
                            if (message.getTimestampLong() < smallestTimestamp || smallestTimestamp == 0) {
                                smallestTimestamp = message.getTimestampLong();
                            }
                            //delete old messages from database if they are older than a week
                            if (System.currentTimeMillis() - smallestTimestamp > 604800000) {
                                mMessageDatabase.removeValue();
                            }
                        }
                    }).start();
                    mRecyclerView.setAdapter(new MessageAdapter(mContext, messages));
                    mRecyclerView.smoothScrollToPosition(messages.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        };
        mMessageDatabase.addValueEventListener(messageListener);
    }

    private void initializeStateListener() {
        mAuthStateListener = firebaseAuth -> {
            FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
            if (firebaseUser == null) {
                launchFireBaseSignIn();
            }
        };
    }

    private void launchFireBaseSignIn() {
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build(),
                new AuthUI.IdpConfig.FacebookBuilder().build(),
                new AuthUI.IdpConfig.TwitterBuilder().build(),
                new AuthUI.IdpConfig.GitHubBuilder().build());

        Intent signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setTosAndPrivacyPolicyUrls(
                        "https://elyahu41.github.io/5adsTOS.html",
                        "https://elyahu41.github.io/5adsPrivacyPolicy.html")
                .build();
        signInLauncher.launch(signInIntent);
    }

    private void onSignInResult(FirebaseAuthUIAuthenticationResult result) {
        if (result.getResultCode() == RESULT_OK) {// Successfully signed in
            mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if (mFirebaseUser != null) {
                mUsersDatabase = mDatabase.getReference(mUsersPath);
                mMessageDatabase = mDatabase.getReference(mMessagesPath);
                initializeDatabaseListener();//to see the value of total and usable tickets
                alertUserToChangeDisplayName();
            }
        } else {
            if (result.getIdpResponse() != null) {
                Log.w(TAG, result.getIdpResponse().getError());
                Log.w(TAG, String.valueOf(Objects.requireNonNull(result.getIdpResponse().getError()).getErrorCode()));
            }
        }
    }

    private void setNotifications() {
        if (!mSharedPreferences.getBoolean("notificationsSet", false)) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 14);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            if (calendar.getTime().compareTo(new Date()) < 0) {
                calendar.add(Calendar.DATE, 1);
            }
            PendingIntent dailyPendingIntent = PendingIntent.getBroadcast(mContext, 0,
                    new Intent(mContext, DailyNotifications.class), PendingIntent.FLAG_IMMUTABLE);
            AlarmManager am = (AlarmManager) mContext.getSystemService(ALARM_SERVICE);
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 0, dailyPendingIntent);
            mSharedPreferences.edit().putBoolean("notificationsSet", true).apply();
        }
    }

    private void alertUserToChangeDisplayName() {
        if (!mSharedPreferences.getBoolean("usernameSet", false)) {
        final EditText edittext = new EditText(mContext);
        edittext.setText(mFirebaseUser.getDisplayName());
        edittext.setGravity(Gravity.CENTER);
        new AlertDialog.Builder(mContext)
                .setTitle("Create a username!")
                .setMessage("Enter a username others can see you by:")
                .setView(edittext)
                .setPositiveButton("Ok", (dialog, which) -> {
                    String username = edittext.getText().toString();
                    mFirebaseUser.updateProfile(
                            new UserProfileChangeRequest.Builder()
                                    .setDisplayName(username)
                                    .build());
                    showIntroDialog();
                })
                .setNegativeButton("No thanks", (dialog, which) -> {
                    Toast.makeText(mContext, "Change your username in the settings at any time!", Toast.LENGTH_LONG).show();
                    showIntroDialog();
                })
                .setCancelable(false)
                .create()
                .show();
            mSharedPreferences.edit().putBoolean("usernameSet", true).apply();
        }
    }

    private void showIntroDialog() {
        new AlertDialog.Builder(mContext)
                .setTitle("Introduction")
                .setMessage("Welcome to the 5 Chats app! The idea behind this app is extremely simple and there are only 2 steps involved.\n\n" +
                        "Step 1. Earn tickets by chatting and watching ads (Max 5 times a day). Each chat gives you 1 ticket. These tickets can be used to " +
                        "potentially win cash or prizes.\n\n" +
                        "Step 2. Submit those tickets into lucky wheel! There are multiple types of wheels ongoing in the \"Spend\" tab. " +
                        "There are also daily login rewards that can help you get to your payout faster!\n\n" +
                        "Be considerate and please spread the word about our app!")
                .setPositiveButton("Ok", ((dialog, which) -> {}))
                .setCancelable(false)
                .create()
                .show();
    }
}