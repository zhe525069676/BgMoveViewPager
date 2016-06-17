package com.zheblog.bgmoveviewpager.widget;

import android.content.Context;
import android.view.animation.Interpolator;
import android.widget.Scroller;

/**
 * Created by liuz on 16/6/17.
 * 控制viewpager滑动速度
 */
public class FixedSpeedScroller extends Scroller {
    private int mDuration = 800; // 默认为800ms

    public FixedSpeedScroller(Context context) {
        super(context);
    }

    public FixedSpeedScroller(Context context, Interpolator interpolator) {
        super(context, interpolator);
    }

    @Override
    public void startScroll(int startX, int startY, int dx, int dy, int duration) {
        super.startScroll(startX, startY, dx, dy, mDuration);
    }

    @Override
    public void startScroll(int startX, int startY, int dx, int dy) {
        super.startScroll(startX, startY, dx, dy, mDuration);
    }

    /**
     * 设置滑动时间
     *
     * @param time
     */
    public void setmDuration(int time) {
        mDuration = time;
    }

    /**
     * 获得滑动时间
     *
     * @return
     */
    public int getmDuration() {
        return mDuration;
    }
}
