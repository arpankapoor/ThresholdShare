package io.github.arpankapoor.thresholdshare;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

public class SelectFilenameFragment extends Fragment {

    public SelectFilenameFragment() {
    }

    public String getFilename() {
        EditText filenameEditText = (EditText) getView().findViewById(R.id.filename_edit_text);
        return filenameEditText.getText().toString();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Set Title
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Enter filename");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_select_filename, container, false);
    }


}
