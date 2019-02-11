package it.unipi.iet.bikedacity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v4.app.ActivityCompat;

import java.util.EnumMap;
import java.util.Map;

public class BikeDaCityUtil {
    public static String[] permissionsNeeded = {Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.WRITE_EXTERNAL_STORAGE};
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
                                                             final String[] permissions,
                                                             final int requestCode){
        return BikeDaCityUtil.createAlertDialogWithPositiveButtonOnly(activity,
                R.string.perm_dialog_title,
                R.string.perm_dialog_message,
                R.string.perm_dialog_button,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick (DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(activity, permissions, requestCode);
                    }
                });
    }

    // Ordered by crescent priority
    public enum Availability{
        NO_AVAILABILITY,
        LOW_AVAILABILITY,
        MEDIUM_AVAILABILITY,
        HIGH_AVAILABILITY
    }

    public static int getOverlayDrawableId (Availability availability, boolean isShowingParking){
        return getOverlayDrawablesIds(isShowingParking).get(availability);
    }

    public static Map<Availability, Integer> getOverlayDrawablesIds (boolean isShowingParking){
        Map<Availability, Integer> idsMap = new EnumMap<>(Availability.class);
        if (isShowingParking){
            idsMap.put(Availability.NO_AVAILABILITY, R.drawable.ic_place_no_availability);
            idsMap.put(Availability.LOW_AVAILABILITY, R.drawable.ic_place_low_availability);
            idsMap.put(Availability.MEDIUM_AVAILABILITY, R.drawable.ic_place_medium_availability);
            idsMap.put(Availability.HIGH_AVAILABILITY, R.drawable.ic_place_high_availability);
        }
        else {
            idsMap.put(Availability.NO_AVAILABILITY, R.drawable.ic_free_bike_no_availability);
            idsMap.put(Availability.LOW_AVAILABILITY, R.drawable.ic_free_bike_low_availability);
            idsMap.put(Availability.MEDIUM_AVAILABILITY, R.drawable.ic_free_bike_medium_availability);
            idsMap.put(Availability.HIGH_AVAILABILITY, R.drawable.ic_free_bike_high_availability);
        }

        return idsMap;
    }

    public static Map<Availability, Drawable> getOverlayDrawables (Context ctx, boolean isShowingParking){
        Map<Availability, Drawable> drawables = new EnumMap<>(Availability.class);
        Resources resources = ctx.getResources();
        if (isShowingParking){
            drawables.put(Availability.NO_AVAILABILITY,
                    resources.getDrawable(R.drawable.ic_place_no_availability));
            drawables.put(Availability.LOW_AVAILABILITY,
                    resources.getDrawable(R.drawable.ic_place_low_availability));
            drawables.put(Availability.MEDIUM_AVAILABILITY,
                    resources.getDrawable(R.drawable.ic_place_medium_availability));
            drawables.put(Availability.HIGH_AVAILABILITY,
                    resources.getDrawable(R.drawable.ic_place_high_availability));
        }
        else {
            drawables.put(Availability.NO_AVAILABILITY,
                    resources.getDrawable(R.drawable.ic_free_bike_no_availability));
            drawables.put(Availability.LOW_AVAILABILITY,
                    resources.getDrawable(R.drawable.ic_free_bike_low_availability));
            drawables.put(Availability.MEDIUM_AVAILABILITY,
                    resources.getDrawable(R.drawable.ic_free_bike_medium_availability));
            drawables.put(Availability.HIGH_AVAILABILITY,
                    resources.getDrawable(R.drawable.ic_free_bike_high_availability));
        }
        return drawables;
    }


    public static Map<BikeDaCityUtil.Availability, String> getOverlayNames (Context ctx){
        Resources resources = ctx.getResources();
        Map<BikeDaCityUtil.Availability, String> overlayNames = new EnumMap<>(BikeDaCityUtil.Availability.class);
        overlayNames.put(BikeDaCityUtil.Availability.NO_AVAILABILITY, resources.getString(R.string.no_availability_overlay_name));
        overlayNames.put(BikeDaCityUtil.Availability.LOW_AVAILABILITY, resources.getString(R.string.low_availability_overlay_name));
        overlayNames.put(BikeDaCityUtil.Availability.MEDIUM_AVAILABILITY, resources.getString(R.string.medium_availability_overlay_name));
        overlayNames.put(BikeDaCityUtil.Availability.HIGH_AVAILABILITY, resources.getString(R.string.high_availability_overlay_name));
        return overlayNames;
    }

    public static int[] getOverlayButtonDrawableIds (){
        // This part is application specific, pay attention to the priority order into BikeDaCityUtil.Availability enum
        int[] backgrounds = new int[BikeDaCityUtil.Availability.values().length << 1];
        backgrounds[0] = R.drawable.ic_place_view_all_h24;
        backgrounds[1] = R.drawable.ic_place_view_up_to_low_h24;
        backgrounds[2] = R.drawable.ic_place_view_up_to_medium_h24;
        backgrounds[3] = R.drawable.ic_place_view_high_h24;
        backgrounds[4] = R.drawable.ic_free_bike_view_all_h24;
        backgrounds[5] = R.drawable.ic_free_bike_view_up_to_low_h24;
        backgrounds[6] = R.drawable.ic_free_bike_view_up_to_medium_h24;
        backgrounds[7] = R.drawable.ic_free_bike_view_high_h24;
        return backgrounds;
    }
}
