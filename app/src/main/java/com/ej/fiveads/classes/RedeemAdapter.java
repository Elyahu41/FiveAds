package com.ej.fiveads.classes;

import android.content.Context;
import android.content.res.Configuration;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.ej.fiveads.R;
import com.google.android.gms.ads.VideoController;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RedeemAdapter extends RecyclerView.Adapter<RedeemAdapter.RedeemViewHolder> {

    private final Context mContext;
    private final List<Object> mPrizesAndAdsList;
    private double mUserBalance;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mUsersDatabase;

    public RedeemAdapter(Context context, List<Object> prizesAndAdsList, double userBalance) {
        this.mContext = context;
        this.mPrizesAndAdsList = prizesAndAdsList;
        this.mUserBalance = userBalance;
    }

    @NonNull
    @Override
    public RedeemAdapter.RedeemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_raffle_row, parent, false);
        return new RedeemAdapter.RedeemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RedeemViewHolder holder, int position) {
        holder.setIsRecyclable(false);
        if (mPrizesAndAdsList.get(position) instanceof String) {
            holder.mAdTitle.setSelected(true);
            holder.mImage.setImageResource(R.drawable.paypal);
            String text = "Redeem\n" + mPrizesAndAdsList.get(position);
            holder.mTitle.setText(text);
            switch (mContext.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
                case Configuration.UI_MODE_NIGHT_YES:
                    holder.mTitle.setTextColor(mContext.getColor(R.color.white));
                    break;
                case Configuration.UI_MODE_NIGHT_NO:
                    holder.mTitle.setTextColor(mContext.getColor(R.color.black));
                    break;
            }
            holder.mAdTitle.setVisibility(View.VISIBLE);
        if ((Double.parseDouble(((String) mPrizesAndAdsList.get(position)).replace("$", ""))) <= mUserBalance) {
                holder.mAdTitle.setText(R.string.available);
                holder.mAdTitle.setBackgroundColor(mContext.getResources().getColor(R.color.light_green, mContext.getTheme()));
                holder.mCardView.setOnClickListener(v -> {
                    String prize = (String) mPrizesAndAdsList.get(position);
                    String prizeAmount = prize.replace("$", "");
                    double prizeAmountDouble = Double.parseDouble(prizeAmount);
                    double newBalance = mUserBalance - prizeAmountDouble;
                    updateUserBalance(newBalance, prizeAmount);
                    mUserBalance = newBalance;

                    new AlertDialog.Builder(mContext)
                            .setTitle("Congratulations!")
                            .setMessage("You have redeemed " + prize + "!!! An email will be sent to you with your prize.")
                            .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss()).show();
                    Toast.makeText(mContext, "You have redeemed " + prize + ". An email will be sent to you soon!", Toast.LENGTH_SHORT).show();
                });
            } else {
                holder.mAdTitle.setText(R.string.not_enough_earned);
                holder.mAdTitle.setBackgroundColor(mContext.getResources().getColor(R.color.red, mContext.getTheme()));
            }
        } else if (mPrizesAndAdsList.get(position) instanceof NativeAd) {
            NativeAdView adView = (NativeAdView) LayoutInflater.from(mContext).inflate(R.layout.ad_unified, null);
            populateNativeAdView((NativeAd) mPrizesAndAdsList.get(position), adView);
            holder.mFrameLayout.addView(adView);
            holder.mCardView.setVisibility(View.GONE);//only show ad
        }
    }

    @Override
    public int getItemCount() {
        return mPrizesAndAdsList.size();
    }

    private void populateNativeAdView(NativeAd nativeAd, NativeAdView adView) {
        adView.setMediaView(adView.findViewById(R.id.ad_media));// Set the media view.
        // Set other ad assets.
        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_app_icon));
        adView.setPriceView(adView.findViewById(R.id.ad_price));
        adView.setStarRatingView(adView.findViewById(R.id.ad_stars));
        adView.setStoreView(adView.findViewById(R.id.ad_store));
        adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));

        // The headline and mediaContent are guaranteed to be in every NativeAd.
        ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());
        adView.getMediaView().setMediaContent(nativeAd.getMediaContent());

        // These assets aren't guaranteed to be in every NativeAd, so it's important to
        // check before trying to display them.
        if (nativeAd.getBody() == null) {
            adView.getBodyView().setVisibility(View.INVISIBLE);
        } else {
            adView.getBodyView().setVisibility(View.VISIBLE);
            ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
        }

        if (nativeAd.getCallToAction() == null) {
            adView.getCallToActionView().setVisibility(View.INVISIBLE);
        } else {
            adView.getCallToActionView().setVisibility(View.VISIBLE);
            ((Button) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
        }

        if (nativeAd.getIcon() == null) {
            adView.getIconView().setVisibility(View.GONE);
        } else {
            ((ImageView) adView.getIconView()).setImageDrawable(
                    nativeAd.getIcon().getDrawable());
            adView.getIconView().setVisibility(View.VISIBLE);
        }

        if (nativeAd.getPrice() == null) {
            adView.getPriceView().setVisibility(View.INVISIBLE);
        } else {
            adView.getPriceView().setVisibility(View.VISIBLE);
            ((TextView) adView.getPriceView()).setText(nativeAd.getPrice());
        }

        if (nativeAd.getStore() == null) {
            adView.getStoreView().setVisibility(View.INVISIBLE);
        } else {
            adView.getStoreView().setVisibility(View.VISIBLE);
            ((TextView) adView.getStoreView()).setText(nativeAd.getStore());
        }

        if (nativeAd.getStarRating() == null) {
            adView.getStarRatingView().setVisibility(View.INVISIBLE);
        } else {
            ((RatingBar) adView.getStarRatingView())
                    .setRating(nativeAd.getStarRating().floatValue());
            adView.getStarRatingView().setVisibility(View.VISIBLE);
        }

        if (nativeAd.getAdvertiser() == null) {
            adView.getAdvertiserView().setVisibility(View.INVISIBLE);
        } else {
            ((TextView) adView.getAdvertiserView()).setText(nativeAd.getAdvertiser());
            adView.getAdvertiserView().setVisibility(View.VISIBLE);
        }

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad.
        adView.setNativeAd(nativeAd);

        // Get the video controller for the ad. One will always be provided, even if the ad doesn't
        // have a video asset.
        VideoController vc = nativeAd.getMediaContent().getVideoController();
        if (vc.hasVideoContent()) {
            float mediaAspectRatio = nativeAd.getMediaContent().getAspectRatio();
            float duration = nativeAd.getMediaContent().getDuration();
            // Create a new VideoLifecycleCallbacks object and pass it to the VideoController. The
            // VideoController will call methods on this object when events occur in the video
            // lifecycle.
            vc.setVideoLifecycleCallbacks(new VideoController.VideoLifecycleCallbacks() {
                @Override
                public void onVideoEnd() {
                    // Publishers should allow native ads to complete video playback before
                    // refreshing or replacing them with another ad in the same UI location.
                    super.onVideoEnd();
                }
            });
        }
    }

    private void updateUserBalance(double value, String prizeAmount) {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance();

        if (firebaseAuth.getCurrentUser() != null) {
            mFirebaseUser = firebaseAuth.getCurrentUser();
        }

        if (mFirebaseUser != null) {
            mUsersDatabase = database.getReference("Users");
        }

        if (mFirebaseUser != null) {
            mUsersDatabase.child(mFirebaseUser.getUid()).child("totalMoneyEarned").setValue(value);//update user balance
        }

        if (mFirebaseUser != null) {
            DatabaseReference mLeaderboardsDatabase = database.getReference("Leaderboards");
            Map<String, String> userData = new HashMap<>();
            userData.put("MoneyRedeemed", prizeAmount);
            userData.put("displayName", mFirebaseUser.getDisplayName());
            userData.put("UID", mFirebaseUser.getUid());
            userData.put("email", mFirebaseUser.getEmail());
            mLeaderboardsDatabase.child("RedeemersUnpaid").push().setValue(userData);

            mLeaderboardsDatabase
                    .child(prizeAmount+"Raffle")
                    .child(Objects.requireNonNull(mFirebaseUser.getDisplayName()))
                    .setValue("");//for Leaderboard fragment
        }
    }

    protected static class RedeemViewHolder extends RecyclerView.ViewHolder {

        private final ImageView mImage;
        private final TextView mTitle;
        private final TextView mAdTitle;
        private final CardView mCardView;
        FrameLayout mFrameLayout;

        public RedeemViewHolder(View itemView) {
            super(itemView);
            mCardView = itemView.findViewById(R.id.cardview);
            mImage = itemView.findViewById(R.id.ivImage);
            mTitle = itemView.findViewById(R.id.tvTitle);
            mAdTitle = itemView.findViewById(R.id.adTitle);
            mFrameLayout = itemView.findViewById(R.id.fl_adplaceholder);
        }
    }
}
