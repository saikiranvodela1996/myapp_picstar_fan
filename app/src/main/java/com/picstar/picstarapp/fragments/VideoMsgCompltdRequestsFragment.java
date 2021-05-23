package com.picstar.picstarapp.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.picstar.picstarapp.R;
import com.picstar.picstarapp.activities.VideoPlayerActivity;
import com.picstar.picstarapp.adapters.PendingVideoMsgsAdapter;
import com.picstar.picstarapp.mvp.models.pendingVideoMsgs.Info;
import com.picstar.picstarapp.mvp.models.pendingVideoMsgs.PendingVideoMsgsResponse;
import com.picstar.picstarapp.mvp.models.pendingVideoMsgs.VideoMsgsPendingReq;
import com.picstar.picstarapp.mvp.presenters.VideoMsgsPendingReqPresenter;
import com.picstar.picstarapp.mvp.views.VideoMsgsPendingView;
import com.picstar.picstarapp.utils.PSRConstants;
import com.picstar.picstarapp.utils.PSR_Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class VideoMsgCompltdRequestsFragment extends BaseFragment implements VideoMsgsPendingView, PSR_Utils.OnSingleBtnDialogClick {


    @BindView(R.id.recycler_View)
    RecyclerView recyclerView;
    @BindView(R.id.noListTv)
    TextView noListTv;
    private String celebrityId;
    private VideoMsgsPendingReqPresenter videoMsgsPendingReqPresenter;
    private PendingVideoMsgsAdapter pendingVideoMsgsAdapter;
    List<Info> compltedvideoMsgs;
    private boolean isLoading = false;
    private boolean isAllPagesShown = false;
    private int currentPage = 1;
    private View footerView;
    private FooterViewHolder footerViewHolder;


    class FooterViewHolder {
        @BindView(R.id.progressBar)
        ProgressBar progressBar;

        private FooterViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View mainView = inflater.inflate(R.layout.videomsg_pendingrequests, container, false);
        ButterKnife.bind(this, mainView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        footerView = inflater.inflate(R.layout.item_loading, recyclerView, false);

        return mainView;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        compltedvideoMsgs = new ArrayList<>();

        videoMsgsPendingReqPresenter = new VideoMsgsPendingReqPresenter();
        videoMsgsPendingReqPresenter.attachMvpView(this);


        footerViewHolder = new FooterViewHolder(footerView);

        pendingVideoMsgsAdapter = new PendingVideoMsgsAdapter(getActivity(), compltedvideoMsgs, true, this);
        recyclerView.setAdapter(pendingVideoMsgsAdapter);
        pendingVideoMsgsAdapter.setFooterView(footerView);
        if (getArguments() != null) {
            Bundle args = getArguments();
            celebrityId = args.getString(PSRConstants.SELECTED_CELEBRITYID);
            getCompletedVideoMsgs();
        }


        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (!recyclerView.canScrollVertically(1)) {
                    if (!isLoading && !isAllPagesShown) {
                        if (PSR_Utils.isOnline(getActivity())) {
                            isLoading = true;
                            currentPage++;
                            getCompletedVideoMsgs();
                            footerViewHolder.progressBar.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
        });
    }


    public void getCompletedVideoMsgs() {
        if (PSR_Utils.isOnline(getActivity())) {
            if (currentPage == 1) {
                PSR_Utils.showProgressDialog(getActivity());
            }
            VideoMsgsPendingReq videoMsgsPendingReq = new VideoMsgsPendingReq();
            videoMsgsPendingReq.setUserId(psr_prefsManager.get(PSRConstants.USERID));
            videoMsgsPendingReq.setCelebrityId(celebrityId);
            videoMsgsPendingReq.setStatus(PSRConstants.CLOSED);
            videoMsgsPendingReq.setPage(currentPage);
            videoMsgsPendingReqPresenter.getPendingVideoMsgs(psr_prefsManager.get(PSRConstants.SELECTED_LANGUAGE), PSR_Utils.getHeader(psr_prefsManager), videoMsgsPendingReq);

        } else {
            PSR_Utils.showNoNetworkAlert(getActivity());
        }
    }

    @Override
    public void gettingPendingorcompltdVideoMsgsSuccess(PendingVideoMsgsResponse response) {
        PSR_Utils.hideProgressDialog();
        isLoading = false;
        footerViewHolder.progressBar.setVisibility(View.GONE);
        if (response.getInfo() == null || response.getInfo().size() == 0 || response.getInfo().isEmpty()) {
            isAllPagesShown = true;
        }


        if (response.getInfo().size() != 0) {
            compltedvideoMsgs.addAll(response.getInfo());
            pendingVideoMsgsAdapter.notifyDataSetChanged();

        } else {
            if (currentPage == 1) {
                recyclerView.setVisibility(View.GONE);
                noListTv.setVisibility(View.VISIBLE);
                noListTv.setText(response.getMessage().toString());
            }
        }
    }

    @Override
    public void userBlocked(String msg) {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.singleBtnAlert(getActivity(), msg, null, this);
    }

    @Override
    public void onClickOk() {
        PSR_Utils.navigateToContacUsScreen(getActivity());
    }

    @Override
    public void gettingPendingorcompltdVideoMsgsFailure(PendingVideoMsgsResponse response) {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.showAlert(getActivity(), response.getMessage().toString(), null);
    }

    @Override
    public void onClickPayNow(Info info) {

        if (info.getServiceRequestTypeId() == Integer.parseInt(PSRConstants.VIDEOMSGS_SERVICE_REQ_ID)) {
            checkRunTimePermissionsNdShareVideo(info.getFilePath().toString());

        }
    }

    @Override
    public void onClickVideo(String videoUrl) {
        Intent intent = new Intent(getActivity(), VideoPlayerActivity.class);
        intent.putExtra(PSRConstants.VIDEOURL, videoUrl);
        startActivity(intent);
    }

    @Override
    public void onSessionExpired() {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.doLogout(getActivity(), psr_prefsManager);
    }

    @Override
    public void onServerError() {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.showAlert(getActivity(), getResources().getString(R.string.somethingwnt_wrong_txt), null);
    }

    @Override
    public Context getMvpContext() {
        return null;
    }

    @Override
    public void onError(Throwable throwable) {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.showAlert(getActivity(), getResources().getString(R.string.somethingwnt_wrong_txt), null);
    }

    @Override
    public void onNoInternetConnection() {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.showNoNetworkAlert(getActivity());
    }

    @Override
    public void onErrorCode(String s) {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.showAlert(getActivity(), getResources().getString(R.string.somethingwnt_wrong_txt), null);
    }


    public void checkRunTimePermissionsNdShareVideo(String videoUrl) {
        Dexter.withActivity(getActivity())
                .withPermissions(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            downloadFile(videoUrl);
                        } else {
                            PSR_Utils.showToast(getActivity(), getResources().getString(R.string.storage_permissions_alert_txt));
                        }
                        // check for permanent denial of any permission
                        if (report.isAnyPermissionPermanentlyDenied()) {
                            PSR_Utils.showToast(getActivity(), getResources().getString(R.string.storage_permissions_alert_txt));
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
                        PSR_Utils.showToast(getActivity(), getActivity().getResources().getString(R.string.somethingwnt_wrong_txt));
                    }
                })
                .onSameThread()
                .check();
    }

    public void downloadFile(String url) {
        final DownloadTask downloadTask = new DownloadTask(getActivity());
        downloadTask.execute(url);
    }

    public class DownloadTask extends AsyncTask<String, Integer, String> {
        private Context context;
        private PowerManager.WakeLock mWakeLock;

        public DownloadTask(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            PSR_Utils.showProgressDialog(getActivity());
        }

        @Override
        protected String doInBackground(String... sUrl) {

            SimpleDateFormat sd = new SimpleDateFormat("yymmhhss");
            String date = sd.format(new Date());


            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {

                URL url = new URL(sUrl[0]);
                //  java.net.URL url = new URL("https://s3.us-west-2.amazonaws.com/picstar/video_messages/ad783736-8b67-4a27-ab03-7c8154d87040");
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                int fileLength = connection.getContentLength();

                input = connection.getInputStream();

                File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        + "/PicStar/video");
                boolean success = true;


                File file = new File(storageDir.getAbsolutePath());
                Log.v("damn", file.getAbsolutePath());
                String[] myFiles;
                if (!file.exists()) {
                    file.mkdirs();
                }
                if (file.list() != null) {
                    myFiles = file.list();
                    for (String s : myFiles) {
                        File myFile = new File(file, s);
                        myFile.delete();
                    }
                }

                File filename = new File(storageDir, date + ".mp4");
                output = new FileOutputStream(filename);

                byte[] data = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    Log.v("damn", "downloading");
                    total += count;

                    output.write(data, 0, count);
                }
                Log.v("damn", "download done");


                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
                connection.disconnect();
                return filename.toString();
            } catch (Exception e) {
                PSR_Utils.hideProgressDialog();
                Log.v("damn", e.getMessage());
            }
            return null;

            //return "bhanu0";
        }

        @Override
        protected void onPostExecute(String file) {
            super.onPostExecute(file);
            PSR_Utils.hideProgressDialog();
            if (file != null) {
                Uri pictureUri = Uri.parse(file);
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, pictureUri);
                shareIntent.setType("video/*");
                context.startActivity(Intent.createChooser(shareIntent, "Shareing Image..."));
            }
        }
    }

}
