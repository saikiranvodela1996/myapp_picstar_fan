package com.picstar.picstarapp.network;

import android.util.Log;

import java.net.ConnectException;
import java.net.UnknownHostException;

import io.reactivex.observers.DisposableObserver;
import retrofit2.HttpException;

/**
 * Created by Administrator on 11/14/2017.
 */

public abstract class CustomDisposableObserver<T> extends DisposableObserver<T> {

    private static final String TAG = CustomDisposableObserver.class.getSimpleName();
    private int errorCode;

    @Override
    public void onError(Throwable e) {

        if (e instanceof HttpException) {
            Log.e(TAG, "-----------------------------------------------------------------------------------------------");
            Log.e(TAG, "Error: " + ((HttpException) e).response().raw().toString());
            Log.e(TAG, "-----------------------------------------------------------------------------------------------");

            errorCode = ((HttpException) e).code();

            if (errorCode != 200) {
                if(errorCode == 401){
                    onSessionExpired();
                    return;
                }
                onServerError();
                return;
            }
        }

        if (e instanceof ConnectException || e instanceof UnknownHostException) {
            onConnectionLost();
            return;
        } else {
            onServerError();
            return;
        }
    }

    public void onComplete() {}

    public abstract void onConnectionLost();

    public abstract void onSessionExpired();

    public void onServerError(){
        onError(new Throwable());
    }
}
