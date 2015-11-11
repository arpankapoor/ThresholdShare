package io.github.arpankapoor.thresholdshare;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;

import com.google.gson.Gson;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import io.github.arpankapoor.user.User;

public class RegisterationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registeration);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void invalidNameAlert(View v) {
        new AlertDialog.Builder(v.getContext())
                .setMessage("Please enter your name")
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                .show();
    }

    public void registerClickHandler(View view) {
        EditText nameEditText = (EditText) findViewById(R.id.name_edit_text);
        String name = nameEditText.getText().toString();
        if (name.equals("")) {
            invalidNameAlert(view);
        } else {
            new RegisterUserTask().execute(name);
        }
    }

    private class RegisterUserTask extends AsyncTask<String, Void, User> {
        @Override
        protected User doInBackground(String... params) {
            Uri uri = Uri.parse(getString(R.string.server_base_url))
                    .buildUpon()
                    .appendPath(getString(R.string.register_user_api))
                    .build();

            String name = params[0];
            JSONObject jsonObject = new JSONObject();
            HttpURLConnection connection = null;
            DataOutputStream dos;
            try {
                jsonObject.put("name", name);
                String jsonStr = jsonObject.toString();
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
                return new Gson().fromJson(returnJsonStr, User.class);
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
        protected void onPostExecute(User user) {
            SharedPreferences sharedPreferences = getSharedPreferences(
                    getString(R.string.preference_file), MODE_PRIVATE);

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("id", user.getId());
            editor.commit();

            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
