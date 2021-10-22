package com.ej.fiveads.activities.ui.spend;

import android.content.Intent;
import android.os.Bundle;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class SpendFragment extends Fragment {

    private FragmentSpendBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //SpendViewModel spendViewModel = new ViewModelProvider(this).get(SpendViewModel.class);

        binding = FragmentSpendBinding.inflate(inflater, container, false);

        RecyclerView mRecyclerView = binding.recyclerview;
        mRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        mRecyclerView.setAdapter(new RaffleAdapter(requireContext(), getListOfRaffles()));

        FloatingActionButton leaderboard = binding.leaderboardButton;
        leaderboard.setOnClickListener(v -> startActivity(new Intent(getContext(), LeaderboardActivity.class)));

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private List<RaffleData> getListOfRaffles() {
        List<RaffleData> mRaffleList = new ArrayList<>();
        mRaffleList.add(new RaffleData(getString(R.string.win_1), R.drawable.one_raffle, "1Raffle"));
        mRaffleList.add(new RaffleData(getString(R.string.win_5), R.drawable.five_raffle, "5Raffle"));
        mRaffleList.add(new RaffleData(getString(R.string.win_10), R.drawable.ten_raffle, "10Raffle"));
        mRaffleList.add(new RaffleData(getString(R.string.win_20), R.drawable.twenty_raffle, "20Raffle"));
        mRaffleList.add(new RaffleData(getString(R.string.win_50), R.drawable.fifty_raffle, "50Raffle"));
        mRaffleList.add(new RaffleData("More Coming Soon!", R.drawable.coming_soon, "$"));//$ isn't a valid database character
        mRaffleList.add(new RaffleData("More Coming Soon!", R.drawable.coming_soon, "$"));
        return mRaffleList;
    }
}