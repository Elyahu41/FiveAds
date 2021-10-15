package com.ej.fiveads.activities.ui.earn;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.MODE_PRIVATE;
import static com.ej.fiveads.activities.MainActivity.mDeviceDefaults;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.ej.fiveads.R;
import com.ej.fiveads.databinding.FragmentEarnBinding;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
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
    private FirebaseUser mFirebaseUser;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private ValueEventListener mTicketListener;
    private FirebaseDatabase mDatabase;
    private DatabaseReference mTicketsDatabase;
    private TextView mTicketAmount;
    private TextView mEnergyAmount;
    private Button mWatchAdButton;
    private FragmentEarnBinding binding;
    private TextView mCountDown;
    private static RewardedAd mRewardedAd;
    private SharedPreferences mSharedPreferences;
    private final String TAG = "MainActivity";
    private final String mTopLevelDatabase = "Users";
    private String mCurrentKeyForTheDay;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        initializeAds();
        //EarnViewModel earnViewModel = new ViewModelProvider(this).get(EarnViewModel.class);

        mFirebaseAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();

        if (mFirebaseAuth.getCurrentUser() != null) {
            mFirebaseUser = mFirebaseAuth.getCurrentUser();
        }

        initializeStateListener();//for user login

        if (mFirebaseUser != null) {
            mTicketsDatabase = mDatabase.getReference(mTopLevelDatabase);//we use the UID for the number of USABLE tickets the user has
            initializeDatabaseListener();//to see the value of total and usable tickets
        }

        mSharedPreferences = requireActivity().getSharedPreferences(mDeviceDefaults, MODE_PRIVATE);
        binding = FragmentEarnBinding.inflate(inflater, container, false);
        mTicketAmount = binding.ticketAmount;
        mEnergyAmount = binding.amountOfEnergyRemaining;
        mWatchAdButton = binding.watchAd;
        mCountDown = binding.countdown;

        updateCurrentEnergy();

        if (mRewardedAd == null) {
            mWatchAdButton.setEnabled(false);
            mWatchAdButton.setText(R.string.ad_loading);
        }

        mWatchAdButton.setOnClickListener(v -> {//TODO configure autoplay all 5 ads //performClick method
            if (mRewardedAd != null) {
                mRewardedAd.show(requireActivity(), rewardItem -> {// Handle the reward.
                    Log.d(TAG, "The user earned the reward.");
                    int rewardAmount = rewardItem.getAmount();//should be only 1
                    String rewardType = rewardItem.getType();//should be Ticket
                    //if (rewardType.equals("Ticket")) {//TODO test
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
                   // }
                });
            } else {
                Toast.makeText(requireContext(), "Ad not ready", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "The rewarded ad wasn't ready yet.");
            }
        });

        return binding.getRoot();
    }

    private void initializeAds() {//TODO find out how to implement mediation
        MobileAds.initialize(requireContext(), initializationStatus -> RewardedAd.load(requireActivity(),
                "ca-app-pub-3940256099942544/5224354917",//TODO replace with ca-app-pub-3199244267160640/4278975683
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
                                mWatchAdButton.setEnabled(false);
                                mWatchAdButton.setText(R.string.ad_loading);
                                initializeAds();
                            }
                        });
                        if (mEnergyAmountAsInt != 0) {
                            mWatchAdButton.setEnabled(true);
                            mWatchAdButton.setText(R.string.watch_ad);
                        }
                        Log.d(TAG, "Ad was loaded.");
                    }
                }));
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
        mTicketAmount.setText(String.valueOf(mNumberOfUsableTickets));
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
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
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
                mTicketAmount.setText(numberOfUsableTickets);
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
            }
        } else {
            if (result.getIdpResponse() != null) {
                Log.w(TAG, result.getIdpResponse().getError());
                Log.w(TAG, String.valueOf(Objects.requireNonNull(result.getIdpResponse().getError()).getErrorCode()));
            }
        }
    }
}