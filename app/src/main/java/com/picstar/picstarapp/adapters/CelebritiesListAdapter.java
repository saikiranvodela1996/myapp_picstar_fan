package com.picstar.picstarapp.adapters;


import android.app.Activity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.picstar.picstarapp.R;
import com.picstar.picstarapp.callbacks.OnClickCelebrity;
import com.picstar.picstarapp.mvp.models.ViewTypes;
import com.picstar.picstarapp.mvp.models.celebrities.Info;
import com.picstar.picstarapp.utils.PSR_Utils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class CelebritiesListAdapter extends RecyclerView.Adapter<CelebritiesListAdapter.ViewHolder> {

    private final Activity activity;
    private final OnClickCelebrity onClickCelebrity;

    int screenWidth = 0;

    private List<Info> celebritiesList;
    private View footerView;

    public CelebritiesListAdapter(Activity activity, List<Info> postItems, OnClickCelebrity onClickCelebrity) {
        this.activity = activity;
        this.celebritiesList = postItems;
        screenWidth = PSR_Utils.getScreenWidth(activity);
        this.onClickCelebrity = onClickCelebrity;
    }


    public void setFooterView(View footerView) {
        this.footerView = footerView;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.celebrities_row, parent, false);
        CelebritiesListAdapter.ViewHolder vh = new CelebritiesListAdapter.ViewHolder(v);
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
        onBind(holder, position);

    }

    @Override
    public int getItemViewType(int position) {
        if (position == getItemCount() - 1)
            return ViewTypes.Footer;
        else
            return ViewTypes.Normal;
    }


    @Override
    public int getItemCount() {
        return celebritiesList.size();
    }


    public void onBind(ViewHolder holder, int position) {

        Info item = celebritiesList.get(position);

        int reqWidth = screenWidth * 3 / 4;

        ViewGroup.LayoutParams layoutParams = holder.celebrityImageView.getLayoutParams();
        layoutParams.width = reqWidth;
        layoutParams.height = reqWidth;
        holder.celebrityImageView.setLayoutParams(layoutParams);


        try {
            holder.celebrityNameTv.setText(item.getUsername());
            holder.celebrityRole.setText(TextUtils.join(", ", item.getCategoriesOfCelebrity().size() != 0 ? item.getCategoriesOfCelebrity() : null));
            holder.favCount.setText(String.valueOf( item.getFavs()));
            holder.liveSelfiesCountTv.setText(String.valueOf( item.getLivePhotoSelfiesCount()));
            holder.photoSelfiesCountTv.setText( String.valueOf(item.getPhotoSelfiesCount()));
            holder.videoMsgsCountTv.setText(String.valueOf( item.getVideoMessagesCount()));
            holder.liveVideosTotalcount.setText(String.valueOf(item.getLiveVideosCount()));

        } catch (Exception e) {
            e.printStackTrace();
        }
        Glide.with(activity)
                .load(item.getProfilePic())
                .centerCrop()
                .placeholder(R.drawable.ic_coverpholder)
                .into(holder.celebrityImageView);
    }


    public class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.extra_layout)
        LinearLayout extraLayout;
        @BindView(R.id.celebrity_name_tv)
        TextView celebrityNameTv;
        @BindView(R.id.celebrity_role)
        TextView celebrityRole;
        @BindView(R.id.celebrity_Img_View)
        ImageView celebrityImageView;
        @BindView(R.id.parentLL)
        LinearLayout parentLL;
        @BindView(R.id.fav_count)
        TextView favCount;
        @BindView(R.id.live_selfies_totalCount_tv)
        TextView liveSelfiesCountTv;
        @BindView(R.id.photo_selfies_totalCount_Tv)
        TextView photoSelfiesCountTv;
        @BindView(R.id.video_msgs_totalCountTv)
        TextView videoMsgsCountTv;
        @BindView(R.id.heart_layout)
        LinearLayout heartLayout;
        @BindView(R.id.live_videos_totalCount_tv)
        TextView liveVideosTotalcount;

        @OnClick(R.id.parentLL)
        void onClickItem(View view) {
            if (celebritiesList.size() != 0) {
                Info item = celebritiesList.get(getAdapterPosition());
                onClickCelebrity.onClickCelebrity(item);
            }
        }

        @OnClick(R.id.heart_layout)
        void onClickHeart(View view) {
            Info item = celebritiesList.get(getAdapterPosition());
            onClickCelebrity.onClickHeart(item);
        }

        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }


    }


}


