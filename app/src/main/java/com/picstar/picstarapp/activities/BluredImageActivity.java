package com.picstar.picstarapp.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.picstar.picstarapp.R;
import com.picstar.picstarapp.mvp.models.videomsgs.createservicerequest.CreateServiceReq;
import com.picstar.picstarapp.mvp.models.videomsgs.createservicerequest.CreateServiceResponse;
import com.picstar.picstarapp.mvp.presenters.PhotoSelfiePresenter;
import com.picstar.picstarapp.mvp.views.PhotoSelfieView;
import com.picstar.picstarapp.utils.PSRConstants;
import com.picstar.picstarapp.utils.PSR_Utils;


import java.net.URL;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import jp.wasabeef.glide.transformations.BlurTransformation;

import static com.bumptech.glide.request.RequestOptions.bitmapTransform;

public class BluredImageActivity extends BaseActivity implements PhotoSelfieView, PSR_Utils.OnAlertDialogOptionSelected, PSR_Utils.OnSingleBtnDialogClick {

    @BindView(R.id.left_side_menu_option)
    ImageView leftSideMenu;
    @BindView(R.id.title_Tv)
    TextView toolbarTitle;
    private Bitmap finalBitmap;
    @BindView(R.id.final_photo)
    ImageView finalPhotoSelfieImgV;
    @BindView(R.id.progress_bar)
    ProgressBar progressBar;
    @BindView(R.id.pay_later)
    Button payLater;

