package com.mct.components.baseui;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.mct.components.toast.ToastUtils;

public abstract class BaseFragment extends Fragment implements BaseView {

    private Toast mToast;
    private BaseFragmentManager mBaseFragmentManager;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (getActivity() instanceof BaseFragmentManager) {
            mBaseFragmentManager = (BaseFragmentManager) getActivity();
        }
    }

    @Override
    public void showLoading() {
    }

    @Override
    public void hideLoading() {
    }

    @Override
    public void onFalse(Throwable t) {
    }

    public boolean isContextNull() {
        return getContext() == null;
    }

    @Nullable
    protected BaseFragmentManager getBaseFragmentManger() {
        return mBaseFragmentManager;
    }

    protected void replaceFragment(Fragment fragment, boolean addToBackStack) {
        if (mBaseFragmentManager != null) {
            if (addToBackStack) {
                mBaseFragmentManager.replaceFragmentToStack(fragment);
            } else {
                mBaseFragmentManager.replaceFragment(fragment);
            }
        }
    }

    protected void replaceFragment(Fragment fragment, boolean addToBackStack, BaseActivity.Anim anim) {
        if (mBaseFragmentManager != null) {
            if (addToBackStack) {
                mBaseFragmentManager.replaceFragmentToStack(fragment, anim);
            } else {
                mBaseFragmentManager.replaceFragment(fragment, anim);
            }
        }
    }

    protected void replaceAndClearBackStack(Fragment fragment) {
        if (mBaseFragmentManager != null) {
            mBaseFragmentManager.replaceAndClearBackStack(fragment);
        }
    }

    protected void replaceAndClearBackStack(Fragment fragment, BaseActivity.Anim anim) {
        if (mBaseFragmentManager != null) {
            mBaseFragmentManager.replaceAndClearBackStack(fragment, anim);
        }
    }

    protected void clearBackStack() {
        if (mBaseFragmentManager != null) {
            mBaseFragmentManager.clearBackStack();
        }
    }

    protected void popLastFragment() {
        if (mBaseFragmentManager != null) {
            mBaseFragmentManager.popLastFragment();
        }
    }

    protected void popFragmentToPosition(int position) {
        if (mBaseFragmentManager != null) {
            mBaseFragmentManager.popFragmentToPosition(position);
        }
    }

    protected void popFragmentByAmount(int amount) {
        if (mBaseFragmentManager != null) {
            mBaseFragmentManager.popFragmentByAmount(amount);
        }
    }

    protected void popToFragment(@NonNull Class<? extends Fragment> cls) {
        if (mBaseFragmentManager != null) {
            mBaseFragmentManager.popToFragment(cls);
        }
    }

    protected void popToFragmentAndRemove(@NonNull Class<? extends Fragment> cls) {
        if (mBaseFragmentManager != null) {
            mBaseFragmentManager.popToFragmentAndRemove(cls);
        }
    }

    // block 1 time auto hide keyboard when fragment change
    protected void pendingDisableAutoHideSoftInput() {
        BaseActivity.sPendingDisableFragmentAutoHideSoftInput = true;
    }

    protected void showSoftInput(View view) {
        if (getActivity() instanceof BaseActivity) {
            ((BaseActivity) getActivity()).showSoftInput(view);
        }
    }

    protected void hideSoftInput() {
        if (getActivity() instanceof BaseActivity) {
            ((BaseActivity) getActivity()).hideSoftInput();
        }
    }

    protected void clearFocus() {
        if (getActivity() instanceof BaseActivity) {
            ((BaseActivity) getActivity()).clearFocus();
        }
    }

    /**
     * @return -1 if SoftInput is hidden or not match base activity<br>
     * softInputHeight if key board is visible
     */
    protected int getSoftInputHeight() {
        if (getActivity() instanceof BaseActivity) {
            return ((BaseActivity) getActivity()).getSoftInputHeight();
        }
        return -1;
    }

    protected void showToast(String msg, int type) {
        showToast(msg, type, true);
    }

    protected void showToast(int msg, int type) {
        showToast(getString(msg), type, true);
    }

    protected void showToast(int msg, int type, boolean showIcon) {
        showToast(getString(msg), type, showIcon);
    }

    protected void showToast(String msg, int type, boolean showIcon) {
        if (getContext() != null) {
            if (mToast != null) {
                mToast.cancel();
                mToast = null;
            }
            mToast = ToastUtils.makeText(getContext(), Toast.LENGTH_SHORT, type, msg, showIcon);
            mToast.show();
        }
    }

}
