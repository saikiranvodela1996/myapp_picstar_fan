package com.picstar.picstarapp.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.github.siyamed.shapeimageview.CircularImageView;
import com.picstar.picstarapp.R;
import com.picstar.picstarapp.fragments.CelebrityEventsFragment;
import com.picstar.picstarapp.mvp.models.ViewTypes;
import com.picstar.picstarapp.mvp.models.celebrityevents.Info;
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

public class CelebrityEventAdapter extends RecyclerView.Adapter<CelebrityEventAdapter.ViewHolder> {

    Activity activity;
    List<Info> celebrityEvents;
    String profilePicUrl;
    CelebrityEventsFragment fragment;
    private View footerView;


    public void setFooterView(View footerView) {
        this.footerView = footerView;
    }




    public CelebrityEventAdapter(Activity activity, List<Info> celebrityEvents, String profilePicUrl, CelebrityEventsFragment fragment) {
        this.activity = activity;
        this.celebrityEvents = celebrityEvents;
        this.profilePicUrl = profilePicUrl;
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.celebrity_event_row, parent, false);
        CelebrityEventAdapter.ViewHolder vh = new CelebrityEventAdapter.ViewHolder(v);
        return vh;
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
            //holder.cardView.setVisibility(View.VISIBLE);

        }
        bindingToItems(holder, position);

    }


    @Override
    public int getItemViewType(int position) {
        if (position == getItemCount() - 1)
            return ViewTypes.Footer;
        else
            return ViewTypes.Normal;
    }


    private void bindingToItems(ViewHolder holder, int position)
    {


        Info info = celebrityEvents.get(position);
        Glide.with(activity)
                .load(profilePicUrl)
                .placeholder(R.drawable.ic_profilepholder)
                .into(holder.celebrityProfilePicImgv);
        holder.eventNameTv.setText(info.getEventName());
        holder.eventDescriptnTv.setText(info.getEventDesc());
        holder.eventLocationTv.setText(info.getEventLocation());


        TimeZone toTZ = TimeZone.getDefault();
        TimeZone fromTZ = TimeZone.getTimeZone("UTC");
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.ENGLISH);
        Date serverDate = null;
        try {
            serverDate = df.parse(info.getEventDate());
            Calendar calInLocal = PSR_Utils.changeTimezoneOfDate(serverDate, fromTZ, toTZ);
            Date dateLocal = calInLocal.getTime();
            holder.eventDate.setText(new SimpleDateFormat("MM.dd.yyyy, hh:mm a").format(dateLocal).toUpperCase());
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (info.getCreatedRequests() == 0) {
            holder.selfieReq.setText(R.string.selfiereq_txt);
        }else
        {
            holder.selfieReq.setText(R.string.selfie_requested_txt);
        }


    }
















    @Override
    public int getItemCount() {
        return celebrityEvents.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
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

        @BindView(R.id.extra_layout)
        LinearLayout extraLayout;

        @OnClick(R.id.selfie_req_tv)
        void onCliclItem(View view) {
            if(selfieReq.getText().toString().equalsIgnoreCase(activity.getString(R.string.selfiereq_txt)))
            {
                Info item = celebrityEvents.get(getAdapterPosition());
                fragment.onClickCelebrityEvent(item);
            }

        }


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
