package com.picstar.picstarapp.adapters;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.github.siyamed.shapeimageview.RoundedImageView;
import com.picstar.picstarapp.R;
import com.picstar.picstarapp.callbacks.OnClickPhotoSelfieHistory;
import com.picstar.picstarapp.mvp.models.ViewTypes;
import com.picstar.picstarapp.mvp.models.eventhistory.Info;
import com.picstar.picstarapp.utils.PSRConstants;
import com.picstar.picstarapp.utils.PSR_Utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import jp.wasabeef.glide.transformations.BlurTransformation;

import static com.bumptech.glide.request.RequestOptions.bitmapTransform;

public class MyHistoryAdapter extends RecyclerView.Adapter<MyHistoryAdapter.ViewHolder> {

    List<Info> pendingHistory;
    Activity activity;
    private View footerView;
    OnClickPhotoSelfieHistory onClickPhotoSelfieHistory;
    boolean isCameFromCompletedHistory = false;
    int blurTransformation = 40;

    public MyHistoryAdapter(Activity activity, List<Info> pendingHistory, OnClickPhotoSelfieHistory onClickPhotoSelfieHistory, boolean isCameFromCompletedHistory) {
        this.activity = activity;
        this.pendingHistory = pendingHistory;
        this.onClickPhotoSelfieHistory = onClickPhotoSelfieHistory;
        this.isCameFromCompletedHistory = isCameFromCompletedHistory;
      /*  if (isCameFromCompletedHistory) {
            blurTransformation = 0;
        }*/
    }

