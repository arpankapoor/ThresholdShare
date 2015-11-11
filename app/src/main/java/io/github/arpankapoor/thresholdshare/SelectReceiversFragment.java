package io.github.arpankapoor.thresholdshare;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import io.github.arpankapoor.user.User;

public class SelectReceiversFragment extends ListFragment {

    public SelectReceiversFragment() {
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new GetUsersTask().execute();
    }

    public List<User> getSelectedReceivers() {
        List<User> selectedUsers = new ArrayList<>();
        ListView listView = getListView();
        int count = listView.getCount();
        SparseBooleanArray sparseBooleanArray = listView.getCheckedItemPositions();

        for (int i = 0; i < count; i++) {
            if (sparseBooleanArray.get(i)) {
                selectedUsers.add((User) listView.getItemAtPosition(i));
            }
        }

        return selectedUsers;
    }

    private class GetUsersTask extends AsyncTask<Void, Void, List<User>> {
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

        protected void onPostExecute(List<User> users) {
            setListAdapter(new ArrayAdapter<>(getActivity(),
                    android.R.layout.simple_list_item_multiple_choice, users));
        }
    }
}
