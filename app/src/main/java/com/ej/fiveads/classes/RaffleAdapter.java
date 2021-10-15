package com.ej.fiveads.classes;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.ej.fiveads.R;
import com.ej.fiveads.activities.RaffleSubmitActivity;

import java.util.List;

public class RaffleAdapter extends RecyclerView.Adapter<RaffleAdapter.RaffleViewHolder> {

    private final List< RaffleData > mRaffleList;
    private final Context mContext;

    public RaffleAdapter(Context mContext, List<RaffleData> mRaffleList) {
        this.mContext = mContext;
        this.mRaffleList = mRaffleList;
    }

    @NonNull
    @Override
    public RaffleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_item_row, parent, false);
        return new RaffleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RaffleViewHolder holder, int position) {
        holder.mImage.setImageResource(mRaffleList.get(position).getRaffleImage());
        holder.mTitle.setText(mRaffleList.get(position).getRaffleName());
        if (!mRaffleList.get(position).getRaffleName().contains("More Coming Soon!")) {
            holder.mCardView.setOnClickListener(view -> {
                Intent mIntent = new Intent(mContext, RaffleSubmitActivity.class);
                mIntent.putExtra("Title", mRaffleList.get(holder.getAdapterPosition()).getRaffleName());
                mIntent.putExtra("Image", mRaffleList.get(holder.getAdapterPosition()).getRaffleImage());
                mIntent.putExtra("DatabaseRef", mRaffleList.get(holder.getAdapterPosition()).getDatabaseRef());
                mContext.startActivity(mIntent);
            });
        }
    }

    @Override
    public int getItemCount() {
        return mRaffleList.size();
    }

    protected static class RaffleViewHolder extends RecyclerView.ViewHolder {

        private final ImageView mImage;
        private final TextView mTitle;
        private final CardView mCardView;

        public RaffleViewHolder(View itemView) {
            super(itemView);
            mCardView = itemView.findViewById(R.id.cardview);
            mImage = itemView.findViewById(R.id.ivImage);
            mTitle = itemView.findViewById(R.id.tvTitle);
        }
    }
}
