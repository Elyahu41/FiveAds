package com.ej.fiveads.classes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ej.fiveads.R;
import com.google.firebase.auth.FirebaseAuth;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private final ArrayList<Message> list;
    public static final int MESSAGE_TYPE_IN = 1;
    public static final int MESSAGE_TYPE_OUT = 2;
    private final DateFormat dateFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());

    public MessageAdapter(Context context, ArrayList<Message> list) {
        this.context = context;
        this.list = list;
    }

    private class MessageInViewHolder extends RecyclerView.ViewHolder {

        TextView messageTV;
        TextView usernameTV;
        TextView timeTV;
        MessageInViewHolder(final View itemView) {
            super(itemView);
            messageTV = itemView.findViewById(R.id.messageText);
            usernameTV = itemView.findViewById(R.id.usernameText);
            timeTV = itemView.findViewById(R.id.date_text);
        }

        void bind(int position) {
            Message message = list.get(position);
            usernameTV.setText(message.getUsername());
            messageTV.setText(message.getContent());
            if (message.getTimestampLong() != null) {
                timeTV.setText(dateFormat.format(new Date(message.getTimestampLong())));
            }
        }
    }

    private class MessageOutViewHolder extends RecyclerView.ViewHolder {

        TextView messageTV;
        TextView usernameTV;
        TextView timeTV;
        MessageOutViewHolder(final View itemView) {
            super(itemView);
            messageTV = itemView.findViewById(R.id.messageText);
            usernameTV = itemView.findViewById(R.id.usernameText);
            timeTV = itemView.findViewById(R.id.date_text);
        }

        void bind(int position) {
            Message message = list.get(position);
            usernameTV.setText(message.getUsername());
            messageTV.setText(message.getContent());
            if (message.getTimestampLong() != null) {
                timeTV.setText(dateFormat.format(new Date(message.getTimestampLong())));
            }
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == MESSAGE_TYPE_IN) {
            return new MessageInViewHolder(LayoutInflater.from(context).inflate(R.layout.message_incoming, parent, false));
        }
        return new MessageOutViewHolder(LayoutInflater.from(context).inflate(R.layout.message_outgoing, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (list.get(position).getUid() == null) {
            return;
        }
        if (list.get(position).getUid().equals(FirebaseAuth.getInstance().getUid())) {
            ((MessageOutViewHolder) holder).bind(position);
        } else {
            ((MessageInViewHolder) holder).bind(position);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (list.get(position).getUid() != null) {
            return list.get(position).getUid().equals(FirebaseAuth.getInstance().getUid()) ? MESSAGE_TYPE_OUT : MESSAGE_TYPE_IN;
        }
        return 0;
    }
}