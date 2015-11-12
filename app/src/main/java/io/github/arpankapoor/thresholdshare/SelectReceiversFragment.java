package io.github.arpankapoor.thresholdshare;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import java.util.ArrayList;
import java.util.List;

import io.github.arpankapoor.db.DbHelper;
import io.github.arpankapoor.db.UserContract;
import io.github.arpankapoor.user.User;

public class SelectReceiversFragment extends ListFragment {

    public SelectReceiversFragment() {
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(
                getString(R.string.preference_file), Context.MODE_PRIVATE);

        int userId = sharedPreferences.getInt("id", -1);

        DbHelper dbHelper = new DbHelper(getActivity());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String rawSql = "SELECT * FROM " +
                UserContract.UserEntry.TABLE_NAME +
                " WHERE " + UserContract.UserEntry._ID + " != " + userId;

        Cursor cursor = db.rawQuery(rawSql, null);

        String[] columns = new String[]{UserContract.UserEntry.COLUMN_NAME_NAME};
        SimpleCursorAdapter mAdapter =
                new SimpleCursorAdapter(getActivity(),
                        android.R.layout.simple_list_item_multiple_choice,
                        cursor, columns, new int[]{android.R.id.text1}, 0);
        setListAdapter(mAdapter);
    }

    public List<User> getSelectedReceivers() {
        List<User> selectedUsers = new ArrayList<>();
        ListView listView = getListView();
        int count = listView.getCount();
        SparseBooleanArray sparseBooleanArray = listView.getCheckedItemPositions();

        for (int i = 0; i < count; i++) {
            if (sparseBooleanArray.get(i)) {
                Cursor cursor = (Cursor) listView.getItemAtPosition(i);
                int userIdColumnIndex = cursor.getColumnIndex(UserContract.UserEntry._ID);
                int userNameColumnIndex = cursor.getColumnIndex(
                        UserContract.UserEntry.COLUMN_NAME_NAME);
                int userId = cursor.getInt(userIdColumnIndex);
                String userName = cursor.getString(userNameColumnIndex);
                selectedUsers.add(new User(userId, userName));
            }
        }

        return selectedUsers;
    }

}
