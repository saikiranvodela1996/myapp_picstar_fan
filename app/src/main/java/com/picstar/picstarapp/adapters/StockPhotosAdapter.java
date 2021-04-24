package com.picstar.picstarapp.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.picstar.picstarapp.R;
import com.picstar.picstarapp.activities.StockPhotosActivity;
import com.picstar.picstarapp.mvp.models.stockphotos.Info;
import com.picstar.picstarapp.utils.PSR_Utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class StockPhotosAdapter extends RecyclerView.Adapter<StockPhotosAdapter.ViewHolder> {


    Activity activity;
    List<Info> photosList;

    public StockPhotosAdapter(Activity activity, List<Info> photosList) {
        this.activity = activity;
        this.photosList = photosList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.stockphotos_row, parent, false);
        StockPhotosAdapter.ViewHolder vh = new StockPhotosAdapter.ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Info info1 = photosList.get(position);

        Glide.with(activity)
                .load(info1.getPhotoUrl())
                .centerCrop()
                .placeholder(R.drawable.ic_coverpholder)
                .into(holder.imageView1);
        int screenWidth = PSR_Utils.getScreenWidth(activity);
        ViewGroup.LayoutParams llparams = holder.parentLayout.getLayoutParams();
        llparams.height = (screenWidth / 2) - 45;
        llparams.width = (screenWidth / 2) - 45;

/*
        if (photosList.size() > position * 3 + 1) {
            holder.imageView2.setVisibility(View.VISIBLE);
            Info info2 = photosList.get(position * 3 + 1);

            Glide.with(activity)
                    .load(info2.getPhotoUrl())
                    .centerCrop()
                    .placeholder(R.drawable.ic_coverpholder)
                    .into(holder.imageView2);

        } else {
            holder.imageView2.setVisibility(View.INVISIBLE);
        }

        if (photosList.size() > position * 3 + 2) {
            holder.imageView3.setVisibility(View.VISIBLE);
            Info info3 = photosList.get(position * 3 + 2);
            Glide.with(activity)
                    .load(info3.getPhotoUrl())
                    .centerCrop()
                    .placeholder(R.drawable.ic_coverpholder)
                    .into(holder.imageView3);
        } else {
            holder.imageView3.setVisibility(View.INVISIBLE);
        }*/
    }

    @Override
    public int getItemCount() {
        return photosList.size();

        /*  return (photosList.size() / 3) + (photosList.size() % 3 > 0 ? 1 : 0);*/
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.parent_Layout)
        LinearLayout parentLayout;
        @BindView(R.id.imageview_1)
        ImageView imageView1;
       /* @BindView(R.id.imageview_2)
        ImageView imageView2;
        @BindView(R.id.imageview_3)
        ImageView imageView3;*/

        @OnClick(R.id.imageview_1)
        void onCLick(View v) {
            if (!PSR_Utils.handleDoubleClick(activity))
                return;
            Info item = photosList.get(getAdapterPosition());
            ((StockPhotosActivity) activity).onPhotoPicked(item);

        }

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
