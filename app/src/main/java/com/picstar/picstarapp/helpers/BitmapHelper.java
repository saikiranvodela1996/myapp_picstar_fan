package com.picstar.picstarapp.helpers;


import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.util.Base64;
import android.util.DisplayMetrics;


import com.picstar.picstarapp.utils.PSRConstants;

import java.io.ByteArrayOutputStream;

/**
 *A helper class for all the operations related to bitmap
 */
public class BitmapHelper implements PSRConstants {

    public static Bitmap overlay(Bitmap bottomBitmap, Bitmap topBitmap, Activity activity){
        try {
            topBitmap = Bitmap.createScaledBitmap(topBitmap, topBitmap.getWidth(), topBitmap.getHeight(), true);

            Canvas canvas = new Canvas(bottomBitmap);

            DisplayMetrics displayMetrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

            canvas.drawBitmap(bottomBitmap, null, new Rect(0,0,bottomBitmap.getWidth(),bottomBitmap.getHeight()), new Paint());
            canvas.drawBitmap(topBitmap, null, new Rect(0,0,bottomBitmap.getWidth(),bottomBitmap.getHeight()), new Paint());
        }catch (Exception e) {
            e.printStackTrace();
        }
        return bottomBitmap;
    }


    public static Bitmap overlayBitmaps(Bitmap bmpBelow, Bitmap bmpAbove,Activity activity) {

        Bitmap bitmapOverlay = Bitmap.createBitmap(bmpAbove.getWidth(), bmpBelow.getHeight(), bmpBelow.getConfig());

        Canvas canvas = new Canvas(bitmapOverlay);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height1 = displayMetrics.heightPixels;
        int width1 = displayMetrics.widthPixels;
        bmpBelow = Bitmap.createScaledBitmap(bmpBelow, width1, bmpAbove.getHeight(), true);

        canvas.drawBitmap(bmpBelow, 0,0, null);
        canvas.drawBitmap(bmpAbove, new Matrix(), null);
        return bitmapOverlay;
    }






   public static Bitmap getResizedBitmap(Bitmap source, int newWidth, int newHeight) throws NullPointerException {
        if (source != null) {
            int width = source.getWidth();
            int height = source.getHeight();
            // create a matrix for the manipulation
            Matrix matrix = new Matrix();
            // resize the bitmap
            matrix.postScale(((float) newWidth) / width, ((float) newHeight) / height);
            // recreate the new Bitmap
            return Bitmap.createBitmap(source, 0, 0, width, height, matrix, true);
        } else {
            throw new NullPointerException("Inputted Bitmap is NULL");
        }
    }

     public static String getBase64String(Bitmap bitmap) {
        try {
            Bitmap converetdImage = getResizedBitmap(bitmap, 600, (600 * bitmap.getHeight()) / bitmap.getWidth());
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            converetdImage.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
            return encoded;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public static Bitmap getRotatedBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.setRotate(angle, 0, 0);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
    public static Bitmap getResizeBitmap(Bitmap bitmap){

        int  bitmapHeight = bitmap.getHeight();
        int bitmapWidth = bitmap.getWidth();
        int width = PIC_WIDTH;
        int height;

        if(bitmapWidth >= width)
        {
            float percentage = ((float) width) / bitmapWidth;
            height =  Math.round(percentage*bitmapHeight);
        }
        else
        {
            width = bitmapWidth;
            height = bitmapHeight;
        }

        return getResizedBitmap(bitmap,width,height);

    }

    public static byte[] getByteArrayOfBitmap(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream.toByteArray();
    }
}
