package io.github.arpankapoor.thresholdshare;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.View;
import android.widget.Button;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import io.github.arpankapoor.user.User;


public class SendMessageActivity extends AppCompatActivity {

    private static final int SELECT_IMAGE_REQ = 1;

    private List<User> receivers = null;
    private InputStream imageStream = null;
    private String filename = null;
    private int thresholdValue = 100;

    private Fragment activeFragment = null;
    private SelectReceiversFragment selectReceiversFragment = null;
    private SelectFilenameFragment selectFilenameFragment = null;
    private SelectThresholdFragment selectThresholdFragment = null;

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
            selectThresholdFragment.setMaxThresholdValue(receivers.size());

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
                imageStream = getContentResolver().openInputStream(data.getData());
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }
        }
    }

    private class SendMessageTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            Uri uri = Uri.parse(getString(R.string.server_base_url))
                    .buildUpon()
                    .appendPath(getString(R.string.send_message_api))
                    .build();

            JSONObject jsonObject = new JSONObject();
            try {
                for (User receiver : receivers) {
                    jsonObject.accumulate("receiver_ids", receiver.getId());
                }
                jsonObject.put("sender_id", 1);
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
            }
            return null;
        }
    }
}
