package io.github.arpankapoor.thresholdshare;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import io.github.arpankapoor.user.User;


public class SendMessageActivity extends AppCompatActivity {

    private static final int SELECT_IMAGE_REQ = 1;

    private List<User> receivers = null;
    private InputStream imageStream = null;
    private File imageFile;
    private String filename = null;
    private int thresholdValue = 100;

    private Fragment activeFragment = null;
    private SelectReceiversFragment selectReceiversFragment = null;
    private SelectFilenameFragment selectFilenameFragment = null;
    private SelectThresholdFragment selectThresholdFragment = null;

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     */
    public static String getPath(final Context context, final Uri uri) {

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
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
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
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
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

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        startActivityForResult(Intent.createChooser(intent, "Select Image"), SELECT_IMAGE_REQ);
    }

    private void setFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.popBackStack();

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
        activeFragment = fragment;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_message);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Select image to send
        selectImage();

        selectFilenameFragment = new SelectFilenameFragment();
        setFragment(selectFilenameFragment);

    }

    public void nextButtonHandler(View view) {
        if (activeFragment == selectFilenameFragment) {

            filename = selectFilenameFragment.getFilename();

            // Set Fragment
            selectReceiversFragment = new SelectReceiversFragment();
            setFragment(selectReceiversFragment);
        } else if (activeFragment == selectReceiversFragment) {

            receivers = selectReceiversFragment.getSelectedReceivers();

            // Select Threshold Value
            selectThresholdFragment = new SelectThresholdFragment();
            setFragment(selectThresholdFragment);

            // Set the max threshold size
            selectThresholdFragment.setMaxThresholdValue(receivers.size() + 1);

            // Change button text
            Button nextButton = (Button) findViewById(R.id.next_button);
            nextButton.setText(getString(R.string.send_message));
        } else {
            thresholdValue = selectThresholdFragment.getThresholdValue();
            new SendMessageTask().execute();
            cancelButtonHandler(view);
        }
    }

    public void cancelButtonHandler(View view) {
        Intent mainIntent = new Intent(view.getContext(), MainActivity.class);
        startActivity(mainIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_IMAGE_REQ && resultCode == RESULT_OK) {
            try {
                Uri imageUri = data.getData();
                imageFile = new File(getPath(this, imageUri));
                Log.v("FILENAME", imageUri.getPath());
                Log.v("FILENAME", imageFile.getPath());
                imageStream = getContentResolver().openInputStream(imageUri);
            } catch (Exception ex) {
                //ex.printStackTrace();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static boolean deleteViaContentProvider(Context context, String fullname)
    {
      Uri uri=getFileUri(context,fullname);

      if (uri==null)
      {
         return false;
      }

      try
      {
         ContentResolver resolver=context.getContentResolver();

         // change type to image, otherwise nothing will be deleted
         ContentValues contentValues = new ContentValues();
         int media_type = 1;
         contentValues.put("media_type", media_type);
         resolver.update(uri, contentValues, null, null);

         return resolver.delete(uri, null, null) > 0;
      }
      catch (Throwable e)
      {
         return false;
      }
   }
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
   private static Uri getFileUri(Context context, String fullname)
   {
      // Note: check outside this class whether the OS version is >= 11
      Uri uri = null;
      Cursor cursor = null;
      ContentResolver contentResolver = null;

      try
      {
         contentResolver=context.getContentResolver();
         if (contentResolver == null)
            return null;

         uri=MediaStore.Files.getContentUri("external");
         String[] projection = new String[2];
         projection[0] = "_id";
         projection[1] = "_data";
         String selection = "_data = ? ";    // this avoids SQL injection
         String[] selectionParams = new String[1];
         selectionParams[0] = fullname;
         String sortOrder = "_id";
         cursor=contentResolver.query(uri, projection, selection, selectionParams, sortOrder);

         if (cursor!=null)
         {
            try
            {
               if (cursor.getCount() > 0) // file present!
               {
                  cursor.moveToFirst();
                  int dataColumn=cursor.getColumnIndex("_data");
                  String s = cursor.getString(dataColumn);
                  if (!s.equals(fullname))
                     return null;
                  int idColumn = cursor.getColumnIndex("_id");
                  long id = cursor.getLong(idColumn);
                  uri= MediaStore.Files.getContentUri("external",id);
               }
               else // file isn't in the media database!
               {
                  ContentValues contentValues=new ContentValues();
                  contentValues.put("_data",fullname);
                  uri = MediaStore.Files.getContentUri("external");
                  uri = contentResolver.insert(uri,contentValues);
               }
            }
            catch (Throwable e)
            {
               uri = null;
            }
            finally
            {
                cursor.close();
            }
         }
      }
      catch (Throwable e)
      {
         uri=null;
      }
      return uri;
   }

    private class SendMessageTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            Uri uri = Uri.parse(getString(R.string.server_base_url))
                    .buildUpon()
                    .appendPath(getString(R.string.send_message_api))
                    .build();

            JSONObject jsonObject = new JSONObject();
            JSONArray receiver_ids = new JSONArray();
            try {
                for (User receiver : receivers) {
                    receiver_ids.put(receiver.getId());
                }
                jsonObject.put("receiver_ids", receiver_ids);

                SharedPreferences sharedPreferences = getSharedPreferences(
                        getString(R.string.preference_file), MODE_PRIVATE);
                jsonObject.put("sender_id", sharedPreferences.getInt("id", 0));
                jsonObject.put("threshold_value", thresholdValue);
                jsonObject.put("filename", filename);

                byte[] imageBytes = IOUtils.toByteArray(imageStream);

                jsonObject.put("image", Base64.encodeToString(imageBytes, Base64.DEFAULT));
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            String jsonStr = jsonObject.toString();

            HttpURLConnection connection = null;
            DataOutputStream dos;

            try {
                URL url = new URL(uri.toString());
                connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json;");
                connection.connect();

                dos = new DataOutputStream(connection.getOutputStream());
                dos.writeBytes(jsonStr);
                dos.flush();
                dos.close();
                connection.getInputStream();
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }

                Log.v("BEFORE", "BEFORE");
                if (deleteViaContentProvider(getApplicationContext(), imageFile.getAbsolutePath())) {
                //if (imageFile.delete()) {
                    Log.v("DELETED", "del");
                } else {
                    Log.v("NOT DELETED", "nd");
                }
                Log.v("AFTER", "AFTER");
            }
            return null;
        }
    }
}
