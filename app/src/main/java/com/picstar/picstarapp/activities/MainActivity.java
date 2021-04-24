package com.picstar.picstarapp.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;


import com.auth0.android.Auth0;
import com.auth0.android.Auth0Exception;
import com.auth0.android.authentication.AuthenticationAPIClient;
import com.auth0.android.authentication.AuthenticationException;
import com.auth0.android.jwt.JWT;
import com.auth0.android.lock.AuthenticationCallback;
import com.auth0.android.lock.InitialScreen;
import com.auth0.android.lock.Lock;
import com.auth0.android.lock.LockCallback;
import com.auth0.android.lock.utils.LockException;
import com.auth0.android.provider.VoidCallback;
import com.auth0.android.provider.WebAuthProvider;
import com.auth0.android.request.AuthRequest;
import com.auth0.android.result.Credentials;
import com.picstar.picstarapp.R;
import com.picstar.picstarapp.mvp.models.login.LoginRequest;
import com.picstar.picstarapp.mvp.models.login.LoginResponse;
import com.picstar.picstarapp.mvp.presenters.LoginPresenter;
import com.picstar.picstarapp.mvp.views.LoginView;

import com.picstar.picstarapp.utils.PSRConstants;
import com.picstar.picstarapp.utils.PSR_Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.OnClick;


public class MainActivity extends BaseActivity implements LoginView {
    private Auth0 auth0;
    private Lock lock;
    private LoginPresenter loginPresenter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        auth0 = new Auth0(this);
        auth0.setOIDCConformant(true);
        loginPresenter = new LoginPresenter();
        loginPresenter.attachMvpView(this);

    }

    @OnClick(R.id.getStarted_btn)
    void onClickBtn(View v) {
        showClassicLock();
     
    }

