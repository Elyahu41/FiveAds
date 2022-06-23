package com.ej.fiveads.activities.ui.redeem;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ej.fiveads.classes.RedeemAdapter;
import com.ej.fiveads.databinding.FragmentRedeemBinding;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RedeemFragment extends Fragment {

    private FragmentRedeemBinding binding;
    private final String TAG = "RedeemFragment";
    private Context mContext;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mUsersDatabase;
    private final ArrayList<NativeAd> mNativeAdArrayList = new ArrayList<>(8);
    private double mUserBalance;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //SpendViewModel spendViewModel = new ViewModelProvider(this).get(SpendViewModel.class);
        binding = FragmentRedeemBinding.inflate(inflater, container, false);

        mContext = getContext();

        RecyclerView mRecyclerView = binding.RedeemRV;
        mRecyclerView.setLayoutManager(new GridLayoutManager(mContext, 1));

        AdLoader adLoader = null;
        if (mContext != null) {
            adLoader = new AdLoader.Builder(mContext, "ca-app-pub-3199244267160640/8510550495")//for development ca-app-pub-3940256099942544/2247696110
                    .forNativeAd(nativeAd -> {// Show the ad.
                        mNativeAdArrayList.add(nativeAd);
                        if (mNativeAdArrayList.size() >= 7) {
                            mRecyclerView.setAdapter(new RedeemAdapter(mContext, getListOfRafflesWithAds(), mUserBalance));
                        }
                    })
                    .withAdListener(new AdListener() {
                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError adError) {// Handle the failure by logging, altering the UI, and so on.
                            Log.d(TAG, adError.getMessage());
                        }
                    })
                    .withNativeAdOptions(new NativeAdOptions.Builder().build())
                    .build();
        }

        if (adLoader != null) {
            adLoader.loadAd(new AdRequest.Builder().build());
            adLoader.loadAd(new AdRequest.Builder().build());
            adLoader.loadAd(new AdRequest.Builder().build());
            adLoader.loadAd(new AdRequest.Builder().build());
            adLoader.loadAd(new AdRequest.Builder().build());
            adLoader.loadAd(new AdRequest.Builder().build());
            adLoader.loadAd(new AdRequest.Builder().build());
        }


        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance();

        if (firebaseAuth.getCurrentUser() != null) {
            mFirebaseUser = firebaseAuth.getCurrentUser();
        }

        if (mFirebaseUser != null) {
            mUsersDatabase = database.getReference("Users");
            initializeDatabaseListeners();//to see the value of money available
        }

        mRecyclerView.setAdapter(new RedeemAdapter(mContext, getListOfRafflesWithAds(), mUserBalance));

        return binding.getRoot();
    }

    private List<Object> getListOfRafflesWithAds() {
        List<Object> mRedeemList = new ArrayList<>();

        mRedeemList.add("$5");
        if (mNativeAdArrayList.size() >= 1) {
            mRedeemList.add(mNativeAdArrayList.get(0));
        }
        mRedeemList.add("$10");
        if (mNativeAdArrayList.size() >= 2) {
            mRedeemList.add(mNativeAdArrayList.get(1));
        }
        mRedeemList.add("$15");
        if (mNativeAdArrayList.size() >= 3) {
            mRedeemList.add(mNativeAdArrayList.get(2));
        }
        mRedeemList.add("$20");
        if (mNativeAdArrayList.size() >= 4) {
            mRedeemList.add(mNativeAdArrayList.get(3));
        }
        mRedeemList.add("$25");
        if (mNativeAdArrayList.size() >= 5) {
            mRedeemList.add(mNativeAdArrayList.get(4));
        }
        mRedeemList.add("$50");
        if (mNativeAdArrayList.size() >= 6) {
            mRedeemList.add(mNativeAdArrayList.get(5));
        }
        mRedeemList.add("$100");
        if (mNativeAdArrayList.size() >= 7) {
            mRedeemList.add(mNativeAdArrayList.get(6));
        }

        return mRedeemList;
    }

    private void initializeDatabaseListeners() {
        ValueEventListener userListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(mFirebaseUser.getUid()).child("totalMoneyEarned").getValue(Double.class) != null) {
                    //noinspection ConstantConditions
                    mUserBalance = dataSnapshot
                            .child(mFirebaseUser.getUid())
                            .child("totalMoneyEarned")
                            .getValue(Double.class);//Objects.requireNonNull() did not work
                    String moneyUserHas = String.format(Locale.ENGLISH, "%,.2f", mUserBalance);
                    String totalMoneyEarned = "Total money available: $" + moneyUserHas;
                    binding.redeemMoneyAvailable.setText(totalMoneyEarned);
                    binding.RedeemRV.setAdapter(new RedeemAdapter(mContext, getListOfRafflesWithAds(), mUserBalance));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        };
        mUsersDatabase.addValueEventListener(userListener);//for users and tickets earned
    }
}
