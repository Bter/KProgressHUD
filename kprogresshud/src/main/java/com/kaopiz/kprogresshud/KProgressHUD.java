/*
 *    Copyright 2015 Kaopiz Software Co., Ltd.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.kaopiz.kprogresshud;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;

public class KProgressHUD {

    public enum Style {
        SPIN_INDETERMINATE, PIE_DETERMINATE, ANNULAR_DETERMINATE, BAR_DETERMINATE
    }

    // To avoid redundant APIs, make the HUD as a wrapper class around a Dialog
    private ProgressDialog mProgressDialog;
    private float mDimAmount;
    private int mWindowColor;
    private float mCornerRadius;
    private Context mContext;

    private int mAnimateSpeed;

    private int mMaxProgress;
    private boolean mIsAutoDismiss;

    private int mGraceTimeMs;
    private Handler mGraceTimer;
    private boolean mFinished;
    private static KProgressShowListener showListener;

    public KProgressHUD(Context context) {
        mContext = context;
        mProgressDialog = new ProgressDialog(context,this);
        mDimAmount = 0;
        //noinspection deprecation
        mWindowColor = context.getResources().getColor(R.color.kprogresshud_default_color);
        mAnimateSpeed = 1;
        mCornerRadius = 10;
        mIsAutoDismiss = true;
        mGraceTimeMs = 0;
        mFinished = false;

        setStyle(Style.SPIN_INDETERMINATE);
    }

    public static void setShowListener(KProgressShowListener showListener) {
        KProgressHUD.showListener = showListener;
    }

    /**
     * Create a new HUD. Have the same effect as the constructor.
     * For convenient only.
     * @param context Activity context that the HUD bound to
     * @return An unique HUD instance
     */
    public static KProgressHUD create(Context context) {
        return new KProgressHUD(context);
    }

  /**
   * Create a new HUD. specify the HUD style (if you use a custom view, you need {@code KProgressHUD.create(Context context)}).
   *
   * @param context Activity context that the HUD bound to
   * @param style One of the KProgressHUD.Style values
   * @return An unique HUD instance
   */
    public static KProgressHUD create(Context context, Style style) {
        return new KProgressHUD(context).setStyle(style);
    }

    /**
     * Specify the HUD style (not needed if you use a custom view)
     * @param style One of the KProgressHUD.Style values
     * @return Current HUD
     */
    public KProgressHUD setStyle(Style style) {
        View view = null;
        switch (style) {
            case SPIN_INDETERMINATE:
                view = new SpinView(mContext);
                break;
            case PIE_DETERMINATE:
                view = new PieView(mContext);
                break;
            case ANNULAR_DETERMINATE:
                view = new AnnularView(mContext);
                break;
            case BAR_DETERMINATE:
                view = new BarView(mContext);
                break;
            // No custom view style here, because view will be added later
        }
        mProgressDialog.setView(view);
        return this;
    }

    /**
     * Specify the dim area around the HUD, like in Dialog
     * @param dimAmount May take value from 0 to 1. Default to 0 (no dimming)
     * @return Current HUD
     */
    public KProgressHUD setDimAmount(float dimAmount) {
        if (dimAmount >= 0 && dimAmount <= 1) {
            mDimAmount = dimAmount;
        }
        return this;
    }

    /**
     * Set HUD size. If not the HUD view will use WRAP_CONTENT instead
     * @param width in dp
     * @param height in dp
     * @return Current HUD
     */
    public KProgressHUD setSize(int width, int height) {
        mProgressDialog.setSize(width, height);
        return this;
    }

    /**
     * @deprecated  As of release 1.1.0, replaced by {@link #setBackgroundColor(int)}
     * @param color ARGB color
     * @return Current HUD
     */
    @Deprecated
    public KProgressHUD setWindowColor(int color) {
        mWindowColor = color;
        return this;
    }

    /**
     * Specify the HUD background color
     * @param color ARGB color
     * @return Current HUD
     */
    public KProgressHUD setBackgroundColor(int color) {
        mWindowColor = color;
        return this;
    }

    /**
     * Specify corner radius of the HUD (default is 10)
     * @param radius Corner radius in dp
     * @return Current HUD
     */
    public KProgressHUD setCornerRadius(float radius) {
        mCornerRadius = radius;
        return this;
    }

    /**
     * Change animation speed relative to default. Used with indeterminate style
     * @param scale Default is 1. If you want double the speed, set the param at 2.
     * @return Current HUD
     */
    public KProgressHUD setAnimationSpeed(int scale) {
        mAnimateSpeed = scale;
        return this;
    }

    /**
     * Optional label to be displayed.
     * @return Current HUD
     */
    public KProgressHUD setLabel(String label) {
        mProgressDialog.setLabel(label);
        return this;
    }

    /**
     * Optional label to be displayed
     * @return Current HUD
     */
    public KProgressHUD setLabel(String label, int color) {
        mProgressDialog.setLabel(label, color);
        return this;
    }

    /**
     * Optional detail description to be displayed on the HUD
     * @return Current HUD
     */
    public KProgressHUD setDetailsLabel(String detailsLabel) {
        mProgressDialog.setDetailsLabel(detailsLabel);
        return this;
    }

    /**
     * Optional detail description to be displayed
     * @return Current HUD
     */
    public KProgressHUD setDetailsLabel(String detailsLabel, int color) {
        mProgressDialog.setDetailsLabel(detailsLabel, color);
        return this;
    }

    /**
     * Max value for use in one of the determinate styles
     * @return Current HUD
     */
    public KProgressHUD setMaxProgress(int maxProgress) {
        mMaxProgress = maxProgress;
        return this;
    }

    public KProgressHUD setUseDimBehind(boolean useDimBehind) {
        mProgressDialog.setUseDimBehind(useDimBehind);
        return this;
    }

    /**
     * Set current progress. Only have effect when use with a determinate style, or a custom
     * view which implements Determinate interface.
     */
    public void setProgress(int progress) {
        mProgressDialog.setProgress(progress);
    }

    /**
     * Provide a custom view to be displayed.
     * @param view Must not be null
     * @return Current HUD
     */
    public KProgressHUD setCustomView(View view) {
        if (view != null) {
            mProgressDialog.setView(view);
        } else {
            throw new RuntimeException("Custom view must not be null!");
        }
        return this;
    }

    /**
     * Specify whether this HUD can be cancelled by using back button (default is false)
     *
     * Setting a cancelable to true with this method will set a null callback,
     * clearing any callback previously set with
     * {@link #setCancellable(DialogInterface.OnCancelListener)}
     *
     * @return Current HUD
     */
    public KProgressHUD setCancellable(boolean isCancellable) {
        mProgressDialog.setCancelable(isCancellable);
        mProgressDialog.setOnCancelListener(null);
        return this;
    }

    /**
     * Specify a callback to run when using the back button (default is null)
     *
     * @param listener The code that will run if the user presses the back
     * button. If you pass null, the dialog won't be cancellable, just like
     * if you had called {@link #setCancellable(boolean)} passing false.
     *
     * @return Current HUD
     */
    public KProgressHUD setCancellable(DialogInterface.OnCancelListener listener) {
        mProgressDialog.setCancelable(null != listener);
        mProgressDialog.setOnCancelListener(listener);
        return this;
    }

    /**
     * Specify whether this HUD closes itself if progress reaches max. Default is true.
     * @return Current HUD
     */
    public KProgressHUD setAutoDismiss(boolean isAutoDismiss) {
        mIsAutoDismiss = isAutoDismiss;
        return this;
    }

    /**
     * Grace period is the time (in milliseconds) that the invoked method may be run without
     * showing the HUD. If the task finishes before the grace time runs out, the HUD will
     * not be shown at all.
     * This may be used to prevent HUD display for very short tasks.
     * Defaults to 0 (no grace time).
     * @param graceTimeMs Grace time in milliseconds
     * @return Current HUD
     */
    public KProgressHUD setGraceTime(int graceTimeMs) {
        mGraceTimeMs = graceTimeMs;
        return this;
    }

    public KProgressHUD show() {
        if (!isShowing()) {
            mFinished = false;
            if (mGraceTimeMs == 0) {
                if(KProgressHUD.showListener != null){
                    KProgressHUD.showListener.onWantShow(this);
                }
                mProgressDialog.show();
            } else {
                mGraceTimer = new Handler();
                mGraceTimer.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mProgressDialog != null && !mFinished) {
                            if(KProgressHUD.showListener != null){
                                KProgressHUD.showListener.onWantShow(KProgressHUD.this);
                            }
                            mProgressDialog.show();
                        }
                    }
                }, mGraceTimeMs);
            }
        }
        return this;
    }

    public boolean isShowing() {
        return mProgressDialog != null && mProgressDialog.isShowing();
    }

    public void dismiss() {
        mFinished = true;
        if (mContext != null && mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        if (mGraceTimer != null) {
            mGraceTimer.removeCallbacksAndMessages(null);
            mGraceTimer = null;
        }
    }

    public Dialog getDialog(){
        return mProgressDialog;
    }

    public synchronized static int getShowingCount(){
        return ProgressDialog.getShowingCount();
    }

    private static class ProgressDialog extends Dialog {
        private static final HashSet<WeakReference<DialogInterface>> showingDialog = new HashSet<>();

        private Determinate mDeterminateView;
        private Indeterminate mIndeterminateView;
        private View mView;
		private TextView mLabelText;
        private TextView mDetailsText;
        private String mLabel;
        private String mDetailsLabel;
        private FrameLayout mCustomViewContainer;
        private BackgroundLayout mBackgroundLayout;
        private int mWidth, mHeight;
        private int mLabelColor = Color.WHITE;
        private int mDetailColor = Color.WHITE;
        private boolean useDimBehind = true;
        private KProgressHUD kProgressHUD;
        private OnDismissListener dismissListener;
        private OnShowListener showListener;
        private OnDismissListener innerDismissListener = new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                showingDialog.remove(new MyWeakReference<DialogInterface>(dialog));
                if(dismissListener != null){
                    dismissListener.onDismiss(dialog);
                }
            }
        };
		
        public ProgressDialog(Context context,KProgressHUD kProgressHUD) {
            super(context);
            this.kProgressHUD = kProgressHUD;
            super.setOnDismissListener(innerDismissListener);
            super.setOnShowListener(new OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    showingDialog.add(new MyWeakReference<DialogInterface>(dialog));
                    if(showListener != null){
                        showListener.onShow(dialog);
                    }
                }
            });
        }

        public synchronized static int getShowingCount(){
            Iterator<WeakReference<DialogInterface>> it = showingDialog.iterator();
            int count = 0;
            while (it.hasNext()){
                WeakReference<DialogInterface> weakReference = it.next();
                if(weakReference == null)continue;
                if(weakReference.get() == null){
                    it.remove();
                    continue;
                }
                count += 1;
            }
            return count;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(R.layout.kprogresshud_hud);

            Window window = getWindow();
            window.setBackgroundDrawable(new ColorDrawable(0));
            if(useDimBehind) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            }else{
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            }
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.dimAmount = kProgressHUD.mDimAmount;
            layoutParams.gravity = Gravity.CENTER;
            window.setAttributes(layoutParams);

            setCanceledOnTouchOutside(false);

            initViews();
        }

        @Override
        public void setOnDismissListener(OnDismissListener listener) {
            this.dismissListener = listener;
        }

        @Override
        public void setOnShowListener(OnShowListener listener) {
            this.showListener = listener;
        }

        @Override
        public void show() {
            super.show();
            showingDialog.add(new MyWeakReference<DialogInterface>(this));
        }

        @Override
        public void dismiss() {
            super.dismiss();
            showingDialog.remove(new MyWeakReference<DialogInterface>(this));
        }

        private void initViews() {
            mBackgroundLayout = (BackgroundLayout) findViewById(R.id.background);
            mBackgroundLayout.setBaseColor(kProgressHUD.mWindowColor);
            mBackgroundLayout.setCornerRadius(kProgressHUD.mCornerRadius);
            if (mWidth != 0) {
                updateBackgroundSize();
            }

            mCustomViewContainer = (FrameLayout) findViewById(R.id.container);
            addViewToFrame(mView);

            if (mDeterminateView != null) {
                mDeterminateView.setMax(kProgressHUD.mMaxProgress);
            }
            if (mIndeterminateView != null) {
                mIndeterminateView.setAnimationSpeed(kProgressHUD.mAnimateSpeed);
            }

            mLabelText = (TextView) findViewById(com.kaopiz.kprogresshud.R.id.label);
            setLabel(mLabel, mLabelColor);
            mDetailsText = (TextView) findViewById(com.kaopiz.kprogresshud.R.id.details_label);
            setDetailsLabel(mDetailsLabel, mDetailColor);
        }

        private void addViewToFrame(View view) {
            if (view == null) return;
            int wrapParam = ViewGroup.LayoutParams.WRAP_CONTENT;
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(wrapParam, wrapParam);
            mCustomViewContainer.addView(view, params);
        }

        private void updateBackgroundSize() {
            ViewGroup.LayoutParams params = mBackgroundLayout.getLayoutParams();
            params.width = Helper.dpToPixel(mWidth, getContext());
            params.height = Helper.dpToPixel(mHeight, getContext());
            mBackgroundLayout.setLayoutParams(params);
        }

        public void setProgress(int progress) {
            if (mDeterminateView != null) {
                mDeterminateView.setProgress(progress);
                if (kProgressHUD.mIsAutoDismiss && progress >= kProgressHUD.mMaxProgress) {
                    dismiss();
                }
            }
        }

        public void setView(View view) {
            if (view != null) {
                if (view instanceof Determinate) {
                    mDeterminateView = (Determinate) view;
                }
                if (view instanceof Indeterminate) {
                    mIndeterminateView = (Indeterminate) view;
                }
                mView = view;
                if (isShowing()) {
                    mCustomViewContainer.removeAllViews();
                    addViewToFrame(view);
                }
            }
        }

        public void setLabel(String label) {
            mLabel = label;
            if (mLabelText != null) {
                if (label != null) {
                    mLabelText.setText(label);
                    mLabelText.setVisibility(View.VISIBLE);
                } else {
                    mLabelText.setVisibility(View.GONE);
                }
            }
        }

        public void setDetailsLabel(String detailsLabel) {
            mDetailsLabel = detailsLabel;
            if (mDetailsText != null) {
                if (detailsLabel != null) {
                    mDetailsText.setText(detailsLabel);
                    mDetailsText.setVisibility(View.VISIBLE);
                } else {
                    mDetailsText.setVisibility(View.GONE);
                }
            }
        }

        public void setLabel(String label, int color) {
            mLabel = label;
            mLabelColor = color;
            if (mLabelText != null) {
                if (label != null) {
                    mLabelText.setText(label);
                    mLabelText.setTextColor(color);
                    mLabelText.setVisibility(View.VISIBLE);
                } else {
                    mLabelText.setVisibility(View.GONE);
                }
            }
        }

        public void setDetailsLabel(String detailsLabel, int color) {
            mDetailsLabel = detailsLabel;
            mDetailColor = color;
            if (mDetailsText != null) {
                if (detailsLabel != null) {
                    mDetailsText.setText(detailsLabel);
                    mDetailsText.setTextColor(color);
                    mDetailsText.setVisibility(View.VISIBLE);
                } else {
                    mDetailsText.setVisibility(View.GONE);
                }
            }
        }

        public void setSize(int width, int height) {
            mWidth = width;
            mHeight = height;
            if (mBackgroundLayout != null) {
                updateBackgroundSize();
            }
        }

        public void setUseDimBehind(boolean useDimBehind) {
            this.useDimBehind = useDimBehind;
        }
    }

    private static class MyWeakReference<T> extends WeakReference<T>{
        private int hashCode;

        public MyWeakReference(T referent) {
            super(referent);
            if(referent != null){
                hashCode = referent.hashCode();
            }else{
                hashCode = super.hashCode();
            }
        }

        public MyWeakReference(T referent, ReferenceQueue<? super T> q) {
            super(referent, q);
            if(referent != null){
                hashCode = referent.hashCode();
            }else{
                hashCode = super.hashCode();
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if(o instanceof MyWeakReference) {
                MyWeakReference<?> that = (MyWeakReference<?>) o;

                Object object = get();
                o = that.get();
                if(object == o)return true;
                if(null == object && o != null){
                    return false;
                }else if(null != object && o == null){
                    return false;
                }else{
                    //都不为空
                    return o.equals(object);
                }
//            return hashCode == that.hashCode;
            }else{
                Object object = get();
                if(object == o)return true;
                if(null == object && o != null){
                    return false;
                }else if(null != object && o == null){
                    return false;
                }else{
                    //都不为空
                    return o.equals(object);
                }
            }
        }

        @Override
        public int hashCode() {
            Object obj;
            if((obj = get()) != null){
                return obj.hashCode();
            }
            return hashCode;
        }
    }
}
