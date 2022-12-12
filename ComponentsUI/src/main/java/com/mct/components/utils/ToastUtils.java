package com.mct.components.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.mct.components.R;

public class ToastUtils {

    public static int INFO = 1;
    public static int ERROR = 2;
    public static int SUCCESS = 3;
    public static int WARNING = 4;
    public static int DEFAULT = 5;

    /**
     * @param context context to initialise toast.
     * @param message message to show to user.
     * @return default toast without icons.
     */
    @NonNull
    public static Toast makeDefaultToast(Context context, int duration, String message) {
        return makeText(context, duration, DEFAULT, message, false);
    }

    /**
     * @param context        context to initialise toast.
     * @param message        message to show to user.
     * @param shouldShowIcon should icon be displayed.
     * @return success toast object.
     */
    @NonNull
    public static Toast makeSuccessToast(Context context, int duration, String message, boolean shouldShowIcon) {
        return makeText(context, duration, SUCCESS, message, shouldShowIcon);
    }

    /**
     * @param context        context to initialise toast
     * @param message        message to show to user
     * @param shouldShowIcon should icon be displayed.
     * @return error toast object.
     */
    @NonNull
    public static Toast makeErrorToast(Context context, int duration, String message, boolean shouldShowIcon) {
        return makeText(context, duration, ERROR, message, shouldShowIcon);
    }

    /**
     * @param context        context to initialise toast
     * @param message        message to show to user
     * @param shouldShowIcon should icon be displayed.
     * @return warning toast object.
     */
    @NonNull
    public static Toast makeWarningToast(Context context, int duration, String message, boolean shouldShowIcon) {
        return makeText(context, duration, WARNING, message, shouldShowIcon);
    }

    /**
     * @param context        context to initialise toast.
     * @param message        message to show to user.
     * @param shouldShowIcon should icon be displayed.
     * @return information toast object.
     */
    @NonNull
    public static Toast makeInfoToast(Context context, int duration, String message, boolean shouldShowIcon) {
        return makeText(context, duration, INFO, message, shouldShowIcon);
    }

    /**
     * @param context  context to initialise toast
     * @param duration duration of toast
     * @param type     type toast
     * @param message  message to show to user
     * @return toast with icons.
     */
    @NonNull
    public static Toast makeText(Context context, int duration, int type, String message) {
        return makeText(context, duration, type, message, true);
    }

    /**
     * @param context    context to initialise toast.
     * @param duration   duration of toast
     * @param type       type toast
     * @param message    message to show to user.
     * @param isShowIcon should icon be displayed.
     * @return custom toast object with user specified customisations.
     */
    @NonNull
    public static Toast makeText(Context context, int duration, int type, String message, boolean isShowIcon) {
        ToastView toastView = new ToastView(context, type, message, isShowIcon);
        Toast toast = new Toast(context);
        toast.setDuration(duration);
        toast.setView(toastView);
        return toast;
    }

    private static class ToastView extends LinearLayout {

        final ImageView imgIcon;
        final TextView tvMessage;

        ToastView(Context context, int type, String message, boolean isShowIcon) {
            super(context);
            int tbPadding = convertDpToPixel(6);
            int lrPadding = convertDpToPixel(12);
            int imgSize = convertDpToPixel(24);
            int childMargin = convertDpToPixel(4);

            setGravity(Gravity.CENTER);
            setPadding(lrPadding, tbPadding, lrPadding, tbPadding);

            imgIcon = new ImageView(context);
            LayoutParams iconParam = new LayoutParams(imgSize, imgSize);
            iconParam.setMargins(childMargin, childMargin, childMargin, childMargin);
            addView(imgIcon, iconParam);

            tvMessage = new TextView(context);
            tvMessage.setTextColor(Color.WHITE);
            tvMessage.setTextSize(16);
            tvMessage.setText(message);
            tvMessage.setGravity(isShowIcon ? Gravity.START : Gravity.CENTER);
            LayoutParams messageParam = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            messageParam.setMargins(childMargin, childMargin, childMargin, childMargin);
            addView(tvMessage, messageParam);

            setIconVisibility(isShowIcon ? View.VISIBLE : View.GONE);
            if (type == SUCCESS) {
                init(getColor("#1EDD00"), R.drawable.cpui_ic_toast_success);
            } else if (type == ERROR) {
                init(getColor("#FF3D00"), R.drawable.cpui_ic_toast_error);
            } else if (type == WARNING) {
                init(getColor("#FFC400"), R.drawable.cpui_ic_toast_warning);
            } else if (type == INFO) {
                init(getColor("#00B0FF"), R.drawable.cpui_ic_toast_info);
            } else if (type == DEFAULT) {
                init(getColor("#555555"), -1);
            }
        }

        void init(int bgColor, int icon) {
            setBgColor(bgColor);
            setIcon(icon);
        }

        void setBgColor(int bgColor) {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.RECTANGLE);
            drawable.setCornerRadius(convertDpToPixel(100));
            drawable.setColor(bgColor);
            setBackground(drawable);
        }

        void setIcon(int icon) {
            imgIcon.setImageResource(icon);
        }

        void setIconVisibility(int visibility) {
            imgIcon.setVisibility(visibility);
        }

        int convertDpToPixel(int dp) {
            return dp * getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT;
        }

        int getColor(String color) {
            return Color.parseColor(color);
        }
    }

}