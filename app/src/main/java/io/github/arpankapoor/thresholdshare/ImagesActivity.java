package io.github.arpankapoor.thresholdshare;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.FileProvider;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import io.github.arpankapoor.db.DbHelper;
import io.github.arpankapoor.db.MessageContract;
import io.github.arpankapoor.db.UserContract;
import io.github.arpankapoor.message.Message;
import io.github.arpankapoor.user.User;

public class ImagesActivity extends AppCompatActivity {

    private DbHelper dbHelper;
    private int userId = -1;
    private int senderId = -1;

    private void setList() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String rawSql =
                "SELECT * " +
                        " FROM " + MessageContract.MessageEntry.TABLE_NAME +
                        " WHERE " +
                        MessageContract.MessageEntry.COLUMN_NAME_SENDER_ID + " = " +
                        senderId;

        Log.v("HI", rawSql);

        Cursor cursor = db.rawQuery(rawSql, null);

        String[] columns = new String[]{MessageContract.MessageEntry.COLUMN_NAME_FILENAME};
        SimpleCursorAdapter mAdapter =
                new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, cursor, columns,
                        new int[]{android.R.id.text1}, CursorAdapter.FLAG_AUTO_REQUERY);

        ListView listView = (ListView) findViewById(R.id.images_list_view);
        listView.setAdapter(mAdapter);
    }

    private void waitAlert() {
        new AlertDialog.Builder(this)
                .setMessage("Please wait for others to provide their decryption key.")
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                .show();
    }

    private void sendKeyAlert(final int messageId) {
        new AlertDialog.Builder(this)
                .setMessage("Send your key for decryption?")
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new SendKeyTask().execute(messageId);
                                dialog.dismiss();
                            }
                        })
                .show();
    }

    private void updateMessageStatus(int messageId, int status) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(MessageContract.MessageEntry.COLUMN_NAME_STATUS, status);

        db.update(MessageContract.MessageEntry.TABLE_NAME, values,
                MessageContract.MessageEntry._ID + " = " + messageId, null);
    }

    private void setListViewListener() {
        final ListView listView = (ListView) findViewById(R.id.images_list_view);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = (Cursor) listView.getItemAtPosition(position);
                int statusColumnIndex = cursor.getColumnIndex(
                        MessageContract.MessageEntry.COLUMN_NAME_STATUS);
                int status = cursor.getInt(statusColumnIndex);

                int messageIdColumnIndex = cursor.getColumnIndex(
                        MessageContract.MessageEntry._ID);
                int messageId = cursor.getInt(messageIdColumnIndex);

                int filenameColumnIndex = cursor.getColumnIndex(
                        MessageContract.MessageEntry.COLUMN_NAME_FILENAME);
                String filename = cursor.getString(filenameColumnIndex);

                if (status == MessageContract.MessageEntry.STATUS_KEY_RCVD) {
                    // Give option to send image
                    sendKeyAlert(messageId);
                } else if (status == MessageContract.MessageEntry.STATUS_KEY_SENT) {
                    // Please wait
                    waitAlert();
                } else {
                    // Show image
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    File file = new File(getFilesDir(), filename);
                    Uri uri = FileProvider.getUriForFile(getApplicationContext(),
                            "io.github.arpankapoor", file);
                    intent.setDataAndType(uri, "image/*");
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_images);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        dbHelper = new DbHelper(this);

        Intent intent = getIntent();
        senderId = intent.getIntExtra("sender_id", 0);
        userId = intent.getIntExtra("user_id", -1);

        setListViewListener();
        callAsynchronousTask();
    }

    private void callAsynchronousTask() {
        final Handler handler = new Handler();
        Timer timer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            new GetUsersTask().execute();
                            new GetMessagesTask().execute();
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                        }
                    }
                });
            }
        };
        timer.schedule(doAsynchronousTask, 0, 10000);
    }

    private class SendKeyTask extends AsyncTask <Integer, Void, Void> {
        private String getKey(int messageId) {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.query(MessageContract.MessageEntry.TABLE_NAME,
                    new String[]{MessageContract.MessageEntry.COLUMN_NAME_KEY},
                    MessageContract.MessageEntry._ID + " = " + messageId,
                    null, null, null, null);

            cursor.moveToFirst();
            int keyColumnIndex = cursor.getColumnIndex(
                    MessageContract.MessageEntry.COLUMN_NAME_KEY);

            return cursor.getString(keyColumnIndex);
        }

        @Override
        protected Void doInBackground(Integer... params) {
            int messageId = params[0];
            String key = getKey(messageId);
            if (key == null || messageId == -1 || userId == -1) {
                return null;
            }

            Uri uri = Uri.parse(getString(R.string.server_base_url))
                    .buildUpon()
                    .appendPath(getString(R.string.send_key_api))
                    .build();

            JSONObject jsonObject = new JSONObject();
            HttpURLConnection connection = null;
            DataOutputStream dos;
            try {
                jsonObject.put("id", userId);
                jsonObject.put("message_id", messageId);
                jsonObject.put("key", key);
                Log.v("HI", userId + "");

                String jsonStr = jsonObject.toString();
                Log.v("HI", jsonStr);
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
                //
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            updateMessageStatus(messageId, MessageContract.MessageEntry.STATUS_KEY_SENT);
            return null;
        }
    }

    private class GetMessagesTask extends AsyncTask<Void, Void, List<Message>> {
        @Override
        protected List<Message> doInBackground(Void... params) {
            if (userId == -1) {
                return null;
            }
            Uri uri = Uri.parse(getString(R.string.server_base_url))
                    .buildUpon()
                    .appendPath(getString(R.string.get_messages_api))
                    .build();

            JSONObject jsonObject = new JSONObject();
            HttpURLConnection connection = null;
            DataOutputStream dos;
            try {
                jsonObject.put("id", userId);
                Log.v("HI", userId + "");
                String jsonStr = jsonObject.toString();
                Log.v("HI", jsonStr);
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
                InputStream is = connection.getInputStream();
                String returnJsonStr = IOUtils.toString(is, (String) null);
                Log.v("HDFLK", returnJsonStr);

                return new GsonBuilder()
                        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                        .create()
                        .fromJson(returnJsonStr, new TypeToken<List<Message>>() {
                        }.getType());
            } catch (Exception ex) {
                //
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<Message> messages) {
            if (messages == null || messages.size() == 0) {
                Log.v("HI", "null");
                return;
            }
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            for (Message message : messages) {
                Log.v("list", message.toString());
                ContentValues values = new ContentValues();
                if (message.getType().equals("key")) {
                    values.put(MessageContract.MessageEntry._ID, message.getMessageId());
                    values.put(MessageContract.MessageEntry.COLUMN_NAME_SENDER_ID, message.getSenderId());
                    values.put(MessageContract.MessageEntry.COLUMN_NAME_FILENAME, message.getFilename());
                    values.put(MessageContract.MessageEntry.COLUMN_NAME_KEY, message.getData());

                    values.put(MessageContract.MessageEntry.COLUMN_NAME_STATUS,
                            MessageContract.MessageEntry.STATUS_KEY_RCVD);

                    // Add the entry
                    db.insert(MessageContract.MessageEntry.TABLE_NAME, null, values);
                } else {
                    FileOutputStream outputStream;
                    try {
                        outputStream = openFileOutput(message.getFilename(), Context.MODE_PRIVATE);
                        byte[] imageBytes = Base64.decode(message.getData(), Base64.DEFAULT);
                        outputStream.write(imageBytes);
                    } catch (Exception ex) {
                        //
                    }

                    int messageId = message.getMessageId();

                    values.put(MessageContract.MessageEntry.COLUMN_NAME_STATUS,
                            MessageContract.MessageEntry.STATUS_IMG_RCVD);
                    // update
                    db.update(MessageContract.MessageEntry.TABLE_NAME,
                            values,
                            MessageContract.MessageEntry._ID + " = " + messageId,
                            null);
                }

                Log.v("HI", message.getMessageId() + message.getFilename() + message.getSenderId());
            }

            setList();
        }
    }

    private class GetUsersTask extends AsyncTask<Void, Void, List<User>> {
        @Override
        protected List<User> doInBackground(Void... params) {
            Uri uri = Uri.parse(getString(R.string.server_base_url))
                    .buildUpon()
                    .appendPath(getString(R.string.get_user_list_api))
                    .build();

            HttpURLConnection connection = null;
            try {
                URL url = new URL(uri.toString());
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream inputStream = connection.getInputStream();
                if (inputStream == null) {
                    return null;
                }

                String jsonStr = IOUtils.toString(inputStream, (String) null);
                if (jsonStr.length() == 0) {
                    return null;
                }

                return new Gson().fromJson(jsonStr, new TypeToken<List<User>>() {
                }.getType());
            } catch (IOException ex) {
                //
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<User> users) {
            if (users == null) {
                return;
            }

            SQLiteDatabase db = dbHelper.getWritableDatabase();

            db.execSQL(UserContract.SQL_DELETE_ENTRIES);
            db.execSQL(UserContract.SQL_CREATE_ENTRIES);

            for (User user : users) {
                ContentValues values = new ContentValues();
                values.put(UserContract.UserEntry.COLUMN_NAME_NAME, user.getName());
                values.put(UserContract.UserEntry._ID, user.getId());

                db.insert(UserContract.UserEntry.TABLE_NAME, null, values);
            }
            setList();
        }
    }
}
