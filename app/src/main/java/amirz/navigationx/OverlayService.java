package amirz.navigationx;

import android.accessibilityservice.AccessibilityService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class OverlayService extends AccessibilityService implements View.OnTouchListener {
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mParams;
    private LinearLayout mView;
    private LinearLayout mInnerView;

    @Override
    public void onCreate() {
        super.onCreate();
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        mParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT < Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY :
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        mParams.gravity = Gravity.BOTTOM;

        try {
            int currentFlags = (Integer) mParams.getClass().getField("privateFlags").get(mParams);
            mParams.getClass().getField("privateFlags").set(mParams, currentFlags|0x00000040);
        } catch (Exception ignored) {
        }

        mView = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.nav_bar, null);
        mView.setOnTouchListener(this);
        mInnerView = mView.findViewById(R.id.nav_line);
        mWindowManager.addView(mView, mParams);

        setTransitionState(0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mView != null) {
            mWindowManager.removeView(mView);
            mView = null;
        }
    }

    private float translateStart;

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                translateStart = motionEvent.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                setTransitionState((int)(translateStart - motionEvent.getRawY()));
                break;
            case MotionEvent.ACTION_UP:
                int moved = (int)(translateStart - motionEvent.getRawY());
                mInnerView.startAnimation(new MoveBarAnimation(moved));
                if (moved >= 64) {
                    Intent i = new Intent(Intent.ACTION_MAIN);
                    i.addCategory(Intent.CATEGORY_HOME);
                    startActivity(i);
                } else if (moved <= 2) {
                    performGlobalAction(GLOBAL_ACTION_BACK);
                    setTransitionState(0);
                }
                break;
        }
        return false;
    }

    private void setTransitionState(int pxUp) {
        if (pxUp < 0) {
            pxUp = 0;
        }

        mInnerView.setTranslationY(-pxUp);
        mInnerView.setAlpha(1 - Math.min(1f, (float)pxUp / (2*0xDD)));
        mView.setBackgroundColor(0x01000000 * Math.min(pxUp, 0xDD));

        mParams.height = mParams.width = pxUp == 0 ?
                ViewGroup.LayoutParams.WRAP_CONTENT :
                ViewGroup.LayoutParams.MATCH_PARENT;

        mWindowManager.updateViewLayout(mView, mParams);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {

    }

    @Override
    public void onInterrupt() {

    }

    class MoveBarAnimation extends Animation {
        private int mStart;

        public MoveBarAnimation(int fromX) {
            mStart = fromX;
            setDuration(200L);
            setInterpolator(new DecelerateInterpolator());
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);
            setTransitionState((int)((1f - interpolatedTime) * mStart));
        }

    }
}
