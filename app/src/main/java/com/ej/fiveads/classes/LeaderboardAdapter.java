package com.ej.fiveads.classes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import com.ej.fiveads.R;

import java.util.List;
import java.util.Locale;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder>{

    private final List<UserData> mUserList;
    private final Context mContext;

    public LeaderboardAdapter(List<UserData> userList, Context mContext) {
        mUserList = userList;
        this.mContext = mContext;
    }

    @NonNull
    @Override
    public LeaderboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_user_row, parent, false);
        return new LeaderboardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LeaderboardViewHolder holder, int position) {
        if (mUserList.get(position).getRank() == 1) {
            holder.itemView.setBackground(AppCompatResources.getDrawable(mContext, R.color.gold));
        }
        if (mUserList.get(position).getRank() == 2) {
            holder.itemView.setBackground(AppCompatResources.getDrawable(mContext, R.color.silver));
        }
        if (mUserList.get(position).getRank() == 3) {
            holder.itemView.setBackground(AppCompatResources.getDrawable(mContext, R.color.bronze));
        }
        holder.mUserRank.setText(String.format("%s.", mUserList.get(position).getRank()));
        holder.mUsername.setText(mUserList.get(position).getName());
        holder.mUserTicketsSubmitted.setText(String.format(Locale.getDefault(), "%,d", mUserList.get(position).getTicketsSubmitted()));
    }

    @Override
    public int getItemCount() {
        return mUserList.size();
    }

    protected static class LeaderboardViewHolder extends RecyclerView.ViewHolder {

        private final TextView mUserRank;
        private final TextView mUsername;
        private final TextView mUserTicketsSubmitted;

        public LeaderboardViewHolder(View itemView) {
            super(itemView);
            mUserRank = itemView.findViewById(R.id.userRank);
            mUsername = itemView.findViewById(R.id.usernameEntry);
            mUserTicketsSubmitted = itemView.findViewById(R.id.userTicketsSubmitted);
        }
    }
}
