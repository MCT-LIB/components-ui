package com.mct.components.baseui;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public interface BaseFragmentManager {

    int getBackStackCount();

    Fragment getCurrentFragment();

    <T extends Fragment> T findFragmentByTag(Class<T> cls);

    void replaceFragment(Fragment fragment);

    void replaceFragment(@NonNull Fragment fragment, BaseActivity.Anim anim);

    void replaceFragmentToStack(Fragment fragment);

    void replaceFragmentToStack(@NonNull Fragment fragment, BaseActivity.Anim anim);

    void replaceAndClearBackStack(Fragment fragment);

    void replaceAndClearBackStack(Fragment fragment, BaseActivity.Anim anim);

    void clearBackStack();

    /**
     * pop last fragment
     */
    void popLastFragment();

    /**
     * @param position is position of fragment in stack
     *                 position >= 0
     */
    void popFragmentToPosition(int position);

    /**
     * @param amount pop các fragment từ cuối theo số lượng
     *               amount > 0
     */
    void popFragmentByAmount(int amount);

    /**
     * @param cls pop to fragment if has
     */
    void popToFragment(@NonNull Class<? extends Fragment> cls);

    /**
     * @param cls pop to fragment and remove it
     */
    void popToFragmentAndRemove(@NonNull Class<? extends Fragment> cls);

}