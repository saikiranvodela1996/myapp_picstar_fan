package com.picstar.picstarapp.helpers;


import android.view.View;
import android.widget.ProgressBar;

import com.picstar.picstarapp.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ProgressBarViewHolder {
    @BindView(R.id.progressBar)
  public ProgressBar progressBar;

    public ProgressBarViewHolder(View view) {
        ButterKnife.bind(this, view);
    }
}
