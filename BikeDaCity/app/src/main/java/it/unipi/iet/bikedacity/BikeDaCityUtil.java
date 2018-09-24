package it.unipi.iet.bikedacity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.app.ActivityCompat;

public class BikeDaCityUtil {
    public static AlertDialog createAlertDialogWithPositiveButtonOnly (Context context,
                                                                       int titleId,
                                                           int messageId,
                                                           int buttonTextId,
                                                           DialogInterface.OnClickListener listener){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(titleId);
        builder.setMessage(messageId);
        builder.setPositiveButton(buttonTextId, listener);
        return builder.create();
    }

    public static AlertDialog createAlertDialogWithTwoButton (Context context,
                                                              int titleId,
                                                              int messageId,
                                                              int positiveButtonTextId,
                                                              int negativeButtonTextId,
                                                              DialogInterface.OnClickListener positiveListener,
                                                              DialogInterface.OnClickListener negativeListener){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(titleId);
        builder.setMessage(messageId);
        builder.setPositiveButton(positiveButtonTextId, positiveListener);
        builder.setNegativeButton(negativeButtonTextId, negativeListener);
        return builder.create();
    }

    public static AlertDialog getPermissionsRationaleDialog (final Activity activity,
                                                             final String[] permissions){
        return BikeDaCityUtil.createAlertDialogWithPositiveButtonOnly(activity,
                R.string.perm_dialog_title,
                R.string.perm_dialog_message,
                R.string.perm_dialog_button,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick (DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(activity, permissions, 0);
                    }
                });
    }
}