    public void setFooterView(View footerView) {
        this.footerView = footerView;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.pending_history_row, parent, false);
        MyHistoryAdapter.ViewHolder vh = new MyHistoryAdapter.ViewHolder(v);
        return vh;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == getItemCount() - 1)
            return ViewTypes.Footer;
        else
            return ViewTypes.Normal;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        int viewType = getItemViewType(position);
        holder.extraLayout.removeAllViews();
        if (viewType == ViewTypes.Footer) {
            // holder.cardView.setVisibility(View.GONE);
            if (footerView.getParent() != null) {
                ((ViewGroup) footerView.getParent()).removeView(footerView);
            }
            holder.extraLayout.addView(footerView);
        } else {
            holder.extraLayout.removeAllViews();

        }
        bindListToItems(holder, position);


    }


    private void bindListToItems(ViewHolder holder, int position) {
        Info info = pendingHistory.get(position);

        holder.celebrityNameTv.setText(info.getCelebrityUser().getUsername());
        TimeZone toTZ = TimeZone.getDefault();
        TimeZone fromTZ = TimeZone.getTimeZone("UTC");
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.ENGLISH);
        Date serverDate = null;
        try {
            serverDate = df.parse(info.getCreatedAt());
            Calendar calInLocal2 = PSR_Utils.changeTimezoneOfDate(serverDate, fromTZ, toTZ);
            Date finalCreatedDate = calInLocal2.getTime();
            holder.createdDateTv.setText(activity.getResources().getString(R.string.created_date_txt) + " " + new SimpleDateFormat("MM/dd/yy").format(finalCreatedDate));
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (info.getServiceRequestTypeId() == Integer.parseInt(PSRConstants.LIVESELFIE_SERVICE_REQ_ID)) {
            try {
                if (isCameFromCompletedHistory) {
                    holder.paynowBtn.setVisibility(View.GONE);
                } else if (info.getStatus().equalsIgnoreCase(PSRConstants.PAYMENTSUCESS)) {
                    holder.paynowBtn.setVisibility(View.GONE);
                }
                holder.eventTypeTv.setText(activity.getResources().getString(R.string.liveselfie_txt));
                holder.eventTypeImv.setImageDrawable(activity.getResources().getDrawable(R.drawable.ic_liveselfie_blue));
                if (info.getLiveEvent() != null) {
                    holder.locationTV.setVisibility(View.VISIBLE);
                    holder.eventNameTv.setVisibility(View.VISIBLE);
                    holder.descriptionTv.setVisibility(View.VISIBLE);
                    holder.locationTV.setText(info.getLiveEvent().getEventLocation());
                    holder.eventNameTv.setText(info.getLiveEvent().getEventName());
                    holder.descriptionTv.setText(info.getLiveEvent().getEventDesc().trim());
                } else {
                    holder.locationTV.setVisibility(View.GONE);
                    holder.eventNameTv.setVisibility(View.GONE);
                    holder.descriptionTv.setVisibility(View.GONE);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            if (info.getFilePath() != null && !info.getFilePath().toString().isEmpty()) {
                holder.photoSelfieimgV.setVisibility(View.VISIBLE);
                holder.progressBar.setVisibility(View.VISIBLE);
                Glide.with(activity).load(info.getFilePath())
                        .placeholder(activity.getResources().getDrawable(R.drawable.ic_profilepholder))
                        .apply(bitmapTransform(new BlurTransformation(isCameFromCompletedHistory ? 1 : 40)))
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                holder.progressBar.setVisibility(View.GONE);
                                return false;
                            }
                        })
                        .into(holder.photoSelfieimgV);

            } else {
                holder.photoSelfieimgV.setVisibility(View.GONE);
                holder.progressBar.setVisibility(View.GONE);
            }


            try {
                if (info.getLiveEvent() != null) {
                    holder.dateTV.setVisibility(View.VISIBLE);
                    Date eventDate = df.parse(info.getLiveEvent().getEventDate());
                    Calendar calInLocal2 = PSR_Utils.changeTimezoneOfDate(eventDate, fromTZ, toTZ);
                    Date localEventDate = calInLocal2.getTime();
                    holder.dateTV.setText(activity.getResources().getString(R.string.event_date_txt) + " " + new SimpleDateFormat("MM/dd/yy, hh:mm a").format(localEventDate).toUpperCase());
                } else {
                    holder.dateTV.setVisibility(View.GONE);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }


        } else if (info.getServiceRequestTypeId() == Integer.parseInt(PSRConstants.PHOTOSELFIE_SERVICE_REQ_ID)) {
            try {
                if (isCameFromCompletedHistory) {
                    holder.paynowBtn.setVisibility(View.GONE);
                } else if (info.getStatus().equalsIgnoreCase(PSRConstants.PAYMENTSUCESS)) {
                    holder.paynowBtn.setVisibility(View.GONE);
                }
                holder.locationTV.setVisibility(View.GONE);
                holder.eventNameTv.setVisibility(View.GONE);
                holder.dateTV.setVisibility(View.GONE);
                holder.descriptionTv.setVisibility(View.GONE);
                holder.photoSelfieimgV.setVisibility(View.VISIBLE);
                holder.progressBar.setVisibility(View.VISIBLE);
                holder.eventTypeTv.setText(activity.getResources().getString(R.string.photoSelfie_txt));
                holder.eventTypeImv.setImageDrawable(activity.getResources().getDrawable(R.drawable.ic_photoselfie_blue));
            } catch (Exception e) {
                e.printStackTrace();
            }
          /*  if(isCameFromCompletedHistory) {
                Glide.with(activity).load(info.getFilePath())
                        .placeholder(activity.getResources().getDrawable(R.drawable.ic_profilepholder))
                        .apply(bitmapTransform(new BlurTransformation(1)))
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                holder.progressBar.setVisibility(View.GONE);
                                return false;
                            }
                        })
                        .into(holder.photoSelfieimgV);
            }
            else{*/
            Glide.with(activity).load(info.getFilePath())
                    .placeholder(activity.getResources().getDrawable(R.drawable.ic_profilepholder))
                    .apply(bitmapTransform(new BlurTransformation(isCameFromCompletedHistory ? 1 : 40)))
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            holder.progressBar.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .into(holder.photoSelfieimgV);
            //  }

        } else if (info.getServiceRequestTypeId() == Integer.parseInt(PSRConstants.VIDEOMSGS_SERVICE_REQ_ID)) {
            if (isCameFromCompletedHistory) {
                holder.paynowBtn.setVisibility(View.GONE);
            } else if (info.getStatus().equalsIgnoreCase(PSRConstants.PAYMENTSUCESS)) {
                holder.paynowBtn.setVisibility(View.GONE);
            }
            holder.locationTV.setVisibility(View.GONE);
            holder.photoSelfieimgV.setVisibility(View.GONE);
            holder.progressBar.setVisibility(View.GONE);
            holder.eventNameTv.setVisibility(View.VISIBLE);
            holder.dateTV.setVisibility(View.VISIBLE);
            holder.eventNameTv.setText(info.getVideoEvent().getVideoEventName());
            holder.descriptionTv.setVisibility(View.VISIBLE);
            try {
                Date eventDate = df.parse(info.getVideoEvent().getVideoEventDate());
                holder.dateTV.setText(activity.getResources().getString(R.string.event_date_txt) + " " + new SimpleDateFormat("MM/dd/yy").format(eventDate));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            holder.descriptionTv.setText(info.getVideoEvent().getVideoEventDesc().trim());
            holder.eventTypeTv.setText(activity.getResources().getString(R.string.videomsg_txt));
            holder.eventTypeImv.setImageDrawable(activity.getResources().getDrawable(R.drawable.ic_videomsg_blue));

        } else if (info.getServiceRequestTypeId() == Integer.parseInt(PSRConstants.LIVE_VIDEO_SERVICE_REQ_ID)) {
            if (isCameFromCompletedHistory) {
                holder.paynowBtn.setVisibility(View.GONE);
            } else if (info.getStatus().equalsIgnoreCase(PSRConstants.PAYMENTSUCESS)) {
                holder.paynowBtn.setVisibility(View.GONE);
            }
            holder.locationTV.setVisibility(View.GONE);
            holder.photoSelfieimgV.setVisibility(View.GONE);
            holder.progressBar.setVisibility(View.GONE);
            holder.eventNameTv.setVisibility(View.VISIBLE);
            holder.dateTV.setVisibility(View.VISIBLE);
            holder.eventNameTv.setText(info.getLiveVideo().getLiveVideoName());
            holder.descriptionTv.setVisibility(View.VISIBLE);
            try {
                Date eventDate = df.parse(info.getLiveVideo().getLiveVideoDatetime());
                holder.dateTV.setText(activity.getResources().getString(R.string.event_date_txt) + " " + new SimpleDateFormat("MM/dd/yy").format(eventDate));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            holder.descriptionTv.setText(info.getLiveVideo().getLiveVideoDesc().trim());
            holder.eventTypeTv.setText(activity.getResources().getString(R.string.live_video_txt));
            holder.eventTypeImv.setImageDrawable(activity.getResources().getDrawable(R.drawable.ic_livevideo_blue));
        }

        holder.statusTV.setText(info.getStatus());
        if (info.getAmount().toString().endsWith(".0")) {
            String amount = info.getAmount().toString().replace(".0", "");
            holder.priceTV.setText(" $" + amount);
        } else {
            holder.priceTV.setText(" $" + info.getAmount().toString());
        }
    }


    @Override
    public int getItemCount() {
        return pendingHistory.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.progress_bar)
        ProgressBar progressBar;
        @BindView(R.id.paynow_btn)
        Button paynowBtn;
        @BindView(R.id.celebrity_nameTV)
        TextView celebrityNameTv;
        @BindView(R.id.event_date_tv)
        TextView dateTV;
        @BindView(R.id.locationTV)
        TextView locationTV;
        @BindView(R.id.eventType_tv)
        TextView eventTypeTv;
        @BindView(R.id.priceTV)
        TextView priceTV;
        @BindView(R.id.statusTV)
        TextView statusTV;
        @BindView(R.id.parentLayout)
        LinearLayout parentLayout;
        @BindView(R.id.extra_layout)
        LinearLayout extraLayout;
        @BindView(R.id.eventType_Imgv)
        ImageView eventTypeImv;
        @BindView(R.id.event_name_TV)
        TextView eventNameTv;
        @BindView(R.id.created_date_TV)
        TextView createdDateTv;
        @BindView(R.id.description_TV)
        TextView descriptionTv;
        @BindView(R.id.photo_selfie_imgV)
        RoundedImageView photoSelfieimgV;

        @OnClick(R.id.photo_selfie_imgV)
        void onClickPhoto(View view) {
            Info info = pendingHistory.get(getAdapterPosition());

            onClickPhotoSelfieHistory.onClickPhotoSelfie(info.getFilePath().toString(), isCameFromCompletedHistory);
        }

      /*  @OnClick(R.id.parentLayout)
        void onClickItem() {
            Info info = pendingHistory.get(getAdapterPosition());

            onClickPhotoSelfieHistory.onClickHistoryRow(info);
        }*/


        @OnClick(R.id.paynow_btn)
        void onClickPaynoeBtn() {
            Info info = pendingHistory.get(getAdapterPosition());
            onClickPhotoSelfieHistory.onClickPaynow(info);
        }


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
