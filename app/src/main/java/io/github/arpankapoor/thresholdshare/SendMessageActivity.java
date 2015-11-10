package io.github.arpankapoor.thresholdshare;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

import io.github.arpankapoor.user.User;


public class SendMessageActivity extends AppCompatActivity {

    private static final int SELECT_IMAGE_REQ = 1;

    private List<User> receivers = null;
    private InputStream imageStream = null;
    private int thresholdValue = 100;
    private int validHours = 1;

    private Fragment activeFragment = null;
    private SelectReceiversFragment selectReceiversFragment = null;
    private SelectThresholdFragment selectThresholdFragment = null;

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        startActivityForResult(Intent.createChooser(intent, "Select Image"), SELECT_IMAGE_REQ);
    }

    private void setFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
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

        // Set Fragment
        selectReceiversFragment = new SelectReceiversFragment();
        setFragment(selectReceiversFragment);
    }

    public void nextButtonHandler(View view) {
        if (activeFragment == selectReceiversFragment) {
            receivers = selectReceiversFragment.getSelectedReceivers();

            // Select Threshold Value
            selectThresholdFragment = new SelectThresholdFragment();
            setFragment(selectThresholdFragment);
            Button nextButton = (Button) findViewById(R.id.next_button);
            nextButton.setText(getString(R.string.send_message));
        } else if (activeFragment == selectThresholdFragment) {
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
}
