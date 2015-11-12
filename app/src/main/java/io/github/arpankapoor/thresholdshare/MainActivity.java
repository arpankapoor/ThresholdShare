package io.github.arpankapoor.thresholdshare;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.DataOutputStream;
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

public class MainActivity extends AppCompatActivity {

    private int userId;
    private DbHelper dbHelper;

    private void setList() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Get users who have sent this user a message
        String rawSql =
                "SELECT DISTINCT " +
                        UserContract.UserEntry.COLUMN_NAME_NAME + ", " +
                        UserContract.UserEntry.TABLE_NAME + "." + UserContract.UserEntry._ID +
                        " FROM " + UserContract.UserEntry.TABLE_NAME +
                        " INNER JOIN " +
                        MessageContract.MessageEntry.TABLE_NAME +
                        " ON " +
                        UserContract.UserEntry.TABLE_NAME + "." + UserContract.UserEntry._ID +
                        " = " +
                        MessageContract.MessageEntry.TABLE_NAME + "." +
                        MessageContract.MessageEntry.COLUMN_NAME_SENDER_ID;
        Log.v("HI", rawSql);

        Cursor cursor = db.rawQuery(rawSql, null);

        String[] columns = new String[]{UserContract.UserEntry.COLUMN_NAME_NAME};
        SimpleCursorAdapter mAdapter =
                new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, cursor, columns,
                        new int[]{android.R.id.text1}, CursorAdapter.FLAG_AUTO_REQUERY);

        ListView listView = (ListView) findViewById(R.id.messages_list_view);
        listView.setAdapter(mAdapter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dbHelper = new DbHelper(this);

        SharedPreferences sharedPreferences = getSharedPreferences(
                getString(R.string.preference_file), MODE_PRIVATE);

        userId = sharedPreferences.getInt("id", -1);

        if (userId == -1) {
            Intent intent = new Intent(this, RegisterationActivity.class);
            startActivity(intent);
            finish();
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent sendMessageIntent = new Intent(view.getContext(), SendMessageActivity.class);
                startActivity(sendMessageIntent);
            }
        });

        callAsynchronousTask();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void callAsynchronousTask() {
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
        timer.schedule(doAsynchronousTask, 0, 30000);
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
                ex.printStackTrace();
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
                values.put(MessageContract.MessageEntry._ID, message.getMessageId());
                values.put(MessageContract.MessageEntry.COLUMN_NAME_SENDER_ID, message.getSenderId());
                values.put(MessageContract.MessageEntry.COLUMN_NAME_FILENAME, message.getFilename());
                if (message.getType().equals("key")) {
                    values.put(MessageContract.MessageEntry.COLUMN_NAME_KEY, message.getData());
                } else {
                    FileOutputStream outputStream;
                    try {
                        outputStream = openFileOutput(message.getFilename(), Context.MODE_PRIVATE);
                        byte[] imageBytes = Base64.decode(message.getData(), Base64.DEFAULT);
                        outputStream.write(imageBytes);
                    } catch (Exception ex) {
                    }
                }

                Log.v("HI", message.getMessageId() + message.getFilename() + message.getSenderId());
                db.insert(MessageContract.MessageEntry.TABLE_NAME, null, values);
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
                ex.printStackTrace();
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
