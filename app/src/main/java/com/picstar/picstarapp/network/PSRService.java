package com.picstar.picstarapp.network;


import com.picstar.picstarapp.mvp.models.categories.CategoriesListResponse;
import com.picstar.picstarapp.mvp.models.celebrities.CelebritiesByIdRequest;
import com.picstar.picstarapp.mvp.models.celebrities.CelebritiesByIdResponse;
import com.picstar.picstarapp.mvp.models.celebrityevents.CelebrityEventsResponse;
import com.picstar.picstarapp.mvp.models.celebrityevents.RequestLiveSelfieRespnse;
import com.picstar.picstarapp.mvp.models.eventhistory.PendingHistoryResponse;
import com.picstar.picstarapp.mvp.models.favourites.AddtoFavRequest;
import com.picstar.picstarapp.mvp.models.favourites.AddtoFavResponse;
import com.picstar.picstarapp.mvp.models.liveeventshistory.LiveEventsHistoryResponse;
import com.picstar.picstarapp.mvp.models.livevideo.create.LiveVideoRequest;
import com.picstar.picstarapp.mvp.models.livevideo.create.LiveVideoResponse;
import com.picstar.picstarapp.mvp.models.livevideo.pending.LiveVideoPendingReq;
import com.picstar.picstarapp.mvp.models.livevideo.pending.PendingLiveVideoResponse;
import com.picstar.picstarapp.mvp.models.login.LoginRequest;
import com.picstar.picstarapp.mvp.models.login.LoginResponse;
import com.picstar.picstarapp.mvp.models.pendingVideoMsgs.PendingVideoMsgsResponse;
import com.picstar.picstarapp.mvp.models.pendingVideoMsgs.VideoMsgsPendingReq;
import com.picstar.picstarapp.mvp.models.pendingliveselfieresponse.LiveSelfiePendingResponse;
import com.picstar.picstarapp.mvp.models.stockphotos.StockPhotosResponse;
import com.picstar.picstarapp.mvp.models.updateprofile.UpdateProfileReq;
import com.picstar.picstarapp.mvp.models.updateprofile.UpdateProfileResponse;
import com.picstar.picstarapp.mvp.models.videomsgs.VideoMsgRequest;
import com.picstar.picstarapp.mvp.models.videomsgs.VideoMsgResponse;
import com.picstar.picstarapp.mvp.models.videomsgs.createservicerequest.CreateServiceReq;
import com.picstar.picstarapp.mvp.models.videomsgs.createservicerequest.CreateServiceResponse;
import com.picstar.picstarapp.mvp.models.videomsgshistoryresponse.VideoMsgsHistoryResponse;
import com.picstar.picstarapp.utils.PSRConstants;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class PSRService {

    private static PSRService instance;
    private static String headerValue=null;

    private Retrofit retrofit;

    private PSRApi psrApi;

    private static final int TIMEOUT = 20;
    //    private static final int TIMEOUT = 600;
    private Request request;

    public static PSRService getInstance(String header) {
        if (instance == null) {
            instance = new PSRService();
        }

        headerValue = header;
        return instance;
    }

    private PSRService() {
        if (retrofit == null || psrApi == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(PSRConstants.Base_API.BASE_URL)
                    .client(createOkHttpClientInterceptor())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(MoshiConverterFactory.create())
                    .build();

            psrApi = retrofit.create(PSRApi.class);
        }
    }

    private OkHttpClient createOkHttpClientInterceptor() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        Interceptor headerInterceptor = new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                Request original = chain.request();
                Request.Builder builder = original.newBuilder();

                if (headerValue != null && !headerValue.isEmpty()) {
                    builder.header(PSRConstants.APP_HEADER, headerValue);
                }
                builder.method(original.method(), original.body());
                request = builder.build();
                return chain.proceed(request);
            }
        };

        return new OkHttpClient.Builder().connectTimeout(TIMEOUT, TimeUnit.SECONDS).readTimeout(TIMEOUT,
                TimeUnit.SECONDS).addInterceptor(interceptor)
                .addNetworkInterceptor(headerInterceptor).build();
    }



    public Observable<LoginResponse> doLogin(LoginRequest request) {
        return psrApi.doLogin(request);
    }

    public Observable<CategoriesListResponse> doGetCategoriesList() {
        return psrApi.doGetCategoriesList();
    }

    public Observable<CelebritiesByIdResponse> doGetCelebritiesById(int page,int id) {
        return psrApi.doGetCelebritiesById(page,id);
    }
    public Observable<CelebritiesByIdResponse> doGetMyFav(int page,String useriD) {
        return psrApi.doGetMyFav(page,useriD);
    }


    public Observable<AddtoFavResponse> doAddToMyFavs(AddtoFavRequest request) {
        return psrApi.doAddToMyFavs(request);
    }


    public Observable<CelebritiesByIdResponse> doSearchForCelebrity( int categoryId,int page,String searchString,String userId) {
        return psrApi.doSearchForCelebrity(categoryId,page,searchString,userId);
    }





    public Observable<CelebrityEventsResponse> doGetEvents(int page, String userId,String celebrityId) {
        return psrApi.doGetEvents(page,userId,celebrityId);
    }


    public Observable<CreateServiceResponse> doRequestForLiveSelfie(CreateServiceReq req) {
        return psrApi.doCreateRequest(req);
    }




    public Observable<VideoMsgResponse> videoMsgRequest(VideoMsgRequest request) {
        return psrApi.videoMsgRequest(request);
    }
    public Observable<CreateServiceResponse> doCreateRequest(CreateServiceReq request) {
        return psrApi.doCreateRequest(request);
    }

    public Observable<PendingVideoMsgsResponse> dogetPendingVideoMsgs(VideoMsgsPendingReq req) {
        return psrApi.dogetPendingVideoMsgs(req.getUserId(),req.getCelebrityId(),req.getStatus(),req.getPage());
    }

    public Observable<StockPhotosResponse> doGetStockPicsOfCelebrity(CelebritiesByIdRequest req) {
        return psrApi.doGetStockPicsOfCelebrity(req.getUserId(),req.getPage());
    }
    public Observable<CelebritiesByIdResponse> doGetRecomCelebritiesById(CelebritiesByIdRequest request) {
        return psrApi.doGetRecomCelebritiesById(request.getPage(),request.getCategoryId());
    }



    public Observable<LiveSelfiePendingResponse> dogetPendingLiveSelfieReqs(VideoMsgsPendingReq req) {
        return psrApi.dogetPendingLiveSelfieReqs(req.getUserId(),req.getCelebrityId(), req.getStatus(),req.getPage());
    }





    public Observable<LiveEventsHistoryResponse> doGetHistory(String userId, int pageNo, int serviceReqId, String status) {
        return psrApi.doGetHistory(userId,pageNo,serviceReqId ,status);
    }


    public Observable<PendingHistoryResponse> doGetPendingHistory(String userId,String statuskey, int pageNo) {
        return psrApi.doGetPendingHistory(userId,statuskey,pageNo);
    }
    public Observable<VideoMsgsHistoryResponse> doGetVideoMsgHistory(String userId, int pageNo, int serviceReqId, String status) {
        return psrApi.doGetVideoMsgHistory(userId,pageNo,serviceReqId ,status);
    }
    public Observable<UpdateProfileResponse> doUpdateUserProfile(UpdateProfileReq req) {
        return psrApi.doUpdateUserProfile(req);
    }


    public Observable<PendingLiveVideoResponse> dogetPendingLiveVideo(LiveVideoPendingReq req) {
        return psrApi.dogetPendingLiveVideo(req.getUserId(),req.getCelebrityId(),req.getStatus(),req.getPage());
    }

    public Observable<LiveVideoResponse> liveVideoRequest(LiveVideoRequest request) {
        return psrApi.liveVideoRequest(request);
    }


    public Observable<CreateServiceResponse> createPaymentServReq(CreateServiceReq request) {
        return psrApi.createPaymentServReq(request);
    }


    public Observable<ResponseBody> callStripeChargesApi(String amount,String currency,String descripn,String token ) {
        return psrApi.callStripeChargesApi( amount, currency, descripn, token);
    }


}
