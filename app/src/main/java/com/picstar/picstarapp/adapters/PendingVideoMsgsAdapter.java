package com.picstar.picstarapp.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.picstar.picstarapp.R;
import com.picstar.picstarapp.mvp.models.ViewTypes;

import com.picstar.picstarapp.mvp.models.pendingVideoMsgs.Info;
import com.picstar.picstarapp.mvp.views.VideoMsgsPendingView;
import com.picstar.picstarapp.utils.PSRConstants;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PendingVideoMsgsAdapter extends RecyclerView.Adapter<PendingVideoMsgsAdapter.ViewHolder> {

    Activity activity;
    List<Info> pendingvideoMsgs;
    boolean isFromClosedRequest;
    private View footerView;
    VideoMsgsPendingView videoMsgsPendingView;

    public void setFooterView(View footerView) {
        this.footerView = footerView;
    }


    public PendingVideoMsgsAdapter(Activity activity, List<Info> pendingvideoMsgs, boolean isFromClosedRequest, VideoMsgsPendingView videoMsgsPendingView) {
        this.activity = activity;
        this.pendingvideoMsgs = pendingvideoMsgs;
        this.isFromClosedRequest = isFromClosedRequest;
        this.videoMsgsPendingView = videoMsgsPendingView;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.pending_videomsgs_row, parent, false);
        PendingVideoMsgsAdapter.ViewHolder vh = new PendingVideoMsgsAdapter.ViewHolder(v);
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
            if (footerView.getParent() != null) {
                ((ViewGroup) footerView.getParent()).removeView(footerView);
            }
            holder.extraLayout.addView(footerView);
        } else {
            holder.extraLayout.removeAllViews();
        }
        bindingDatatoList(holder, position);


    }


    private void bindingDatatoList(ViewHolder holder, int position) {

        Info info = pendingvideoMsgs.get(position);

        TimeZone toTZ = TimeZone.getDefault();
        TimeZone fromTZ = TimeZone.getTimeZone("UTC");//yyyy-MM-dd'T'HH:mm:ss.SSSZ
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        Date serverDate = null;
        try {
            serverDate = df.parse(info.getVideoEvent().getVideoEventDate());
            holder.eventDateTv.setText(new SimpleDateFormat("MM/dd/yyyy").format(serverDate));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        holder.eventNameTv.setText(info.getVideoEvent().getVideoEventName());
        holder.eventDescriptnTv.setText(info.getVideoEvent().getVideoEventDesc());

        if (isFromClosedRequest) {
            if (info.getVideoEvent().getVideoEventStatus() == 8) {
                holder.requestStatusTv.setText(activity.getResources().getString(R.string.completed_txt));
                holder.cardView.setBackground(activity.getResources().getDrawable(R.drawable.completed_bg));
            } else if (info.getVideoEvent().getVideoEventStatus() == 3) {
                holder.requestStatusTv.setText(activity.getResources().getString(R.string.rejected_txt));
                holder.cardView.setBackground(activity.getResources().getDrawable(R.drawable.rejected_bg));
            }
        }


        if (isFromClosedRequest) {
            holder.payNowBtn.setVisibility(View.GONE);
            holder.statusTv.setText(info.getStatus());
        } else if (info.getStatus().equalsIgnoreCase(PSRConstants.PAYMENTSUCESS)) {
            holder.payNowBtn.setVisibility(View.GONE);
            holder.statusTv.setText(info.getStatus());
        }

    }


    @Override
    public int getItemCount() {
        return pendingvideoMsgs.size();
    }


    class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.root_card_view)
        CardView cardView;
        @BindView(R.id.paynow_btn)
        Button payNowBtn;
        @BindView(R.id.status_tv)
        TextView statusTv;
        @BindView(R.id.eventName_tv)
        TextView eventNameTv;
        @BindView(R.id.eventdate_tv)
        TextView eventDateTv;
        @BindView(R.id.eventdescriptn_tv)
        TextView eventDescriptnTv;
        @BindView(R.id.parent_layout)
        LinearLayout parentLayout;


        @BindView(R.id.extra_layout)
        LinearLayout extraLayout;


        @BindView(R.id.request_status_tv)
        TextView requestStatusTv;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @OnClick(R.id.paynow_btn)
        void onClickPayNow(View view) {
            Info info = pendingvideoMsgs.get(getAdapterPosition());
            videoMsgsPendingView.onClickPayNow(info);
        }
    }


}
