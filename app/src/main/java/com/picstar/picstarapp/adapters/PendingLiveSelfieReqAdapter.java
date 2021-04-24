package com.picstar.picstarapp.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.github.siyamed.shapeimageview.ShaderImageView;
import com.picstar.picstarapp.R;
import com.picstar.picstarapp.callbacks.OnClickPhotoSelfieHistory;
import com.picstar.picstarapp.fragments.LiveSelfiePendingEventsFragment;
import com.picstar.picstarapp.mvp.models.ViewTypes;
import com.picstar.picstarapp.mvp.models.pendingliveselfieresponse.Info;
import com.picstar.picstarapp.mvp.views.PendingLiveSelfieView;
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
import de.hdodenhof.circleimageview.CircleImageView;
import jp.wasabeef.glide.transformations.BlurTransformation;

import static com.bumptech.glide.request.RequestOptions.bitmapTransform;

public class PendingLiveSelfieReqAdapter extends RecyclerView.Adapter<PendingLiveSelfieReqAdapter.ViewHolder> {

    Activity activity;
    List<Info> pendingList;
    String profilePicUrl;
    LiveSelfiePendingEventsFragment fragment;
    private View footerView;
    PendingLiveSelfieView pendingLiveSelfieView;

    public void setFooterView(View footerView) {
        this.footerView = footerView;
    }


    public PendingLiveSelfieReqAdapter(Activity activity, List<Info> pendingList, String url, LiveSelfiePendingEventsFragment fragment, PendingLiveSelfieView pendingLiveSelfieView) {

        this.activity = activity;
        this.pendingList = pendingList;
        profilePicUrl = url;
        this.fragment = fragment;
        this.pendingLiveSelfieView = pendingLiveSelfieView;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.liveselfie_pending_cmpltd_row, parent, false);
        PendingLiveSelfieReqAdapter.ViewHolder vh = new PendingLiveSelfieReqAdapter.ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int viewType = getItemViewType(position);
        holder.extraLayout.removeAllViews();
        if (viewType == ViewTypes.Footer) {
            if (footerView.getParent() != null) {
                ((ViewGroup) footerView.getParent()).removeView(footerView);
            }
            holder.extraLayout.addView(footerView);
        } else {
            holder.extraLayout.removeAllViews();
        }
        bindingToItem(holder, position);

    }


    @Override
    public int getItemViewType(int position) {
        if (position == getItemCount() - 1)
            return ViewTypes.Footer;
        else
            return ViewTypes.Normal;
    }


    private void bindingToItem(ViewHolder holder, int position) {


        Info info = pendingList.get(position);
        Glide.with(activity)
                .load(profilePicUrl)

                .placeholder(R.drawable.ic_profilepholder)
                .into(holder.celebrityProfilePicImgv);
        holder.eventNameTv.setText(info.getLiveEvent().getEventName());
        holder.eventDescriptnTv.setText(info.getLiveEvent().getEventDesc());
        holder.eventLocationTv.setText(info.getLiveEvent().getEventLocation());


        TimeZone toTZ = TimeZone.getDefault();
        TimeZone fromTZ = TimeZone.getTimeZone("UTC");
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.ENGLISH);
        Date serverDate = null;
        try {
            serverDate = df.parse(info.getLiveEvent().getEventDate());
            Calendar calInLocal = PSR_Utils.changeTimezoneOfDate(serverDate, fromTZ, toTZ);
            Date dateLocal = calInLocal.getTime();
            holder.eventDate.setText(new SimpleDateFormat("MM.dd.yyyy, hh:mm a").format(dateLocal).toUpperCase());
        } catch (ParseException e) {
            e.printStackTrace();
        }


        holder.selfieReq.setText(info.getStatus());
        holder.selfieReq.setTextColor(ContextCompat.getColor(activity, R.color.toolBar_bgcolor));


    /*    if (info.getFilePath() != null && !info.getFilePath().toString().isEmpty()) {

            holder.selfieImgView.setVisibility(View.VISIBLE);
            holder.progressBar.setVisibility(View.VISIBLE);
            Glide.with(activity).load(info.getFilePath())
                    .placeholder(activity.getResources().getDrawable(R.drawable.ic_profilepholder))
                    .apply(bitmapTransform(new BlurTransformation(40)))
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
                    .into(holder.selfieImgView);
        }*/

        if (info.getFilePath() == null || !info.getStatus().equalsIgnoreCase(PSRConstants.PAYMENTSUCESS)) {
            holder.payNowBtn.setVisibility(View.VISIBLE);
        }


    }


    @Override
    public int getItemCount() {
        return pendingList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.paynow_btn)
        Button payNowBtn;
        @BindView(R.id.progress_bar)
        ProgressBar progressBar;
        @BindView(R.id.selfie_imgV)
        ShaderImageView selfieImgView;
        @BindView(R.id.extra_layout)
        LinearLayout extraLayout;
        @BindView(R.id.celebrityprofile_pic)
        CircleImageView celebrityProfilePicImgv;

        @BindView(R.id.eventname_tv)
        TextView eventNameTv;

        @BindView(R.id.eventlocation_tv)
        TextView eventLocationTv;

        @BindView(R.id.event_date)
        TextView eventDate;

        @BindView(R.id.event_descriptn_tv)
        TextView eventDescriptnTv;
        @BindView(R.id.selfie_req_tv)
        TextView selfieReq;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @OnClick(R.id.selfie_imgV)
        void onClickPic(View view) {
            Info info = pendingList.get(getAdapterPosition());
            pendingLiveSelfieView.onClickPhotoSelfie(info.getFilePath().toString(), false);
        }

        @OnClick(R.id.paynow_btn)
        void onClickPaynow(View view) {
            Info info = pendingList.get(getAdapterPosition());
            pendingLiveSelfieView.onClickPaynow(info);

        }
    }
}
