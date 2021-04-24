package com.picstar.picstarapp.activities;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Layout;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.github.siyamed.shapeimageview.ShaderImageView;
import com.picstar.picstarapp.R;
import com.picstar.picstarapp.mvp.models.celebrities.Info;
import com.picstar.picstarapp.mvp.models.celebrities.ServicesOffering;
import com.picstar.picstarapp.utils.PSRConstants;
import com.picstar.picstarapp.utils.PSR_Utils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.picstar.picstarapp.utils.PSR_Utils.getServiceCost;

public class CelebrityDetailsActivity extends BaseActivity {
    @BindView(R.id.left_side_menu_option)
    ImageView leftSideMenu;
    @BindView(R.id.search_option_btn)
    ImageView searchOption;
    @BindView(R.id.title_Tv)
    TextView titleTv;
    @BindView(R.id.celebrityname_tv)
    TextView celebrityNameTv;
    @BindView(R.id.celebrity_location_tv)
    TextView celebrityLocationTv;
    @BindView(R.id.coverpic_ImgVw)
    ImageView coverPicImgV;
    @BindView(R.id.profilePic_imgvw)
    ShaderImageView profilePicImgV;
    @BindView(R.id.celebrity_name_full_tv)
    TextView celebrityFullName;
    @BindView(R.id.celebrity_descriptn_tv)
    TextView celebrityDescrptnTv;
    @BindView(R.id.viewmore_txt)
    TextView viewMoreTv;
    @BindView(R.id.lksnc)
    View lksnc;




    @BindView(R.id.service_one_layout)
    LinearLayout serviceOneLayout;
    @BindView(R.id.service_one_imgv)
    ImageView serviceOneImgv;
    @BindView(R.id.service_one_amount)
    TextView serviceOneAmount;
    @BindView(R.id.service_one_name)
    TextView serviceOneName;
    @BindView(R.id.service_one_totalcount)
    TextView serviceOneTotalCount;


    @BindView(R.id.service_two_layout)
    LinearLayout serviceTwoLayout;
    @BindView(R.id.service_two_imgv)
    ImageView serviceTwoImgv;
    @BindView(R.id.service_two_amount)
    TextView serviceTwoAmount;
    @BindView(R.id.service_two_name)
    TextView serviceTwoName;
    @BindView(R.id.service_two_totalcount)
    TextView serviceTwoTotalCount;

    @BindView(R.id.service_three_layout)
    LinearLayout serviceThreeLayout;
    @BindView(R.id.service_three_imgv)
    ImageView serviceThreeImgv;
    @BindView(R.id.service_three_amount)
    TextView serviceThreeAmount;
    @BindView(R.id.service_three_name)
    TextView serviceThreeName;
    @BindView(R.id.service_three_totalcount)
    TextView serviceThreeTotalCount;

    @BindView(R.id.service_four_layout)
    LinearLayout serviceFourLayout;
    @BindView(R.id.service_four_imgv)
    ImageView serviceFourImgv;
    @BindView(R.id.service_four_amount)
    TextView serviceFourAmount;
    @BindView(R.id.service_four_name)
    TextView serviceFourName;
    @BindView(R.id.service_four_totalcount)
    TextView serviceFourTotalCount;
    @BindView(R.id.view_underline)
    View viewUnderline;
    @BindView(R.id.service_one_amount_textview2)
    TextView serviceOneAmountTextview2;
    @BindView(R.id.merchandise_btn)
    Button merchandiseBtn;
    @BindView(R.id.service_one_layout2)
    LinearLayout serviceOneLayout2;

