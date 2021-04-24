package com.picstar.picstarapp.network;

import com.picstar.picstarapp.mvp.models.categories.CategoriesListResponse;
import com.picstar.picstarapp.mvp.models.celebrities.CelebritiesByIdResponse;
import com.picstar.picstarapp.mvp.models.celebrityevents.CelebrityEventsResponse;
import com.picstar.picstarapp.mvp.models.celebrityevents.RequestLiveSelfieRespnse;
import com.picstar.picstarapp.mvp.models.eventhistory.PendingHistoryResponse;
import com.picstar.picstarapp.mvp.models.favourites.AddtoFavRequest;
import com.picstar.picstarapp.mvp.models.favourites.AddtoFavResponse;
import com.picstar.picstarapp.mvp.models.history.HistoryResponse;
import com.picstar.picstarapp.mvp.models.liveeventshistory.LiveEventsHistoryResponse;
import com.picstar.picstarapp.mvp.models.livevideo.create.LiveVideoRequest;
import com.picstar.picstarapp.mvp.models.livevideo.create.LiveVideoResponse;
import com.picstar.picstarapp.mvp.models.livevideo.pending.PendingLiveVideoResponse;
import com.picstar.picstarapp.mvp.models.login.LoginRequest;
import com.picstar.picstarapp.mvp.models.login.LoginResponse;
import com.picstar.picstarapp.mvp.models.pendingVideoMsgs.PendingVideoMsgsResponse;
import com.picstar.picstarapp.mvp.models.pendingliveselfieresponse.LiveSelfiePendingResponse;
import com.picstar.picstarapp.mvp.models.recommCelebrtys.RecommCelebrityResponse;
import com.picstar.picstarapp.mvp.models.stockphotos.StockPhotosResponse;
import com.picstar.picstarapp.mvp.models.updateprofile.UpdateProfileReq;
import com.picstar.picstarapp.mvp.models.updateprofile.UpdateProfileResponse;
import com.picstar.picstarapp.mvp.models.videomsgs.VideoMsgRequest;
import com.picstar.picstarapp.mvp.models.videomsgs.VideoMsgResponse;
import com.picstar.picstarapp.mvp.models.videomsgs.createservicerequest.CreateServiceReq;
import com.picstar.picstarapp.mvp.models.videomsgs.createservicerequest.CreateServiceResponse;
import com.picstar.picstarapp.mvp.models.videomsgshistoryresponse.VideoMsgsHistoryResponse;
import com.picstar.picstarapp.utils.PSRConstants;

import io.reactivex.Observable;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;

import static com.picstar.picstarapp.utils.PSRConstants.APP_HEADER;

public interface PSRApi {

    @POST("login_user")
    Observable<LoginResponse> doLogin(@Body LoginRequest loginRequest);

    @GET("getAllCategories")
    Observable<CategoriesListResponse> doGetCategoriesList();


    @GET("getCelebritiesByCategory?per_page=10")
    Observable<CelebritiesByIdResponse> doGetCelebritiesById(@Query("page") int page, @Query("category_id") int param1);


    @GET("getCelebritiesByCategory?per_page=10")
    Observable<CelebritiesByIdResponse> doGetMyFav(@Query("page") int page, @Query("user_id") String param1);


    @POST("addCelebrityToMyFavourites")
    Observable<AddtoFavResponse> doAddToMyFavs(@Body AddtoFavRequest request);


    @GET("getCelebritySearchResults?per_page=10")
    Observable<CelebritiesByIdResponse> doSearchForCelebrity(@Query("category_id") int categoryId, @Query("page") int page,
                                                             @Query("search_keyword") String searchString,
                                                             @Query("user_id") String userId);


    @GET("getEventsOfCelebrity?per_page=10")
    Observable<CelebrityEventsResponse> doGetEvents(@Query("page") int page, @Query("user_id") String param1, @Query("celebrity_id") String celebrityID);

  /*  @POST("createServiceRequest-new")
    Observable<CreateServiceResponse> doRequestForLiveSelfie(@Body CreateServiceReq request);*/


    @POST("createVideoEvent")
    Observable<VideoMsgResponse> videoMsgRequest(@Body VideoMsgRequest request);


    @POST("createServiceRequest-new")
    Observable<CreateServiceResponse> doCreateRequest(@Body CreateServiceReq request);


    @GET("getVideoEventsOfUserAndCelebrityByStatus?per_page=10")
    Observable<PendingVideoMsgsResponse> dogetPendingVideoMsgs(@Query("user_id") String userId,
                                                               @Query("celebrity_id") String celebrityId,
                                                               @Query("status") String status,
                                                               @Query("page") int page);


    @GET("getStockPhotosOfCelebrity?per_page=10")
    Observable<StockPhotosResponse> doGetStockPicsOfCelebrity(@Query("user_id") String celebrityId,

                                                              @Query("page") int pageno);


    @GET("getLiveSelfieServiceRequestsOfUser?per_page=10")
    Observable<LiveSelfiePendingResponse> dogetPendingLiveSelfieReqs(@Query("user_id") String userId,
                                                                     @Query("celebrity_id") String celebrityId,
                                                                     @Query("status") String status,

                                                                     @Query("page") int pageno);


    @GET("getCelebrityRecommendations?per_page=10")
    Observable<CelebritiesByIdResponse> doGetRecomCelebritiesById(
            @Query("page") int pageNo,
            @Query("category_id") int categoryID);


    @GET("getServiceRequestsOfUser?per_page=10")
    Observable<LiveEventsHistoryResponse> doGetHistory(@Query("user_id") String userId,
                                                       @Query("page") int pageno,
                                                       @Query("service_request_type_id") int serviceReqId,
                                                       @Query("status") String status);


    @GET("getServiceRequestsOfUser?per_page=10")
    Observable<PendingHistoryResponse> doGetPendingHistory(@Query("user_id") String userId,
                                                           @Query("status") String statuskey,
                                                           @Query("page") int pageno);


    @GET("getServiceRequestsOfUser?per_page=10")
    Observable<VideoMsgsHistoryResponse> doGetVideoMsgHistory(@Query("user_id") String userId,
                                                              @Query("page") int pageno,
                                                              @Query("service_request_type_id") int serviceReqId,
                                                              @Query("status") String status);


    @PUT("updateUserDetails")
    Observable<UpdateProfileResponse> doUpdateUserProfile(@Body UpdateProfileReq request);

    @GET("getLiveVideosOfUserAndCelebrityByStatus?per_page=10")
    Observable<PendingLiveVideoResponse> dogetPendingLiveVideo(@Query("user_id") String userId,
                                                               @Query("celebrity_id") String celebrityId,
                                                               @Query("status") String status,
                                                               @Query("page") int page);



    @POST("createLiveVideoEvent")
    Observable<LiveVideoResponse> liveVideoRequest(@Body LiveVideoRequest request);


    @POST("createServiceRequest-new")
    Observable<CreateServiceResponse> createPaymentServReq(@Body CreateServiceReq request);

    @FormUrlEncoded
    @Headers({"Content-Type:application/x-www-form-urlencoded"})
    @POST("https://api.stripe.com/v1/charges")
    Observable<ResponseBody> callStripeChargesApi( @Field("amount") String amount,
                                                   @Field("currency") String currency,
                                                   @Field("description") String descritpn,
                                                   @Field("source") String stripeToken);

}
