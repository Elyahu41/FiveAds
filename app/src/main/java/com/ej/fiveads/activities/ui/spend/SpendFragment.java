package com.ej.fiveads.activities.ui.spend;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ej.fiveads.R;
import com.ej.fiveads.activities.LeaderboardActivity;
import com.ej.fiveads.classes.RaffleAdapter;
import com.ej.fiveads.classes.RaffleData;
import com.ej.fiveads.databinding.FragmentSpendBinding;

import java.util.ArrayList;
import java.util.List;

public class SpendFragment extends Fragment {

    private FragmentSpendBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //SpendViewModel spendViewModel = new ViewModelProvider(this).get(SpendViewModel.class);

        binding = FragmentSpendBinding.inflate(inflater, container, false);

        Button leaderboard = binding.leaderboardButton;
        leaderboard.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), LeaderboardActivity.class));
        });
        RecyclerView mRecyclerView = binding.recyclerview;
        mRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 1));
        mRecyclerView.setAdapter(new RaffleAdapter(requireContext(), getListOfRaffles()));

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private List<RaffleData> getListOfRaffles() {
        List<RaffleData> mRaffleList = new ArrayList<>();
        mRaffleList.add(new RaffleData(getString(R.string.win_20), R.drawable.twenty_raffle, "20Raffle"));
        mRaffleList.add(new RaffleData("More Coming Soon!", R.drawable.coming_soon, "$"));
        mRaffleList.add(new RaffleData("More Coming Soon!", R.drawable.coming_soon, "$"));//just added this to be able to scroll through the recyclerview, remove when more raffles are added
        return mRaffleList;
    }
}