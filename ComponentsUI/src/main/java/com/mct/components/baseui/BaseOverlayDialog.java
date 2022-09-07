package com.mct.components.baseui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Build;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.CallSuper;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AlertDialog;

import com.mct.components.utils.ScreenUtils;

public abstract class BaseOverlayDialog extends BaseOverlayLifecycle {

    private static final String TAG = "MCT_B_Dialog";
    protected Context context;
    /**
     * The Android InputMethodManger, for ensuring the keyboard dismiss on dialog dismiss.
     */
    private final InputMethodManager inputMethodManager;

    public BaseOverlayDialog(@NonNull Context context) {
        this.context = context;
        inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    /**
     * The dialog currently displayed by this controller.
     * Null until [onCreate] is called, or if it has been dismissed.
     */
    private AlertDialog dialog = null;

    /**
     * Creates the dialog shown by this controller.
     * Note that the cancelable value and the dismiss listener will be overridden
     * with internal values once, so any values for them defined here will not be kept.
     *
     * @return the builder for the dialog to be created.
     */
    @NonNull
    protected abstract AlertDialog.Builder onCreateDialog();

    /**
     * Setup the dialog view.
     * Called once the dialog is created and first show,
     * it allows the implementation to initialize the content views.
     *
     * @param dialog the newly created dialog.
     */
    protected abstract void onDialogCreated(@NonNull AlertDialog dialog);

    @Override
    protected void onCreate() {
        dialog = initDialogListener(onCreateDialog());
        initWindow(dialog);
        onDialogCreated(dialog);
    }

    @Override
    public final void start() {
        if (!isShowing) {
            isShowing = true;
            dialog.show();
            onVisibilityChanged(true);
        }
        super.start();
    }

    @Override
    public final void stop(boolean hideUi) {
        if (hideUi && isShowing) {
            hideSoftInput();
            isShowing = false;
            dialog.hide();
            onVisibilityChanged(false);
        }
        super.stop(hideUi);
    }

    @Override
    protected void onDismissed() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    @Override
    public final void showSubOverlay(BaseOverlayLifecycle baseOverlayLifecycle, Boolean hideCurrent) {
        super.showSubOverlay(baseOverlayLifecycle, hideCurrent);
        hideSoftInput();
    }

    ///////////////////////////////////////////////////////////////////////////
    // PROTECTED AREA < can override to set or change property >
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Overlay
     *
     * @return true if the dialog is overlay, false isn't
     */
    protected boolean isOverlay() {
        return false;
    }

    /**
     * DialogAnim
     */
    @StyleRes
    protected int getDialogAnim() {
        return 0;
    }

    /**
     * BackgroundResource
     * Background of dialog window
     * Note : BackgroundDrawableResource | just accept res has tail ".xml"
     */
    @DrawableRes
    protected int getBackgroundResource() {
        return 0;
    }

    /**
     * BackgroundColor
     * Color of dialog window
     */
    @ColorInt
    protected int getBackgroundColor() {
        return Color.WHITE;
    }

    /**
     * CornerRadius
     *
     * @return corner of dialog </br>
     * corner >= 0 | Unit : dp
     */
    protected int getCornerRadius() {
        return 0;
    }

    protected int getSoftInputMode() {
        return WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN;
    }

    /**
     * Called when the visibility of the dialog has changed due
     * to a call to {@link #start()} or {@link #stop(boolean)}.
     * <p>
     * Once the sub element is dismissed, this method will be called again,
     * notifying for the new visibility of the dialog.
     *
     * @param visible the dialog visibility value. True for visible, false for hidden.
     */
    protected void onVisibilityChanged(boolean visible) {
        Log.e(TAG, (visible ? "show overlay " : "hide overlay ") + hashCode());
    }

    protected void onInitWindow(@NonNull Window window) {
    }

    /**
     * Handle the dialog dismissing.
     */
    @CallSuper
    protected void onDialogDismissed() {
        isShowing = false;
        dialog = null;
        context = null;
    }

    ///////////////////////////////////////////////////////////////////////////
    // PRIVATE AREA
    ///////////////////////////////////////////////////////////////////////////

    @NonNull
    private AlertDialog initDialogListener(@NonNull AlertDialog.Builder builder) {
        // init dialog listener
        return builder.setOnDismissListener(dialogInterface -> {
            dismiss();
            hideSoftInput();
            onDialogDismissed();
        }).setOnKeyListener((dialogInterface, i, keyEvent) -> {
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK
                    && keyEvent.getAction() == KeyEvent.ACTION_UP) {
                dismiss();
                return true;
            }
            return false;
        }).create();
    }

    private void initWindow(@NonNull AlertDialog dialog) {
        // init window
        Window window = dialog.getWindow();
        // overlay
        if (isOverlay()) {
            window.setType(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                    WindowManager.LayoutParams.TYPE_PHONE);
        }
        // animations
        if (getDialogAnim() != 0) {
            window.getAttributes().windowAnimations = getDialogAnim();
        }
        // background
        if (getBackgroundResource() != 0) {
            window.setBackgroundDrawableResource(getBackgroundResource());
        } else {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setColor(getBackgroundColor());
            drawable.setCornerRadius(ScreenUtils.dp2px(getCornerRadius()));
            window.setBackgroundDrawable(new InsetDrawable(drawable, ScreenUtils.dp2px(16)));
        }
        // soft input
        window.setSoftInputMode(getSoftInputMode());
        // auto hide soft input
        window.getDecorView().setOnTouchListener((view, motionEvent) -> {
            hideSoftInput();
            view.performClick();
            return false;
        });
        onInitWindow(window);
    }

    /**
     * Hide the software keyboard.
     */
    private void hideSoftInput() {
        inputMethodManager.hideSoftInputFromWindow(dialog.getWindow()
                .getDecorView().getWindowToken(), 0);
    }

}