    private String path;
    private String celebrityID;
    private PhotoSelfiePresenter photoSelfiePresenter;
    private int photoId;
    private AmazonS3Client s3Client;
    private String pictureName;
    private String bucketName;
    private int serviceReqId;
    private String photoCost;
    private Integer liveEventId;
    private boolean isCameFromHistory = false;
    private String eventId;
    private String serviceReqTypeId;
    private boolean comingFromHistory = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_blurred_image);
        ButterKnife.bind(this);
        photoSelfiePresenter = new PhotoSelfiePresenter();
        photoSelfiePresenter.attachMvpView(this);

        leftSideMenu.setImageResource(R.drawable.ic_back);

        if (getIntent() != null) {
            serviceReqTypeId = getIntent().getStringExtra(PSRConstants.SERVICEREQTYPEID);
            if (serviceReqTypeId.equalsIgnoreCase(PSRConstants.PHOTOSELFIE_SERVICE_REQ_ID)) {
                toolbarTitle.setText(getString(R.string.photoSelfie_txt));
                photoId = getIntent().getIntExtra(PSRConstants.CELEBRITYPHOTOID, 0);
                photoCost = getIntent().getStringExtra(PSRConstants.PHOTOSELFIECOST);
                bucketName = PSRConstants.PHOTOSELFIEBUCKETNAME;
            } else if (serviceReqTypeId.equalsIgnoreCase(PSRConstants.LIVESELFIE_SERVICE_REQ_ID)) {
                isCameFromHistory = getIntent().getBooleanExtra(PSRConstants.ISCAMEFROMHISTORY, false);
                if (isCameFromHistory && getIntent().hasExtra(PSRConstants.EVENTID) && getIntent().hasExtra(PSRConstants.SERVICEREQID)) {
                    liveEventId = Integer.parseInt(getIntent().getStringExtra(PSRConstants.EVENTID));
                    serviceReqId = getIntent().getIntExtra(PSRConstants.SERVICEREQID, 0);
                    comingFromHistory = true;
                }

                toolbarTitle.setText(getString(R.string.liveselfie_txt));
                photoCost = getIntent().getStringExtra(PSRConstants.LIVESELFIECOST);
                bucketName = PSRConstants.LIVESELFIESBUCKETNAME;
            }
            path = getIntent().getStringExtra("FINALSELFIE");
            celebrityID = getIntent().getStringExtra(PSRConstants.CELEBRITYID);
            finalBitmap = PSR_Utils.getBitmap(path);
            progressBar.setVisibility(View.VISIBLE);
            Glide.with(this).load(finalBitmap)
                    .apply(bitmapTransform(new BlurTransformation(40)))
                    .placeholder(getResources().getDrawable(R.drawable.ic_coverpholder))
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
                    .into(finalPhotoSelfieImgV);
        }

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        int width1 = displayMetrics.widthPixels;
        ViewGroup.LayoutParams params = finalPhotoSelfieImgV.getLayoutParams();
        params.height = width1;
        finalPhotoSelfieImgV.setLayoutParams(params);


    }


    @OnClick(R.id.left_side_menu_option)
    void onClickBack(View view) {
        PSR_Utils.showDialog(this, "", this);
    }


    @OnClick(R.id.pay_later)
    void onClickPayLater(View view) {
        uploadtoS3();
    }

    @OnClick(R.id.pay_now)
    void onClickPaynow(View view) {
        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra(PSRConstants.ISCAMEFROMHISTORY, false);
        intent.putExtra(PSRConstants.SERVICECOST, photoCost);
        intent.putExtra(PSRConstants.CELEBRITYID, celebrityID);
        intent.putExtra("FINALSELFIE", path);
        intent.putExtra(PSRConstants.CELEBRITYPHOTOID, photoId);
        intent.putExtra(PSRConstants.BUCKETNAME, bucketName);
        intent.putExtra(PSRConstants.SERVICEREQTYPEID, serviceReqTypeId);
        intent.putExtra(PSRConstants.SERVICEREQID, serviceReqId);
        if (liveEventId != null) {
            intent.putExtra(PSRConstants.EVENTID, liveEventId.toString());
        }
        startActivity(intent);
    }


    @Override
    public void onBackPressed() {
        PSR_Utils.showDialog(this, "", this);
    }

    private void uploadtoS3() {
        if (PSR_Utils.isOnline(this)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    PSR_Utils.showProgressDialog(BluredImageActivity.this);
                }
            });
            Thread myUploadTask = new Thread(new Runnable() {
                public void run() {
                    try {
                        s3Client = new AmazonS3Client(new BasicAWSCredentials(PSRConstants.S3BUCKETACCESSKEYID, PSRConstants.S3BUCKETSECRETACCESSKEY));
                        s3Client.setRegion(Region.getRegion(Regions.US_WEST_2));
                        pictureName = UUID.randomUUID().toString();
                        Log.d("PICTURENAME", pictureName);
                        PutObjectRequest por = new PutObjectRequest(bucketName, pictureName, new java.io.File(path));
                        s3Client.putObject(por);

                        URL url = s3Client.getUrl(bucketName, pictureName);

                        if (url != null) {
                            CreateServiceReq createServiceReq = new CreateServiceReq();
                            createServiceReq.setCelebrityId(celebrityID);
                            createServiceReq.setUserId(psr_prefsManager.get(PSRConstants.USERID));
                            createServiceReq.setFilePath(url.getProtocol() + "://" + url.getHost() + url.getPath());
                            createServiceReq.setPhotoId(photoId);
                            createServiceReq.setServiceRequestTypeId(Integer.parseInt(serviceReqTypeId));
                            createServiceReq.setEventId(liveEventId);
                            createServiceReq.setServiceRequestId(serviceReqId);
                            photoSelfiePresenter.uploadPhotoSelfie(PSR_Utils.getHeader(psr_prefsManager), createServiceReq);
                        } else {
                            PSR_Utils.hideProgressDialog();
                            PSR_Utils.showToast(BluredImageActivity.this, getResources().getString(R.string.unableto_savephoto_txt));
                        }
                        Log.d("BUCKETURL", url.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                        PSR_Utils.hideProgressDialog();
                        PSR_Utils.showToast(BluredImageActivity.this, getResources().getString(R.string.unableto_savephoto_txt));
                    }

                }
            });
            myUploadTask.start();
        } else {
            PSR_Utils.showNoNetworkAlert(this);
        }

    }

    @Override
    public void onCreatingServiceReqSuccess(CreateServiceResponse response) {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.singleBtnAlert(this, response.getMessage(), null, this);

    }

    @Override
    public void onCreatingServiceReqFailure(CreateServiceResponse response) {
        PSR_Utils.hideProgressDialog();
        if (PSR_Utils.isOnline(this)) {
            Thread myTask = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        s3Client.deleteObject(new DeleteObjectRequest(bucketName, pictureName));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            myTask.start();
        }
        PSR_Utils.showAlert(this, response.getMessage(), null);

    }

    @Override
    public Context getMvpContext() {
        return null;
    }

    @Override
    public void onError(Throwable throwable) {
        PSR_Utils.hideProgressDialog();
        deleteS3UploadedImage();
    }


    private void deleteS3UploadedImage() {
        if (PSR_Utils.isOnline(this)) {
            Thread myTask = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        s3Client.deleteObject(new DeleteObjectRequest(bucketName, pictureName));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            myTask.start();
        }
        PSR_Utils.showAlert(this, getResources().getString(R.string.somethingwnt_wrong_txt), null);
    }

    @Override
    public void onNoInternetConnection() {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.showNoNetworkAlert(this);
    }

    @Override
    public void onErrorCode(String s) {
        PSR_Utils.hideProgressDialog();
        deleteS3UploadedImage();
    }


    @Override
    public void onDialogOKClick() {
        uploadtoS3();
    }

    @Override
    public void onDialogNoBtnClick() {
        finish();
    }

    @Override
    public void onClickOk() {
        Intent intent = new Intent(BluredImageActivity.this, DashBoardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onSessionExpired() {
        PSR_Utils.hideProgressDialog();
        if (PSR_Utils.isOnline(this)) {
            Thread myTask = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        s3Client.deleteObject(new DeleteObjectRequest(bucketName, pictureName));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            myTask.start();
        }
        PSR_Utils.doLogout(this, psr_prefsManager);
    }

    @Override
    public void onServerError() {
        PSR_Utils.hideProgressDialog();
        deleteS3UploadedImage();
    }

    public static class lDialogViewHolder2 {
        @BindView(R.id.yes_tv)
        TextView yesTv;
        @BindView(R.id.no_tv)
        TextView noTv;
        @BindView(R.id.dialog_title_tv)
        TextView title;

        @BindView(R.id.dialog_text_tv)
        TextView dialogText;

        lDialogViewHolder2(View view) {
            ButterKnife.bind(this, view);
        }
    }


}