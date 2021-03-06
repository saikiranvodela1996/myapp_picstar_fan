package com.picstar.picstarapp.liveselfiecam;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Video.VideoColumns;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.picstar.picstarapp.campkg.others.MyDebug;
import com.picstar.picstarapp.campkg.others.PreferenceKeys;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Provides access to the filesystem. Supports both standard and Storage
 *  Access Framework.
 */
public class StorageUtils {
    private static final String TAG = "StorageUtils";

    static final int MEDIA_TYPE_IMAGE = 1;
    static final int MEDIA_TYPE_VIDEO = 2;
    static final int MEDIA_TYPE_PREFS = 3;
    static final int MEDIA_TYPE_GYRO_INFO = 4;

    private final Context context;
    private final MyApplicationInterface applicationInterface;
    private Uri last_media_scanned;

    private final static File base_folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);

    // for testing:
    public volatile boolean failed_to_scan;

    StorageUtils(Context context, MyApplicationInterface applicationInterface) {
        this.context = context;
        this.applicationInterface = applicationInterface;
    }

    public Uri getLastMediaScanned() {
        return last_media_scanned;
    }

    void clearLastMediaScanned() {
        last_media_scanned = null;
    }

    void announceUri(Uri uri, boolean is_new_picture, boolean is_new_video) {
        if( MyDebug.LOG )
            Log.d(TAG, "announceUri: " + uri);
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ) {
            if( MyDebug.LOG )
                Log.d(TAG, "broadcasts deprecated on Android 7 onwards, so don't send them");
            // see note above; the intents won't be delivered, so might as well save the trouble of trying to send them
        }
        else if( is_new_picture ) {
            // note, we reference the string directly rather than via Camera.ACTION_NEW_PICTURE, as the latter class is now deprecated - but we still need to broadcast the string for other apps
            context.sendBroadcast(new Intent( "android.hardware.action.NEW_PICTURE" , uri));
            // for compatibility with some apps - apparently this is what used to be broadcast on Android?
            context.sendBroadcast(new Intent("com.android.camera.NEW_PICTURE", uri));

            if( MyDebug.LOG ) // this code only used for debugging/logging
            {
                String[] CONTENT_PROJECTION = { Images.Media.DATA, Images.Media.DISPLAY_NAME, Images.Media.MIME_TYPE, Images.Media.SIZE, Images.Media.DATE_TAKEN, Images.Media.DATE_ADDED };
                Cursor c = context.getContentResolver().query(uri, CONTENT_PROJECTION, null, null, null);
                if( c == null ) {
                    if( MyDebug.LOG )
                        Log.e(TAG, "Couldn't resolve given uri [1]: " + uri);
                }
                else if( !c.moveToFirst() ) {
                    if( MyDebug.LOG )
                        Log.e(TAG, "Couldn't resolve given uri [2]: " + uri);
                }
                else {
                    String file_path = c.getString(c.getColumnIndex(Images.Media.DATA));
                    String file_name = c.getString(c.getColumnIndex(Images.Media.DISPLAY_NAME));
                    String mime_type = c.getString(c.getColumnIndex(Images.Media.MIME_TYPE));
                    long date_taken = c.getLong(c.getColumnIndex(Images.Media.DATE_TAKEN));
                    long date_added = c.getLong(c.getColumnIndex(Images.Media.DATE_ADDED));
                    Log.d(TAG, "file_path: " + file_path);
                    Log.d(TAG, "file_name: " + file_name);
                    Log.d(TAG, "mime_type: " + mime_type);
                    Log.d(TAG, "date_taken: " + date_taken);
                    Log.d(TAG, "date_added: " + date_added);
                    c.close();
                }
            }
        }
        else if( is_new_video ) {
            context.sendBroadcast(new Intent("android.hardware.action.NEW_VIDEO", uri));

        }
    }


    public void broadcastFile(final File file, final boolean is_new_picture, final boolean is_new_video, final boolean set_last_scanned) {
        if( MyDebug.LOG )
            Log.d(TAG, "broadcastFile: " + file.getAbsolutePath());
        // note that the new method means that the new folder shows up as a file when connected to a PC via MTP (at least tested on Windows 8)
        if( file.isDirectory() ) {
        }
        else {
            failed_to_scan = true; // set to true until scanned okay
            if( MyDebug.LOG )
                Log.d(TAG, "failed_to_scan set to true");
            MediaScannerConnection.scanFile(context, new String[] { file.getAbsolutePath() }, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            failed_to_scan = false;
                            if( MyDebug.LOG ) {
                                Log.d(TAG, "Scanned " + path + ":");
                                Log.d(TAG, "-> uri=" + uri);
                            }
                            if( set_last_scanned ) {
                                last_media_scanned = uri;
                                if( MyDebug.LOG )
                                    Log.d(TAG, "set last_media_scanned to " + last_media_scanned);
                            }
                            announceUri(uri, is_new_picture, is_new_video);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                applicationInterface.scannedFile(file, uri);
                            }

                            // it seems caller apps seem to prefer the content:// Uri rather than one based on a File
                            // update for Android 7: seems that passing file uris is now restricted anyway, see https://code.google.com/p/android/issues/detail?id=203555
                            Activity activity = (Activity)context;
                            String action = activity.getIntent().getAction();
                            if( MediaStore.ACTION_VIDEO_CAPTURE.equals(action) ) {
                                if( MyDebug.LOG )
                                    Log.d(TAG, "from video capture intent");
                                Intent output = new Intent();
                                output.setData(uri);
                                activity.setResult(Activity.RESULT_OK, output);
                                activity.finish();
                            }
                        }
                    }
            );
        }
    }

    /** Wrapper for broadcastFile, when we only have a Uri (e.g., for SAF)
     */
    public File broadcastUri(final Uri uri, final boolean is_new_picture, final boolean is_new_video, final boolean set_last_scanned) {
        if( MyDebug.LOG )
            Log.d(TAG, "broadcastUri: " + uri);
        File real_file = getFileFromDocumentUriSAF(uri, false);
        if( MyDebug.LOG )
            Log.d(TAG, "real_file: " + real_file);
        if( real_file != null ) {
            if( MyDebug.LOG )
                Log.d(TAG, "broadcast file");
            broadcastFile(real_file, is_new_picture, is_new_video, set_last_scanned);
            return real_file;
        }
        else {
            if( MyDebug.LOG )
                Log.d(TAG, "announce SAF uri");
            announceUri(uri, is_new_picture, is_new_video);
        }
        return null;
    }

    public boolean isUsingSAF() {
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            if( sharedPreferences.getBoolean(PreferenceKeys.getUsingSAFPreferenceKey(), false) ) {
                return true;
            }
        }
        return false;
    }

    public String getSaveLocation() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PreferenceKeys.getSaveLocationPreferenceKey(), "OpenCamera2");
    }

    // only valid if isUsingSAF()
    String getSaveLocationSAF() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PreferenceKeys.getSaveLocationSAFPreferenceKey(), "");
    }

    // only valid if isUsingSAF()
    private Uri getTreeUriSAF() {
        String folder_name = getSaveLocationSAF();
        return Uri.parse(folder_name);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    File getImageFolder() {
        File file;
        if( isUsingSAF() ) {
            Uri uri = getTreeUriSAF();
            file = getFileFromDocumentUriSAF(uri, true);
        }
        else {
            String folder_name = getSaveLocation();
            file = getImageFolder(folder_name);
        }
        return file;
    }

    public static File getBaseFolder() {
        return base_folder;
    }

    private static File getImageFolder(String folder_name) {
        File file;
        if( folder_name.length() > 0 && folder_name.lastIndexOf('/') == folder_name.length()-1 ) {
            folder_name = folder_name.substring(0, folder_name.length()-1);
        }
        if( folder_name.startsWith("/") ) {
            file = new File(folder_name);
        }
        else {
            file = new File(getBaseFolder(), folder_name);
        }
        return file;
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public File getFileFromDocumentUriSAF(Uri uri, boolean is_folder) {
        if( MyDebug.LOG ) {
            Log.d(TAG, "getFileFromDocumentUriSAF: " + uri);
            Log.d(TAG, "is_folder?: " + is_folder);
        }
        String authority = uri.getAuthority();
        if( MyDebug.LOG ) {
            Log.d(TAG, "authority: " + authority);
            Log.d(TAG, "scheme: " + uri.getScheme());
            Log.d(TAG, "fragment: " + uri.getFragment());
            Log.d(TAG, "path: " + uri.getPath());
            Log.d(TAG, "last path segment: " + uri.getLastPathSegment());
        }
        File file = null;
        if( "com.android.externalstorage.documents".equals(authority) ) {
            final String id = is_folder ? DocumentsContract.getTreeDocumentId(uri) : DocumentsContract.getDocumentId(uri);
            if( MyDebug.LOG )
                Log.d(TAG, "id: " + id);
            String [] split = id.split(":");
            if( split.length >= 2 ) {
                String type = split[0];
                String path = split[1];

                File [] storagePoints = new File("/storage").listFiles();

                if( "primary".equalsIgnoreCase(type) ) {
                    final File externalStorage = Environment.getExternalStorageDirectory();
                    file = new File(externalStorage, path);
                }
                for(int i=0;storagePoints != null && i<storagePoints.length && file==null;i++) {
                    File externalFile = new File(storagePoints[i], path);
                    if( externalFile.exists() ) {
                        file = externalFile;
                    }
                }
                if( file == null ) {
                    // just in case?
                    file = new File(path);
                }
            }
        }
        else if( "com.android.providers.downloads.documents".equals(authority) ) {
            if( !is_folder ) {
                final String id = DocumentsContract.getDocumentId(uri);
                if( id.startsWith("raw:") ) {
                    // unclear if this is needed for Open Camera, but on Vibrance HDR
                    // on some devices (at least on a Chromebook), I've had reports of id being of the form
                    // "raw:/storage/emulated/0/Download/..."
                    String filename = id.replaceFirst("raw:", "");
                    file = new File(filename);
                }
                else {
                    try {
                        final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));

                        String filename = getDataColumn(contentUri, null, null);
                        if( filename != null )
                            file = new File(filename);
                    }
                    catch(NumberFormatException e) {
                        Log.e(TAG,"failed to parse id: " + id);
                        e.printStackTrace();
                    }
                }
            }
            else {
                if( MyDebug.LOG )
                    Log.d(TAG, "downloads uri not supported for folders");

            }
        }
        else if( "com.android.providers.media.documents".equals(authority) ) {
            final String docId = DocumentsContract.getDocumentId(uri);
            final String[] split = docId.split(":");
            final String type = split[0];

            Uri contentUri = null;
            switch (type) {
                case "image":
                    contentUri = Images.Media.EXTERNAL_CONTENT_URI;
                    break;
                case "video":
                    contentUri = Video.Media.EXTERNAL_CONTENT_URI;
                    break;
                case "audio":
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    break;
            }

            final String selection = "_id=?";
            final String[] selectionArgs = new String[] {
                    split[1]
            };

            String filename = getDataColumn(contentUri, selection, selectionArgs);
            if( filename != null )
                file = new File(filename);
        }

        if( MyDebug.LOG ) {
            if( file != null )
                Log.d(TAG, "file: " + file.getAbsolutePath());
            else
                Log.d(TAG, "failed to find file");
        }
        return file;
    }

    private String getDataColumn(Uri uri, String selection, String [] selectionArgs) {
        final String column = "_data";
        final String[] projection = {
                column
        };

        Cursor cursor = null;
        try {
            cursor = this.context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        }
        catch(IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch(SecurityException e) {
            // have received crashes from Google Play for this
            e.printStackTrace();
        }
        finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    private String createMediaFilename(int type, String suffix, int count, String extension, Date current_date) {
        String index = "";
        if( count > 0 ) {
            index = "_" + count; // try to find a unique filename
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean useZuluTime = sharedPreferences.getString(PreferenceKeys.getSaveZuluTimePreferenceKey(), "local").equals("zulu");
        String timeStamp;
        if( useZuluTime ) {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd_HHmmss'Z'", Locale.US);
            fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
            timeStamp = fmt.format(current_date);
        }
        else {
            timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(current_date);
        }
        String mediaFilename;
        switch (type) {
            case MEDIA_TYPE_GYRO_INFO: // gyro info files have same name as the photo (but different extension)
            case MEDIA_TYPE_IMAGE: {
                String prefix = sharedPreferences.getString(PreferenceKeys.getSavePhotoPrefixPreferenceKey(), "IMG_");
                mediaFilename = prefix + timeStamp + suffix + index + extension;
                break;
            }
            case MEDIA_TYPE_VIDEO: {
                String prefix = sharedPreferences.getString(PreferenceKeys.getSaveVideoPrefixPreferenceKey(), "VID_");
                mediaFilename = prefix + timeStamp + suffix + index + extension;
                break;
            }
            case MEDIA_TYPE_PREFS: {
                // good to use a prefix that sorts before IMG_ and VID_: annoyingly when using SAF, it doesn't seem possible to
                // only show the xml files, and it always defaults to sorting alphabetically...
                String prefix = "BACKUP_OC_";
                mediaFilename = prefix + timeStamp + suffix + index + extension;
                break;
            }
            default:
                // throw exception as this is a programming error
                if (MyDebug.LOG)
                    Log.e(TAG, "unknown type: " + type);
                throw new RuntimeException();
        }
        return mediaFilename;
    }

    // only valid if !isUsingSAF()
    File createOutputMediaFile(int type, String suffix, String extension, Date current_date) throws IOException {
        File mediaStorageDir = getImageFolder();
        return createOutputMediaFile(mediaStorageDir, type, suffix, extension, current_date);
    }

    void createFolderIfRequired(File folder) throws IOException {
        if( !folder.exists() ) {
            if( MyDebug.LOG )
                Log.d(TAG, "create directory: " + folder);
            if( !folder.mkdirs() ) {
                Log.e(TAG, "failed to create directory");
                throw new IOException();
            }
            broadcastFile(folder, false, false, false);
        }
    }

    // only valid if !isUsingSAF()
    @SuppressLint("SimpleDateFormat")
    File createOutputMediaFile(File mediaStorageDir, int type, String suffix, String extension, Date current_date) throws IOException {
        createFolderIfRequired(mediaStorageDir);

        // Create a media file name
        File mediaFile = null;
        for(int count=0;count<100;count++) {
        	 {
                String mediaFilename = createMediaFilename(type, suffix, count, "." + extension, current_date);
                mediaFile = new File(mediaStorageDir.getPath() + File.separator + mediaFilename);
            }
            if( !mediaFile.exists() ) {
                break;
            }
        }

        if( MyDebug.LOG ) {
            Log.d(TAG, "getOutputMediaFile returns: " + mediaFile);
        }
        if( mediaFile == null )
            throw new IOException();
        return mediaFile;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    Uri createOutputFileSAF(String filename, String mimeType) throws IOException {
        try {
            Uri treeUri = getTreeUriSAF();
            if( MyDebug.LOG )
                Log.d(TAG, "treeUri: " + treeUri);
            Uri docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri));
            if( MyDebug.LOG )
                Log.d(TAG, "docUri: " + docUri);
            Uri fileUri = DocumentsContract.createDocument(context.getContentResolver(), docUri, mimeType, filename);
            if( MyDebug.LOG )
                Log.d(TAG, "returned fileUri: " + fileUri);
            if( fileUri == null )
                throw new IOException();
            return fileUri;
        }
        catch(IllegalArgumentException e) {
            if( MyDebug.LOG )
                Log.e(TAG, "createOutputMediaFileSAF failed with IllegalArgumentException");
            e.printStackTrace();
            throw new IOException();
        }
        catch(IllegalStateException e) {
            e.printStackTrace();
            throw new IOException();
        }
        catch(SecurityException e) {
            if( MyDebug.LOG )
                Log.e(TAG, "createOutputMediaFileSAF failed with SecurityException");
            e.printStackTrace();
            throw new IOException();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    Uri createOutputMediaFileSAF(int type, String suffix, String extension, Date current_date) throws IOException {
        String mimeType;
        switch (type) {
            case MEDIA_TYPE_IMAGE:
                switch (extension) {
                    case "dng":
                        mimeType = "image/dng";
                        break;
                    case "webp":
                        mimeType = "image/webp";
                        break;
                    case "png":
                        mimeType = "image/png";
                        break;
                    default:
                        mimeType = "image/jpeg";
                        break;
                }
                break;
            case MEDIA_TYPE_VIDEO:
                switch( extension ) {
                    case "3gp":
                        mimeType = "video/3gpp";
                        break;
                    case "webm":
                        mimeType = "video/webm";
                        break;
                    default:
                        mimeType = "video/mp4";
                        break;
                }
                break;
            case MEDIA_TYPE_PREFS:
                mimeType = "text/xml";
                break;
            case MEDIA_TYPE_GYRO_INFO:
                mimeType = "text/xml";
                break;
            default:
                // throw exception as this is a programming error
                if (MyDebug.LOG)
                    Log.e(TAG, "unknown type: " + type);
                throw new RuntimeException();
        }
        String mediaFilename = createMediaFilename(type, suffix, 0, "." + extension, current_date);
        return createOutputFileSAF(mediaFilename, mimeType);
    }

    public static class Media {
        final long id;
        final boolean video;
        public final Uri uri;
        final long date;
        final int orientation;
        public final String path;

        Media(long id, boolean video, Uri uri, long date, int orientation, String path) {
            this.id = id;
            this.video = video;
            this.uri = uri;
            this.date = date;
            this.orientation = orientation;
            this.path = path;
        }
    }

    private Media getLatestMedia(boolean video) {
        if( MyDebug.LOG )
            Log.d(TAG, "getLatestMedia: " + (video ? "video" : "images"));
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {

            if( MyDebug.LOG )
                Log.e(TAG, "don't have READ_EXTERNAL_STORAGE permission");
            return null;
        }
        Media media = null;
        Uri baseUri = video ? Video.Media.EXTERNAL_CONTENT_URI : Images.Media.EXTERNAL_CONTENT_URI;
        final int column_id_c = 0;
        final int column_date_taken_c = 1;
        final int column_data_c = 2; // full path and filename, including extension
        final int column_orientation_c = 3; // for images only
        String [] projection = video ? new String[] {VideoColumns._ID, VideoColumns.DATE_TAKEN, VideoColumns.DATA} : new String[] {ImageColumns._ID, ImageColumns.DATE_TAKEN, ImageColumns.DATA, ImageColumns.ORIENTATION};
        // for images, we need to search for JPEG/etc and RAW, to support RAW only mode (even if we're not currently in that mode, it may be that previously the user did take photos in RAW only mode)
        String selection = video ? "" : ImageColumns.MIME_TYPE + "='image/jpeg' OR " +
                ImageColumns.MIME_TYPE + "='image/webp' OR " +
                ImageColumns.MIME_TYPE + "='image/png' OR " +
                ImageColumns.MIME_TYPE + "='image/x-adobe-dng'";
        String order = video ? VideoColumns.DATE_TAKEN + " DESC," + VideoColumns._ID + " DESC" : ImageColumns.DATE_TAKEN + " DESC," + ImageColumns._ID + " DESC";
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(baseUri, projection, selection, null, order);
            if( cursor != null && cursor.moveToFirst() ) {
                if( MyDebug.LOG )
                    Log.d(TAG, "found: " + cursor.getCount());
                // now sorted in order of date - scan to most recent one in the Open Camera save folder
                boolean found = false;
                File save_folder = getImageFolder(); // may be null if using SAF
                String save_folder_string = save_folder == null ? null : save_folder.getAbsolutePath() + File.separator;
                if( MyDebug.LOG )
                    Log.d(TAG, "save_folder_string: " + save_folder_string);
                do {
                    String path = cursor.getString(column_data_c);
                    if( MyDebug.LOG )
                        Log.d(TAG, "path: " + path);
                    // path may be null on Android 4.4!: http://stackoverflow.com/questions/3401579/get-filename-and-path-from-uri-from-mediastore
                    if( save_folder_string == null || (path != null && path.contains(save_folder_string) ) ) {
                        if( MyDebug.LOG )
                            Log.d(TAG, "found most recent in Open Camera folder");
                        // we filter files with dates in future, in case there exists an image in the folder with incorrect datestamp set to the future
                        // we allow up to 2 days in future, to avoid risk of issues to do with timezone etc
                        long date = cursor.getLong(column_date_taken_c);
                        long current_time = System.currentTimeMillis();
                        if( date > current_time + 172800000 ) {
                            if( MyDebug.LOG )
                                Log.d(TAG, "skip date in the future!");
                        }
                        else {
                            found = true;
                            break;
                        }
                    }
                }
                while( cursor.moveToNext() );
                if( found ) {
                    // make sure we prefer JPEG/etc (non RAW) if there's a JPEG/etc version of this image
                    // this is because we want to support RAW only and JPEG+RAW modes
                    String path = cursor.getString(column_data_c);
                    if( MyDebug.LOG )
                        Log.d(TAG, "path: " + path);
                    // path may be null on Android 4.4, see above!
                    if( path != null && path.toLowerCase(Locale.US).endsWith(".dng") ) {
                        if( MyDebug.LOG )
                            Log.d(TAG, "try to find a non-RAW version of the DNG");
                        int dng_pos = cursor.getPosition();
                        boolean found_non_raw = false;
                        String path_without_ext = path.toLowerCase(Locale.US);
                        if( path_without_ext.indexOf(".") > 0 )
                            path_without_ext = path_without_ext.substring(0, path_without_ext.lastIndexOf("."));
                        if( MyDebug.LOG )
                            Log.d(TAG, "path_without_ext: " + path_without_ext);
                        while( cursor.moveToNext() ) {
                            String next_path = cursor.getString(column_data_c);
                            if( MyDebug.LOG )
                                Log.d(TAG, "next_path: " + next_path);
                            if( next_path == null )
                                break;
                            String next_path_without_ext = next_path.toLowerCase(Locale.US);
                            if( next_path_without_ext.indexOf(".") > 0 )
                                next_path_without_ext = next_path_without_ext.substring(0, next_path_without_ext.lastIndexOf("."));
                            if( MyDebug.LOG )
                                Log.d(TAG, "next_path_without_ext: " + next_path_without_ext);
                            if( !path_without_ext.equals(next_path_without_ext) )
                                break;
                            // so we've found another file with matching filename - is it a JPEG/etc?
                            if( next_path.toLowerCase(Locale.US).endsWith(".jpg") ) {
                                if( MyDebug.LOG )
                                    Log.d(TAG, "found equivalent jpeg");
                                found_non_raw = true;
                                break;
                            }
                            else if( next_path.toLowerCase(Locale.US).endsWith(".webp") ) {
                                if( MyDebug.LOG )
                                    Log.d(TAG, "found equivalent webp");
                                found_non_raw = true;
                                break;
                            }
                            else if( next_path.toLowerCase(Locale.US).endsWith(".png") ) {
                                if( MyDebug.LOG )
                                    Log.d(TAG, "found equivalent png");
                                found_non_raw = true;
                                break;
                            }
                        }
                        if( !found_non_raw ) {
                            if( MyDebug.LOG )
                                Log.d(TAG, "can't find equivalent jpeg/etc");
                            cursor.moveToPosition(dng_pos);
                        }
                    }
                }
                if( !found ) {
                    if( MyDebug.LOG )
                        Log.d(TAG, "can't find suitable in Open Camera folder, so just go with most recent");
                    cursor.moveToFirst();
                }
                long id = cursor.getLong(column_id_c);
                long date = cursor.getLong(column_date_taken_c);
                int orientation = video ? 0 : cursor.getInt(column_orientation_c);
                Uri uri = ContentUris.withAppendedId(baseUri, id);
                String path = cursor.getString(column_data_c);
                if( MyDebug.LOG )
                    Log.d(TAG, "found most recent uri for " + (video ? "video" : "images") + ": " + uri);
                media = new Media(id, video, uri, date, orientation, path);
            }
        }
        catch(Exception e) {
            // have had exceptions such as SQLiteException, NullPointerException reported on Google Play from within getContentResolver().query() call
            if( MyDebug.LOG )
                Log.e(TAG, "Exception trying to find latest media");
            e.printStackTrace();
        }
        finally {
            if( cursor != null ) {
                cursor.close();
            }
        }
        return media;
    }

    public Media getLatestMedia() {
        Media image_media = getLatestMedia(false);
        Media video_media = getLatestMedia(true);
        Media media = null;
        if( image_media != null && video_media == null ) {
            if( MyDebug.LOG )
                Log.d(TAG, "only found images");
            media = image_media;
        }
        else if( image_media == null && video_media != null ) {
            if( MyDebug.LOG )
                Log.d(TAG, "only found videos");
            media = video_media;
        }
        else if( image_media != null && video_media != null ) {
            if( MyDebug.LOG ) {
                Log.d(TAG, "found images and videos");
                Log.d(TAG, "latest image date: " + image_media.date);
                Log.d(TAG, "latest video date: " + video_media.date);
            }
            if( image_media.date >= video_media.date ) {
                if( MyDebug.LOG )
                    Log.d(TAG, "latest image is newer");
                media = image_media;
            }
            else {
                if( MyDebug.LOG )
                    Log.d(TAG, "latest video is newer");
                media = video_media;
            }
        }
        if( MyDebug.LOG )
            Log.d(TAG, "return latest media: " + media);
        return media;
    }



}
