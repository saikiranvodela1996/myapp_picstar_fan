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
import androidx.cardview.widget.CardView;
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

public class RecommendedCelebritiesAdapter extends RecyclerView.Adapter<RecommendedCelebritiesAdapter.ViewHolder> {
    private  int screenWidth=0;
    Activity activity;
    List<Info> celebrityList;
    private View footerView;
   private  OnClickCelebrity onClickRecommendcelebrity;
    public RecommendedCelebritiesAdapter(Activity activity, List<Info> celebrityList, OnClickCelebrity onClickCelebrity) {
        this.activity = activity;
        this.celebrityList = celebrityList;
        screenWidth = PSR_Utils.getScreenWidth(activity);
        this.onClickRecommendcelebrity=onClickCelebrity;
    }



    public void setFooterView(View footerView) {
        this.footerView = footerView;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recommended_celebrity_row, parent, false);
        ViewHolder vh = new ViewHolder(v);
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
        bindToItems(holder, position);


    }


    private  void bindToItems( ViewHolder holder, int position){
        int reqWidth = screenWidth * 3 / 5;
        ViewGroup.LayoutParams llparams = holder.cardViewLayout.getLayoutParams();
        llparams.width = reqWidth*3/4;
        llparams.height=reqWidth;
        holder.cardViewLayout.setLayoutParams(llparams);


        Info info = celebrityList.get(position);
        holder.celebrityNameTv.setText(info.getUsername());
        holder.celebrityRoleTv.setText(TextUtils.join(", ", info.getCategoriesOfCelebrity().size()!=0?info.getCategoriesOfCelebrity():null));
        //  holder.celebrityRoleTv.setText(info.get);

        Glide.with(activity)
                .load(info.getProfilePic())
                .centerCrop()
                .placeholder(R.drawable.ic_coverpholder)
                .into(holder.celebrityImgV);
    }

    @Override
    public int getItemCount() {
        return celebrityList.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == getItemCount() - 1)
            return ViewTypes.Footer;
        else
            return ViewTypes.Normal;
    }


    public class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.recomm_celebrity_Image_View)
        ImageView celebrityImgV;
        @BindView(R.id.card_layout)
        CardView cardViewLayout;

        @BindView(R.id.recomm_celbrty_nameTv)
        TextView celebrityNameTv;
        @BindView(R.id.recomm_celbrty_roleTv)
        TextView celebrityRoleTv;
        @BindView(R.id.parent_layout)
        LinearLayout parentLayout;
        @BindView(R.id.extra_layout)
        LinearLayout extraLayout;

        @OnClick(R.id.parent_layout)void onClickRecommCel(View view){
            if (celebrityList.size() != 0) {
                Info item = celebrityList.get(getAdapterPosition());
                onClickRecommendcelebrity.onClickRecommendCelebrity(item);
            }
        }
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
