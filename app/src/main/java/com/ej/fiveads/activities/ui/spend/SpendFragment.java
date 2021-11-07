package com.ej.fiveads.activities.ui.spend;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ej.fiveads.R;
import com.ej.fiveads.activities.LeaderboardActivity;
import com.ej.fiveads.classes.RaffleAdapter;
import com.ej.fiveads.classes.RaffleData;
import com.ej.fiveads.databinding.FragmentSpendBinding;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class SpendFragment extends Fragment {

    private FragmentSpendBinding binding;
    private final ArrayList<NativeAd> mNativeAdArrayList = new ArrayList<>(5);
    private final String TAG = "SpendFragment";

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //SpendViewModel spendViewModel = new ViewModelProvider(this).get(SpendViewModel.class);
        binding = FragmentSpendBinding.inflate(inflater, container, false);
        RecyclerView mRecyclerView = binding.recyclerview;
        mRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 1));

        AdLoader adLoader = new AdLoader.Builder(requireContext(), "ca-app-pub-3199244267160640/8510550495")//for development ca-app-pub-3940256099942544/2247696110
                .forNativeAd(nativeAd -> {// Show the ad.
                    mNativeAdArrayList.add(nativeAd);
                    mRecyclerView.setAdapter(new RaffleAdapter(requireContext(), getListOfRafflesWithAds()));
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError adError) {// Handle the failure by logging, altering the UI, and so on.
                        Log.d(TAG, adError.getMessage());
                    }
                })
                .withNativeAdOptions(new NativeAdOptions.Builder().build())
                .build();

        adLoader.loadAds(new AdRequest.Builder().build(), 5);

        mRecyclerView.setAdapter(new RaffleAdapter(requireContext(), getListOfRafflesWithAds()));

        FloatingActionButton leaderboard = binding.leaderboardButton;
        leaderboard.setOnClickListener(v -> startActivity(new Intent(getContext(), LeaderboardActivity.class)));

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        for (NativeAd nativeAd:mNativeAdArrayList) {
            nativeAd.destroy();
        }
        super.onDestroyView();
        binding = null;
    }

    private List<Object> getListOfRafflesWithAds() {
        List<Object> mRaffleList = new ArrayList<>();
        mRaffleList.add(new RaffleData(getString(R.string.win_1), R.drawable.amazongc, "1Raffle"));
        if (mNativeAdArrayList.size() >= 1) {
            mRaffleList.add(mNativeAdArrayList.get(0));
        }
        mRaffleList.add(new RaffleData(getString(R.string.win_5), R.drawable.amazongc, "5Raffle"));
        if (mNativeAdArrayList.size() >= 2) {
            mRaffleList.add(mNativeAdArrayList.get(1));
        }
        mRaffleList.add(new RaffleData(getString(R.string.win_10), R.drawable.amazongc, "10Raffle"));
        if (mNativeAdArrayList.size() >= 3) {
            mRaffleList.add(mNativeAdArrayList.get(2));
        }
        mRaffleList.add(new RaffleData(getString(R.string.win_20), R.drawable.amazongc, "20Raffle"));
        if (mNativeAdArrayList.size() >= 4) {
            mRaffleList.add(mNativeAdArrayList.get(3));
        }
        mRaffleList.add(new RaffleData(getString(R.string.win_50), R.drawable.amazongc, "50Raffle"));
        if (mNativeAdArrayList.size() >= 5) {
            mRaffleList.add(mNativeAdArrayList.get(4));
        }
        mRaffleList.add(new RaffleData("More \nComing Soon!", R.drawable.coming_soon, "$"));//$ isn't a valid database character
        return mRaffleList;
    }
}