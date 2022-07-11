package com.mct.components.baseui;

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.mct.components.R;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseActivity extends AppCompatActivity implements BaseFragmentManager {

    private static final String TAG = "MCT_B_Activity";

    // disable auto hide soft input 1 time
    static boolean sPendingDisableFragmentAutoHideSoftInput;

    private boolean isBackPress;
    private final List<Integer> fragmentIds = new ArrayList<>();

    /**
     * The Container to replace fragment
     *
     * @return @IdRes
     */
    @IdRes
    protected abstract int getContainerId();

    /**
     * just show toast or nothing
     */
    protected abstract void showToastOnBackPressed();

    @Override
    public void onBackPressed() {
        Fragment fragment = getCurrentFragment();
        if (fragment instanceof OnBackPressed && ((OnBackPressed) fragment).onBackPressed()) {
            return;
        }
        if (getBackStackCount() == 0) {
            if (isBackPress) {
                super.onBackPressed();
                isBackPress = false;
            } else {
                showToastOnBackPressed();
                new Handler(Looper.getMainLooper()).postDelayed(() -> isBackPress = false, 3000);
                isBackPress = true;
            }
        } else {
            popLastFragment();
            isBackPress = false;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // FRAGMENT MANAGER
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public int getBackStackCount() {
        return getSupportFragmentManager().getBackStackEntryCount();
    }

    @Override
    public Fragment getCurrentFragment() {
        return getSupportFragmentManager().findFragmentById(getContainerId());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Fragment> T findFragmentByTag(Class<T> cls) {
        try {
            return (T) getSupportFragmentManager().findFragmentByTag(cls.getName());
        } catch (NullPointerException | ClassCastException t) {
            return null;
        }
    }

    @Override
    public void replaceFragment(Fragment fragment) {
        replaceFragment(fragment, Anim.NONE);
    }

    @Override
    public void replaceFragment(@NonNull Fragment fragment, Anim anim) {
        tryAutoHideSoftInput();
        if (getBackStackCount() > 0) {
            sPendingDisableFragmentAutoHideSoftInput = true;
            popLastFragment();
            sPendingDisableFragmentAutoHideSoftInput = true;
            replaceFragmentToStack(fragment, anim);
            return;
        }
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        setAnim(transaction, anim);
        transaction.replace(getContainerId(), fragment, fragment.getClass().getName());
        transaction.commit();
    }

    @Override
    public void replaceFragmentToStack(Fragment fragment) {
        replaceFragmentToStack(fragment, Anim.NONE);
    }

    @Override
    public void replaceFragmentToStack(@NonNull Fragment fragment, Anim anim) {
        tryAutoHideSoftInput();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        setAnim(transaction, anim);
        transaction.replace(getContainerId(), fragment, fragment.getClass().getName());
        transaction.addToBackStack(fragment.getClass().getName());
        int id = transaction.commit();
        fragmentIds.add(id);
        Log.e(TAG, "replaceFragmentToStack: " + id + " : " + fragment.getClass().getName());
    }

    @Override
    public void replaceAndClearBackStack(Fragment fragment) {
        replaceAndClearBackStack(fragment, Anim.NONE);
    }

    @Override
    public void replaceAndClearBackStack(Fragment fragment, Anim anim) {
        clearBackStack();
        replaceFragment(fragment, anim);
    }

    @Override
    public void clearBackStack() {
        if (getBackStackCount() > 0) {
            fragmentIds.clear();
            getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void popLastFragment() {
        tryAutoHideSoftInput();
        if (!fragmentIds.isEmpty()) {
            fragmentIds.remove(fragmentIds.size() - 1);
        }
        getSupportFragmentManager().popBackStack();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void popFragmentToPosition(int position) {
        tryAutoHideSoftInput();
        if (fragmentIds.size() > position) {
            fragmentIds.subList(position + 1, fragmentIds.size()).clear();
            getSupportFragmentManager().popBackStack(fragmentIds.remove(position), FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void popFragmentByAmount(int amount) {
        tryAutoHideSoftInput();
        int size = fragmentIds.size();
        if (size > size - amount) {
            fragmentIds.subList(size - amount + 1, size).clear();
            getSupportFragmentManager().popBackStack((fragmentIds.remove(size - amount)), FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void popToFragment(@NonNull Class<? extends Fragment> cls) {
        popBackStackFragment(cls, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void popToFragmentAndRemove(@NonNull Class<? extends Fragment> cls) {
        popBackStackFragment(cls, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    private void popBackStackFragment(@NonNull Class<? extends Fragment> cls, int flag) {
        tryAutoHideSoftInput();
        boolean isPopped = getSupportFragmentManager().popBackStackImmediate(cls.getName(), flag);
        if (isPopped && fragmentIds.size() > getBackStackCount()) {
            fragmentIds.subList(getBackStackCount(), fragmentIds.size()).clear();
        }
    }

    public enum Anim {
        NONE, FADE,
        TRANSIT_FADE, TRANSIT_OPEN,
        RIGHT_IN_LEFT_OUT, LEFT_IN_RIGHT_OUT,
        RIGHT_IN_LEFT_OUT_70, LEFT_IN_RIGHT_OUT_70
    }

    private void setAnim(@NonNull FragmentTransaction transaction, @NonNull Anim anim) {
        switch (anim) {
            case FADE:
                transaction.setCustomAnimations(R.anim.cpui_anim_fade_in, R.anim.cpui_anim_fade_out, R.anim.cpui_anim_fade_in, R.anim.cpui_anim_fade_out);
                break;
            case TRANSIT_FADE:
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                break;
            case TRANSIT_OPEN:
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                break;
            case RIGHT_IN_LEFT_OUT:
                transaction.setCustomAnimations(R.anim.cpui_anim_right_in, R.anim.cpui_anim_left_out, R.anim.cpui_anim_left_in, R.anim.cpui_anim_right_out);
                break;
            case LEFT_IN_RIGHT_OUT:
                transaction.setCustomAnimations(R.anim.cpui_anim_left_in, R.anim.cpui_anim_right_out, R.anim.cpui_anim_right_in, R.anim.cpui_anim_left_out);
                break;
            case RIGHT_IN_LEFT_OUT_70:
                transaction.setCustomAnimations(R.anim.cpui_anim_right_in, R.anim.cpui_anim_left_out_70, R.anim.cpui_anim_left_in, R.anim.cpui_anim_right_out_70);
                break;
            case LEFT_IN_RIGHT_OUT_70:
                transaction.setCustomAnimations(R.anim.cpui_anim_left_in, R.anim.cpui_anim_right_out_70, R.anim.cpui_anim_right_in, R.anim.cpui_anim_left_out_70);
                break;
            case NONE:
                transaction.setTransition(FragmentTransaction.TRANSIT_NONE);
                break;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // KEY BOARD MANAGER
    ///////////////////////////////////////////////////////////////////////////

    public void clearFocus() {
        View view = getWindow().getCurrentFocus();
        if (view != null) {
            view.clearFocus();
        }
        getAndFocusFakeView();
    }

    public void showSoftInput(@NonNull View view) {
        if (!view.isFocused()) {
            view.requestFocus();
        }
        if (!isSoftInputVisible()) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm == null) {
                return;
            }
            imm.showSoftInput(view, 0);
        }
    }

    public void hideSoftInput() {
        if (isSoftInputVisible()) {
            View view = getWindow().getCurrentFocus();
            if (view == null) {
                view = getAndFocusFakeView();
            }
            hideSoftInput(view);
        }
    }

    public void hideSoftInput(@NonNull final View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) {
            return;
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public boolean isSoftInputVisible() {
        return getSoftInputHeight() != -1;
    }

    /**
     * @return -1 if keyboard is hidden<br>
     * keyboardHeight if key board is visible
     */
    public int getSoftInputHeight() {
        final View contentView = findViewById(android.R.id.content);
        Rect r = new Rect();
        contentView.getWindowVisibleDisplayFrame(r);
        final int screenHeight = contentView.getRootView().getHeight();
        final int softInputHeight = screenHeight - r.bottom;
        Log.e(TAG, "getKeyboardHeight: " + softInputHeight);
        return softInputHeight > screenHeight * 0.15 ? softInputHeight : -1;
    }

    @NonNull
    private View getAndFocusFakeView() {
        View decorView = getWindow().getDecorView();
        View focusView = decorView.findViewWithTag("keyboardTagView");
        View fakeView;
        if (focusView == null) {
            fakeView = new AppCompatEditText(getWindow().getContext()) {
                @Override
                public boolean onCheckIsTextEditor() {
                    return false;// disable auto show key board when resume
                }
            };
            fakeView.setTag("keyboardTagView");
            ((ViewGroup) decorView).addView(fakeView, 0, 0);
        } else {
            fakeView = focusView;
        }
        fakeView.requestFocus();
        return fakeView;
    }

    private void tryAutoHideSoftInput() {
        if (sPendingDisableFragmentAutoHideSoftInput) {
            sPendingDisableFragmentAutoHideSoftInput = false;
            return;
        }
        hideSoftInput();
    }

    ///////////////////////////////////////////////////////////////////////////
    // INTERFACE
    ///////////////////////////////////////////////////////////////////////////

    public interface OnBackPressed {
        /**
         * If you return true the back press will not be taken into account, otherwise the activity will act naturally
         *
         * @return true if your processing has priority if not false
         */
        boolean onBackPressed();
    }

}
