package com.picstar.picstarapp.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;


import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.picstar.picstarapp.R;
import com.picstar.picstarapp.helpers.LocaleHelper;
import com.picstar.picstarapp.mvp.models.videomsgs.createservicerequest.CreateServiceReq;
import com.picstar.picstarapp.mvp.models.videomsgs.createservicerequest.CreateServiceResponse;
import com.picstar.picstarapp.mvp.presenters.PaymentPresenter;
import com.picstar.picstarapp.mvp.views.PaymentView;
import com.picstar.picstarapp.utils.PSRConstants;
import com.picstar.picstarapp.utils.PSR_Utils;
import com.stripe.android.ApiResultCallback;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.Stripe;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.android.view.CardInputListener;
import com.stripe.android.view.CardInputWidget;
import com.stripe.android.view.CardValidCallback;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;


import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.OkHttpClient;

import static com.picstar.picstarapp.utils.PSR_Utils.getServiceCost;

public class PaymentActivity extends BaseActivity implements PaymentView, PSR_Utils.OnSingleBtnDialogClick{

    @BindView(R.id.left_side_menu_option)
    ImageView leftSideMenu;
    @BindView(R.id.title_Tv)
    TextView toolbarTitle;
    CardInputWidget cardInputWidget;
    Button payButton;
    private AmazonS3Client s3Client;
    private String pictureName;
    private String bucketName;
    private String serviceReqTypeId;

    private Stripe stripe;
    Double amountDouble = null;


    private OkHttpClient httpClient = new OkHttpClient();
    static ProgressDialog progressDialog;
    private String photoCost;
    private String celebrityId;
    private int celebrityPhotoId;
    private String photopath;
    private PaymentPresenter paymentPresenter;