    private boolean isClickedViewMore = false;
    static int MAXLINES = 100;
    static int MINLINES = 4;
    Info selectedcelebrityDetails;
    private int screenWidth = 0;
    private boolean isLiveSelfieServiceProvided=false;
    private String liveSelfieCost;
    private String photoSelfieCost;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.celebrity_details_layout);
        ButterKnife.bind(this);
        leftSideMenu.setImageResource(R.drawable.ic_back);

        titleTv.setText(getString(R.string.profile_txt));

        TextView tv_yourtext = (TextView) findViewById(R.id.celebrity_descriptn_tv);
        //  hasEllipsis(tv_yourtext);
        if (getIntent() != null) {
            selectedcelebrityDetails = (Info) getIntent().getSerializableExtra("CELEBRITY_DETAILS");
            updatingUi(selectedcelebrityDetails);
        }


    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @OnClick(R.id.search_option_btn)
    void onClickCamera(View v) {
        if(isLiveSelfieServiceProvided) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(CelebrityDetailsActivity.this, LiveSelfieCameraActivity.class);
                    intent.putExtra(PSRConstants.SERVICECOST,liveSelfieCost);
                    intent.putExtra(PSRConstants.CELEBRITYID, selectedcelebrityDetails.getUserId());
                    intent.putExtra(PSRConstants.SELECTEDCELEBRITYNAME,selectedcelebrityDetails.getUsername());
                    intent.putExtra(PSRConstants.ISCAMEFROMHISTORY,false);
                    startActivity(intent);

                }
            };
            PSR_Utils.checkPermissionToProgress(this, runnable);

        }
    }


    @OnClick(R.id.left_side_menu_option)
    void onClickSideMenu(View v) {
        finish();
    }

    @OnClick(R.id.merchandise_btn)
    void onClickMerchandise(View v) {
        int color = Color.TRANSPARENT;
        Drawable background = lksnc.getBackground();
        if (background instanceof ColorDrawable)
            color = ((ColorDrawable) background).getColor();
        PSR_Utils.showAlert(this, "Work in Progress", null);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    void updatingUi(Info celebrity) {
        screenWidth = PSR_Utils.getScreenWidth(this);
        celebrityNameTv.setText(celebrity.getUsername());
        celebrityLocationTv.setText(celebrity.getCelebrityLocation());
        celebrityFullName.setText(celebrity.getUsername());
        celebrityDescrptnTv.setText(celebrity.getCelebrityDescription());
        Glide.with(this)
                .load(celebrity.getCoverPic())
                .centerCrop()
                .placeholder(R.drawable.ic_coverpholder)
                .into(coverPicImgV);
        Glide.with(this)
                .load(celebrity.getProfilePic())
                .centerCrop()
                .placeholder(R.drawable.ic_profilepholder)
                .into(profilePicImgV);




        if (celebrity.getServicesOffering().size() != 0) {

            for(ServicesOffering servicesOffering:celebrity.getServicesOffering()){

                if(servicesOffering.getServiceId()==Integer.parseInt(PSRConstants.LIVESELFIE_SERVICE_REQ_ID)){
                    searchOption.setImageResource(R.drawable.ic_camera);
                    liveSelfieCost=servicesOffering.getServiceCost().toString();
                    isLiveSelfieServiceProvided=true;
                }else if(servicesOffering.getServiceId()==Integer.parseInt(PSRConstants.PHOTOSELFIE_SERVICE_REQ_ID)){
                    photoSelfieCost= servicesOffering.getServiceCost().toString();
                }
            }


            if (celebrity.getServicesOffering().size() == 1) {
                updatUiForSingleService(celebrity.getServicesOffering().get(0));
            } else if (celebrity.getServicesOffering().size() == 2) {
                updateUiforTwoServices(celebrity.getServicesOffering());
            } else if (celebrity.getServicesOffering().size() == 3) {
                updateUiforThreeServices(celebrity.getServicesOffering());
            } else if (celebrity.getServicesOffering().size() == 4) {
                updateUiforFourServices(celebrity.getServicesOffering());
            }

        }



            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            int screenwidth = displayMetrics.widthPixels;
            int buttonWidth = displayMetrics.widthPixels / 3 - 40;
            coverPicImgV.getLayoutParams().height = screenwidth;


        celebrityDescrptnTv.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Layout l = celebrityDescrptnTv.getLayout();
                if (l != null) {
                    int lines = l.getLineCount();
                    if (lines > 0)
                        if (l.getEllipsisCount(lines - 1) > 0) {
                            Log.d("TAG", "Text is ellipsized");
                            viewMoreTv.setVisibility(View.VISIBLE);
                        }/*else
                        {
                            Log.d("TAG", "Text is  not ellipsized");
                            viewMoreTv.setVisibility(View.GONE);
                        }*/

                }
            }
        });


    }


    @OnClick(R.id.viewmore_txt)
    void onClickViewMore(View view) {
        if (isClickedViewMore) {
            viewMoreTv.setText(R.string.viewmore_txt);
            isClickedViewMore = false;
            celebrityDescrptnTv.setEllipsize(TextUtils.TruncateAt.END);
            celebrityDescrptnTv.setMaxLines(MINLINES);

        } else {
            viewMoreTv.setText(R.string.viewless_txt);
            isClickedViewMore = true;
            celebrityDescrptnTv.setMaxLines(MAXLINES);
            celebrityDescrptnTv.setEllipsize(null);
        }
    }


    @OnClick(R.id.service_one_layout)
    void onClickServiceOne(View view) {
        navigateToCorrespondingServiceScreen(Integer.parseInt(serviceOneLayout.getTag().toString()));
    }

    @OnClick(R.id.service_two_layout)
    void onClickServiceTwo(View view) {
        navigateToCorrespondingServiceScreen(Integer.parseInt(serviceTwoLayout.getTag().toString()));
    }

    @OnClick(R.id.service_three_layout)
    void onClickServiceThree(View view) {
        navigateToCorrespondingServiceScreen(Integer.parseInt(serviceThreeLayout.getTag().toString()));
    }

    @OnClick(R.id.service_four_layout)
    void onClickServiceFour(View view) {
        navigateToCorrespondingServiceScreen(Integer.parseInt(serviceFourLayout.getTag().toString()));
    }

    private void updatUiForSingleService(ServicesOffering offering) {

        serviceOneLayout.setVisibility(View.VISIBLE);
        serviceOneAmount.setVisibility(View.GONE);
        serviceOneLayout2.setVisibility(View.VISIBLE);

        serviceOneLayout.setTag(offering.getServiceId());
        serviceOneName.setText(offering.getServiceDetails().getServiceName());
        serviceOneImgv.setBackground(getServiceBg(offering.getServiceId()));
        serviceOneImgv.setImageDrawable(getServiceIcon(offering.getServiceId()));
        serviceOneAmountTextview2.setText("$ " + getServiceCost(offering.getServiceCost().toString()));


        serviceOneTotalCount.setText(getServiceTotalCount(offering.getServiceId()));

    }

    private void updateUiforTwoServices(List<ServicesOffering> offeringList) {

        serviceOneLayout.setVisibility(View.VISIBLE);
        serviceOneLayout.setTag(offeringList.get(0).getServiceId());
        serviceOneName.setText(offeringList.get(0).getServiceDetails().getServiceName());
        serviceOneImgv.setBackground(getServiceBg(offeringList.get(0).getServiceId()));
        serviceOneImgv.setImageDrawable(getServiceIcon(offeringList.get(0).getServiceId()));
        serviceOneAmount.setText("$ " + getServiceCost(offeringList.get(0).getServiceCost().toString()));


        serviceOneTotalCount.setText(getServiceTotalCount(offeringList.get(0).getServiceId()));


        serviceTwoLayout.setVisibility(View.VISIBLE);
        serviceTwoLayout.setTag(offeringList.get(1).getServiceId());
        serviceTwoName.setText(offeringList.get(1).getServiceDetails().getServiceName());
        serviceTwoImgv.setBackground(getServiceBg(offeringList.get(1).getServiceId()));
        serviceTwoImgv.setImageDrawable(getServiceIcon(offeringList.get(1).getServiceId()));
        serviceTwoAmount.setText("$ " + getServiceCost(offeringList.get(1).getServiceCost().toString()));


        serviceTwoTotalCount.setText(getServiceTotalCount(offeringList.get(1).getServiceId()));

    }


    private void updateUiforThreeServices(List<ServicesOffering> offeringList) {

        serviceOneLayout.setVisibility(View.VISIBLE);
        serviceOneAmount.setVisibility(View.GONE);
        serviceOneLayout2.setVisibility(View.VISIBLE);

        serviceOneLayout.setTag(offeringList.get(0).getServiceId());
        serviceOneName.setTextSize(15);
        serviceOneName.setText(offeringList.get(0).getServiceDetails().getServiceName());
        serviceOneImgv.setBackground(getServiceBg(offeringList.get(0).getServiceId()));
        serviceOneImgv.setImageDrawable(getServiceIcon(offeringList.get(0).getServiceId()));
        serviceOneAmountTextview2.setText("$ " + getServiceCost(offeringList.get(0).getServiceCost().toString()));
        serviceOneTotalCount.setText(getServiceTotalCount(offeringList.get(0).getServiceId()));


        serviceThreeLayout.setVisibility(View.VISIBLE);
        serviceThreeLayout.setTag(offeringList.get(1).getServiceId());
        serviceThreeName.setText(offeringList.get(1).getServiceDetails().getServiceName());
        serviceThreeImgv.setBackground(getServiceBg(offeringList.get(1).getServiceId()));
        serviceThreeImgv.setImageDrawable(getServiceIcon(offeringList.get(1).getServiceId()));
        serviceThreeAmount.setText("$ " + getServiceCost(offeringList.get(1).getServiceCost().toString()));
        serviceThreeTotalCount.setText(getServiceTotalCount(offeringList.get(1).getServiceId()));


        serviceFourLayout.setVisibility(View.VISIBLE);
        serviceFourLayout.setTag(offeringList.get(2).getServiceId());
        serviceFourName.setText(offeringList.get(2).getServiceDetails().getServiceName());
        serviceFourImgv.setBackground(getServiceBg(offeringList.get(2).getServiceId()));
        serviceFourImgv.setImageDrawable(getServiceIcon(offeringList.get(2).getServiceId()));
        serviceFourAmount.setText("$ " + getServiceCost(offeringList.get(2).getServiceCost().toString()));

        serviceFourTotalCount.setText(getServiceTotalCount(offeringList.get(2).getServiceId()));


    }

    private void updateUiforFourServices(List<ServicesOffering> offeringList) {

        serviceOneLayout.setVisibility(View.VISIBLE);
        serviceOneLayout.setTag(offeringList.get(0).getServiceId());
        serviceOneName.setText(offeringList.get(0).getServiceDetails().getServiceName());
        serviceOneImgv.setBackground(getServiceBg(offeringList.get(0).getServiceId()));
        serviceOneImgv.setImageDrawable(getServiceIcon(offeringList.get(0).getServiceId()));
        serviceOneAmount.setText("$ " + getServiceCost(offeringList.get(0).getServiceCost().toString()));

        serviceOneTotalCount.setText(getServiceTotalCount(offeringList.get(0).getServiceId()));

        serviceTwoLayout.setVisibility(View.VISIBLE);
        serviceTwoLayout.setTag(offeringList.get(1).getServiceId());
        serviceTwoName.setText(offeringList.get(1).getServiceDetails().getServiceName());
        serviceTwoImgv.setBackground(getServiceBg(offeringList.get(1).getServiceId()));
        serviceTwoImgv.setImageDrawable(getServiceIcon(offeringList.get(1).getServiceId()));
        serviceTwoAmount.setText("$ " + getServiceCost(offeringList.get(1).getServiceCost().toString()));


        serviceTwoTotalCount.setText(getServiceTotalCount(offeringList.get(1).getServiceId()));

        serviceThreeLayout.setVisibility(View.VISIBLE);
        serviceThreeLayout.setTag(offeringList.get(2).getServiceId());
        serviceThreeName.setText(offeringList.get(2).getServiceDetails().getServiceName());
        serviceThreeImgv.setBackground(getServiceBg(offeringList.get(2).getServiceId()));
        serviceThreeImgv.setImageDrawable(getServiceIcon(offeringList.get(2).getServiceId()));
        serviceThreeAmount.setText("$ " + getServiceCost(offeringList.get(2).getServiceCost().toString()));

        serviceThreeTotalCount.setText(getServiceTotalCount(offeringList.get(2).getServiceId()));


        serviceFourLayout.setVisibility(View.VISIBLE);
        serviceFourLayout.setTag(offeringList.get(3).getServiceId());
        serviceFourName.setText(offeringList.get(3).getServiceDetails().getServiceName());
        serviceFourImgv.setBackground(getServiceBg(offeringList.get(3).getServiceId()));
        serviceFourImgv.setImageDrawable(getServiceIcon(offeringList.get(3).getServiceId()));
        serviceFourAmount.setText("$ " + getServiceCost(offeringList.get(3).getServiceCost().toString()));
        serviceFourTotalCount.setText(getServiceTotalCount(offeringList.get(3).getServiceId()));

    }






    private void navigateToCorrespondingServiceScreen(int serviceId) {
        if (serviceId == 1) {
            Intent intent = new Intent(this, StockPhotosActivity.class);
            intent.putExtra(PSRConstants.USERID, selectedcelebrityDetails.getUserId());
            intent.putExtra(PSRConstants.PHOTOSELFIECOST,photoSelfieCost);
            startActivity(intent);
        } else if (serviceId == 2) {
            Intent intent = new Intent(this, LiveSelfieActivity.class);
            intent.putExtra(PSRConstants.USERID, selectedcelebrityDetails.getUserId());
            intent.putExtra(PSRConstants.PROFILEPICURl, selectedcelebrityDetails.getProfilePic());
            startActivity(intent);
        } else if (serviceId == 3) {
            Intent intent = new Intent(this, VideoMessageActivity.class);
            intent.putExtra(PSRConstants.USERID, selectedcelebrityDetails.getUserId());
            startActivity(intent);
        } else if (serviceId == 4) {
            Intent intent = new Intent(this, LiveVideoActivity.class);
            intent.putExtra(PSRConstants.USERID, selectedcelebrityDetails.getUserId());
            startActivity(intent);

        }

    }

    private String getServiceTotalCount(int serviceId) {
        String serviceTotalCount = "";
        if (serviceId == 1) {
            serviceTotalCount = String.valueOf(selectedcelebrityDetails.getPhotoSelfiesCount());

        } else if (serviceId == 2) {
            serviceTotalCount =String.valueOf( selectedcelebrityDetails.getLivePhotoSelfiesCount());
        } else if (serviceId == 3) {
            serviceTotalCount = String.valueOf(selectedcelebrityDetails.getVideoMessagesCount());

        } else if (serviceId == 4) {
            serviceTotalCount = String.valueOf(selectedcelebrityDetails.getLiveVideosCount());

        }
        return serviceTotalCount;

    }

    private Drawable getServiceBg(int serviceId) {
        Drawable drawable = null;
        if (serviceId == 1) {
            drawable = getResources().getDrawable(R.drawable.photoselfie_bg);

        } else if (serviceId == 2) {
            drawable = getResources().getDrawable(R.drawable.liveselfie_bg);
        } else if (serviceId == 3) {
            drawable = getResources().getDrawable(R.drawable.videomsg_bg);

        } else if (serviceId == 4) {
            drawable = getResources().getDrawable(R.drawable.livevideo_bg);

        }
        return drawable;
    }

    Drawable getServiceIcon(int serviceId) {
        Drawable drawable = null;
        if (serviceId == 1) {
            drawable = getResources().getDrawable(R.drawable.ic_photoselfie_white);

        } else if (serviceId == 2) {
            drawable = getResources().getDrawable(R.drawable.ic_liveselfie_white);
        } else if (serviceId == 3) {
            drawable = getResources().getDrawable(R.drawable.ic_videomsg_white);

        } else if (serviceId == 4) {
            drawable = getResources().getDrawable(R.drawable.ic_livevideo);

        }
        return drawable;
    }



}
