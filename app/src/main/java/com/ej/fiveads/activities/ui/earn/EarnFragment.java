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
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.ej.fiveads.R;
import com.ej.fiveads.databinding.FragmentEarnBinding;
import com.ej.fiveads.notifications.DailyNotifications;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

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
    private ValueEventListener mTicketListener;
    private FirebaseDatabase mDatabase;
    private DatabaseReference mTicketsDatabase;
    private TextView mTicketAmount;
    private TextView mEnergyAmount;
    private Button mWatchAdButton;
    private TextView mCountDown;
    private static RewardedAd mRewardedAd;
    private SharedPreferences mSharedPreferences;
    private final String TAG = "EarnFragment";
    private final String mTopLevelDatabase = "Users";
    private String mCurrentKeyForTheDay;
    private boolean isAddingTickets = false;
    private CountDownTimer mCountDownTimer;
    private AdView mAdView;
    private Context mContext;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //EarnViewModel earnViewModel = new ViewModelProvider(this).get(EarnViewModel.class);
        binding = FragmentEarnBinding.inflate(inflater, container, false);
        mContext = getContext();
        initializeAds();

        mFirebaseAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();

        if (mFirebaseAuth.getCurrentUser() != null) {
            mFirebaseUser = mFirebaseAuth.getCurrentUser();
        }

        initializeStateListener();//for user login

        if (mFirebaseUser != null) {
            mTicketsDatabase = mDatabase.getReference(mTopLevelDatabase);//we use the User/UID for the number of USABLE tickets the user has
            initializeDatabaseListener();//to see the value of total and usable tickets
        }

        mSharedPreferences = requireActivity().getSharedPreferences(mDeviceDefaults, MODE_PRIVATE);
        mTicketAmount = binding.ticketAmount;
        mEnergyAmount = binding.amountOfEnergyRemaining;
        mWatchAdButton = binding.watchAd;
        mCountDown = binding.countdown;

        updateCurrentEnergy();

        if (mRewardedAd == null) {
            mWatchAdButton.setEnabled(false);
            mWatchAdButton.setText(R.string.ad_loading);
        }

        mWatchAdButton.setOnClickListener(v -> {//TODO configure autoplay all 5 ads #performClick method
            if (mRewardedAd != null) {
                mRewardedAd.show(requireActivity(), rewardItem -> {// Handle the reward.
                    recreateTimer(rewardItem);
                    Log.d(TAG, "The user earned the reward.");
                    int rewardAmount = rewardItem.getAmount();//should be only 5
                    if (rewardItem.getType().equals("Tickets")) {
                        mTicketsDatabase.child(mFirebaseUser.getUid()).child("usableTickets").setValue(mNumberOfUsableTickets + rewardAmount);
                        mTicketsDatabase.child(mFirebaseUser.getUid()).child("totalTicketsEarned").setValue(mNumberOfTotalTickets + rewardAmount);
                        if (mEnergyAmountAsInt > 0) {
                            mEnergyAmountAsInt--;
                        }
                        if (mEnergyAmountAsInt == 0) {
                            mWatchAdButton.setEnabled(false);
                            mWatchAdButton.setText(R.string.come_back_tomorrow);
                        }
                        String updatedEnergy = String.valueOf(mEnergyAmountAsInt);
                        mEnergyAmount.setText(updatedEnergy);
                        mSharedPreferences.edit().putInt(mCurrentKeyForTheDay, mEnergyAmountAsInt).apply();
                    }
                    initializeAds();
                });
            } else {
                mWatchAdButton.setEnabled(false);
                mWatchAdButton.setText(R.string.ad_loading);
                initializeAds();
                Toast.makeText(mContext, "Ad not ready", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "The rewarded ad wasn't ready yet.");
            }
        });

        setNotifications();
        return binding.getRoot();
    }

    private void recreateTimer(RewardItem rewardItem) {
        mCountDownTimer = new CountDownTimer(2500, 500) {

            int currentTickets = mNumberOfUsableTickets;
            final int FINAL_TICKETS = currentTickets + rewardItem.getAmount();
            final int resetTextColor = mTicketAmount.getTextColors().getDefaultColor();

            @Override
            public void onTick(long millisUntilFinished) {
                isAddingTickets = true;
                mTicketAmount.setTextColor(mContext.getResources().getColor(R.color.green, mContext.getTheme()));
                currentTickets += 1;
                mTicketAmount.setText(String.format(Locale.getDefault(), "%,d", currentTickets));
            }

            @Override
            public void onFinish() {
                mTicketAmount.setTextColor(resetTextColor);
                mTicketAmount.setText(String.format(Locale.getDefault(), "%,d", FINAL_TICKETS));
                isAddingTickets = false;
            }
        };
    }

    private void initializeAds() {
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
                                            mWatchAdButton.setEnabled(false);
                                            mWatchAdButton.setText(R.string.ad_loading);
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
                                            mWatchAdButton.setEnabled(false);
                                            mWatchAdButton.setText(R.string.ad_loading);
                                            if (mEnergyAmountAsInt != 0) {
                                                initializeAds();
                                            }
                                            mCountDownTimer.start();
                                        }
                                    });
                                    if (mEnergyAmountAsInt != 0) {
                                        mWatchAdButton.setEnabled(true);
                                        mWatchAdButton.setText(R.string.get_tickets);
                                    }
                                    Log.d(TAG, "Rewarded Ad was loaded.");
                                }
                            });
                    mAdView = binding.adView;
                    mAdView.loadAd(new AdRequest.Builder().build());
                    Log.d(TAG, "Banner Ad was loaded.");
        });
    }

    private void updateCurrentEnergy() {
        Calendar calendar = Calendar.getInstance();
        mCurrentKeyForTheDay = "" + calendar.get(Calendar.DAY_OF_YEAR) + "" + calendar.get(Calendar.YEAR);
        mEnergyAmountAsInt = mSharedPreferences.getInt(mCurrentKeyForTheDay, DEFAULT_MAX_ENERGY);//probably should save in server or somewhere else
        String currentEnergy = String.valueOf(mEnergyAmountAsInt);
        mEnergyAmount.setText(currentEnergy);
    }

    private void startCountdownTimer() {
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
                mCountDown.setText(timeTillMidnight);
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
            mWatchAdButton.setEnabled(false);
            mWatchAdButton.setText(R.string.come_back_tomorrow);
            startCountdownTimer();
        }
        if (!isAddingTickets) {
            mTicketAmount.setText(String.valueOf(mNumberOfUsableTickets));
        }
        if (mAdView != null) {
            mAdView.resume();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mAdView != null) {
            mAdView.destroy();
        }
        binding = null;
    }

    @Override
    public void onPause() {
        if (mAdView != null) {
            mAdView.pause();
        }
        super.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
        if (mTicketsDatabase != null) {
            mTicketsDatabase.addValueEventListener(mTicketListener);//not sure if needed
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        if (mTicketsDatabase != null) {
            mTicketsDatabase.removeEventListener(mTicketListener);//not sure if needed
        }
    }

    private void initializeDatabaseListener() {
        mTicketListener = new ValueEventListener() {// Read from the database whenever there's a change
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
                String numberOfUsableTickets = String.format(Locale.getDefault(),"%,d", mNumberOfUsableTickets);
                if (!isAddingTickets) {
                    mTicketAmount.setText(numberOfUsableTickets);
                }
                Log.d(TAG, "Usable Tickets Value is: " + mNumberOfUsableTickets);
                Log.d(TAG, "Total Tickets Earned Value is: " + mNumberOfTotalTickets);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        };
        mTicketsDatabase.addValueEventListener(mTicketListener);
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
                mTicketsDatabase = mDatabase.getReference(mTopLevelDatabase);
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
                .create()
                .show();
            mSharedPreferences.edit().putBoolean("usernameSet", true).apply();
        }
    }

    private void showIntroDialog() {
        new AlertDialog.Builder(mContext)
                .setTitle("Introduction")
                .setMessage("Welcome to the 5 Ads app! The idea behind this app is extremely simple and there are only 2 steps involved.\n\n" +
                        "Step 1. Earn tickets by watching ads (Max 5 a day). Each ad watched gives you 5 tickets. These tickets can be used to " +
                        "potentially win cash or prizes.\n\n" +
                        "Step 2. Submit those tickets into raffles! There are multiple raffles ongoing each month in the \"Spend\" tab. " +
                        "The more tickets submitted the higher your chances to win are! Try and submit as many tickets as possible! " +
                        "You can also save your tickets up for the next raffle!\n\n" +
                        "The more ads you watch the more our income is, as our income improves, more frequent prizes and raffles will be added! " +
                        "Please spread the word about our app!")
                .setPositiveButton("Ok", ((dialog, which) -> {}))
                .setCancelable(false)
                .create()
                .show();
    }
}