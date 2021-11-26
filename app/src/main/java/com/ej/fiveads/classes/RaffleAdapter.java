package com.ej.fiveads.classes;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.ej.fiveads.R;
import com.ej.fiveads.activities.RaffleSubmitActivity;
import com.google.android.gms.ads.VideoController;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;

import java.util.List;

public class RaffleAdapter extends RecyclerView.Adapter<RaffleAdapter.RaffleViewHolder> {

    private final List<Object> mRaffleAndAdList;
    private final Context mContext;

    public RaffleAdapter(Context mContext, List<Object> mRaffleAndAdList) {
        this.mContext = mContext;
        this.mRaffleAndAdList = mRaffleAndAdList;
    }

    @NonNull
    @Override
    public RaffleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_raffle_row, parent, false);
        return new RaffleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RaffleViewHolder holder, int position) {
        holder.setIsRecyclable(false);
        if (mRaffleAndAdList.get(position) instanceof RaffleData) {
            if (((RaffleData) mRaffleAndAdList.get(position)).getRaffleType() == RaffleData.WEEKLY) {
                holder.mAdTitle.setText(R.string.weekly);
                holder.mAdTitle.setBackgroundColor(mContext.getResources().getColor(R.color.red, mContext.getTheme()));
            } else if (((RaffleData) mRaffleAndAdList.get(position)).getRaffleType() == RaffleData.MONTHLY) {
                holder.mAdTitle.setText(R.string.monthly);
                holder.mAdTitle.setBackgroundColor(mContext.getResources().getColor(R.color.yellow, mContext.getTheme()));
            }
            holder.mImage.setImageResource(((RaffleData) mRaffleAndAdList.get(position)).getRaffleImage());
            holder.mTitle.setText(((RaffleData) mRaffleAndAdList.get(position)).getRaffleName());
            if (!(((RaffleData) mRaffleAndAdList.get(position)).getRaffleImage() == R.drawable.coming_soon)) {
                holder.mCardView.setOnClickListener(view -> {
                    Intent mIntent = new Intent(mContext, RaffleSubmitActivity.class);
                    mIntent.putExtra("Title", ((RaffleData) mRaffleAndAdList.get(position)).getRaffleName());
                    mIntent.putExtra("Image", ((RaffleData) mRaffleAndAdList.get(position)).getRaffleImage());
                    mIntent.putExtra("DatabaseRef", ((RaffleData) mRaffleAndAdList.get(position)).getDatabaseRef());
                    mIntent.putExtra("RaffleType", ((RaffleData) mRaffleAndAdList.get(position)).getRaffleType());
                    mContext.startActivity(mIntent);
                });
            }
        } else if (mRaffleAndAdList.get(position) instanceof NativeAd) {
            NativeAdView adView = (NativeAdView) LayoutInflater.from(mContext).inflate(R.layout.ad_unified, null);
            populateNativeAdView((NativeAd) mRaffleAndAdList.get(position), adView);
            holder.mFrameLayout.addView(adView);
            holder.mCardView.setVisibility(View.GONE);
        }
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

    @Override
    public int getItemCount() {
        return mRaffleAndAdList.size();
    }

    protected static class RaffleViewHolder extends RecyclerView.ViewHolder {

        private final ImageView mImage;
        private final TextView mTitle;
        private final TextView mAdTitle;
        private final CardView mCardView;
        FrameLayout mFrameLayout;

        public RaffleViewHolder(View itemView) {
            super(itemView);
            mCardView = itemView.findViewById(R.id.cardview);
            mImage = itemView.findViewById(R.id.ivImage);
            mTitle = itemView.findViewById(R.id.tvTitle);
            mAdTitle = itemView.findViewById(R.id.adTitle);
            mFrameLayout = itemView.findViewById(R.id.fl_adplaceholder);
        }
    }
}