    private boolean isCameFromHistory = false;
    private String s3UploadedImageUrl;
    private int eventId;
    private int serviceReqId=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_payment);
        ButterKnife.bind(this);
        paymentPresenter = new PaymentPresenter();
        paymentPresenter.attachMvpView(this);
        cardInputWidget = findViewById(R.id.cardInputWidget);
        cardInputWidget.setPostalCodeEnabled(false);
        cardInputWidget.setPostalCodeRequired(false);
        cardInputWidget.setCardHint("xxxx xxxx xxxx xxxx");
        leftSideMenu.setImageResource(R.drawable.ic_back);
        leftSideMenu.setImageResource(R.drawable.ic_back);
        toolbarTitle.setText(getResources().getString(R.string.payment_txt));
        if (getIntent() != null) {
            serviceReqTypeId = getIntent().getStringExtra(PSRConstants.SERVICEREQTYPEID);

            isCameFromHistory = getIntent().getBooleanExtra(PSRConstants.ISCAMEFROMHISTORY, false);

            if (serviceReqTypeId.equalsIgnoreCase(PSRConstants.LIVESELFIE_SERVICE_REQ_ID)) {

                if (isCameFromHistory && getIntent().hasExtra(PSRConstants.EVENTID)) {
                    s3UploadedImageUrl = getIntent().getStringExtra(PSRConstants.S3UPLOADED_IMAGEURL);
                    eventId = Integer.parseInt(getIntent().getStringExtra(PSRConstants.EVENTID));
                }
                else if(isCameFromHistory){
                    s3UploadedImageUrl = getIntent().getStringExtra(PSRConstants.S3UPLOADED_IMAGEURL);
                }else {
                    photopath = getIntent().getStringExtra("FINALSELFIE");
                    bucketName = getIntent().getStringExtra(PSRConstants.BUCKETNAME);
                }



            } else if (serviceReqTypeId.equalsIgnoreCase(PSRConstants.PHOTOSELFIE_SERVICE_REQ_ID)) {

                if(isCameFromHistory){
                    s3UploadedImageUrl = getIntent().getStringExtra(PSRConstants.S3UPLOADED_IMAGEURL);
                }else {
                    photopath = getIntent().getStringExtra("FINALSELFIE");
                    bucketName = getIntent().getStringExtra(PSRConstants.BUCKETNAME);
                }
                celebrityPhotoId = getIntent().getIntExtra(PSRConstants.CELEBRITYPHOTOID, 0);

            } else if (serviceReqTypeId.equalsIgnoreCase(PSRConstants.VIDEOMSGS_SERVICE_REQ_ID)) {




            } else if (serviceReqTypeId.equalsIgnoreCase(PSRConstants.LIVE_VIDEO_SERVICE_REQ_ID)) {

            }
            if(getIntent().hasExtra(PSRConstants.SERVICEREQID)){
                serviceReqId = getIntent().getIntExtra(PSRConstants.SERVICEREQID, 0);
            }

            celebrityId = getIntent().getStringExtra(PSRConstants.CELEBRITYID);
            String cost = getIntent().getStringExtra(PSRConstants.SERVICECOST);
            if(cost!=null){
                photoCost=getServiceCost(cost);
            }

        }


        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Transaction in progress");
        progressDialog.setCancelable(false);
        httpClient = new OkHttpClient();
        PaymentConfiguration.init(getApplicationContext(), PSRConstants.STRIPE_PUBLISHABLE_KEY);
        //Initialize
        stripe = new Stripe(
                getApplicationContext(),
                Objects.requireNonNull(PSRConstants.STRIPE_PUBLISHABLE_KEY)
        );


        Button payButton = findViewById(R.id.payButton);
        payButton.setText(getResources().getString(R.string.pay_txt) + " $ " + photoCost);
        WeakReference<PaymentActivity> weakActivity = new WeakReference<>(this);
        payButton.setOnClickListener((View view) -> {
            if (PSR_Utils.isOnline(this)) {
                Card card = cardInputWidget.getCard();
                if (card != null) {
                    PSR_Utils.showProgressDialog(this);
                    stripe = new Stripe(getApplicationContext(), PaymentConfiguration.getInstance(getApplicationContext()).getPublishableKey());
                    stripe.createCardToken(card, new ApiResultCallback<Token>() {
                        @Override
                        public void onSuccess(@NonNull Token result) {
                            String tokenID = result.getId();
                            sendTokenToServer(tokenID);
                        }

                        @Override
                        public void onError(@NonNull Exception e) {
                            PSR_Utils.hideProgressDialog();
                            PSR_Utils.showToast(PaymentActivity.this, getResources().getString(R.string.plstry_again_txt));
                            e.printStackTrace();
                            // Handle error
                        }
                    });
                }
            } else {
                PSR_Utils.showNoNetworkAlert(this);
            }
        });


        cardInputWidget.setCardValidCallback(new CardValidCallback() {
            @Override
            public void onInputChanged(boolean isValidCard, @NotNull Set<? extends Fields> set) {
                Log.d("ISVALIDCARD", isValidCard + "");
                if (isValidCard) {
                    payButton.setVisibility(View.VISIBLE);
                } else {
                    payButton.setVisibility(View.GONE);
                }

            }
        });
        cardInputWidget.setCardInputListener(new CardInputListener() {
            @Override
            public void onFocusChange(@NotNull String s) {
                int i = 0;
            }

            @Override
            public void onCardComplete() {
                int i = 0;
            }

            @Override
            public void onExpirationComplete() {
                int i = 0;
            }

            @Override
            public void onCvcComplete() {
                int i = 0;
            }
        });
    }


    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase, LocaleHelper.getLanguage(newBase)));
    }
    @OnClick(R.id.left_side_menu_option)
    void onClickBack(View view) {
        finish();
    }

    private void sendTokenToServer(String tokenID) {

        if (PSR_Utils.isOnline(PaymentActivity.this)) {
            paymentPresenter.doCallStripeChargesApi("",PSRConstants.Stripe_secret_KEY, photoCost, "inr", "test", tokenID);
        } else {
            PSR_Utils.showNoNetworkAlert(PaymentActivity.this);
        }
    }

    private void uploadtoS3(String paymentResponse) {
        if (PSR_Utils.isOnline(this)) {
            Thread myUploadTask = new Thread(new Runnable() {
                public void run() {
                    try {
                        s3Client = new AmazonS3Client(new BasicAWSCredentials(PSRConstants.S3BUCKETACCESSKEYID, PSRConstants.S3BUCKETSECRETACCESSKEY));
                        s3Client.setRegion(Region.getRegion(Regions.US_WEST_2));
                        pictureName = UUID.randomUUID().toString()+PSRConstants.IMAGE_FILE_EXTENSION;
                        Log.d("PICTURENAME", pictureName);
                        PutObjectRequest por = new PutObjectRequest(bucketName, pictureName, new java.io.File(photopath));
                        s3Client.putObject(por);

                        URL url = s3Client.getUrl(bucketName, pictureName);

                        if (url != null) {
                            String s3UploadedImageUrl = url.getProtocol() + "://" + url.getHost() + url.getPath();
                            createServiceReq(s3UploadedImageUrl, paymentResponse);

                        } else {
                            PSR_Utils.hideProgressDialog();
                            PSR_Utils.showToast(PaymentActivity.this, getResources().getString(R.string.unableto_savephoto_txt));
                        }
                        Log.d("BUCKETURL", url.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                        PSR_Utils.hideProgressDialog();
                        PSR_Utils.showToast(PaymentActivity.this, getResources().getString(R.string.unableto_savephoto_txt));
                    }

                }
            });
            myUploadTask.start();

        } else {
            PSR_Utils.showNoNetworkAlert(this);
        }
    }


    private void createServiceReq(String imageUrl, String paymentResponse) {
        if (PSR_Utils.isOnline(this)) {
            CreateServiceReq createServiceReq = new CreateServiceReq();
            createServiceReq.setCelebrityId(celebrityId);
            createServiceReq.setUserId(psr_prefsManager.get(PSRConstants.USERID));
            createServiceReq.setFilePath(imageUrl);
            createServiceReq.setPhotoId(celebrityPhotoId);
            createServiceReq.setServiceRequestTypeId(Integer.parseInt(serviceReqTypeId));
            createServiceReq.setServiceRequestId(serviceReqId);
            createServiceReq.setEventId(eventId);
            createServiceReq.setPaymentInfo(paymentResponse);
            paymentPresenter.doCreatePaymentServReq(psr_prefsManager.get(PSRConstants.SELECTED_LANGUAGE),PSR_Utils.getHeader(psr_prefsManager), createServiceReq);
        } else {
            PSR_Utils.showNoNetworkAlert(this);
        }
    }

    @Override
    public void onSessionExpired() {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.doLogout(this, psr_prefsManager);
    }

    @Override
    public void onServerError() {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.showAlert(this, getResources().getString(R.string.somethingwnt_wrong_txt), null);
    }

    @Override
    public Context getMvpContext() {
        return null;
    }

    @Override
    public void onError(Throwable throwable) {
        PSR_Utils.hideProgressDialog();
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
        PSR_Utils.showAlert(this, getResources().getString(R.string.somethingwnt_wrong_txt), null);
    }

    @Override
    public void onGettingChargesResponseSuccess(String paymentResponse) {
        if (isCameFromHistory) {
            createServiceReq(s3UploadedImageUrl, paymentResponse);
        } else {
            uploadtoS3(paymentResponse);
        }

        Log.d("PAYMENT_RESPONSE", paymentResponse);
    }

    @Override
    public void onGettingChargesResponseFailure() {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.showAlert(this, getResources().getString(R.string.plstry_again_txt), null);
    }

    @Override
    public void onCreatingPaymentServReqSuccess(CreateServiceResponse response) {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.singleBtnAlert(this, response.getMessage(), null, this);
    }

    @Override
    public void userBlocked(String msg) {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.singleBtnAlert(this,msg,null,this);
    }

    @Override
    public void onCreatingPaymentServReqFailure(CreateServiceResponse response) {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.showAlert(this, response.getMessage(), null);
    }

    @Override
    public void onClickOk() {
        Intent intent = new Intent(PaymentActivity.this, DashBoardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

}
