package com.mct.components.utils;

import static androidx.dynamicanimation.animation.DynamicAnimation.OnAnimationEndListener;
import static androidx.dynamicanimation.animation.DynamicAnimation.X;
import static androidx.dynamicanimation.animation.DynamicAnimation.Y;

import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import java.util.ArrayList;
import java.util.List;

public class TouchUtils {

    private static final String TAG = "TouchMoveUtils";

    public static final int TOP_LEFT = 0;
    public static final int TOP_RIGHT = 1;
    public static final int BOT_LEFT = 2;
    public static final int BOT_RIGHT = 3;

    @IntDef({TOP_LEFT, TOP_RIGHT, BOT_LEFT, BOT_RIGHT})
    public @interface Corner {
    }

    public static final int TYPE_GROW = 0;
    public static final int TYPE_SHRINK = 1;

    @IntDef({TYPE_GROW, TYPE_SHRINK})
    public @interface ScaleType {
    }

    private TouchUtils() {
        throw new UnsupportedOperationException("u can't instantiate this...");
    }

    public static <T extends BaseTouchListener> void setTouchListener(View v, T listener) {
        if (v == null) {
            return;
        }
        v.setOnTouchListener(listener);
    }

    ///////////////////////////////////////////////////////////////////////////
    // LISTENERS
    ///////////////////////////////////////////////////////////////////////////

    private static abstract class BaseTouchListener implements View.OnTouchListener {

        protected static final int STATE_DOWN = 0;
        protected static final int STATE_MOVE = 1;

        private int touchSlop;
        private int state;
        private int lastX, lastY;

        protected final int getState() {
            return state;
        }

        @Override
        public final boolean onTouch(View view, MotionEvent event) {
            onActionTouch(view, event);
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    return onDown(view, event);
                case MotionEvent.ACTION_MOVE:
                    return onMove(view, event);
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    return onStop(view, event);
                default:
                    return false;
            }
        }

        private boolean onDown(View view, MotionEvent event) {
            Log.d(TAG, "onActionDown ----- state = STATE_DOWN");
            if (touchSlop == 0) {
                touchSlop = ViewConfiguration.get(view.getContext()).getScaledTouchSlop();
            }
            int x = (int) event.getRawX();
            int y = (int) event.getRawY();
            resetTouch(x, y);
            view.setPressed(true);
            return onActionDown(view, event);
        }

        private boolean onMove(View view, @NonNull MotionEvent event) {
            int x = (int) event.getRawX();
            int y = (int) event.getRawY();
            if (lastX == -1) {
                // not receive down should reset
                resetTouch(x, y);
                view.setPressed(true);
            }
            if (state != STATE_MOVE) {
                if (Math.abs(x - lastX) >= touchSlop || Math.abs(y - lastY) >= touchSlop) {
                    Log.d(TAG, "onActionMove ----- state = STATE_MOVE");
                    state = STATE_MOVE;
                }
            }
            return onActionMove(view, event);
        }

        private boolean onStop(View view, MotionEvent event) {
            boolean b = onActionStop(view, event);
            resetTouch(-1, -1);
            view.setPressed(false);
            return b;
        }

        protected void onActionTouch(@NonNull View view, @NonNull MotionEvent event) {
        }

        protected boolean onActionDown(@NonNull View view, @NonNull MotionEvent event) {
            return false;
        }

        protected boolean onActionMove(@NonNull View view, @NonNull MotionEvent event) {
            return false;
        }

        protected boolean onActionStop(@NonNull View view, @NonNull MotionEvent event) {
            return false;
        }

        protected boolean isTouching() {
            return state == STATE_MOVE;
        }

