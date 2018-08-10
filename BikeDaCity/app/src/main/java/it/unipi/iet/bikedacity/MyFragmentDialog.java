package it.unipi.iet.bikedacity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

public class MyFragmentDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Apply the custom layout to the activity, since it is a DialogFragment the root parameter
        // can be set to null
        builder.setView(inflater.inflate(R.layout.about, null));
        builder.setTitle(R.string.about_dialog_title);
        builder.setPositiveButton("Ok", null);
        return builder.create();
    }
}
