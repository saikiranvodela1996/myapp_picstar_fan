package com.picstar.picstarapp.utils;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowMetrics;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.picstar.picstarapp.R;
import com.picstar.picstarapp.activities.LiveSelfieCameraActivity;
import com.picstar.picstarapp.activities.MainActivity;
import com.picstar.picstarapp.activities.PaymentActivity;
import com.picstar.picstarapp.callbacks.OnClickCelebrity;
import com.picstar.picstarapp.callbacks.ProfileUpdateSuccess;
import com.picstar.picstarapp.campkg.others.MyDebug;
import com.picstar.picstarapp.customui.ProgressHUD;
import com.picstar.picstarapp.helpers.PSR_PrefsManager;
import com.picstar.picstarapp.mvp.models.eventhistory.Info;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import butterknife.BindView;
import butterknife.ButterKnife;
import jp.wasabeef.glide.transformations.BlurTransformation;

import static com.bumptech.glide.request.RequestOptions.bitmapTransform;

public class PSR_Utils {
    private static Gson gson;
    private static ProgressHUD progressHUD;
    public static Bitmap bitmap;
    private static long mLastClickAt = System.currentTimeMillis();
    private static AlertDialog dialog;

    public static String saveToInternalStorage(Bitmap bitmapImage,Context context) {
        ContextWrapper cw = new ContextWrapper(context);
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath = new File(directory, "finalPhotoselfie.jpeg");
        if (mypath.exists()) {
            mypath.delete();
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 50, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return mypath.getAbsolutePath();
    }


    public static boolean handleDoubleClick(Activity activity) {
        try {
            if (System.currentTimeMillis() - mLastClickAt < 1000) {
                return false;
            } else {
                mLastClickAt = System.currentTimeMillis();
                return true;
            }
        } catch (Exception e){
            Toast.makeText(activity,e.getMessage(),Toast.LENGTH_LONG).show();
        }
        return true;
    }

    public static   String getServiceCost(String cost) {
        String serviceCost = "";
        if (cost.endsWith(".0")) {
            serviceCost = cost.replace(".0", "");
        } else {
            serviceCost = cost;
        }
        return serviceCost;

    }

    public static String getMonthName(int mon) {
        String[] monthName = {"January", "February",
                "March", "April", "May", "June", "July",
                "August", "September", "October", "November",
                "December"};

        Calendar cal = Calendar.getInstance();
        String month = monthName[mon];

        return month;
    }

    public static void hideKeyboardIfOpened(Activity activity) {
        try {
            InputMethodManager inputMethodManager =
                    (InputMethodManager) activity.getSystemService(
                            Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(
                    activity.getWindow().getDecorView().getRootView().getWindowToken(), 0);
           /* InputMethodManager inputManager = (InputMethodManager) activity
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(activity.getCurrentFocus()
                    .getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);*/
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static String getId(Activity activity) {

        String androidId = Settings.Secure.getString(activity.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        return androidId;
    }

    public static Gson getGsonInstance() {
        if (gson == null) {
            gson = new GsonBuilder().setLenient().create();
        }
        return gson;
    }

    public static String getHeader(PSR_PrefsManager psr_prefsManager) {
        // return "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6InBxdVh1YXFScTJQczdqejhoRVNLLSJ9.eyJnaXZlbl9uYW1lIjoic2Fpa2lyYW4iLCJmYW1pbHlfbmFtZSI6InZvZGVsYSIsIm5pY2tuYW1lIjoic2Fpa2lyYW52ZGwiLCJuYW1lIjoic2Fpa2lyYW4gdm9kZWxhIiwicGljdHVyZSI6Imh0dHBzOi8vbGgzLmdvb2dsZXVzZXJjb250ZW50LmNvbS9hLS9BT2gxNEdoT3FZWUdrVGJVVWtPdkMxWWtGdGhGOGFnX2txN2dRa1FSdzl1VDRRPXM5Ni1jIiwibG9jYWxlIjoiZW4iLCJ1cGRhdGVkX2F0IjoiMjAyMS0wMi0xMFQxMjoxNDo0Mi43OTdaIiwiZW1haWwiOiJzYWlraXJhbnZkbEBnbWFpbC5jb20iLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiaXNzIjoiaHR0cHM6Ly9kZXYtZndodWdtdXMudXMuYXV0aDAuY29tLyIsInN1YiI6Imdvb2dsZS1vYXV0aDJ8MTAzNDM4NDc2Mzc5ODYzMzc1ODk1IiwiYXVkIjoiSzRhUUdJZnRvU3ZmMU9aVmFzeDllbU1qa0ZGNURDd2EiLCJpYXQiOjE2MTI5NTkyODMsImV4cCI6MTYxNTU1MTI4Mywibm9uY2UiOiJuOEdnM1dyYm1ra3VBTEw5TW5EbUFlY2l3RXpqY3ZUQUhLT0J3T2tWdlE4In0.NmEozDHbUdlykmDZkEBzEshx_ttJcEV7vyIUYoIGYqSL7_Ik4H7Jtc2ffLG1TLCzggWPiVe9h4cIH9R9gkyT9sFGW-7WKWRfiVvq8pkk-JRv474tRo_0rs7KtV8RNH3pUaDpmI8p3FJf82k4YipBmsb6kYBX_y1Csv8LXPQQAyVCiaCNNiFoc5EOV9ZLR0dNlfrfXCIkSOvy17DMHULMRF5f-O0pbhuQ18c-CeZoK2uo3I3oKrzwy4pi-2_zn7tlP-ZGTdIsezduTV-lX_D5DmyMk6SAJ3vagWQyrlN-uuFTerMENIsfrjYRO2xRqpgYKMgXi1Wa4-_-1Q6ZCqux8Q";
        return psr_prefsManager.get(PSRConstants.TOKEN);
    }


    public static void showProgressDialog(Activity activity) {
        if (activity != null) {
            try {
                if (progressHUD != null && progressHUD.isShowing()) {
                    progressHUD.dismiss();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                progressHUD = ProgressHUD.show(activity, "", false, true, null);
            }
        }
    }


    public static void hideProgressDialog() {
        try {
            Log.d("ProgressBar", "hideProgressDialog");
            if (progressHUD != null && progressHUD.isShowing()) {
                progressHUD.dismiss();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static String toLocalDateString(String date1) {
        try {
            //String dateStr = "2021-07-11T04:44:32.450+00:00";
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZ", Locale.ENGLISH);
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = df.parse(date1);
            df.setTimeZone(TimeZone.getDefault());
            String formattedDate = df.format(date);



       /* Date utcDate = new Date(utcTimeStamp);
        DateFormat df = new SimpleDateFormat("YYYY-DD-MM HH:MM:SS");
        df.setTimeZone(TimeZone.getTimeZone("PST"));
        return df.format(utcDate);*/
            return formattedDate;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Calendar changeTimezoneOfDate(Date date, TimeZone fromTZ, TimeZone toTZ) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        long millis = calendar.getTimeInMillis();
        long fromOffset = fromTZ.getOffset(millis);
        long toOffset = toTZ.getOffset(millis);
        long convertedTime = millis - (fromOffset - toOffset);
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(convertedTime);
        return c;
    }

    public static int getScreenWidth(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics windowMetrics = activity.getWindowManager().getCurrentWindowMetrics();
            Insets insets = windowMetrics.getWindowInsets()
                    .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());
            return windowMetrics.getBounds().width() - insets.left - insets.right;
        } else {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            return displayMetrics.widthPixels;
        }
    }


    public static boolean isOnline(Context activity) {
        if (activity != null) {
            NetworkInfo info = ((ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
            return (info != null && info.getState() == NetworkInfo.State.CONNECTED);
        }
        return false;
    }

    public static void showNoNetworkAlert(final Activity act) {
        if (act != null) {
            PSR_Utils.alertMessage(act, act.getResources().getString(R.string.noInternet_txt), null);
        }
    }

    public static class InternetDialogViewHolder {

        @BindView(R.id.internet_alert_tv_message)
        TextView message;
        @BindView(R.id.singlebtn_alert_btn)
        TextView okayButtonm;

        InternetDialogViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

    public static void alertMessage(final Activity act, String message, Runnable runnable) {
        if (act != null) {
            try {
                int width = act.getResources().getDisplayMetrics().widthPixels;
                final Dialog dialog = new Dialog(act, R.style.DialogTheme);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                View view = act.getLayoutInflater().inflate(R.layout.single_message_layout, null);
                InternetDialogViewHolder viewHolder = new InternetDialogViewHolder(view);
                dialog.setContentView(view);
                dialog.setCanceledOnTouchOutside(false);


                viewHolder.message.setText(message);
                viewHolder.okayButtonm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
                if (dialog == null || dialog.getWindow() == null) {
                    return;
                }
                int dialogWidth = (int) (width * 0.80f);
               /* if (act.getResources().getBoolean(R.bool.isTablet)) {
                    dialogWidth = (int) (width * 0.75f);
                }*/
                int dialogHeight = ViewGroup.LayoutParams.WRAP_CONTENT;
                dialog.getWindow().setLayout(dialogWidth, dialogHeight);
                dialog.show();
                dialog.setOnDismissListener(dialog1 -> {
                    if (runnable != null) {
                        runnable.run();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public static void successAlert(final Activity act, String message, final ProfileUpdateSuccess profileUpdateSuccess) {
        if (act != null) {
            try {
                int width = act.getResources().getDisplayMetrics().widthPixels;
                final Dialog dialog = new Dialog(act, R.style.DialogTheme);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                View view = act.getLayoutInflater().inflate(R.layout.single_message_layout, null);
                InternetDialogViewHolder viewHolder = new InternetDialogViewHolder(view);
                dialog.setContentView(view);
                dialog.setCanceledOnTouchOutside(false);
                viewHolder.message.setText(message);
                viewHolder.okayButtonm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(profileUpdateSuccess!=null){
                            profileUpdateSuccess.onUpdatingSuccess();
                        }
                        dialog.dismiss();
                    }
                });
                if (dialog == null || dialog.getWindow() == null) {
                    return;
                }
                int dialogWidth = (int) (width * 0.80f);

                int dialogHeight = ViewGroup.LayoutParams.WRAP_CONTENT;
                dialog.getWindow().setLayout(dialogWidth, dialogHeight);
                dialog.show();



            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void showAlert(final Activity act, String message, Runnable runnable) {
        if (act != null) {
            try {
                int width = act.getResources().getDisplayMetrics().widthPixels;
                final Dialog dialog = new Dialog(act, R.style.DialogTheme);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                View view = act.getLayoutInflater().inflate(R.layout.single_message_layout, null);
                InternetDialogViewHolder viewHolder = new InternetDialogViewHolder(view);
                dialog.setContentView(view);
                dialog.setCanceledOnTouchOutside(false);
                viewHolder.message.setText(message);
                viewHolder.okayButtonm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
                if (dialog == null || dialog.getWindow() == null) {
                    return;
                }
                int dialogWidth = (int) (width * 0.80f);

                int dialogHeight = ViewGroup.LayoutParams.WRAP_CONTENT;
                dialog.getWindow().setLayout(dialogWidth, dialogHeight);
                dialog.show();

                dialog.setOnDismissListener(dialog1 -> {
                    if (runnable != null) {
                        runnable.run();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }





    public static void showToast(Activity activity, String msg) {
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
    }


    public static void singleButtonAlert(final Activity act, String title, String message, Runnable runnable) {
        if (act != null) {
            try {
                int width = act.getResources().getDisplayMetrics().widthPixels;
                final Dialog dialog = new Dialog(act, R.style.DialogTheme);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                View view = act.getLayoutInflater().inflate(R.layout.orderplaced_layout, null);
                lDialogViewHolder viewHolder = new lDialogViewHolder(view);
                dialog.setContentView(view);
                dialog.setCanceledOnTouchOutside(false);

               /* viewHolder.alertTitle.setText(title);
                viewHolder.message.setText(message);
                viewHolder.okayButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });*/
                if (dialog == null || dialog.getWindow() == null) {
                    return;
                }
                int dialogWidth = (int) (width * 0.80f);
                int dialogHeight = ViewGroup.LayoutParams.WRAP_CONTENT;
                dialog.getWindow().setLayout(dialogWidth, dialogHeight);
                dialog.show();
                dialog.setOnDismissListener(dialog1 -> {
                    if (runnable != null) {
                        runnable.run();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static class lDialogViewHolder {


        lDialogViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }


    public static void checkPermissionToProgress(Activity activity, Runnable runnable) {
        try {
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                runnable.run();
            } else {
                Dexter.withActivity(activity)
                        .withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
                        .withListener(new MultiplePermissionsListener() {
                            @Override
                            public void onPermissionsChecked(MultiplePermissionsReport report) {
                                if (report.areAllPermissionsGranted()) {
                                    runnable.run();
                                } else {
                                    Toast.makeText(activity, R.string.cam_Permision_deny_txt, Toast.LENGTH_SHORT).show();
                                }

                            }

                            @Override
                            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                                token.continuePermissionRequest();
                            }


                        }).check();
                        /*.withListener(new PermissionListener() {
                            @Override
                            public void onPermissionGranted(PermissionGrantedResponse response) {
                                runnable.run();
                            }

                            @Override
                            public void onPermissionDenied(PermissionDeniedResponse response) {

                                Toast.makeText(activity, R.string.cam_Permision_deny_txt, Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                                token.continuePermissionRequest();
                            }
                        }).check();*/

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void showImageAlert(Context context, String imageString,boolean isCameFromCompletedHistory) {
        try {

            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_image_fullview, null);
            LinearLayout frameLayout = (LinearLayout) dialogView.findViewById(R.id.linear_Layout);
            final ImageView imageView = (ImageView) dialogView.findViewById(R.id.imageFullView);

            ProgressBar progressBar =(ProgressBar)dialogView.findViewById(R.id.progressbar);
            ImageView closeBtn =(ImageView)dialogView.findViewById(R.id.buttonClose);
            int blurTransformation=40;
            if(isCameFromCompletedHistory){
                blurTransformation=1;
            }
            Glide.with(context)
                    .load(imageString)
                    .placeholder(context.getResources().getDrawable(R.drawable.ic_profilepholder))
                    .apply(bitmapTransform(new BlurTransformation(blurTransformation)))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                          progressBar.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .into(imageView);
             Dialog dialog=new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            dialog.setContentView(dialogView);

            closeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                }
            });


            Window window = dialog.getWindow();
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setGravity(Gravity.CENTER);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static Bitmap getBitmap(String path) {
        Bitmap bitmap = null;
        try {
            File f = new File(path);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            bitmap = BitmapFactory.decodeStream(new FileInputStream(f), null, options);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }




    public static void logoutDialog(final Activity activity,  final OnClickCelebrity clickListener) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setContentView(R.layout.layout_logout);
        dialog.getWindow().setDimAmount(.75f);
        final TextView msgTv = dialog.findViewById(R.id.msgTv);
        TextView cancelBtn = dialog.findViewById(R.id.cancel_tv);
        TextView logoutTv = dialog.findViewById(R.id.logout_tv);

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clickListener != null) {
                    dialog.dismiss();
                }

            }
        });
        logoutTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clickListener != null) {
                    clickListener.onClickLogout();
                }
                dialog.dismiss();
            }
        });
        dialog.show();
    }





    public static void showDialog(final Activity activity, final String message, final OnAlertDialogOptionSelected clickListener) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setContentView(R.layout.layout_confirmation2);
        dialog.getWindow().setDimAmount(.75f);
        final TextView msgTv = dialog.findViewById(R.id.msgTv);
        TextView yesButton = dialog.findViewById(R.id.yes_tv);
        TextView noBtn = dialog.findViewById(R.id.no_tv);

        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clickListener != null) {
                    clickListener.onDialogOKClick();
                }
                dialog.dismiss();
            }
        });
        noBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clickListener != null) {
                    clickListener.onDialogNoBtnClick();
                }
                dialog.dismiss();
            }
        });
        dialog.show();
    }


    public interface OnAlertDialogOptionSelected {
        void onDialogOKClick();

        void onDialogNoBtnClick();
    }

    public interface OnSingleBtnDialogClick {
        void onClickOk();
    }

    public static void singleBtnAlert(final Activity act, String message, Runnable runnable, final OnSingleBtnDialogClick onSingleBtnDialogClick) {
        if (act != null) {
            try {
                int width = act.getResources().getDisplayMetrics().widthPixels;
                final Dialog dialog = new Dialog(act, R.style.DialogTheme);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                View view = act.getLayoutInflater().inflate(R.layout.single_message_layout, null);
                InternetDialogViewHolder viewHolder = new InternetDialogViewHolder(view);
                dialog.setContentView(view);
                dialog.setCanceledOnTouchOutside(false);
                viewHolder.message.setText(message);
                viewHolder.okayButtonm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (onSingleBtnDialogClick != null) {
                            onSingleBtnDialogClick.onClickOk();
                        }
                        dialog.dismiss();
                    }
                });
                if (dialog == null || dialog.getWindow() == null) {
                    return;
                }
                int dialogWidth = (int) (width * 0.80f);

                int dialogHeight = ViewGroup.LayoutParams.WRAP_CONTENT;
                dialog.getWindow().setLayout(dialogWidth, dialogHeight);
                dialog.show();
                dialog.setOnDismissListener(dialog1 -> {
                    if (runnable != null) {
                        runnable.run();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public static void doLogout(Activity activity, PSR_PrefsManager psr_prefsManager) {
        psr_prefsManager.clearAllPrefs();
        Intent intent = new Intent(activity, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
    }





    public static void navigatingAccordingly(Activity activity,Info info) {

        if (info.getServiceRequestTypeId() == Integer.parseInt(PSRConstants.LIVESELFIE_SERVICE_REQ_ID)) {
            if (info.getLiveEvent() == null && info.getFilePath() != null) {
                ////DirectLiveSelfie...Navigate to payment..
                Intent intent = new Intent(activity, PaymentActivity.class);
                intent.putExtra(PSRConstants.SERVICECOST, info.getAmount().toString());
                intent.putExtra(PSRConstants.SERVICEREQTYPEID,PSRConstants.LIVESELFIE_SERVICE_REQ_ID);
                intent.putExtra(PSRConstants.ISCAMEFROMHISTORY, true);
                intent.putExtra(PSRConstants.CELEBRITYID, info.getCelebrityId());
                intent.putExtra(PSRConstants.SERVICEREQID, info.getServiceRequestId());
                intent.putExtra(PSRConstants.S3UPLOADED_IMAGEURL, info.getFilePath().toString());
                activity.startActivity(intent);

            } else if (info.getLiveEvent() != null && info.getFilePath() != null && !info.getStatus().equalsIgnoreCase(PSRConstants.PAYMENTSUCESS)) {

                /// live selfie taken but payment is not completed......go to payment screen.....
                Intent intent = new Intent(activity, PaymentActivity.class);
                intent.putExtra(PSRConstants.S3UPLOADED_IMAGEURL,info.getFilePath().toString());
                intent.putExtra(PSRConstants.SERVICEREQTYPEID,PSRConstants.LIVESELFIE_SERVICE_REQ_ID);
                intent.putExtra(PSRConstants.ISCAMEFROMHISTORY,true);
                intent.putExtra(PSRConstants.SERVICEREQID,info.getServiceRequestId());
                intent.putExtra(PSRConstants.EVENTID, info.getLiveEvent().getEventId().toString());
                intent.putExtra(PSRConstants.SERVICECOST, info.getAmount().toString());
                intent.putExtra(PSRConstants.CELEBRITYID, info.getCelebrityId());
                intent.putExtra(PSRConstants.SELECTEDCELEBRITYNAME, info.getCelebrityUser().getUsername());
                activity.startActivity(intent);

            } else if (info.getLiveEvent() != null && info.getFilePath() == null) {
                ///LIVE selfie requested but LIVESELFIE didnt taken with celebrity......

                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(activity, LiveSelfieCameraActivity.class);
                        intent.putExtra(PSRConstants.EVENTID,  info.getLiveEvent().getEventId().toString());
                        intent.putExtra(PSRConstants.SERVICECOST, info.getAmount().toString());
                        intent.putExtra(PSRConstants.CELEBRITYID, info.getCelebrityId());
                        intent.putExtra(PSRConstants.SELECTEDCELEBRITYNAME, info.getCelebrityUser().getUsername());
                        intent.putExtra(PSRConstants.ISCAMEFROMHISTORY, true);
                        intent.putExtra(PSRConstants.SERVICEREQID,info.getServiceRequestId());
                        activity.startActivity(intent);
                    }
                };
                PSR_Utils.checkPermissionToProgress(activity, runnable);

            }

        } else if (info.getServiceRequestTypeId() == Integer.parseInt(PSRConstants.PHOTOSELFIE_SERVICE_REQ_ID)&& info.getFilePath()!=null) {
            ///Here photo selfie is created but payment is not made .so NAvigate  to paymentscreen...
            Intent intent =new Intent(activity,PaymentActivity.class);
            intent.putExtra(PSRConstants.ISCAMEFROMHISTORY, true);
            intent.putExtra(PSRConstants.CELEBRITYPHOTOID,info.getPhotoId());
            intent.putExtra(PSRConstants.SERVICECOST, info.getAmount().toString());
            intent.putExtra(PSRConstants.CELEBRITYID, info.getCelebrityId());
            intent.putExtra(PSRConstants.SERVICEREQID,info.getServiceRequestId());
            intent.putExtra(PSRConstants.SERVICEREQTYPEID,PSRConstants.PHOTOSELFIE_SERVICE_REQ_ID);
            intent.putExtra(PSRConstants.S3UPLOADED_IMAGEURL, info.getFilePath().toString());
            activity.startActivity(intent);


        } else if (info.getServiceRequestTypeId() == Integer.parseInt(PSRConstants.VIDEOMSGS_SERVICE_REQ_ID) && !info.getStatus().equalsIgnoreCase(PSRConstants.PAYMENTSUCESS)) {
            ///VIDEOMSG IS REQUESTED BUT PAYMENT IS NOT DONE..So....navigating to payment...
            Intent intent =new Intent(activity,PaymentActivity.class);
            intent.putExtra(PSRConstants.ISCAMEFROMHISTORY, true);
            intent.putExtra(PSRConstants.SERVICECOST, info.getAmount().toString());
            intent.putExtra(PSRConstants.CELEBRITYID, info.getCelebrityId());
            intent.putExtra(PSRConstants.SERVICEREQID,info.getServiceRequestId());
            intent.putExtra(PSRConstants.SERVICEREQTYPEID,PSRConstants.VIDEOMSGS_SERVICE_REQ_ID);
            activity.startActivity(intent);
        } else if (info.getServiceRequestTypeId() == Integer.parseInt(PSRConstants.LIVE_VIDEO_SERVICE_REQ_ID) && !info.getStatus().equalsIgnoreCase(PSRConstants.PAYMENTSUCESS)) {
            ///LIVEVIDEO IS REQUESTED BUT PAYMENT IS NOT DONE...So....navigating to payment...
            Intent intent =new Intent(activity,PaymentActivity.class);
            intent.putExtra(PSRConstants.ISCAMEFROMHISTORY, true);
            intent.putExtra(PSRConstants.SERVICECOST, info.getAmount().toString());
            intent.putExtra(PSRConstants.CELEBRITYID, info.getCelebrityId());
            intent.putExtra(PSRConstants.SERVICEREQID,info.getServiceRequestId());
            intent.putExtra(PSRConstants.SERVICEREQTYPEID,PSRConstants.LIVE_VIDEO_SERVICE_REQ_ID);
            activity.startActivity(intent);
        }

    }






}