        protected void resetTouch(int x, int y) {
            lastX = x;
            lastY = y;
            state = STATE_DOWN;
        }

    }

    public static abstract class TouchMoveCornerListener extends BaseTouchListener {

        private static final int MIN_TAP_TIME = 1000;
        private static final int MIN_FLING_TIME = 250;
        private static final int THRESHOLD_VELOCITY_X = 400;
        private static final int THRESHOLD_VELOCITY_Y = 430;
        private static final int FLING_DISTANCE_PASS_PERCENT = 1;

        private static final int UNKNOWN = -1;
        private static final int LEFT = 1;
        private static final int UP = 2;
        private static final int RIGHT = 3;
        private static final int DOWN = 4;

        private boolean isInit;
        private Rect area, moveArea;
        private SpringAnimation springX, springY;
        private VelocityTracker velocityTracker;
        private int minimumFlingVelocity;
        private int maximumFlingVelocity;

        private float dX, dY;
        private Point beginPosition;

        @NonNull
        public abstract Rect initArea(View view);

        @Override
        protected final void onActionTouch(@NonNull View view, @NonNull MotionEvent event) {
            super.onActionTouch(view, event);
            if (velocityTracker == null) {
                velocityTracker = VelocityTracker.obtain();
            }
            velocityTracker.addMovement(event);
        }

        @Override
        protected final boolean onActionDown(@NonNull View view, @NonNull MotionEvent event) {
            super.onActionDown(view, event);
            if (!isInit) {
                init(view);
            }
            dX = view.getX() - event.getRawX();
            dY = view.getY() - event.getRawY();
            beginPosition = getCenter(view);
            resetForce(false);
            if (springX.isRunning()) {
                springX.cancel();
            }
            if (springY.isRunning()) {
                springY.cancel();
            }
            return onDown(view, event);
        }

        @Override
        protected final boolean onActionMove(@NonNull View view, @NonNull MotionEvent event) {
            super.onActionMove(view, event);
            float x = event.getRawX() + dX;
            float y = event.getRawY() + dY;
            if (!isCanMoveOutArea()) {
                x = coerceIn(x, moveArea.left, moveArea.right);
                y = coerceIn(y, moveArea.top, moveArea.bottom);
            }
            springX.animateToFinalPosition(x);
            springY.animateToFinalPosition(y);

            if (getState() == STATE_MOVE) {
                return onMove(view, event);
            }
            return true;
        }

        @Override
        protected final boolean onActionStop(@NonNull View view, @NonNull MotionEvent event) {
            super.onActionStop(view, event);
            long eventTime = event.getEventTime() - event.getDownTime();
            boolean isHandleClick = false;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (getState() == STATE_DOWN) {
                    boolean canClick = true;
                    if (isOnlyCornerCanClick()) {
                        canClick = isNearCornerPoint(new Point((int) view.getX(), (int) view.getY()), moveArea);
                    }
                    if (canClick) {
                        if (eventTime <= getMinTapTime()) {
                            Log.d(TAG, "onActionStop: PERFORM CLICK");
                            view.performClick();
                        } else {
                            Log.d(TAG, "onActionStop: PERFORM LONG CLICK");
                            view.performLongClick();
                        }
                    } else {
                        Log.d(TAG, "onActionStop: NOT NEAR CORNER -> BLOCK CLICK");
                    }
                    isHandleClick = true;
                }
            }
            if (!isHandleClick && eventTime < getMinFlingTime()) {
                Log.d(TAG, "onActionStop: HANDLE SWIPE");
                handleFling(view);
            } else {
                Log.d(TAG, "onActionStop: MOVE TO CORNER");
                moveToCorner(view);
            }
            velocityTracker.recycle();
            velocityTracker = null;
            return onStop(view, event);
        }

        protected final void setArea(@NonNull View v, @NonNull Rect rect) {
            if (rect.equals(area)) {
                return;
            }
            int right = rect.right - v.getWidth();
            int bottom = rect.bottom - v.getHeight();
            area = rect;
            moveArea = new Rect(area.left, area.top, right, bottom);
        }

        /* ----------------- CAN OVERRIDE TO RECEIVE OR MODIFY ---------------------------------- */

        /**
         * This func can control min and max position
         * when anim move to corner
         */
        @NonNull
        protected Rect initAnimArea(@NonNull View view) {
            int width = 2 * view.getWidth();
            int height = 2 * view.getHeight();
            return new Rect(
                    area.left - width,
                    area.top - height,
                    area.right + width,
                    area.bottom + height
            );
        }

        protected boolean onDown(View view, MotionEvent event) {
            return true;
        }

        protected boolean onMove(View view, MotionEvent event) {
            return true;
        }

        protected boolean onStop(View view, MotionEvent event) {
            return true;
        }

        protected void onMovedToCorner(@NonNull View view, @Corner int corner, Point cornerPoint) {
        }

        protected boolean isOnlyCornerCanClick() {
            return true;
        }

        protected boolean isCanMoveOutArea() {
            return true;
        }

        protected float getStiffnessX() {
            return SpringForce.STIFFNESS_VERY_LOW;
        }

        protected float getStiffnessY() {
            return SpringForce.STIFFNESS_VERY_LOW;
        }

        protected float getDampingRatioX() {
            return SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY;
        }

        protected float getDampingRatioY() {
            return SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY;
        }

        protected int getMinTapTime() {
            return MIN_TAP_TIME;
        }

        protected int getMinFlingTime() {
            return MIN_FLING_TIME;
        }

        protected int getThresholdVelocityX() {
            return THRESHOLD_VELOCITY_X;
        }

        protected int getThresholdVelocityY() {
            return THRESHOLD_VELOCITY_Y;
        }

        protected int getFlingDistancePassPercent() {
            return FLING_DISTANCE_PASS_PERCENT;
        }

        /* -------------------------------- PRIVATE AREA ---------------------------------------- */

        private void init(@NonNull View v) {
            isInit = true;
            setArea(v, initArea(v));
            minimumFlingVelocity = ViewConfiguration.get(v.getContext()).getScaledMinimumFlingVelocity();
            maximumFlingVelocity = ViewConfiguration.get(v.getContext()).getScaledMaximumFlingVelocity();

            Rect animArea = initAnimArea(v);
            springX = new SpringAnimation(v, X, 0);
            springY = new SpringAnimation(v, Y, 0);
            springX.setMinValue(animArea.left);
            springX.setMaxValue(animArea.right);
            springY.setMinValue(animArea.top);
            springY.setMaxValue(animArea.bottom);

            resetForce(false);
        }

        private void resetForce(boolean isMoveToCorner) {
            if (isMoveToCorner) {
                springX.getSpring().setDampingRatio(getDampingRatioX()).setStiffness(getStiffnessX());
                springY.getSpring().setDampingRatio(getDampingRatioY()).setStiffness(getStiffnessY());
            } else {
                float dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY;
                float stiffness = SpringForce.STIFFNESS_HIGH;
                springX.getSpring().setDampingRatio(dampingRatio).setStiffness(stiffness);
                springY.getSpring().setDampingRatio(dampingRatio).setStiffness(stiffness);
            }
        }

        private void handleFling(@NonNull View view) {
            int vx = 0, vy = 0;

            if (velocityTracker != null) {
                velocityTracker.computeCurrentVelocity(1000, maximumFlingVelocity);
                vx = (int) velocityTracker.getXVelocity();
                vy = (int) velocityTracker.getYVelocity();
                if (Math.abs(vx) < minimumFlingVelocity) {
                    vx = 0;
                }
                if (Math.abs(vy) < minimumFlingVelocity) {
                    vy = 0;
                }
            }
            Point currPosition = getCenter(view);
            final int distanceX = currPosition.x - beginPosition.x;
            final int distanceY = currPosition.y - beginPosition.y;

            boolean vxPass = Math.abs(vx) > minimumFlingVelocity + getThresholdVelocityX();
            boolean vyPass = Math.abs(vy) > minimumFlingVelocity + getThresholdVelocityY();
            boolean dxPass = 100f * Math.abs(distanceX) / area.width() > getFlingDistancePassPercent();
            boolean dyPass = 100f * Math.abs(distanceY) / area.height() > getFlingDistancePassPercent();

            int directionVer; // LEFT | RIGHT | UNKNOWN
            int directionHoz; // UP   | DOWN  | UNKNOWN
            directionVer = vxPass && dxPass ? distanceX > 0 ? RIGHT : distanceX < 0 ? LEFT : UNKNOWN : UNKNOWN;
            directionHoz = vyPass && dyPass ? distanceY > 0 ? DOWN : distanceY < 0 ? UP : UNKNOWN : UNKNOWN;

            Log.d(TAG, "handleFling: " +
                    vx + " | " + vy + " | " +
                    directionVer + " | " + directionHoz);

            Integer corner;
            switch (directionVer) {
                case LEFT:
                    if (directionHoz == UP) {
                        corner = TOP_LEFT;
                        break;
                    }
                    if (directionHoz == DOWN) {
                        corner = BOT_LEFT;
                        break;
                    }
                    corner = beginPosition.y < area.height() / 2 ? TOP_LEFT : BOT_LEFT;
                    break;
                case RIGHT:
                    if (directionHoz == UP) {
                        corner = TOP_RIGHT;
                        break;
                    }
                    if (directionHoz == DOWN) {
                        corner = BOT_RIGHT;
                        break;
                    }
                    corner = beginPosition.y < area.height() / 2 ? TOP_RIGHT : BOT_RIGHT;
                    break;
                case UNKNOWN:
                    if (directionHoz == UP) {
                        corner = beginPosition.x < area.width() / 2 ? TOP_LEFT : TOP_RIGHT;
                        break;
                    }
                    if (directionHoz == DOWN) {
                        corner = beginPosition.x < area.width() / 2 ? BOT_LEFT : BOT_RIGHT;
                        break;
                    }
                default:
                    corner = null;
                    break;
            }
            if (corner != null) {
                moveToCorner(view, corner);
            } else {
                moveToCorner(view);
            }
        }

        @Corner
        protected final int getCorner(View view) {
            return calcCorner(getCenter(view), area);
        }

        protected final void moveToCorner(View view) {
            moveToCorner(view, getCorner(view));
        }

        protected final void moveToCorner(@NonNull View view, @Corner int corner) {
            Point cornerPoint = TouchUtils.getCorner(moveArea, corner);
            OnAnimationEndListener endListener = new OnAnimationEndListener() {
                @Override
                public void onAnimationEnd(DynamicAnimation animation, boolean canceled, float value, float velocity) {
                    if (!canceled && !springX.isRunning() && !springY.isRunning()) {
                        onMovedToCorner(view, corner, cornerPoint);
                    }
                    if (!springX.isRunning()) springX.removeEndListener(this);
                    if (!springY.isRunning()) springY.removeEndListener(this);
                }
            };
            resetForce(true);
            springX.addEndListener(endListener);
            springY.addEndListener(endListener);
            springX.animateToFinalPosition(cornerPoint.x);
            springY.animateToFinalPosition(cornerPoint.y);
        }

        protected final void clearAnimation() {
            if (isInit) {
                springX.cancel();
                springY.cancel();
            }
        }
    }

    public static class TouchScaleListener extends BaseTouchListener {

        private static final int PIVOT_TYPE = ScaleAnimation.RELATIVE_TO_SELF;
        private static final float PIVOT_VAL = 0.5f;
        private static final float DEFAULT_REAL_SCALE = 1f;
        private static final float DEFAULT_DELTA_SCALE = 0.25f;
        private static final int DEFAULT_DURATION = 250;
        private static final int MIN_TAP_TIME = 1000;

        @Override
        protected final void onActionTouch(@NonNull View view, @NonNull MotionEvent event) {
            super.onActionTouch(view, event);
        }

        @Override
        protected final boolean onActionDown(@NonNull View view, @NonNull MotionEvent event) {
            super.onActionDown(view, event);
            float from = getRealScale();
            float to = from + (getScaleType() == TYPE_GROW ? getDeltaScale() : -getDeltaScale());
            ScaleAnimation scaleAnimation = new ScaleAnimation(from, to, from, to, PIVOT_TYPE, PIVOT_VAL, PIVOT_TYPE, PIVOT_VAL);
            scaleAnimation.setDuration(getDuration());
            scaleAnimation.setFillAfter(true);
            view.startAnimation(scaleAnimation);
            return true;
        }

        @Override
        protected final boolean onActionStop(@NonNull View view, @NonNull MotionEvent event) {
            super.onActionStop(view, event);
            boolean isHasClick = false;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (getState() == STATE_DOWN) {
                    isHasClick = true;
                    long eventTime = event.getEventTime() - event.getDownTime();
                    if (eventTime <= getMinTapTime()) {
                        Log.d(TAG, "onActionStop: PERFORM CLICK");
                        view.performClick();
                    } else {
                        Log.d(TAG, "onActionStop: PERFORM LONG CLICK");
                        view.performLongClick();
                    }
                }
            }
            float delta = (getDeltaScale() + (isHasClick ? 0.2f : 0)) * (getDeltaScale() == TYPE_GROW ? 1 : -1);
            float to = getRealScale();
            float from = to + delta;
            if (from < 0) {
                from = 0;
            }
            ScaleAnimation scaleAnimation = new ScaleAnimation(from, to, from, to, PIVOT_TYPE, PIVOT_VAL, PIVOT_TYPE, PIVOT_VAL);
            scaleAnimation.setDuration(getDuration());
            if (isHasClick) {
                scaleAnimation.setInterpolator(new OvershootInterpolator());
            }
            view.startAnimation(scaleAnimation);
            return true;
        }

        @ScaleType
        protected int getScaleType() {
            return TYPE_SHRINK;
        }

        protected float getRealScale() {
            return DEFAULT_REAL_SCALE;
        }

        /**
         * Real scale (+ | -) delta scale => new scale
         */
        protected float getDeltaScale() {
            return DEFAULT_DELTA_SCALE;
        }

        protected int getDuration() {
            return DEFAULT_DURATION;
        }

        protected int getMinTapTime() {
            return MIN_TAP_TIME;
        }

    }

    ///////////////////////////////////////////////////////////////////////////
    // Below are private UTILS FUNC
    ///////////////////////////////////////////////////////////////////////////

    private static float coerceIn(float value, float min, float max) {
        return Math.min(Math.max(value, min), max);
    }

    @NonNull
    private static Point getCenter(@NonNull View view) {
        return new Point(
                (int) (view.getX() + (view.getWidth() / 2)),
                (int) (view.getY() + (view.getHeight() / 2)));
    }

    @NonNull
    private static Point getCorner(Rect area, @Corner int corner) {
        switch (corner) {
            case TOP_LEFT:
                return new Point(area.left, area.top);
            case TOP_RIGHT:
                return new Point(area.right, area.top);
            case BOT_LEFT:
                return new Point(area.left, area.bottom);
            case BOT_RIGHT:
                return new Point(area.right, area.bottom);
        }
        return new Point();
    }

    private static boolean isNearCornerPoint(@NonNull Point p, @NonNull Rect area) {
        List<Pair<Integer, Double>> cornerDistances = getCornerDistances(p, area);
        for (Pair<Integer, Double> cornerDistance : cornerDistances) {
            if (cornerDistance.second <= 5) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return </br>first: corner from 0 -> 4:
     * </br>second: minDistance
     * </br>See Also: ${{@link Corner}}
     */
    @Corner
    private static int calcCorner(Point p, @NonNull Rect area) {
        List<Pair<Integer, Double>> cornerDistances = getCornerDistances(p, area);
        Pair<Integer, Double> minDistance = cornerDistances.get(0);
        for (int i = 0; i < cornerDistances.size(); i++) {
            Pair<Integer, Double> cornerDistance = cornerDistances.get(i);
            if (minDistance.second > cornerDistance.second) {
                minDistance = cornerDistance;
            }
        }
        return minDistance.first;
    }

    @NonNull
    private static List<Pair<Integer, Double>> getCornerDistances(@NonNull Point p, @NonNull Rect area) {
        Point tl = new Point(area.left, area.top);
        Point tr = new Point(area.right, area.top);
        Point bl = new Point(area.left, area.bottom);
        Point br = new Point(area.right, area.bottom);

        List<Pair<Integer, Double>> cornerDistances = new ArrayList<>();
        cornerDistances.add(new Pair<>(TOP_LEFT, distance(p, tl)));
        cornerDistances.add(new Pair<>(TOP_RIGHT, distance(p, tr)));
        cornerDistances.add(new Pair<>(BOT_LEFT, distance(p, bl)));
        cornerDistances.add(new Pair<>(BOT_RIGHT, distance(p, br)));
        return cornerDistances;
    }

    private static double distance(@NonNull Point a, @NonNull Point b) {
        int dx = a.x - b.x;
        int dy = a.y - b.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

}
