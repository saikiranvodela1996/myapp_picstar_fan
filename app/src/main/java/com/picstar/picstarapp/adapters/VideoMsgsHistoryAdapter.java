package com.picstar.picstarapp.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.picstar.picstarapp.R;
import com.picstar.picstarapp.mvp.models.ViewTypes;
import com.picstar.picstarapp.mvp.models.videomsgshistoryresponse.ServiceRequest;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class VideoMsgsHistoryAdapter extends RecyclerView.Adapter<VideoMsgsHistoryAdapter.ViewHolder> {

    List<ServiceRequest> videoMsgsHistoryList;
    Activity activity;
    private View footerView;

    public VideoMsgsHistoryAdapter(Activity activity, List<ServiceRequest> videoMsgsHistoryList) {
        this.activity = activity;
        this.videoMsgsHistoryList = videoMsgsHistoryList;
    }

    public void setFooterView(View footerView) {
        this.footerView = footerView;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.videomsg_history_row, parent, false);
        VideoMsgsHistoryAdapter.ViewHolder vh = new VideoMsgsHistoryAdapter.ViewHolder(v);
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
        bindListToItems(holder, position);

    }


    private void bindListToItems(ViewHolder holder, int position) {
        ServiceRequest info = videoMsgsHistoryList.get(position);

        holder.nameTV.setText(info.getCelebrityUser().getUsername());

        holder.statusTV.setText(info.getStatus());
        if (info.getAmount().toString().endsWith(".0")) {
            String amount = info.getAmount().toString().replace(".0", "");
            holder.priceTV.setText(" $ " + amount);
        } else {
            holder.priceTV.setText(" $ " + info.getAmount().toString());
        }
    }


    @Override
    public int getItemCount() {
        return videoMsgsHistoryList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.celebrtynameTV)
        TextView nameTV;
        @BindView(R.id.event_date_tv)
        TextView dateTV;
        @BindView(R.id.locationTV)
        TextView locationTV;
        @BindView(R.id.eventType_tv)
        TextView recodeTypeTV;
        @BindView(R.id.priceTV)
        TextView priceTV;
        @BindView(R.id.statusTV)
        TextView statusTV;
        @BindView(R.id.parentLayout)
        LinearLayout cardView;
        @BindView(R.id.extra_layout)
        LinearLayout extraLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }


}
