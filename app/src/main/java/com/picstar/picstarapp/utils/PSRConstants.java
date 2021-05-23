package com.picstar.picstarapp.utils;


import com.picstar.picstarapp.BuildConfig;

public interface PSRConstants {
    String LIVE_VIDEO_SERVICE_REQ_ID = "4";
    String VIDEOMSGS_SERVICE_REQ_ID = "3";
    String PHOTOSELFIE_SERVICE_REQ_ID = "1";
    String LIVESELFIE_SERVICE_REQ_ID = "2";
    String historyUpcomingKey = "upcoming";
    String historyPendingKey = "pending";
    String historyCompletedKey = "completed";
    int FAVOURITES_CATEGORY_ID = -2;
    int PIC_WIDTH = 600;
    String APP_HEADER = "Authorization";
    String CONTENTTYPE="Content-Type";
    String TOKEN = "bearerToken";
    String USERID = "userId";
    String USERPROFILEPIC = "userProfilePic";
    String USERPHONENUMBER = "userPhoneNumber";
    String USERGENDER = "userGender";
    String USEREMAIL = "email";
    String USERNAME = "username";
    String USERDOB = "userdob";
    String USERSERVERDOB = "userServerDob";
    String PROFILEPICURl = "profilepic";
    String LIVESELFIECOST = "liveselfieCost";
    String SELFIECOST = "selfieCost";
    String SERVICECOST="serviceCost";
    String SELECTED_LANGUAGE = "Locale.Helper.Selected.Language";
    String IMAGE_FILE_EXTENSION = ".jpeg";
    String DATE_TIME_FORMAT_IN_FILE_NAMES = "yyyyMMdd_HHmmss";
    String ISCAMEFROMHISTORY="isCameFromHistory";
    String PHOTOSELFIECOST = "photoSelfieCost";
    String BUCKETNAME="bucketName";
    String SERVICEREQTYPEID ="serviceReqTypeId";
    String SERVICEREQID ="serviceReqId";
    String CELEBRITYID = "CELEBRITYID";
    String S3UPLOADED_IMAGEURL="s3UploadedImageUrl";
    String SELECTEDCELEBRITYNAME = "selectedCelebName";
    String SERVIEREQID = "serviceRedId";
    String CELEBRITYPHOTOID = "celebrityPhotoID";
    String CATEGORYID = "";
    String ISLOGGEDIN = "isLoggedIn";
    String EVENTID="eventId";
    String SELECTED_CELEBRITYID = "selectedCelebrity";
    String VIDEOURL = "videoUrl";
    String PRIVACY_POLICY_URL = "privacy_policy_url";
    String CONTACT_US_PHONE_NO = "contact_us_phone_no";
    String CONTACT_US_EMAIL = "contact_us_email";
    String CONTACT_US_ADDRESS = "contact_us_address";





    String PHOTOSELFIEBUCKETNAME = "picstar/photo_selfies";
    String PROFILEPICS = "picstar/profile_pics";
    String LIVESELFIESBUCKETNAME = "picstar/live_selfies";

    String PENDING = "PENDING";
    String CLOSED = "COMPLETED";

    String PAYMENTSUCESS="Payment Success";

    String S3BUCKETACCESSKEYID = "AKIAZ2HMNTJVTXUKXGVN";
    String S3BUCKETSECRETACCESSKEY = "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";

    String STRIPE_PUBLISHABLE_KEY = "pk_test_51IY7quSAuuaGDkpGFLeHyBfbkfxTJT4tomxB4lOnyQvjKUHbWB2MB1z7WQRzFfL3RUOgDnHM6Td2D6DyScsxnyWP00hBf67V6b";


    String STRIPE_URL = "https://api.stripe.com/v1/charges";

    String Stripe_secret_KEY = "Bearer sk_test_51IY7quSAuuaGDkpGF7QU2uq8rhAVcYgxbiliZzJ99y7HPrnIqTiD2E1ogVgnfIRMgn3SZYMAWge4ZLlHgxszgQXD004V6xOIKD";


    class Base_API {
        public static final String BASE_URL = BuildConfig.BASE_URL;
    }

    class Prefs_Keys {

        public static final String KEY_PHOT_POS = "photoPosition";
        public static final String INFO_ORIENTATION = "infoOrientation";
    }

    class Extras_Keys {
        public static final String KEY_PHOT_POS = "photoPosition";
    }
}