/*    @OnClick(R.id.logout_btn)
    void onClickLogoutBtn(View v) {
        logout();


    }*/

   /* private void showClassicLock() {
        final Lock.Builder builder = Lock.newBuilder(auth0, callback).withAudience("https://"+ getResources().getString(R.string.com_auth0_domain) +"/userinfo");
        builder.withScheme("demo");
        builder.withScope("userid DOB openid profile email offline_access read:current_user update:current_user_metadata");
        builder.closable(true);
        builder.useLabeledSubmitButton(true);
        builder.loginAfterSignUp(true);
        builder.allowLogIn(true);
        builder.allowSignUp(true);
        builder.allowForgotPassword(true);
        builder.initialScreen(InitialScreen.FORGOT_PASSWORD);
        builder.initialScreen(InitialScreen.SIGN_UP);
        builder.initialScreen(InitialScreen.LOG_IN);
        builder.initialScreen(InitialScreen.LOG_IN);
        builder.allowedConnections(generateConnections());
        builder.setDefaultDatabaseConnection("Username-Password-Authentication");
        builder.hideMainScreenTitle(false);
        lock = builder.build(this);
        startActivity(lock.newIntent(this));
    }*/


    private void showClassicLock() {
        final Lock.Builder builder = Lock.newBuilder(getAccount(), callback);
        builder.withScheme("demo");
        //builder.closable(true);
        builder.withScope("userid DOB openid profile email offline_access read:current_user update:current_user_metadata");
        builder.useLabeledSubmitButton(true);
        builder.loginAfterSignUp(true);
        builder.allowLogIn(true);
        builder.allowSignUp(true);
        builder.allowForgotPassword(true);
        builder.initialScreen(InitialScreen.LOG_IN);

        builder.allowedConnections(generateConnections());
        builder.setDefaultDatabaseConnection("Username-Password-Authentication");
        builder.hideMainScreenTitle(false);
        lock = builder.build(this);
        startActivity(lock.newIntent(this));


    }


    private void doLogin() {
        AuthenticationAPIClient authenticationAPIClient = new AuthenticationAPIClient(getAccount());
        authenticationAPIClient.login("ak@g.co", "123456aB", "Username-Password-Authentication")
                .setScope("openid profile email username")
                .start(new com.auth0.android.callback.AuthenticationCallback<Credentials>() {
                    @Override
                    public void onSuccess(@Nullable Credentials payload) {
                        int i = 0;

                    }

                    @Override
                    public void onFailure(@NonNull AuthenticationException error) {
                        int i = 0;

                    }
                });



     /* AuthAPI auth = new AuthAPI("me.auth0.com", "B3c6RYhk1v9SbIJcRIOwu62gIUGsnze", "2679NfkaBn62e6w5E8zNEzjr-yWfkaBne");
      try {
          TokenHolder result = auth.login("me@auth0.com", "topsecret")
                          .setScope("openid email nickname")
                          .execute();
    } catch (Auth0Exception e) {
         //Something happened
      }*/
    }

    private List<String> generateConnections() {
        List<String> connections = new ArrayList<>();
        connections.add("Username-Password-Authentication");

        connections.add("mfa-connection");
        connections.add("with-strength");
        connections.add("google-oauth2");
        connections.add("twitter");
        connections.add("facebook");
        connections.add("paypal-sandbox");
        return connections;
    }

    private Auth0 getAccount() {
        Auth0 account = new Auth0(getString(R.string.com_auth0_client_id), getString(R.string.com_auth0_domain));
        account.setOIDCConformant(true);
        account.setLoggingEnabled(true);
        return account;
    }


    private final LockCallback callback = new AuthenticationCallback() {
        @Override
        public void onAuthentication(@NonNull Credentials credentials) {
            // showResult("OK > " + credentials.getAccessToken());
            PSR_Utils.showProgressDialog(MainActivity.this);
            Log.e("idtoken", credentials.getIdToken());
            Log.e("accesstoken", credentials.getAccessToken());
            JWT jwt2 = new JWT(credentials.getIdToken());
            psr_prefsManager.save(PSRConstants.TOKEN, "Bearer" + " " + credentials.getIdToken());
            int loginType = 0;
            // String userId = jwt2.getClaim("sub").asObject(String.class).split("\\|")[1];
            String userId = jwt2.getClaim("sub").asObject(String.class);
            Log.e("USERID", userId);

            String logInWith = jwt2.getClaim("sub").asObject(String.class).split("\\|")[0];
            Log.e("user_Id", userId);
            if (logInWith.equals("facebook")) {
                loginType = 2;
            } else if (logInWith.equals("google-oauth2")) {
                loginType = 1;
            } else if (logInWith.equals("Username-Password-Authentication")) {
                loginType = 0;

            }
            String auth0Username = "";
            if (jwt2.getClaim("username").asObject(String.class) != null) {
                auth0Username = jwt2.getClaim("username").asObject(String.class);
            }

            String givenName = jwt2.getClaim("given_name").asObject(String.class);
            String profilePhoto = jwt2.getClaim("picture").asObject(String.class);
            String userName = jwt2.getClaim("name").asObject(String.class);
            String email = jwt2.getClaim("email").asObject(String.class);
            String lastName = jwt2.getClaim("family_name").asObject(String.class);
            Log.e("username", userName);
            Log.e("profilepic", profilePhoto);

            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setAuth0Username(auth0Username);
            loginRequest.setUserId(userId);
            loginRequest.setLoginType(loginType);
            loginRequest.setProfilePic(profilePhoto);
            loginRequest.setEmail(email);
            loginRequest.setFirstName(givenName);
            loginRequest.setUsername(userName);
            loginRequest.setLastName(lastName);
            loginRequest.setFcmRegToken("ascxasccacascasxc");
            loginRequest.setDeviceType("android");
            loginRequest.setDeviceId(PSR_Utils.getId(MainActivity.this));
            loginRequest.setDob("");
            loginPresenter.doLogin("Bearer " + credentials.getIdToken(), loginRequest);

        }

        @Override
        public void onCanceled() {
            int i = 0;
            // showResult("User pressed back.");
        }

        @Override
        public void onError(@NonNull LockException error) {
            int i = 0;

        }
    };




   /* private void login() {
        WebAuthProvider.login(auth0)
                .withScheme("demo")
                .withScope("userid DOB openid profile email offline_access read:current_user update:current_user_metadata")
                .withAudience(String.format("https://%s/userinfo", getString(R.string.com_auth0_domain)))
                .start(this, new AuthCallback() {
                    @Override
                    public void onFailure(@NonNull final Dialog dialog) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog.show();
                            }
                        });
                    }

                    @Override
                    public void onFailure(final AuthenticationException exception) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Error: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }


                    @Override
                    public void onSuccess(@NonNull final Credentials credentials) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.e("idtoken", credentials.getIdToken());
                                Log.e("accesstoken", credentials.getAccessToken());
                                JWT jwt2 = new JWT(credentials.getIdToken());
                                int loginType = 0;
                                String userId = jwt2.getClaim("sub").asObject(String.class).split("\\|")[1];
                                String logInWith = jwt2.getClaim("sub").asObject(String.class).split("\\|")[0];

                                if (logInWith.equals("facebook")) {

                                    loginType = 2;
                                } else {
                                    loginType = 1;
                                }
                                String givenName = jwt2.getClaim("given_name").asObject(String.class);
                                String profilePhoto = jwt2.getClaim("picture").asObject(String.class);
                                String name = jwt2.getClaim("name").asObject(String.class);
                                String email = jwt2.getClaim("email").asObject(String.class);
                                LoginRequest loginRequest = new LoginRequest();
                                loginRequest.setUserId(userId);
                                loginRequest.setLoginType(loginType);
                                loginRequest.setProfilePhoto(profilePhoto);
                                loginRequest.setEmail(email);
                                loginRequest.setFirstName(name);
                                loginRequest.setUserName(givenName);
                                loginPresenter.doLogin(loginRequest);





*//*
                                String accessToken = credentials.getAccessToken();
                                usersClient = new UsersAPIClient(auth0, accessToken);
                                authenticationAPIClient = new AuthenticationAPIClient(auth0);*//*
     *//*  Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                intent.putExtra(EXTRA_ACCESS_TOKEN, credentials.getAccessToken());
                                startActivity(intent);
                                finish();*//*
                            }
                        });
                    }
                });
    }*/


    private void logout() {
        WebAuthProvider.logout(auth0)
                .withScheme("demo")
                .start(this, new VoidCallback() {
                    @Override
                    public void onSuccess(Void payload) {
                        Toast.makeText(MainActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Auth0Exception error) {
                        //Log out canceled, keep the user logged in
                        //    showNextActivity();
                    }
                });
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
    public void onLoginSuccess(LoginResponse response) {
        PSR_Utils.hideProgressDialog();
        psr_prefsManager.save(PSRConstants.USERID, response.getInfo().getUserId());
        psr_prefsManager.save(PSRConstants.USERNAME, response.getInfo().getUsername());
        if (response.getInfo().getProfilePic() != null) {
            psr_prefsManager.save(PSRConstants.USERPROFILEPIC, response.getInfo().getProfilePic());
        }
        if (response.getInfo().getEmail() != null) {
            psr_prefsManager.save(PSRConstants.USEREMAIL, response.getInfo().getEmail());
        }
        if (response.getInfo().getPhoneNumber() != null) {
            psr_prefsManager.save(PSRConstants.USERPHONENUMBER, response.getInfo().getPhoneNumber().toString());
        }
        if (response.getInfo().getGender() != null) {
            if (response.getInfo().getGender().equalsIgnoreCase("M")) {
                psr_prefsManager.save(PSRConstants.USERGENDER, getResources().getString(R.string.male_txt));
            } else if (response.getInfo().getGender().equalsIgnoreCase("F")) {
                psr_prefsManager.save(PSRConstants.USERGENDER, getResources().getString(R.string.female_txt));
            } else if (response.getInfo().getGender().equalsIgnoreCase("O")) {
                psr_prefsManager.save(PSRConstants.USERGENDER, getResources().getString(R.string.other_txt));
            }
        }
        if (response.getInfo().getDob() != null) {
            try {
                Date serverDate = null;
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.ENGLISH);
                serverDate = df.parse(response.getInfo().getDob());
                psr_prefsManager.save(PSRConstants.USERDOB, new SimpleDateFormat("MMMM dd,yyyy").format(serverDate));
                psr_prefsManager.save(PSRConstants.USERSERVERDOB, new SimpleDateFormat("yyyy-MM-dd").format(serverDate));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        psr_prefsManager.save(PSRConstants.ISLOGGEDIN, true);
        Intent intent = new Intent(this, DashBoardActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onLoginFailed(LoginResponse response) {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.showAlert(this, response.getMessage().toString(), null);
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






















/*

    public void login1() {

        lock = Lock.newBuilder(auth0, callback)
                .withAudience("https://"+getString(R.string.com_auth0_domain)+"/userinfo")
                // ... Options
                .build(this);
    }

    private LockCallback callback = new AuthenticationCallback() {
        @Override
        public void onAuthentication(Credentials credentials) {
            //Authenticated
            int i=0;
        }

        @Override
        public void onCanceled() {
            //User pressed back
            int i=0;
        }

        @Override
        public void onError(LockException error) {
            //Exception occurred
            int i=0;
        }
    };
*/


}