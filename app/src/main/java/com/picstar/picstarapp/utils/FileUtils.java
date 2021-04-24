package com.picstar.picstarapp.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.text.DecimalFormat;
import java.util.Hashtable;

/**
 * DateUtils related to file handling (for sending / downloading file messages).
 */

public class FileUtils {

    public static void downloadFile(Context context, String url, String fileName) {
        DownloadManager.Request downloadRequest = new DownloadManager.Request(Uri.parse(url));
        downloadRequest.setTitle(fileName);

        // in order for this if to run, you must use the android 3.2 to compile your app
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            downloadRequest.allowScanningByMediaScanner();
            downloadRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        }

        downloadRequest.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(downloadRequest);
    }


    /**
     * Converts byte value to String.
     */
    public static String toReadableFileSize(long size) {
        if (size <= 0) return "0KB";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static void saveToFile(File file, String data) throws IOException {
        File tempFile = File.createTempFile("sendbird", "temp");
        FileOutputStream fos = new FileOutputStream(tempFile);
        fos.write(data.getBytes());
        fos.close();

        if (!tempFile.renameTo(file)) {
            throw new IOException("Error to rename file to " + file.getAbsolutePath());
        }
    }

    public static String loadFromFile(File file) throws IOException {
        FileInputStream stream = new FileInputStream(file);
        Reader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[8192];
        int read;
        while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
            builder.append(buffer, 0, read);
        }
        return builder.toString();
    }

    public static void deleteFile(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
    }



    public static void copy(File src, File dest) {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(src));
             BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(dest))) {

            byte[] buffer = new byte[1024 * 1024];
            int read = 0;
            while ((read = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, read);
            }
            bis.close();
            bos.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static String getPath(final Context context, final Uri uri) {

        try {
            final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

            // DocumentProvider
            if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    if ("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    }

                    // TODO handle non-primary volumes
                }
                // DownloadsProvider
                else if (isDownloadsDocument(uri)) {

                    final String id = DocumentsContract.getDocumentId(uri);
                    final Uri contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                    return getDataColumn(context, contentUri, null, null);
                }
                // MediaProvider
                else if (isMediaDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    Uri contentUri = null;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }

                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[]{
                            split[1]
                    };

                    return getDataColumn(context, contentUri, selection, selectionArgs);
                }
            }
            // MediaStore (and general)
            else if ("content".equalsIgnoreCase(uri.getScheme())) {

                // Return the remote address
                if (isGooglePhotosUri(uri))
                    return uri.getLastPathSegment();

                return getDataColumn(context, uri, null, null);
            }
            // File
            else if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    public static boolean reduceImageFileSize(String path, int maxSize) {
        boolean result = false;
        try {
            File img = new File(path);
            BitmapFactory.Options options = new BitmapFactory.Options();
            Bitmap bitmap = null;
            options.inSampleSize = 1;
            while (img.length() > maxSize) {
                options.inSampleSize = options.inSampleSize + 1;
                bitmap = BitmapFactory.decodeFile(path, options);
                img.delete();
                try {
                    FileOutputStream fos = new FileOutputStream(path);
                    bitmap.compress(path.toLowerCase().endsWith("png") ?
                            Bitmap.CompressFormat.PNG :
                            Bitmap.CompressFormat.JPEG, 100, fos);
                    fos.close();
                    result = true;
                } catch (Exception errVar) {
                    errVar.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory == null) {
            return;
        }
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }

    public static float getFileSizeInMB(File file) {
        return ((float) file.length() / (1024 * 1024));
    }
}
