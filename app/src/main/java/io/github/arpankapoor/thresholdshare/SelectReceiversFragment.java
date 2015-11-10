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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
            BufferedReader reader = null;

            try {
                URL url = new URL(uri.toString());
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream inputStream = connection.getInputStream();
                StringBuilder buffer = new StringBuilder();
                if (inputStream == null) {
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }

                if (buffer.length() == 0) {
                    return null;
                }

                String json = buffer.toString();

                // Convert to list of User objects
                Gson gson = new Gson();
                return gson.fromJson(json, new TypeToken<List<User>>() {
                }.getType());
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException ex) {
                        ex.printStackTrace();
                    }
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
