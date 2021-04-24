package com.picstar.picstarapp.customui;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.drawable.AnimationDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.picstar.picstarapp.R;


public class ProgressHUD extends Dialog {

    public ProgressHUD(Activity context, int theme) {
        super(context, theme);
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        ImageView imageView = findViewById(R.id.spinnerImageView);
        AnimationDrawable spinner = (AnimationDrawable) imageView.getBackground();
        spinner.start();
    }

    public void setMessage(CharSequence message) {
        if (message != null && message.length() > 0) {
            TextView txt = findViewById(R.id.message);
            txt.setVisibility(View.VISIBLE);
            txt.setText(message);
            txt.invalidate();
        }
    }


    public static com.picstar.picstarapp.customui.ProgressHUD show(Activity context, CharSequence message, boolean indeterminate, boolean cancelable, OnCancelListener cancelListener) {
        com.picstar.picstarapp.customui.ProgressHUD dialog = null;
        try {
            if (context != null) {
                dialog = new com.picstar.picstarapp.customui.ProgressHUD(context, R.style.ProgressHUD);
                dialog.setTitle("");
                dialog.setContentView(R.layout.progress_hud);


                if (message == null || message.length() == 0) {
                    dialog.findViewById(R.id.message).setVisibility(View.GONE);
                } else {
                    TextView txt = (TextView) dialog.findViewById(R.id.message);
                    txt.setText(message);
                }
                dialog.setCancelable(false);
                dialog.setOnCancelListener(cancelListener);
              //  dialog.getWindow().getAttributes().gravity = Gravity.CENTER;
               // int dialogHeight = ViewGroup.LayoutParams.WRAP_CONTENT;
               // dialog.getWindow().setLayout(500, 300);
                WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
                lp.dimAmount = 0.2f;
                dialog.getWindow().setAttributes(lp);
                dialog.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dialog;
    }
}
