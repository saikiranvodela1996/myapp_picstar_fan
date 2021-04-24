package com.picstar.picstarapp.liveselfiecam;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.picstar.picstarapp.activities.LiveSelfieCameraActivity;
import com.picstar.picstarapp.campkg.others.MyDebug;
import com.picstar.picstarapp.campkg.others.PreferenceKeys;

import java.util.ArrayList;
import java.util.Locale;

/** Manages speech recognition for remote control.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class SpeechControl {
    private static final String TAG = "SpeechControl";

    private final LiveSelfieCameraActivity main_activity;

    private SpeechRecognizer speechRecognizer;
    private boolean speechRecognizerIsStarted;

    public SpeechControl(final LiveSelfieCameraActivity main_activity) {
        this.main_activity = main_activity;
    }

    void startSpeechRecognizerIntent() {
        if( MyDebug.LOG )
            Log.d(TAG, "startSpeechRecognizerIntent");
        if( speechRecognizer != null ) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en_US"); // since we listen for "cheese", ensure this works even for devices with different language settings
            speechRecognizer.startListening(intent);
        }
    }

    private void speechRecognizerStopped() {
        if( MyDebug.LOG )
            Log.d(TAG, "speechRecognizerStopped");
        speechRecognizerIsStarted = false;
    }

    public void initSpeechRecognizer() {
        if( MyDebug.LOG )
            Log.d(TAG, "initSpeechRecognizer");
        // in theory we could create the speech recognizer always (hopefully it shouldn't use battery when not listening?), though to be safe, we only do this when the option is enabled (e.g., just in case this doesn't work on some devices!)
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(main_activity);
        boolean want_speech_recognizer = sharedPreferences.getString(PreferenceKeys.AudioControlPreferenceKey, "none").equals("voice");
        if( speechRecognizer == null && want_speech_recognizer ) {
            if( MyDebug.LOG )
                Log.d(TAG, "create new speechRecognizer");
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(main_activity);
            if( speechRecognizer != null ) {
                speechRecognizerIsStarted = false;
                speechRecognizer.setRecognitionListener(new RecognitionListener() {
                    private void restart() {
                        if( MyDebug.LOG )
                            Log.d(TAG, "RecognitionListener: restart");
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            public void run() {
                                startSpeechRecognizerIntent();
                            }
                        }, 250);

						/*freeSpeechRecognizer();
						Handler handler = new Handler();
						handler.postDelayed(new Runnable() {
							public void run() {
								initSpeechRecognizer();
								startSpeechRecognizerIntent();
					        	speechRecognizerIsStarted = true;
							}
						}, 500);*/
                    }

                    @Override
                    public void onBeginningOfSpeech() {
                        if( MyDebug.LOG )
                            Log.d(TAG, "RecognitionListener: onBeginningOfSpeech");
                        if( !speechRecognizerIsStarted ) {
                            if( MyDebug.LOG )
                                Log.d(TAG, "...but speech recognition already stopped");
                            return;
                        }
                    }

                    @Override
                    public void onBufferReceived(byte[] buffer) {
                        if( MyDebug.LOG )
                            Log.d(TAG, "RecognitionListener: onBufferReceived");
                        if( !speechRecognizerIsStarted ) {
                            if( MyDebug.LOG )
                                Log.d(TAG, "...but speech recognition already stopped");
                            return;
                        }
                    }

                    @Override
                    public void onEndOfSpeech() {
                        if( MyDebug.LOG )
                            Log.d(TAG, "RecognitionListener: onEndOfSpeech");
                        if( !speechRecognizerIsStarted ) {
                            if( MyDebug.LOG )
                                Log.d(TAG, "...but speech recognition already stopped");
                            return;
                        }
                        //speechRecognizerStopped();
                        restart();
                    }

                    @Override
                    public void onError(int error) {
                        if( MyDebug.LOG )
                            Log.d(TAG, "RecognitionListener: onError: " + error);
                        if( !speechRecognizerIsStarted ) {
                            if( MyDebug.LOG )
                                Log.d(TAG, "...but speech recognition already stopped");
                            return;
                        }
                        if( error != SpeechRecognizer.ERROR_NO_MATCH ) {
                            // we sometime receive ERROR_NO_MATCH straight after listening starts
                            // it seems that the end is signalled either by ERROR_SPEECH_TIMEOUT or onEndOfSpeech()
                            //speechRecognizerStopped();
							/*if( error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ) {
								if( MyDebug.LOG )
									Log.d(TAG, "RecognitionListener: ERROR_RECOGNIZER_BUSY");
								freeSpeechRecognizer();

								Handler handler = new Handler();
								handler.postDelayed(new Runnable() {
									public void run() {
										initSpeechRecognizer();
										startSpeechRecognizerIntent();
							        	speechRecognizerIsStarted = true;
									}
								}, 500);
							}
							else*/ {
                                restart();
                            }
                        }
                    }

                    @Override
                    public void onEvent(int eventType, Bundle params) {
                        if( MyDebug.LOG )
                            Log.d(TAG, "RecognitionListener: onEvent");
                        if( !speechRecognizerIsStarted ) {
                            if( MyDebug.LOG )
                                Log.d(TAG, "...but speech recognition already stopped");
                            return;
                        }
                    }

                    @Override
                    public void onPartialResults(Bundle partialResults) {
                        if( MyDebug.LOG )
                            Log.d(TAG, "RecognitionListener: onPartialResults");
                        if( !speechRecognizerIsStarted ) {
                            if( MyDebug.LOG )
                                Log.d(TAG, "...but speech recognition already stopped");
                            return;
                        }
                    }

                    @Override
                    public void onReadyForSpeech(Bundle params) {
                        if( MyDebug.LOG )
                            Log.d(TAG, "RecognitionListener: onReadyForSpeech");
                        if( !speechRecognizerIsStarted ) {
                            if( MyDebug.LOG )
                                Log.d(TAG, "...but speech recognition already stopped");
                            return;
                        }
                    }

                    public void onResults(Bundle results) {
                        if( MyDebug.LOG )
                            Log.d(TAG, "RecognitionListener: onResults");
                        if( !speechRecognizerIsStarted ) {
                            if( MyDebug.LOG )
                                Log.d(TAG, "...but speech recognition already stopped");
                            return;
                        }
                        ArrayList<String> list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                        boolean found = false;
                        final String trigger = "cheese";
                        //String debug_toast = "";
                        for(int i=0;list != null && i<list.size();i++) {
                            String text = list.get(i);
                            if( MyDebug.LOG ) {
                                float [] scores = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);
                                if( scores != null )
                                    Log.d(TAG, "text: " + text + " score: " + scores[i]);
                            }
							/*if( i > 0 )
								debug_toast += "\n";
							debug_toast += text + " : " + results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)[i];*/
                            if( text.toLowerCase(Locale.US).contains(trigger) ) {
                                found = true;
                            }
                        }
                        //preview.showToast(null, debug_toast); // debug only!
                        if( found ) {
                            if( MyDebug.LOG )
                                Log.d(TAG, "audio trigger from speech recognition");
                            main_activity.audioTrigger();
                        }
                        else if( list != null && list.size() > 0 ) {
                            String toast = list.get(0) + "?";
                        }
                    }

                    @Override
                    public void onRmsChanged(float rmsdB) {
                    }
                });

            }
        }
        else if( speechRecognizer != null && !want_speech_recognizer ) {
            if( MyDebug.LOG )
                Log.d(TAG, "stop existing SpeechRecognizer");
            stopSpeechRecognizer();
        }
    }

    private void freeSpeechRecognizer() {
        if( MyDebug.LOG )
            Log.d(TAG, "freeSpeechRecognizer");
        speechRecognizer.cancel();
        try {
            speechRecognizer.destroy();
        }
        catch(IllegalArgumentException e) {
            // reported from Google Play - unclear why this happens, but might as well catch
            Log.e(TAG, "exception destroying speechRecognizer");
            e.printStackTrace();
        }
        speechRecognizer = null;
    }

    public void stopSpeechRecognizer() {
        if( MyDebug.LOG )
            Log.d(TAG, "stopSpeechRecognizer");
        if( speechRecognizer != null ) {
            speechRecognizerStopped();
            freeSpeechRecognizer();
        }
    }

    boolean isStarted() {
        return speechRecognizerIsStarted;
    }

    public void stopListening() {
        speechRecognizer.stopListening();
        this.speechRecognizerStopped();
    }

    public boolean hasSpeechRecognition() {
        return speechRecognizer != null;
    }
}

