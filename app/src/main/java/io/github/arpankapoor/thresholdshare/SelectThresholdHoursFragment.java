package io.github.arpankapoor.thresholdshare;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;

public class SelectThresholdHoursFragment extends Fragment {

    public SelectThresholdHoursFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Set Title
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Select Time Frame");

        NumberPicker thresholdPicker = (NumberPicker) getView().
                findViewById(R.id.thresholdHoursPicker);
        thresholdPicker.clearFocus();
        thresholdPicker.setMinValue(1);
        thresholdPicker.setMaxValue(100);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_select_threshold_hours, container, false);
    }
}
