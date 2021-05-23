package com.picstar.picstarapp.activities;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.bumptech.glide.Glide;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.picstar.picstarapp.R;
import com.picstar.picstarapp.callbacks.ProfileUpdateSuccess;
import com.picstar.picstarapp.helpers.LocaleHelper;
import com.picstar.picstarapp.mvp.models.updateprofile.UpdateProfileReq;
import com.picstar.picstarapp.mvp.models.updateprofile.UpdateProfileResponse;
import com.picstar.picstarapp.mvp.models.videomsgs.createservicerequest.CreateServiceReq;
import com.picstar.picstarapp.mvp.presenters.UpdateUserProfilePresenter;
import com.picstar.picstarapp.mvp.views.UpdateUserProfileView;
import com.picstar.picstarapp.utils.FileUtils;
import com.picstar.picstarapp.utils.PSRConstants;
import com.picstar.picstarapp.utils.PSR_Utils;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import br.com.packapps.retropicker.callback.CallbackPicker;
import br.com.packapps.retropicker.config.Retropicker;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static java.security.AccessController.getContext;

public class MyProfileActivity extends BaseActivity implements AdapterView.OnItemSelectedListener, UpdateUserProfileView, ProfileUpdateSuccess, PSR_Utils.OnSingleBtnDialogClick {
    @BindView(R.id.username_et)
    EditText userNameEt;
    @BindView(R.id.username_tv)
    TextView userNameTv;
    @BindView(R.id.name_ic_next)
    ImageView userNameNextIc;
    @BindView(R.id.email_tv)
    TextView emailTv;
    @BindView(R.id.phone_number_et)
    EditText phoneNumberEt;
    @BindView(R.id.phone_number_next_btn)
    ImageView phoneNumberNextIc;
    @BindView(R.id.phone_number_tv)
    TextView phoneNumberTv;
    @BindView(R.id.birthday_tv)
    TextView dateOfBirthTv;
    @BindView(R.id.gender_spinner)
    Spinner genderSpinner;
    @BindView(R.id.gender_tv)
    TextView genderTv;
    @BindView(R.id.profilepic_imgView)
    ImageView profilePicImgv;
    @BindView(R.id.username_main_TV)
    TextView toolBarUserName;
    private DatePickerDialog picker;
    String[] items;
    UpdateUserProfilePresenter updateUserProfilePresenter;
    private AmazonS3Client s3Client;
    private String pictureName;
    public String imagePath;
    private String finalDateOfBirth;
    private boolean isCameFirst = true;
    private String clickedPhotoPath = "";
    private static final int CLICK_PHOTO = 5;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_myprofile);
        ButterKnife.bind(this);
        items = new String[]{getResources().getString(R.string.male_txt), getResources().getString(R.string.female_txt), getResources().getString(R.string.other_txt)};
        updateUserProfilePresenter = new UpdateUserProfilePresenter();
        updateUserProfilePresenter.attachMvpView(this);
        toolBarUserName.setText(psr_prefsManager.get(PSRConstants.USERNAME));
        userNameTv.setText(psr_prefsManager.get(PSRConstants.USERNAME));
        emailTv.setText(psr_prefsManager.get(PSRConstants.USEREMAIL));
        phoneNumberTv.setText(psr_prefsManager.get(PSRConstants.USERPHONENUMBER));
        dateOfBirthTv.setText(psr_prefsManager.get(PSRConstants.USERDOB));
        finalDateOfBirth = psr_prefsManager.get(PSRConstants.USERSERVERDOB);
        genderTv.setText(psr_prefsManager.get(PSRConstants.USERGENDER));
        if (savedInstanceState != null) {
            clickedPhotoPath = savedInstanceState.getString("PHOTOPATH");
        }
        Spinner spin = (Spinner) findViewById(R.id.gender_spinner);
        spin.setOnItemSelectedListener(this);
        //  if (savedInstanceState == null) {
        Glide.with(this)
                .load(psr_prefsManager.get(PSRConstants.USERPROFILEPIC))
                .centerCrop()
                .placeholder(R.drawable.ic_profilepholder)
                .into(profilePicImgv);
        // }

        ArrayAdapter aa = new ArrayAdapter(this, android.R.layout.simple_spinner_item, items);
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin.setAdapter(aa);
        for (int i = 0; i < items.length; i++) {
            if (items[i].equalsIgnoreCase(psr_prefsManager.get(PSRConstants.USERGENDER))) {
                spin.setSelection(i);

            }
        }
        checkRunTimePermissions();
        userNameEt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_LEFT = 0;
                final int DRAWABLE_TOP = 1;
                final int DRAWABLE_RIGHT = 2;
                final int DRAWABLE_BOTTOM = 3;
                //  if (!searchEt.getText().toString().isEmpty()) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getRawX() >= (userNameEt.getRight() - userNameEt.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        // your action here

                        userNameEt.setVisibility(View.GONE);
                        userNameNextIc.setVisibility(View.VISIBLE);
                        userNameTv.setVisibility(View.VISIBLE);
                        userNameTv.setText(userNameEt.getText().toString().trim());
                        PSR_Utils.hideKeyboardIfOpened(MyProfileActivity.this);
                        return true;
                    }
                    //   }
                }
                return false;
            }
        });
        phoneNumberEt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_RIGHT = 2;

                //  if (!searchEt.getText().toString().isEmpty()) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getRawX() >= (phoneNumberEt.getRight() - phoneNumberEt.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        // your action here

                        phoneNumberEt.setVisibility(View.GONE);
                        phoneNumberNextIc.setVisibility(View.VISIBLE);
                        phoneNumberTv.setVisibility(View.VISIBLE);
                        phoneNumberTv.setText(phoneNumberEt.getText().toString().trim());
                        PSR_Utils.hideKeyboardIfOpened(MyProfileActivity.this);
                        return true;
                    }
                    //   }
                }
                return false;
            }
        });
    }


    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase, LocaleHelper.getLanguage(newBase)));
    }

    @OnClick(R.id.profilepic_imgView)
    void onClickProfilePic(View view) {
        profilePicDialog();
    }

    @OnClick({R.id.username_tv, R.id.name_ic_next})
    void onClickUserName(View view) {
        userNameEt.setVisibility(View.VISIBLE);
        userNameEt.setText(userNameTv.getText().toString());

        userNameTv.setVisibility(View.GONE);
        userNameNextIc.setVisibility(View.GONE);
    }

    @OnClick({R.id.phone_number_tv, R.id.phone_number_next_btn})
    void onClickPhoneNumb(View view) {
        phoneNumberTv.setVisibility(View.GONE);
        phoneNumberNextIc.setVisibility(View.GONE);
        phoneNumberEt.setVisibility(View.VISIBLE);
        phoneNumberEt.setText(phoneNumberTv.getText().toString());


    }


    @OnClick({R.id.birthday_tv, R.id.birthday_ic_next})
    void onClickDob(View view) {
        final Calendar cldr = Calendar.getInstance();
        int day = cldr.get(Calendar.DAY_OF_MONTH);
        int month = cldr.get(Calendar.MONTH);
        int year = cldr.get(Calendar.YEAR);
        picker = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        dateOfBirthTv.setText(PSR_Utils.getMonthName(monthOfYear) + " " + dayOfMonth + ", " + year);
                        String finalmonth;
                        int month = monthOfYear + 1;
                        if (month < 10) {
                            finalmonth = "0" + month;
                        } else {
                            finalmonth = month + "";
                        }
                        finalDateOfBirth = year + "-" + finalmonth + "-" + dayOfMonth;

                    }
                }, year, month, day);
        picker.getDatePicker().setMaxDate(new Date().getTime());
        picker.show();

    }

    @OnClick({R.id.gender_tv, R.id.gender_next_imgv})
    void onClickGendertV(View view) {
        genderSpinner.performClick();
    }


    @OnClick(R.id.back_btn)
    void onClickBack(View view) {
        PSR_Utils.hideKeyboardIfOpened(this);
        finish();
    }

    @OnClick(R.id.submit_btn)
    void onClickSubmit(View view) {

        if (userNameTv.getText().toString().trim().isEmpty()) {
            PSR_Utils.showAlert(this, getResources().getString(R.string.entername_alert_msg), null);
            return;
        }
        if (genderTv.getText().toString().trim().isEmpty()) {
            PSR_Utils.showAlert(this, getResources().getString(R.string.select_gender_alertmsg), null);
            return;
        }

        if (dateOfBirthTv.getText().toString().trim().isEmpty()) {
            PSR_Utils.showAlert(this, getResources().getString(R.string.select_dob_alertmsg), null);
            return;
        }

        if (!isValidE123(phoneNumberTv.getText().toString())) {
            PSR_Utils.showAlert(this, getResources().getString(R.string.entervalid_phonenumb_alertmsg), null);
            return;
        }

        if (PSR_Utils.isOnline(this)) {
            PSR_Utils.showProgressDialog(this);
            if (imagePath != null && !imagePath.isEmpty()) {
                uploadtoS3(imagePath);
            } else {
                updateProfile(psr_prefsManager.get(PSRConstants.USERPROFILEPIC));
            }

        } else {
            PSR_Utils.showNoNetworkAlert(this);
        }
    }

    public static boolean isValidE123(String s) {
        Pattern p = Pattern.compile("^\\+(?:[0-9] ?){6,14}[0-9]$");
        Matcher m = p.matcher(s);
        return m.matches();

    }

    private void updateProfile(String url) {
        String gender;
        if (genderTv.getText().toString().equalsIgnoreCase("Male")) {
            gender = "M";
        } else if (genderTv.getText().toString().equalsIgnoreCase("Female")) {
            gender = "F";
        } else {
            gender = "O";
        }
        UpdateProfileReq updateProfileReq = new UpdateProfileReq();
        updateProfileReq.setUserId(psr_prefsManager.get(PSRConstants.USERID));
        updateProfileReq.setUsername(userNameTv.getText().toString().trim());
        updateProfileReq.setProfilePic(url);
        updateProfileReq.setDob(finalDateOfBirth);
        updateProfileReq.setGender(gender);
        updateProfileReq.setPhoneNumber(phoneNumberTv.getText().toString().trim());
        updateUserProfilePresenter.updateUserProfile(psr_prefsManager.get(PSRConstants.SELECTED_LANGUAGE), PSR_Utils.getHeader(psr_prefsManager), updateProfileReq);
    }


    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        genderTv.setText(items[i]);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        int i = 0;
    }

    @Override
    public void onUpdatingSuccess(UpdateProfileResponse response) {
        PSR_Utils.hideProgressDialog();
        psr_prefsManager.save(PSRConstants.USERPROFILEPIC, response.getInfo().getProfilePic());
        psr_prefsManager.save(PSRConstants.USERNAME, response.getInfo().getUsername());
        psr_prefsManager.save(PSRConstants.USERPHONENUMBER, response.getInfo().getPhoneNumber());
        if (response.getInfo().getGender().equalsIgnoreCase("M")) {
            psr_prefsManager.save(PSRConstants.USERGENDER, getResources().getString(R.string.male_txt));
        } else if (response.getInfo().getGender().equalsIgnoreCase("F")) {
            psr_prefsManager.save(PSRConstants.USERGENDER, getResources().getString(R.string.female_txt));
        } else if (response.getInfo().getGender().equalsIgnoreCase("O")) {
            psr_prefsManager.save(PSRConstants.USERGENDER, getResources().getString(R.string.other_txt));
        }
        try {
            Date serverDate = null;
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.ENGLISH);
            serverDate = df.parse(response.getInfo().getDob());
            psr_prefsManager.save(PSRConstants.USERDOB, new SimpleDateFormat("MMMM dd,yyyy").format(serverDate));
            psr_prefsManager.save(PSRConstants.USERSERVERDOB, new SimpleDateFormat("yyyy-MM-dd").format(serverDate));

        } catch (Exception e) {
            e.printStackTrace();
        }

        PSR_Utils.successAlert(this, response.getMessage(), this);
    }

    @Override
    public void userBlocked(String msg) {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.singleBtnAlert(this, msg, null, this);
    }

    @Override
    public void onUpdatingFailure(UpdateProfileResponse response) {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.showAlert(this, response.getMessage(), null);
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


    private void profilePicDialog() {
        final String[] items = {getResources().getString(R.string.gallery_txt), getResources().getString(R.string.camera_txt)};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.chooseoption_txt));
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int clickedPosition) {
                if (clickedPosition == 0) {
                    if (ActivityCompat.checkSelfPermission(MyProfileActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MyProfileActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        openGalleryApp();
                    } else {
                        PSR_Utils.showToast(MyProfileActivity.this, getResources().getString(R.string.gallery_runtimepermission_alert_txt));
                    }

                } else if (clickedPosition == 1) {
                    if (ActivityCompat.checkSelfPermission(MyProfileActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MyProfileActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        launchCamera();
                    } else {
                        PSR_Utils.showToast(MyProfileActivity.this, getResources().getString(R.string.cam_Permision_deny_txt));
                    }
                }
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }


    public void openGalleryApp() {
        try {
            Retropicker.Builder builder = new Retropicker.Builder(this)
                    .setTypeAction(Retropicker.GALLERY_PICKER) //Para abrir a galeria passe Retropicker.GALLERY_PICKER
                    .checkPermission(true);

            builder.enquee(new CallbackPicker() {
                @Override
                public void onSuccess(Bitmap bitmap, String uri) {
                    if (bitmap != null) {
                        //  Uri uri = getImageUri(MyProfileActivity.this, bitmap);
                        CropImage.activity(Uri.parse(uri))
                                .setAllowFlipping(false)
                                .setAspectRatio(1, 1)
                                .setCropMenuCropButtonTitle("Done")
                                .start(MyProfileActivity.this);

                    }
                }

                @Override
                public void onFailure(Throwable e) {
                    PSR_Utils.showToast(MyProfileActivity.this, e.getLocalizedMessage());
                }
            });
            Retropicker retropicker = builder.create();
            retropicker.open();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        String path = Environment.getExternalStorageDirectory().toString();
        File imageFile = new File(path, "picstar_profile.jpg");
        try (FileOutputStream out = new FileOutputStream(imageFile)) {
            inImage.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Uri.fromFile(imageFile);


      /*ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);*/


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                profilePicImgv.setImageURI(result.getUri());
                MyProfileActivity.this.imagePath = FileUtils.getPath(MyProfileActivity.this, result.getUri());
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                PSR_Utils.showToast(this, "error");
                Exception error = result.getError();
            }
        } else if (requestCode == CLICK_PHOTO) {
            if (resultCode == RESULT_OK) {
                CropImage.activity(Uri.fromFile(new File(clickedPhotoPath)))
                        .setAllowFlipping(false)
                        .setAspectRatio(1, 1)
                        .setCropMenuCropButtonTitle("Done")
                        .start(MyProfileActivity.this);
            }
        }

    }

    public void checkRunTimePermissions() {
        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        // check if all permissions are granted

                        if (report.areAllPermissionsGranted()) {
                            //runnable.run();
                        } else {
                            PSR_Utils.showToast(MyProfileActivity.this, getResources().getString(R.string.galleryandcamera_runtimepermission_alert_txt));
                        }
                        // check for permanent denial of any permission
                        if (report.isAnyPermissionPermanentlyDenied()) {
                            PSR_Utils.showToast(MyProfileActivity.this, getResources().getString(R.string.galleryandcamera_runtimepermission_alert_txt));
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).
                withErrorListener(new PermissionRequestErrorListener() {
                    @Override
                    public void onError(DexterError error) {
                        PSR_Utils.showToast(MyProfileActivity.this, getResources().getString(R.string.somethingwnt_wrong_txt));
                    }
                })
                .onSameThread()
                .check();
    }

    private static int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }


    public void launchCamera() {

        try {

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            clickedPhotoPath = PSR_Utils.getClickedPhotoPath(getApplicationContext());

            if (clickedPhotoPath == null) {

                PSR_Utils.showAlert(this, getResources().getString(R.string.somethingwnt_wrong_txt), null);
                return;
            }
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(clickedPhotoPath)));
            startActivityForResult(Intent.createChooser(intent, "Click Photo"), CLICK_PHOTO);
        } catch (Exception e) {
            e.printStackTrace();
        }

        /*
        try {
            Retropicker.Builder builder = new Retropicker.Builder(this)
                    .setTypeAction(Retropicker.CAMERA_PICKER)
                    .checkPermission(true);

            builder.enquee(new CallbackPicker() {
                @Override
                public void onSuccess(Bitmap bitmap, String imagePath) {
                    try {
                        if (bitmap != null) {
                            Uri uri = null;
                            ExifInterface exif = new ExifInterface(imagePath);
                            int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                            int rotationInDegrees = exifToDegrees(rotation);
                            if (rotationInDegrees == 0) {
                                uri = getImageUri(MyProfileActivity.this, bitmap);
                            } else {
                                bitmap = rotateImage(bitmap, rotationInDegrees);
                                uri = getImageUri(MyProfileActivity.this, bitmap);
                            }

                            CropImage.activity(uri)
                                    .setAllowFlipping(false)
                                    .setAspectRatio(1, 1)
                                    .setCropMenuCropButtonTitle("Done")
                                    .start(MyProfileActivity.this);

                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                }

                @Override
                public void onFailure(Throwable e) {
                    PSR_Utils.showToast(MyProfileActivity.this, e.getLocalizedMessage());
                }
            });

            Retropicker retropicker = builder.create();
            retropicker.open();


        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }


    private void uploadtoS3(String imagePath) {
        if (PSR_Utils.isOnline(this)) {

            Thread myUploadTask = new Thread(new Runnable() {
                public void run() {
                    try {
                        s3Client = new AmazonS3Client(new BasicAWSCredentials(PSRConstants.S3BUCKETACCESSKEYID, PSRConstants.S3BUCKETSECRETACCESSKEY));
                        s3Client.setRegion(Region.getRegion(Regions.US_WEST_2));
                        pictureName = UUID.randomUUID().toString() + PSRConstants.IMAGE_FILE_EXTENSION;
                        Log.d("PICTURENAME", pictureName);
                        PutObjectRequest por = new PutObjectRequest(PSRConstants.PROFILEPICS, pictureName, new java.io.File(imagePath));
                        s3Client.putObject(por);

                        URL url = s3Client.getUrl(PSRConstants.PROFILEPICS, pictureName);
                        if (url != null) {
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    updateProfile(url.toString());
                                }
                            });
                        } else {
                            PSR_Utils.hideProgressDialog();
                            PSR_Utils.showToast(MyProfileActivity.this, getResources().getString(R.string.unableto_savephoto_txt));
                        }
                        Log.d("BUCKETURL", url.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                        PSR_Utils.hideProgressDialog();
                        PSR_Utils.showToast(MyProfileActivity.this, getResources().getString(R.string.unableto_savephoto_txt));
                    }

                }
            });
            myUploadTask.start();
        } else {
            PSR_Utils.showNoNetworkAlert(this);
        }

    }


    @Override
    public void onUpdatingSuccess() {
        finish();
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("PHOTOPATH", clickedPhotoPath);
    }

    @Override
    public void onClickOk() {
        PSR_Utils.navigateToContacUsScreen(this);
    }
}
