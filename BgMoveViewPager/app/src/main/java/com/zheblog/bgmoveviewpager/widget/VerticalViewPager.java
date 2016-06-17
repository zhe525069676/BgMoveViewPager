package com.zheblog.bgmoveviewpager.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.v4.os.ParcelableCompat;
import android.support.v4.os.ParcelableCompatCreatorCallbacks;
import android.support.v4.view.KeyEventCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.support.v4.widget.EdgeEffectCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.FocusFinder;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.Interpolator;
import android.widget.Scroller;

import com.zheblog.bgmoveviewpager.adapter.VerticalPagerAdapter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * 垂直滑动的ViewPager
 * Created by liuz on 16/6/16.
 */
public class VerticalViewPager extends ViewGroup {

    private static final String TAG = "VerticalViewPager";

    private static final boolean DEBUG = false;

    private static final boolean USE_CACHE = false;

    /**
     * ViewPager
     */
    private static final int DEFAULT_OFFSCREEN_PAGES = 1;

    /**
     *
     */
    private static final int MAX_SETTLE_DURATION = 600; // ms

    /**
     *
     */
    private static final int MIN_DISTANCE_FOR_FLING = 25; // dips

    /**
     * layout attributes
     */
    private static final int[] LAYOUT_ATTRS = new int[]{android.R.attr.layout_gravity};

    /**
     *
     *
     */
    static class ItemInfo {
        /**
         * item object
         */
        Object object;

        /**
         * 
         */
        int position;

        /**
         * crolling
         */
        boolean scrolling;
    }

    /**
     * ItemInfo temInfos
     */
    private static final Comparator<ItemInfo> COMPARATOR = new Comparator<ItemInfo>() {
        @Override
        public int compare(ItemInfo lhs, ItemInfo rhs) {
            return lhs.position - rhs.position;
        }
    };

    // XXX
    /**
     * Scroller
     */
    private static final Interpolator sInterpolator = new Interpolator() {
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };

    /**
     * View Page
     */
    private final ArrayList<ItemInfo> mItems = new ArrayList<ItemInfo>();

    /**
     * Vertical Pager Adapter
     */
    private VerticalPagerAdapter mAdapter;

    /**
     * Index of currently displayed page.
     */
    private int mCurItem;

    /**
     * Index of the displayed page that need to restore.
     */
    private int mRestoredCurItem = -1;

    /**
     * Restored Adapter State
     */
    private Parcelable mRestoredAdapterState = null;

    /**
     * Restored Class Loader
     */
    private ClassLoader mRestoredClassLoader = null;

    /**
     * Scroller
     */
    private Scroller mScroller;

    /**
     * PagerObserver
     */
    private PagerObserver mObserver;

    /**
     *
     */
    private int mPageMargin;

    /**
     * Margin Drawable object
     */
    private Drawable mMarginDrawable;

    // XXX bounds
    /**
     * Left Page Bound
     */
    private int mLeftPageBounds;

    /**
     * Right Page Bound
     */
    private int mRightPageBounds;

    /**
     * Child Width Measure Spec
     */
    private int mChildWidthMeasureSpec;

    /**
     * Child Height Measure Spec
     */
    private int mChildHeightMeasureSpec;

    /**
     * onlayout true = yes , false = no
     */
    private boolean mInLayout;

    /**
     * Scrolling Cache
     */
    private boolean mScrollingCacheEnabled;

    /**
     * Populate Pending
     */
    private boolean mPopulatePending;

    /**
     * Scrolling
     */
    private boolean mScrolling;

    /**
     * Off screen page limit
     */
    private int mOffscreenPageLimit = DEFAULT_OFFSCREEN_PAGES;

    /**
     * is being dragged
     */
    private boolean mIsBeingDragged;

    /**
     * is unabled to drag
     */
    private boolean mIsUnableToDrag;

    /**
     * Touch
     */
    private int mTouchSlop;

    // / XXX
    private float mInitialMotionY;

    /**
     * Position of the last motion event.
     */
    private float mLastMotionX;
    private float mLastMotionY;

    /**
     * ID of the active pointer. This is used to retain consistency during drags/flings if multiple
     * pointers are used.
     */
    private int mActivePointerId = INVALID_POINTER;
    /**
     * Sentinel value for no current active pointer. Used by {@link #mActivePointerId}.
     */
    private static final int INVALID_POINTER = -1;

    /**
     * Determines speed during touch scrolling
     */
    private VelocityTracker mVelocityTracker;
    private int mMinimumVelocity;
    private int mMaximumVelocity;
    private int mFlingDistance;

    private boolean mFakeDragging;
    private long mFakeDragBeginTime;

    // XXX
    private EdgeEffectCompat mTopEdge;
    private EdgeEffectCompat mBottomEdge;

    private boolean mFirstLayout = true;
    private boolean mCalledSuper;
    private int mDecorChildCount;

    private OnPageChangeListener mOnPageChangeListener;
    private OnPageChangeListener mInternalPageChangeListener;
    private OnAdapterChangeListener mAdapterChangeListener;

    /**
     * Indicates that the pager is in an idle, settled state. The current page is fully in view and
     * no animation is in progress.
     */
    public static final int SCROLL_STATE_IDLE = 0;

    /**
     * Indicates that the pager is currently being dragged by the user.
     */
    public static final int SCROLL_STATE_DRAGGING = 1;

    /**
     * Indicates that the pager is in the process of settling to a final position.
     */
    public static final int SCROLL_STATE_SETTLING = 2;

    private int mScrollState = SCROLL_STATE_IDLE;

    /**
     * Callback interface for responding to changing state of the selected page.
     */
    public interface OnPageChangeListener {

        /**
         * This method will be invoked when the current page is scrolled, either as part of a
         * programmatically initiated smooth scroll or a user initiated touch scroll.
         *
         * @param position             Position index of the first page currently being displayed. Page
         *                             position+1 will be visible if positionOffset is nonzero.
         * @param positionOffset       Value from [0, 1) indicating the offset from the page at position.
         * @param positionOffsetPixels Value in pixels indicating the offset from position.
         */
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels);

        /**
         * This method will be invoked when a new page becomes selected. Animation is not
         * necessarily complete.
         *
         * @param position Position index of the new selected page.
         */
        public void onPageSelected(int position);

        /**
         * Called when the scroll state changes. Useful for discovering when the user begins
         * dragging, when the pager is automatically settling to the current page, or when it is
         * fully stopped/idle.
         *
         * @param state The new scroll state.
         * @see VerticalViewPager#SCROLL_STATE_IDLE
         * @see VerticalViewPager#SCROLL_STATE_DRAGGING
         * @see VerticalViewPager#SCROLL_STATE_SETTLING
         */
        public void onPageScrollStateChanged(int state);
    }

    private boolean isScroll;

    public void setScroll(boolean scroll) {
        isScroll = scroll;
    }

    /**
     * Simple implementation of the {@link OnPageChangeListener} interface with stub implementations
     * of each method. Extend this if you do not intend to override every method of
     * {@link OnPageChangeListener}.
     */
    public static class SimpleOnPageChangeListener implements OnPageChangeListener {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            // This space for rent
        }

        @Override
        public void onPageSelected(int position) {
            // This space for rent
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            // This space for rent
        }
    }

    /**
     * Used internally to monitor when adapters are switched.
     */
    interface OnAdapterChangeListener {
        void onAdapterChanged(VerticalPagerAdapter oldAdapter,
                              VerticalPagerAdapter newAdapter);
    }

    /**
     * Used internally to tag special types of child views that should be added as pager decorations
     * by default.
     */
    interface Decor {
    }

    public VerticalViewPager(Context context) {
        super(context);
        initViewPager();
    }

    public VerticalViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        initViewPager();
    }

    /**
     *
     */
    void initViewPager() {
        setWillNotDraw(false);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        setFocusable(true);
        final Context context = getContext();
        mScroller = new Scroller(context, sInterpolator);
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration);
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        //
        mTopEdge = new EdgeEffectCompat(context);
        mBottomEdge = new EdgeEffectCompat(context);

        final float density = context.getResources().getDisplayMetrics().density;
        mFlingDistance = (int) (MIN_DISTANCE_FOR_FLING * density);
    }

    /**
     * Scroll State
     *
     * @param newState new state
     */
    private void setScrollState(int newState) {
        if (mScrollState == newState) {
            return;
        } /* end of if */

        mScrollState = newState;
        if (mOnPageChangeListener != null) {
            mOnPageChangeListener.onPageScrollStateChanged(newState);
        } /* end of if */
    }

    /**
     * Set a PagerAdapter that will supply views for this pager as needed.
     *
     * @param adapter Adapter to use
     */
    public void setAdapter(VerticalPagerAdapter adapter) {
        if (mAdapter != null) {
            //mAdapter.unregisterDataSetObserver(mObserver);
            mAdapter.startUpdate(this);
            for (int i = 0; i < mItems.size(); i++) {
                final ItemInfo ii = mItems.get(i);
                mAdapter.destroyItem(this, ii.position, ii.object);
            } /* end of for */
            mAdapter.finishUpdate(this);
            mItems.clear();
            removeNonDecorViews();
            mCurItem = 0;
            scrollTo(0, 0);
        } /* end of if */

        final VerticalPagerAdapter oldAdapter = mAdapter;
        mAdapter = adapter;

        if (mAdapter != null) {
            if (mObserver == null) {
                mObserver = new PagerObserver();
            } /* end of if以下是添加反射代码 */
            try {
                Method regMethod = mAdapter.getClass().getMethod("registerDataSetObserver", mObserver.getClass());
                regMethod.invoke(mAdapter, mObserver);
            } catch (Exception e) {
                e.printStackTrace();
            }
            //mAdapter.registerDataSetObserver(mObserver);
            mPopulatePending = false;
            if (mRestoredCurItem >= 0) {
                mAdapter.restoreState(mRestoredAdapterState, mRestoredClassLoader);
                setCurrentItemInternal(mRestoredCurItem, false, true);
                mRestoredCurItem = -1;
                mRestoredAdapterState = null;
                mRestoredClassLoader = null;
            } else {
                populate();
            } /* end of if */
        } /* end of if */

        if (mAdapterChangeListener != null && oldAdapter != adapter) {
            mAdapterChangeListener.onAdapterChanged(oldAdapter, adapter);
        } /* end of if */
    }

    /**
     * remove non decor view
     */
    private void removeNonDecorViews() {
        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (!lp.isDecor) {
                removeViewAt(i);
                i--;
            } /* end of if */
        } /* end of for */
    }

    /**
     * Retrieve the current adapter supplying pages.
     *
     * @return The currently registered PagerAdapter
     */
    public VerticalPagerAdapter getAdapter() {
        return mAdapter;
    }

    /**
     * Set On Adapter Changer Listener
     *
     * @param listener listener
     */
    void setOnAdapterChangeListener(OnAdapterChangeListener listener) {
        mAdapterChangeListener = listener;
    }

    /**
     * Set the currently selected page. If the ViewPager has already been through its first
     * layout there will be a smooth animated transition between the current item and the
     * specified item.
     *
     * @param item Item index to select
     */
    public void setCurrentItem(int item) {
        mPopulatePending = false;
        setCurrentItemInternal(item, !mFirstLayout, false);
    }

    /**
     * Set the currently selected page.
     *
     * @param item         Item index to select
     * @param smoothScroll True to smoothly scroll to the new item, false to transition immediately
     */
    public void setCurrentItem(int item, boolean smoothScroll) {
        mPopulatePending = false;
        setCurrentItemInternal(item, smoothScroll, false);
    }

    /**
     * @return curretnt item index
     */
    public int getCurrentItem() {
        return mCurItem;
    }

    /**
     * Set current item internal
     *
     * @param item         item index
     * @param smoothScroll is smooth scroll
     * @param always       is always
     */
    void setCurrentItemInternal(int item, boolean smoothScroll, boolean always) {
        //设置切换速度，默认为800ms
        setCurrentItemInternal(item, smoothScroll, always, 800);
    }

    /**
     * Set current item internal
     *
     * @param item         item index
     * @param smoothScroll is smooth scroll
     * @param always       is always
     * @param velocity
     */
    void setCurrentItemInternal(int item, boolean smoothScroll, boolean always, int velocity) {
        if (mAdapter == null || mAdapter.getCount() <= 0) {
            setScrollingCacheEnabled(false);
            return;
        } /* end of if */
        if (!always && mCurItem == item && mItems.size() != 0) {
            setScrollingCacheEnabled(false);
            return;
        } /* end of if */
        if (item < 0) {
            item = 0; //
        } else if (item >= mAdapter.getCount()) {
            item = mAdapter.getCount() - 1; //
        } /* end of if */
        final int pageLimit = mOffscreenPageLimit;
        if (item > (mCurItem + pageLimit) || item < (mCurItem - pageLimit)) {
            // We are doing a jump by more than one page.  To avoid
            // glitches, we want to keep all current pages in the view
            // until the scroll ends.
            for (int i = 0; i < mItems.size(); i++) {
                mItems.get(i).scrolling = true;
            } /* end of for */
        } /* end of if */
        final boolean dispatchSelected = mCurItem != item;
        mCurItem = item;
        populate();
        final int destY = (getHeight() + mPageMargin) * item;
        if (smoothScroll) {
            smoothScrollTo(0, destY, velocity);
            if (dispatchSelected && mOnPageChangeListener != null) {
                mOnPageChangeListener.onPageSelected(item);
            } /* end of if */
            if (dispatchSelected && mInternalPageChangeListener != null) {
                mInternalPageChangeListener.onPageSelected(item);
            } /* end of if */
        } else {
            if (dispatchSelected && mOnPageChangeListener != null) {
                mOnPageChangeListener.onPageSelected(item);
            } /* end of if */
            if (dispatchSelected && mInternalPageChangeListener != null) {
                mInternalPageChangeListener.onPageSelected(item);
            } /* end of if */
            completeScroll();
            scrollTo(0, destY);
        } /* end of if */
    }


    /**
     * Set a listener that will be invoked whenever the page changes or is incrementally
     * scrolled. See {@link OnPageChangeListener}.
     *
     * @param listener Listener to set
     */
    public void setOnPageChangeListener(OnPageChangeListener listener) {
        mOnPageChangeListener = listener;
    }

    /**
     * Set a separate OnPageChangeListener for internal use by the support library.
     *
     * @param listener Listener to set
     * @return The old listener that was set, if any.
     */
    OnPageChangeListener setInternalPageChangeListener(OnPageChangeListener listener) {
        OnPageChangeListener oldListener = mInternalPageChangeListener;
        mInternalPageChangeListener = listener;
        return oldListener;
    }

    /**
     * Returns the number of pages that will be retained to either side of the
     * current page in the view hierarchy in an idle state. Defaults to 1.
     *
     * @return How many pages will be kept offscreen on either side
     * @see #setOffscreenPageLimit(int)
     */
    public int getOffscreenPageLimit() {
        return mOffscreenPageLimit;
    }

    /**
     * Set the number of pages that should be retained to either side of the
     * current page in the view hierarchy in an idle state. Pages beyond this
     * limit will be recreated from the adapter when needed.
     * <p>
     * <p>This is offered as an optimization. If you know in advance the number
     * of pages you will need to support or have lazy-loading mechanisms in place
     * on your pages, tweaking this setting can have benefits in perceived smoothness
     * of paging animations and interaction. If you have a small number of pages (3-4)
     * that you can keep active all at once, less time will be spent in layout for
     * newly created view subtrees as the user pages back and forth.</p>
     * <p>
     * <p>You should keep this limit low, especially if your pages have complex layouts.
     * This setting defaults to 1.</p>
     *
     * @param limit How many pages will be kept offscreen in an idle state.
     */
    public void setOffscreenPageLimit(int limit) {
        if (limit < DEFAULT_OFFSCREEN_PAGES) {
            Log.w(TAG, "Requested offscreen page limit " + limit + " too small; defaulting to " +
                    DEFAULT_OFFSCREEN_PAGES);
            limit = DEFAULT_OFFSCREEN_PAGES;
        } /* end of if */
        if (limit != mOffscreenPageLimit) {
            mOffscreenPageLimit = limit;
            populate();
        } /* end of if */
    }

    //XXX

    /**
     * Set the margin between pages.
     *
     * @param marginPixels Distance between adjacent pages in pixels
     * @see #getPageMargin()
     * @see #setPageMarginDrawable(Drawable)
     * @see #setPageMarginDrawable(int)
     */
    public void setPageMargin(int marginPixels) {
        final int oldMargin = mPageMargin;
        mPageMargin = marginPixels;

        final int height = getHeight();
        recomputeScrollPosition(height, height, marginPixels, oldMargin);

        requestLayout();
    }

    /**
     * Return the margin between pages.
     *
     * @return The size of the margin in pixels
     */
    public int getPageMargin() {
        return mPageMargin;
    }

    /**
     * Set a drawable that will be used to fill the margin between pages.
     *
     * @param d Drawable to display between pages
     */
    public void setPageMarginDrawable(Drawable d) {
        mMarginDrawable = d;
        if (d != null) refreshDrawableState();
        setWillNotDraw(d == null);
        invalidate();
    }

    /**
     * Set a drawable that will be used to fill the margin between pages.
     *
     * @param resId Resource ID of a drawable to display between pages
     */
    public void setPageMarginDrawable(int resId) {
        setPageMarginDrawable(getContext().getResources().getDrawable(resId));
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == mMarginDrawable;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        final Drawable d = mMarginDrawable;
        if (d != null && d.isStateful()) {
            d.setState(getDrawableState());
        } /* end of if */
    }

    // We want the duration of the page snap animation to be influenced by the distance that
    // the screen has to travel, however, we don't want this duration to be effected in a
    // purely linear fashion. Instead, we use this method to moderate the effect that the distance
    // of travel has on the overall snap duration.
    float distanceInfluenceForSnapDuration(float f) {
        f -= 0.5f; // center the values about 0.
        f *= 0.3f * Math.PI / 2.0f;
        return (float) Math.sin(f);
    }

    /**
     * Like {@link View#scrollBy}, but scroll smoothly instead of immediately.
     *
     * @param x the number of pixels to scroll by on the X axis
     * @param y the number of pixels to scroll by on the Y axis
     */
    void smoothScrollTo(int x, int y) {
        smoothScrollTo(x, y, 0);
    }

    //XXX

    /**
     * Like {@link View#scrollBy}, but scroll smoothly instead of immediately.
     *
     * @param x        the number of pixels to scroll by on the X axis
     * @param y        the number of pixels to scroll by on the Y axis
     * @param velocity the velocity associated with a fling, if applicable. (0 otherwise)
     */
    void smoothScrollTo(int x, int y, int velocity) {
        if (getChildCount() == 0) {
            // Nothing to do.
            setScrollingCacheEnabled(false);
            return;
        } /* end of if */
        int sx = getScrollX();
        int sy = getScrollY();
        int dx = x - sx; //distance of x
        int dy = y - sy; //distance of y
        if (dx == 0 && dy == 0) {
            completeScroll();
            setScrollState(SCROLL_STATE_IDLE);
            return;
        } /* end of if */

        setScrollingCacheEnabled(true);
        mScrolling = true;
        setScrollState(SCROLL_STATE_SETTLING);

        final int height = getHeight();
        final int halfHeight = height / 2; //
        final float distanceRatio = Math.min(1f, 1.0f * Math.abs(dy) / height);
        final float distance = halfHeight + halfHeight *
                distanceInfluenceForSnapDuration(distanceRatio);

        int duration = 0;
        velocity = Math.abs(velocity);
        if (velocity > 0) {
            duration = 4 * Math.round(1000 * Math.abs(distance / velocity));
        } else {
            final float pageDelta = (float) Math.abs(dy) / (height + mPageMargin);
            duration = (int) ((pageDelta + 1) * 100);
        } /* end of if */
        duration = Math.min(duration, MAX_SETTLE_DURATION);

        mScroller.startScroll(sx, sy, dx, dy, duration);
        invalidate();
    }

    /**
     * View Pager
     *
     * @param position
     * @param index    index
     */
    void addNewItem(int position, int index) {
        ItemInfo ii = new ItemInfo();
        ii.position = position;
        ii.object = mAdapter.instantiateItem(this, position);
        if (index < 0) {
            mItems.add(ii);
        } else {
            mItems.add(index, ii);
        } /* end of if */
    }

    /**
     * This method only gets called if our observer is attached, so mAdapter is non-null
     */
    void dataSetChanged() {
        // This method only gets called if our observer is attached, so mAdapter is non-null.

        boolean needPopulate = mItems.size() < 3 && mItems.size() < mAdapter.getCount();
        int newCurrItem = -1;

        boolean isUpdating = false;
        for (int i = 0; i < mItems.size(); i++) {
            final ItemInfo ii = mItems.get(i);
            final int newPos = mAdapter.getItemPosition(ii.object);

            if (newPos == VerticalPagerAdapter.POSITION_UNCHANGED) {
                //  the position of the given item has not changed
                continue;
            } /* end of if */

            if (newPos == VerticalPagerAdapter.POSITION_NONE) {
                //  the item is no longer present in the adapter.
                mItems.remove(i);
                i--;

                if (!isUpdating) {
                    mAdapter.startUpdate(this);
                    isUpdating = true;
                } /* end of if */

                mAdapter.destroyItem(this, ii.position, ii.object);
                needPopulate = true;

                if (mCurItem == ii.position) {
                    // Keep the current item in the valid range
                    newCurrItem = Math.max(0, Math.min(mCurItem, mAdapter.getCount() - 1));
                } /* end of if */
                continue;
            } /* end of if */

            if (ii.position != newPos) {
                if (ii.position == mCurItem) {
                    // Our current item changed position. Follow it.
                    newCurrItem = newPos;
                } /* end of if */

                ii.position = newPos;
                needPopulate = true;
            } /* end of if */
        } /* end of for */

        if (isUpdating) {
            mAdapter.finishUpdate(this);
        } /* end of if */

        Collections.sort(mItems, COMPARATOR); // item

        if (newCurrItem >= 0) {
            // TODO This currently causes a jump.
            setCurrentItemInternal(newCurrItem, false, true);
            needPopulate = true;
        } /* end of if */
        if (needPopulate) {
            populate();
            requestLayout();
        } /* end of if */
    }

    /**
     *
     */
    void populate() {
        if (mAdapter == null) {
            return;
        } /* end of if */

        // Bail now if we are waiting to populate.  This is to hold off
        // on creating views from the time the user releases their finger to
        // fling to a new position until we have finished the scroll to
        // that position, avoiding glitches from happening at that point.
        if (mPopulatePending) {
            if (DEBUG) Log.i(TAG, "populate is pending, skipping for now...");
            return;
        } /* end of if */

        // Also, don't populate until we are attached to a window.  This is to
        // avoid trying to populate before we have restored our view hierarchy
        // state and conflicting with what is restored.
        if (getWindowToken() == null) {
            return;
        } /* end of if */

        mAdapter.startUpdate(this);

        final int pageLimit = mOffscreenPageLimit;
        final int startPos = Math.max(0, mCurItem - pageLimit);
        final int N = mAdapter.getCount();
        final int endPos = Math.min(N - 1, mCurItem + pageLimit);

        if (DEBUG) Log.v(TAG, "populating: startPos=" + startPos + " endPos=" + endPos);

        // Add and remove pages in the existing list.
        int lastPos = -1;
        for (int i = 0; i < mItems.size(); i++) {
            ItemInfo ii = mItems.get(i);
            if ((ii.position < startPos || ii.position > endPos) && !ii.scrolling) {
                if (DEBUG) Log.i(TAG, "removing: " + ii.position + " @ " + i);
                mItems.remove(i);
                i--;
                mAdapter.destroyItem(this, ii.position, ii.object);
            } else if (lastPos < endPos && ii.position > startPos) {
                // The next item is outside of our range, but we have a gap
                // between it and the last item where we want to have a page
                // shown.  Fill in the gap.
                lastPos++;
                if (lastPos < startPos) {
                    lastPos = startPos;
                } /* end of if */
                while (lastPos <= endPos && lastPos < ii.position) {
                    if (DEBUG) Log.i(TAG, "inserting: " + lastPos + " @ " + i);
                    addNewItem(lastPos, i);
                    lastPos++;
                    i++;
                } /* end of while */
            } /* end of if */
            lastPos = ii.position;
        } /* end of if */

        // Add any new pages we need at the end.
        lastPos = mItems.size() > 0 ? mItems.get(mItems.size() - 1).position : -1;
        if (lastPos < endPos) {
            lastPos++;
            lastPos = lastPos > startPos ? lastPos : startPos;
            while (lastPos <= endPos) {
                if (DEBUG) Log.i(TAG, "appending: " + lastPos);
                addNewItem(lastPos, -1);
                lastPos++;
            } /* end of while */
        } /* end of if */

        if (DEBUG) {
            Log.i(TAG, "Current page list:");
            for (int i = 0; i < mItems.size(); i++) {
                Log.i(TAG, "#" + i + ": page " + mItems.get(i).position);
            } /* end of for */
        } /* end of if */

        ItemInfo curItem = null;
        for (int i = 0; i < mItems.size(); i++) {
            if (mItems.get(i).position == mCurItem) {
                curItem = mItems.get(i);
                break;
            } /* end of if */
        } /* end of for */
        mAdapter.setPrimaryItem(this, mCurItem, curItem != null ? curItem.object : null);

        mAdapter.finishUpdate(this);

        if (hasFocus()) {
            View currentFocused = findFocus();
            ItemInfo ii = currentFocused != null ? infoForAnyChild(currentFocused) : null;
            if (ii == null || ii.position != mCurItem) {
                for (int i = 0; i < getChildCount(); i++) {
                    View child = getChildAt(i);
                    ii = infoForChild(child);
                    if (ii != null && ii.position == mCurItem) {
                        if (child.requestFocus(FOCUS_FORWARD)) {
                            break;
                        } /* end of if */
                    } /* end of if */
                } /* end of for */
            } /* end of if */
        } /* end of if */
    }

    /**
     * This is the persistent state that is saved by ViewPager.  Only needed
     * if you are creating a sublass of ViewPager that must save its own
     * state, in which case it should implement a subclass of this which
     * contains that state.
     */
    public static class SavedState extends BaseSavedState {
        int position;
        Parcelable adapterState;
        ClassLoader loader;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(position);
            out.writeParcelable(adapterState, flags);
        }

        @Override
        public String toString() {
            return "FragmentPager.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " position=" + position + "}";
        }

        public static final Creator<SavedState> CREATOR
                = ParcelableCompat.newCreator(new ParcelableCompatCreatorCallbacks<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                return new SavedState(in, loader);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        });

        SavedState(Parcel in, ClassLoader loader) {
            super(in);
            if (loader == null) {
                loader = getClass().getClassLoader();
            } /* end of if */
            position = in.readInt();
            adapterState = in.readParcelable(loader);
            this.loader = loader;
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.position = mCurItem;
        if (mAdapter != null) {
            ss.adapterState = mAdapter.saveState();
        } /* end of if */
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        } /* end of if */

        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        if (mAdapter != null) {
            mAdapter.restoreState(ss.adapterState, ss.loader);
            setCurrentItemInternal(ss.position, false, true);
        } else {
            mRestoredCurItem = ss.position;
            mRestoredAdapterState = ss.adapterState;
            mRestoredClassLoader = ss.loader;
        } /* end of if */
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (!checkLayoutParams(params)) {
            params = generateLayoutParams(params);
        } /* end of if */
        final LayoutParams lp = (LayoutParams) params;
        lp.isDecor |= child instanceof Decor;
        if (mInLayout) {
            if (lp != null && lp.isDecor) {
                throw new IllegalStateException("Cannot add pager decor view during layout");
            } /* end of if */
            addViewInLayout(child, index, params);
            child.measure(mChildWidthMeasureSpec, mChildHeightMeasureSpec);
        } else {
            super.addView(child, index, params);
        } /* end of if */

        if (USE_CACHE) {
            if (child.getVisibility() != GONE) {
                child.setDrawingCacheEnabled(mScrollingCacheEnabled);
            } else {
                child.setDrawingCacheEnabled(false);
            } /* end of if */
        } /* end of if */
    }

    /**
     * 鍙栧緱鐩墠瑭查爜鐨剓@link ItemInfo}
     *
     * @param child child {@link View} object
     * @return return {@link ItemInfo} if is view from object, other return null
     */
    ItemInfo infoForChild(View child) {
        for (int i = 0; i < mItems.size(); i++) {
            ItemInfo ii = mItems.get(i);
            if (mAdapter.isViewFromObject(child, ii.object)) {
                return ii;
            } /* end of if */
        } /* end of for */
        return null;
    }

    ItemInfo infoForAnyChild(View child) {
        ViewParent parent;
        while ((parent = child.getParent()) != this) {
            if (parent == null || !(parent instanceof View)) {
                return null;
            } /* end of if */
            child = (View) parent;
        } /* end of while */
        return infoForChild(child);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mFirstLayout = true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // For simple implementation, or internal size is always 0.
        // We depend on the container to specify the layout size of
        // our view.  We can't really know what it is since we will be
        // adding and removing different arbitrary views and do not
        // want the layout to change as this happens.
        setMeasuredDimension(getDefaultSize(0, widthMeasureSpec),
                getDefaultSize(0, heightMeasureSpec));

        // Children are just made to fill our space.
        int childWidthSize = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
        int childHeightSize = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();

        /*
         * Make sure all children have been properly measured. Decor views first.
         * Right now we cheat and make this less complicated by assuming decor
         * views won't intersect. We will pin to edges based on gravity.
         */
        int size = getChildCount();
        for (int i = 0; i < size; ++i) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp != null && lp.isDecor) {
                    final int hgrav = lp.gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
                    final int vgrav = lp.gravity & Gravity.VERTICAL_GRAVITY_MASK;
                    Log.d(TAG, "gravity: " + lp.gravity + " hgrav: " + hgrav + " vgrav: " + vgrav);
                    int widthMode = MeasureSpec.AT_MOST;
                    int heightMode = MeasureSpec.AT_MOST;
                    boolean consumeVertical = vgrav == Gravity.TOP || vgrav == Gravity.BOTTOM;
                    boolean consumeHorizontal = hgrav == Gravity.LEFT || hgrav == Gravity.RIGHT;

                    if (consumeVertical) {
                        widthMode = MeasureSpec.EXACTLY;
                    } else if (consumeHorizontal) {
                        heightMode = MeasureSpec.EXACTLY;
                    } /* end of if */

                    final int widthSpec = MeasureSpec.makeMeasureSpec(childWidthSize, widthMode);
                    final int heightSpec = MeasureSpec.makeMeasureSpec(childHeightSize, heightMode);
                    child.measure(widthSpec, heightSpec);

                    if (consumeVertical) {
                        childHeightSize -= child.getMeasuredHeight();
                    } else if (consumeHorizontal) {
                        childWidthSize -= child.getMeasuredWidth();
                    } /* end of if */
                } /* end of if */
            } /* end of if */
        } /* end of for */

        mChildWidthMeasureSpec = MeasureSpec.makeMeasureSpec(childWidthSize, MeasureSpec.EXACTLY);
        mChildHeightMeasureSpec = MeasureSpec.makeMeasureSpec(childHeightSize, MeasureSpec.EXACTLY);

        // Make sure we have created all fragments that we need to have shown.
        mInLayout = true;
        populate();
        mInLayout = false;

        // Page views next.
        size = getChildCount();
        for (int i = 0; i < size; ++i) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                if (DEBUG) Log.v(TAG, "Measuring #" + i + " " + child
                        + ": " + mChildWidthMeasureSpec);

                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp == null || !lp.isDecor) {
                    child.measure(mChildWidthMeasureSpec, mChildHeightMeasureSpec);
                } /* end of if */
            } /* end of if */
        } /* end of for */
    }

    // XXX鍨傜洿
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Make sure scroll position is set correctly.
        if (h != oldh) {
            recomputeScrollPosition(h, oldh, mPageMargin, mPageMargin);
        } /* end of if */
    }

    // XXX鍨傜洿

    /**
     * 閲嶆柊瑷堢畻Scroll浣嶇疆
     *
     * @param height    楂樺害
     * @param oldHeight 鑸婇珮搴�
     * @param margin    閭婄晫
     * @param oldMargin 鑸婇倞鐣�
     */
    private void recomputeScrollPosition(int height, int oldHeight, int margin, int oldMargin) {
        final int heightWithMargin = height + margin;
        if (oldHeight > 0) {
            final int oldScrollPos = getScrollY();
            final int oldwwm = oldHeight + oldMargin;
            final int oldScrollItem = oldScrollPos / oldwwm;
            final float scrollOffset = (float) (oldScrollPos % oldwwm) / oldwwm;
            final int scrollPos = (int) ((oldScrollItem + scrollOffset) * heightWithMargin);
            scrollTo(getScrollX(), scrollPos);
            if (!mScroller.isFinished()) {
                // We now return to your regularly scheduled scroll, already in progress.
                final int newDuration = mScroller.getDuration() - mScroller.timePassed();
                mScroller.startScroll(0, scrollPos, mCurItem * heightWithMargin, 0, newDuration);
            } /* end of if */
        } else {
            int scrollPos = mCurItem * heightWithMargin;
            if (scrollPos != getScrollY()) {
                completeScroll();
                scrollTo(getScrollX(), scrollPos);
            } /* end of if */
        } /* end of if */
    }

    // XXX 鍨傜洿
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mInLayout = true;
        populate();
        mInLayout = false;

        final int count = getChildCount();
        int width = r - l;
        int height = b - t;
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();
        final int scrollY = getScrollY();

        int decorCount = 0;

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                ItemInfo ii;
                int childLeft = 0;
                int childTop = 0;
                if (lp.isDecor) {
                    //XXX isDecor鐐篺alse锛屾毇鏅傛矑鐢ㄥ埌
                    final int hgrav = lp.gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
                    final int vgrav = lp.gravity & Gravity.VERTICAL_GRAVITY_MASK;
                    switch (hgrav) {
                        default:
                            childLeft = paddingLeft;
                            break;
                        case Gravity.LEFT:
                            childLeft = paddingLeft;
                            paddingLeft += child.getMeasuredWidth();
                            break;
                        case Gravity.CENTER_HORIZONTAL:
                            childLeft = Math.max((width - child.getMeasuredWidth()) / 2,
                                    paddingLeft);
                            break;
                        case Gravity.RIGHT:
                            childLeft = width - paddingRight - child.getMeasuredWidth();
                            paddingRight += child.getMeasuredWidth();
                            break;
                    } /* end of switch */
                    switch (vgrav) {
                        default:
                            childTop = paddingTop;
                            break;
                        case Gravity.TOP:
                            childTop = paddingTop;
                            paddingTop += child.getMeasuredHeight();
                            break;
                        case Gravity.CENTER_VERTICAL:
                            childTop = Math.max((height - child.getMeasuredHeight()) / 2,
                                    paddingTop);
                            break;
                        case Gravity.BOTTOM:
                            childTop = height - paddingBottom - child.getMeasuredHeight();
                            paddingBottom += child.getMeasuredHeight();
                            break;
                    } /* end of switch */

                    //XXX 绱�寗y杌哥Щ鍕曡窛闆�
                    childTop += scrollY;
                    decorCount++;
                    child.layout(childLeft, childTop,
                            childLeft + child.getMeasuredWidth(),
                            childTop + child.getMeasuredHeight());
                } else if ((ii = infoForChild(child)) != null) {
                    //XXX 瑷堢畻ViewPager姣忎竴闋佺殑閭婄晫
                    int toff = (height + mPageMargin) * ii.position;
                    childLeft = paddingLeft;
                    childTop = paddingTop + toff;

                    if (DEBUG) Log.v(TAG, "Positioning #" + i + " " + child + " f=" + ii.object
                            + ":" + childLeft + "," + childTop + " " + child.getMeasuredWidth()
                            + "x" + child.getMeasuredHeight());
                    child.layout(childLeft, childTop,
                            childLeft + child.getMeasuredWidth(),
                            childTop + child.getMeasuredHeight());
                } /* end of if */
            } /* end of if */
        } /* end of for */

        //XXX 瑷畾宸﹀彸閭婄晫
        mLeftPageBounds = paddingLeft;
        mRightPageBounds = width - paddingRight;
        mDecorChildCount = decorCount;
        mFirstLayout = false;
    }

    @Override
    public void computeScroll() {
        if (DEBUG) Log.i(TAG, "computeScroll: finished=" + mScroller.isFinished());
        if (!mScroller.isFinished()) {
            if (mScroller.computeScrollOffset()) {
                if (DEBUG) Log.i(TAG, "computeScroll: still scrolling");
                int oldX = getScrollX();
                int oldY = getScrollY();
                int x = mScroller.getCurrX();
                int y = mScroller.getCurrY();

                if (oldX != x || oldY != y) {
                    scrollTo(x, y);
                    pageScrolled(y);
                } /* end of if */

                // Keep on drawing until the animation has finished.
                invalidate();
                return;
            } /* end of if */
        } /* end of if */

        // Done with scroll, clean up state.
        completeScroll();
    }

    /**
     * page scrolled
     *
     * @param ypos
     */
    private void pageScrolled(int ypos) {
        final int heightWithMargin = getHeight() + mPageMargin;
        final int position = ypos / heightWithMargin;
        final int offsetPixels = ypos % heightWithMargin;
        final float offset = (float) offsetPixels / heightWithMargin;

        mCalledSuper = false;
        onPageScrolled(position, offset, offsetPixels);
        if (!mCalledSuper) {
            throw new IllegalStateException("onPageScrolled did not call superclass implementation");
        } /* end of if */
    }

    //XXX

    /**
     * This method will be invoked when the current page is scrolled, either as part
     * of a programmatically initiated smooth scroll or a user initiated touch scroll.
     * If you override this method you must call through to the superclass implementation
     * (e.g. super.onPageScrolled(position, offset, offsetPixels)) before onPageScrolled
     * returns.
     *
     * @param position     Position index of the first page currently being displayed.
     *                     Page position+1 will be visible if positionOffset is nonzero.
     * @param offset       Value from [0, 1) indicating the offset from the page at position.
     * @param offsetPixels Value in pixels indicating the offset from position.
     */
    protected void onPageScrolled(int position, float offset, int offsetPixels) {
        // Offset any decor views if needed - keep them on-screen at all times.
        if (mDecorChildCount > 0) {
            final int scrollY = getScrollY();
            int paddingTop = getPaddingTop();
            int paddingBottom = getPaddingBottom();
            final int height = getHeight();
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (!lp.isDecor) continue;

                final int vgrav = lp.gravity & Gravity.VERTICAL_GRAVITY_MASK;
                int childTop = 0;
                switch (vgrav) {
                    default:
                        childTop = paddingTop;
                        break;
                    case Gravity.TOP:
                        childTop = paddingTop;
                        paddingTop += child.getHeight();
                        break;
                    case Gravity.CENTER_HORIZONTAL:
                        childTop = Math.max((height - child.getMeasuredHeight()) / 2,
                                paddingTop);
                        break;
                    case Gravity.BOTTOM:
                        childTop = height - paddingBottom - child.getMeasuredHeight();
                        paddingBottom += child.getMeasuredHeight();
                        break;
                } /* end of switch */
                childTop += scrollY;

                final int childOffset = childTop - child.getTop();
                if (childOffset != 0) {
                    child.offsetTopAndBottom(childOffset);
                } /* end of if */
            } /* end of for */
        } /* end of for */

        if (mOnPageChangeListener != null) {
            mOnPageChangeListener.onPageScrolled(position, offset, offsetPixels);
        } /* end of if */
        if (mInternalPageChangeListener != null) {
            mInternalPageChangeListener.onPageScrolled(position, offset, offsetPixels);
        } /* end of if */
        mCalledSuper = true;
    }

    /**
     * ScrollScroll
     * Scroll populate
     */
    private void completeScroll() {
        boolean needPopulate = mScrolling;
        if (needPopulate) {
            // Done with scroll, no longer want to cache view drawing.
            setScrollingCacheEnabled(false);
            mScroller.abortAnimation();
            int oldX = getScrollX();
            int oldY = getScrollY();
            int x = mScroller.getCurrX();
            int y = mScroller.getCurrY();
            if (oldX != x || oldY != y) {
                scrollTo(x, y);
            } /* end of if */
            setScrollState(SCROLL_STATE_IDLE);
        } /* end of if */
        mPopulatePending = false;
        mScrolling = false;
        for (int i = 0; i < mItems.size(); i++) {
            ItemInfo ii = mItems.get(i);
            if (ii.scrolling) {
                needPopulate = true;
                ii.scrolling = false;
            } /* end of if */
        } /* end of for */
        if (needPopulate) {
            populate();
        } /* end of if */
    }


    //
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
      /*
       * This method JUST determines whether we want to intercept the motion.
       * If we return true, onMotionEvent will be called and we do the actual
       * scrolling there.
       */
        if (!isScroll) {
            return false;
        }

        final int action = ev.getAction() & MotionEventCompat.ACTION_MASK;

        // Always take care of the touch gesture being complete.
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            // Release the drag.
            if (DEBUG) Log.v(TAG, "Intercept done!");
            mIsBeingDragged = false;
            mIsUnableToDrag = false;
            mActivePointerId = INVALID_POINTER;
            if (mVelocityTracker != null) {
                mVelocityTracker.recycle();
                mVelocityTracker = null;
            } /* end of if */
            return false;
        } /* end of if */

        // Nothing more to do here if we have decided whether or not we
        // are dragging.
        if (action != MotionEvent.ACTION_DOWN) {
            if (mIsBeingDragged) {
                if (DEBUG) Log.v(TAG, "Intercept returning true!");
                return true;
            } /* end of if */
            if (mIsUnableToDrag) {
                if (DEBUG) Log.v(TAG, "Intercept returning false!");
                return false;
            } /* end of if */
        } /* end of if */

        switch (action) {
            case MotionEvent.ACTION_MOVE: {
              /*
               * mIsBeingDragged == false, otherwise the shortcut would have caught it. Check
               * whether the user has moved far enough from his original down touch.
               */

              /*
              * Locally do absolute value. mLastMotionX is set to the x value
              * of the down event.
              */
                final int activePointerId = mActivePointerId;
                if (activePointerId == INVALID_POINTER) {
                    // If we don't have a valid id, the touch down wasn't on content.
                    break;
                } /* end of if */

                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, activePointerId);
                final float x = MotionEventCompat.getX(ev, pointerIndex);
                final float xDiff = Math.abs(x - mLastMotionX);
                final float y = MotionEventCompat.getY(ev, pointerIndex);
                final float dy = y - mLastMotionY;
                final float yDiff = Math.abs(dy);

                if (DEBUG) Log.v(TAG, "Moved x to " + x + "," + y + " diff=" + xDiff + "," + yDiff);

                if (canScroll(this, false, (int) dy, (int) x, (int) y)) {
                    // Nested view has scrollable area under this point. Let it be handled there.
                    mInitialMotionY = mLastMotionY = y;
                    mLastMotionX = x;
                    return false;
                } /* end of if */
                if (yDiff > mTouchSlop && yDiff > xDiff) {
                    if (DEBUG) Log.v(TAG, "Starting drag!");
                    mIsBeingDragged = true;
                    setScrollState(SCROLL_STATE_DRAGGING);
                    mLastMotionY = y;
                    setScrollingCacheEnabled(true);
                } else {
                    if (xDiff > mTouchSlop) {
                        // The finger has moved enough in the horizontal
                        // direction to be counted as a drag...  abort
                        // any attempt to drag vertically, to work correctly
                        // with children that have scrolling containers.
                        if (DEBUG) Log.v(TAG, "Starting unable to drag!");
                        mIsUnableToDrag = true;
                    } /* end of if */
                } /* end of if */
                break;
            } /* end of case */

            case MotionEvent.ACTION_DOWN: {
              /*
               * Remember location of down touch.
               * ACTION_DOWN always refers to pointer index 0.
               */
                mLastMotionX = ev.getX();
                mLastMotionY = mInitialMotionY = ev.getY();
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);

                if (mScrollState == SCROLL_STATE_SETTLING) {
                    // Let the user 'catch' the pager as it animates.
                    mIsBeingDragged = true;
                    mIsUnableToDrag = false;
                    setScrollState(SCROLL_STATE_DRAGGING);
                } else {
                    completeScroll();
                    mIsBeingDragged = false;
                    mIsUnableToDrag = false;
                } /* end of if */

                if (DEBUG) Log.v(TAG, "Down at " + mLastMotionX + "," + mLastMotionY
                        + " mIsBeingDragged=" + mIsBeingDragged
                        + "mIsUnableToDrag=" + mIsUnableToDrag);
                break;
            } /* end of case */

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
        } /* end of switch */

        if (!mIsBeingDragged) {
            // Track the velocity as long as we aren't dragging.
            // Once we start a real drag we will track in onTouchEvent.
            if (mVelocityTracker == null) {
                mVelocityTracker = VelocityTracker.obtain();
            } /* end of if */
            mVelocityTracker.addMovement(ev);
        } /* end of if */

      /*
       * The only time we want to intercept motion events is if we are in the
       * drag mode.
       */
        return mIsBeingDragged;
    }


    // XXX 鍨傜洿
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mFakeDragging) {
            // A fake drag is in progress already, ignore this real one
            // but still eat the touch events.
            // (It is likely that the user is multi-touching the screen.)
            return true;
        } /* end of if */

        if (ev.getAction() == MotionEvent.ACTION_DOWN && ev.getEdgeFlags() != 0) {
            // Don't handle edge touches immediately -- they may actually belong to one of our
            // descendants.
            return false;
        } /* end of if */

        if (mAdapter == null || mAdapter.getCount() == 0) {
            // Nothing to present or scroll; nothing to touch.
            return false;
        } /* end of if */

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } /* end of if */
        mVelocityTracker.addMovement(ev);

        final int action = ev.getAction();
        boolean needsInvalidate = false;

        switch (action & MotionEventCompat.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                /*
                 * If being flinged and user touches, stop the fling. isFinished
                 * will be false if being flinged.
                 */
                completeScroll();

                // Remember where the motion event started
                mLastMotionY = mInitialMotionY = ev.getY();
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                break;
            } /* end of case */
            case MotionEvent.ACTION_MOVE:
                if (!mIsBeingDragged) {
                    final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                    final float x = MotionEventCompat.getX(ev, pointerIndex);
                    final float xDiff = Math.abs(x - mLastMotionX);
                    final float y = MotionEventCompat.getY(ev, pointerIndex);
                    final float yDiff = Math.abs(y - mLastMotionY);
                    if (DEBUG)
                        Log.v(TAG, "Moved x to " + x + "," + y + " diff=" + xDiff + "," + yDiff);
                    if (yDiff > mTouchSlop && yDiff > xDiff) {
                        if (DEBUG) Log.v(TAG, "Starting drag!");
                        mIsBeingDragged = true;
                        mLastMotionY = y;
                        setScrollState(SCROLL_STATE_DRAGGING);
                        setScrollingCacheEnabled(true);
                    } /* end of if */
                } /* end of if */
                if (mIsBeingDragged) {
                    // Scroll to follow the motion event
                    final int activePointerIndex = MotionEventCompat.findPointerIndex(
                            ev, mActivePointerId);
                    final float y = MotionEventCompat.getY(ev, activePointerIndex);
                    final float deltaY = mLastMotionY - y;
                    mLastMotionY = y;
                    float oldScrollY = getScrollY();
                    float scrollY = oldScrollY + deltaY;
                    final int height = getHeight();
                    final int heightWithMargin = height + mPageMargin;

                    final int lastItemIndex = mAdapter.getCount() - 1;
                    final float topBound = Math.max(0, (mCurItem - 1) * heightWithMargin);
                    final float bottomBound =
                            Math.min(mCurItem + 1, lastItemIndex) * heightWithMargin;
                    if (scrollY < topBound) {
                        if (topBound == 0) {
                            float over = -scrollY;
                            needsInvalidate = mTopEdge.onPull(over / height);
                        } /* end of if */
                        scrollY = topBound;
                    } else if (scrollY > bottomBound) {
                        if (bottomBound == lastItemIndex * heightWithMargin) {
                            float over = scrollY - bottomBound;
                            needsInvalidate = mBottomEdge.onPull(over / height);
                        } /* end of if */
                        scrollY = bottomBound;
                    } /* end of if */
                    // Don't lose the rounded component
                    mLastMotionY += scrollY - (int) scrollY;
                    scrollTo(getScrollX(), (int) scrollY);
                    pageScrolled((int) scrollY);
                } /* end of if */
                break;
            case MotionEvent.ACTION_UP:
                if (mIsBeingDragged) {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int initialVelocity = (int) VelocityTrackerCompat.getYVelocity(
                            velocityTracker, mActivePointerId);
                    mPopulatePending = true;
                    final int heightWithMargin = getHeight() + mPageMargin;
                    final int scrollY = getScrollY();
                    final int currentPage = scrollY / heightWithMargin;
                    final float pageOffset = (float) (scrollY % heightWithMargin) / heightWithMargin;
                    final int activePointerIndex =
                            MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                    final float y = MotionEventCompat.getY(ev, activePointerIndex);
                    final int totalDelta = (int) (y - mInitialMotionY);
                    int nextPage = determineTargetPage(currentPage, pageOffset, initialVelocity,
                            totalDelta);
                    setCurrentItemInternal(nextPage, true, true, initialVelocity);

                    mActivePointerId = INVALID_POINTER;
                    endDrag();
                    needsInvalidate = mTopEdge.onRelease() | mBottomEdge.onRelease();
                } /* end of if */
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mIsBeingDragged) {
                    setCurrentItemInternal(mCurItem, true, true);
                    mActivePointerId = INVALID_POINTER;
                    endDrag();
                    needsInvalidate = mTopEdge.onRelease() | mBottomEdge.onRelease();
                } /* end of if */
                break;
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                final int index = MotionEventCompat.getActionIndex(ev);
                final float y = MotionEventCompat.getY(ev, index);
                mLastMotionY = y;
                mActivePointerId = MotionEventCompat.getPointerId(ev, index);
                break;
            } /* end of case */
            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                mLastMotionY = MotionEventCompat.getY(ev,
                        MotionEventCompat.findPointerIndex(ev, mActivePointerId));
                break;
        } /* end of switch */
        if (needsInvalidate) {
            invalidate();
        } /* end of if */
        return true;
    }

    // XXX

    /**
     * @param currentPage current page index
     * @param pageOffset  page
     * @param velocity
     * @param deltaY
     * @return target page
     */
    private int determineTargetPage(int currentPage, float pageOffset, int velocity, int deltaY) {
        int targetPage;
        if (Math.abs(deltaY) > mFlingDistance && Math.abs(velocity) > mMinimumVelocity) {
            targetPage = velocity > 0 ? currentPage : currentPage + 1;
        } else {
            targetPage = (int) (currentPage + pageOffset + 0.5f);
        } /* end of if */

        return targetPage;
    }

    @Override
    public void draw(Canvas canvas) {
        // XXX
        super.draw(canvas);
        boolean needsInvalidate = false;

        final int overScrollMode = ViewCompat.getOverScrollMode(this);
        if (overScrollMode == ViewCompat.OVER_SCROLL_ALWAYS ||
                (overScrollMode == ViewCompat.OVER_SCROLL_IF_CONTENT_SCROLLS &&
                        mAdapter != null && mAdapter.getCount() > 1)) {
            if (!mTopEdge.isFinished()) {
                final int restoreCount = canvas.save();
                final int width = getWidth() - getPaddingLeft() - getPaddingRight();

                canvas.rotate(270);
                canvas.translate(-width + getPaddingLeft(), 0);
                mTopEdge.setSize(width, getHeight());
                needsInvalidate |= mTopEdge.draw(canvas);
                canvas.restoreToCount(restoreCount);
            } /* end of if */
            if (!mBottomEdge.isFinished()) {
                final int restoreCount = canvas.save();
                final int width = getWidth() - getPaddingLeft() - getPaddingRight();
                final int height = getHeight();
                final int itemCount = mAdapter != null ? mAdapter.getCount() : 1;

                canvas.rotate(180);
                canvas.translate(-width + getPaddingLeft(), -itemCount * (height + mPageMargin) + mPageMargin);
                mBottomEdge.setSize(width, height);
                needsInvalidate |= mBottomEdge.draw(canvas);
                canvas.restoreToCount(restoreCount);
            } /* end of if */
        } else {
            mTopEdge.finish();
            mBottomEdge.finish();
        } /* end of if */

        if (needsInvalidate) {
            // Keep animating
            invalidate();
        } /* end of if */
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //XXX
        //Draw the margin drawable if needed.
        if (mPageMargin > 0 && mMarginDrawable != null) {
            final int scrollY = getScrollY();
            final int height = getHeight();
            final int offset = scrollY % (height + mPageMargin);
            if (offset != 0) {
                // Pages fit completely when settled; we only need to draw when in between
                final int top = scrollY - offset + height;
                mMarginDrawable.setBounds(mLeftPageBounds, top, mRightPageBounds, top + mPageMargin);
                mMarginDrawable.draw(canvas);
            } /* end of if */
        } /* end of if */
    }

    /**
     * Start a fake drag of the pager.
     * <p>
     * <p>A fake drag can be useful if you want to synchronize the motion of the ViewPager
     * with the touch scrolling of another view, while still letting the ViewPager
     * control the snapping motion and fling behavior. (e.g. parallax-scrolling tabs.)
     * Call {@link #fakeDragBy(float)} to simulate the actual drag motion. Call
     * {@link #endFakeDrag()} to complete the fake drag and fling as necessary.
     * <p>
     * <p>During a fake drag the ViewPager will ignore all touch events. If a real drag
     * is already in progress, this method will return false.
     *
     * @return true if the fake drag began successfully, false if it could not be started.
     * @see #fakeDragBy(float)
     * @see #endFakeDrag()
     */
    public boolean beginFakeDrag() {
        if (mIsBeingDragged) {
            return false;
        } /* end of if */
        mFakeDragging = true;
        setScrollState(SCROLL_STATE_DRAGGING);
        // XXX
        mInitialMotionY = mLastMotionY = 0;
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        } /* end of if */
        final long time = SystemClock.uptimeMillis();
        final MotionEvent ev = MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, 0, 0, 0);
        mVelocityTracker.addMovement(ev);
        ev.recycle();
        mFakeDragBeginTime = time;
        return true;
    }

    /**
     * End a fake drag of the pager.
     *
     * @see #beginFakeDrag()
     * @see #fakeDragBy(float)
     */
    public void endFakeDrag() {
        //XXX
        if (!mFakeDragging) {
            throw new IllegalStateException("No fake drag in progress. Call beginFakeDrag first.");
        } /* end of if */

        final VelocityTracker velocityTracker = mVelocityTracker;
        velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
        int initialVelocity = (int) VelocityTrackerCompat.getXVelocity(
                velocityTracker, mActivePointerId);
        mPopulatePending = true;
        final int totalDelta = (int) (mLastMotionY - mInitialMotionY);
        final int scrollY = getScrollY();
        final int heightWithMargin = getHeight() + mPageMargin;
        final int currentPage = scrollY / heightWithMargin;
        final float pageOffset = (float) (scrollY % heightWithMargin) / heightWithMargin;
        int nextPage = determineTargetPage(currentPage, pageOffset, initialVelocity, totalDelta);
        setCurrentItemInternal(nextPage, true, true, initialVelocity);
        endDrag();

        mFakeDragging = false;
    }


    // XXX

    /**
     * Fake drag by an offset in pixels. You must have called {@link #beginFakeDrag()} first.
     *
     * @param yOffset Offset in pixels to drag by.
     * @see #beginFakeDrag()
     * @see #endFakeDrag()
     */
    public void fakeDragBy(float yOffset) {
        if (!mFakeDragging) {
            throw new IllegalStateException("No fake drag in progress. Call beginFakeDrag first.");
        } /* end of if */

        mLastMotionY += yOffset;
        float scrollY = getScrollY() - yOffset;
        final int height = getHeight();
        final int heightWithMargin = height + mPageMargin;

        final float topBound = Math.max(0, (mCurItem - 1) * heightWithMargin);
        final float bottomBound =
                Math.min(mCurItem + 1, mAdapter.getCount() - 1) * heightWithMargin;
        if (scrollY < topBound) {
            scrollY = topBound;
        } else if (scrollY > bottomBound) {
            scrollY = bottomBound;
        } /* end of if */
        // Don't lose the rounded component
        mLastMotionY += scrollY - (int) scrollY;
        scrollTo(getScrollX(), (int) scrollY);
        pageScrolled((int) scrollY);

        // Synthesize an event for the VelocityTracker.
        final long time = SystemClock.uptimeMillis();
        final MotionEvent ev = MotionEvent.obtain(mFakeDragBeginTime, time, MotionEvent.ACTION_MOVE,
                0, mLastMotionY, 0);
        mVelocityTracker.addMovement(ev);
        ev.recycle();
    }

    /**
     * Returns true if a fake drag is in progress.
     *
     * @return true if currently in a fake drag, false otherwise.
     * @see #beginFakeDrag()
     * @see #fakeDragBy(float)
     * @see #endFakeDrag()
     */
    public boolean isFakeDragging() {
        return mFakeDragging;
    }

    // XXX

    /**
     * on secondary pointer up
     * This was our active pointer going up. Choose a new active pointer and adjust accordingly.
     *
     * @param ev MotionEvent
     */
    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mLastMotionY = MotionEventCompat.getY(ev, newPointerIndex);
            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
            if (mVelocityTracker != null) {
                mVelocityTracker.clear();
            } /* end of if */
        } /* end of if */
    }

    /**
     * end drag
     */
    private void endDrag() {
        mIsBeingDragged = false;
        mIsUnableToDrag = false;

        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        } /* end of if */
    }

    /**
     * ScrollingCacheEnabled
     *
     * @param enabled enabled or disabled
     */
    private void setScrollingCacheEnabled(boolean enabled) {
        if (mScrollingCacheEnabled != enabled) {
            mScrollingCacheEnabled = enabled;
            if (USE_CACHE) {
                final int size = getChildCount();
                for (int i = 0; i < size; ++i) {
                    final View child = getChildAt(i);
                    if (child.getVisibility() != GONE) {
                        child.setDrawingCacheEnabled(enabled);
                    } /* end of if */
                } /* end of for */
            } /* end of if */
        } /* end of if */
    }

    //XXX

    /**
     * Tests scrollability within child views of v given a delta of dy.
     *
     * @param v      View to test for vertical scrollability
     * @param checkV Whether the view v passed should itself be checked for scrollability (true),
     *               or just its children (false).
     * @param dy     Delta scrolled in pixels
     * @param x      X coordinate of the active touch point
     * @param y      Y coordinate of the active touch point
     * @return true if child views of v can be scrolled by delta of dx.
     */
    protected boolean canScroll(View v, boolean checkV, int dy, int x, int y) {
        if (v instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) v;
            final int scrollX = v.getScrollX();
            final int scrollY = v.getScrollY();
            final int count = group.getChildCount();
            // Count backwards - let topmost views consume scroll distance first.
            for (int i = count - 1; i >= 0; i--) {
                // This will not work for transformed views in Honeycomb+
                final View child = group.getChildAt(i);
                if (x + scrollX >= child.getLeft() && x + scrollX < child.getRight() &&
                        y + scrollY >= child.getTop() && y + scrollY < child.getBottom() &&
                        canScroll(child, true, dy, x + scrollX - child.getLeft(),
                                y + scrollY - child.getTop())) {
                    return true;
                } /* end of if */
            } /* end of for */
        } /* end of if */

        return checkV && ViewCompat.canScrollVertically(v, -dy);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Let the focused view and/or our descendants get the key first
        return super.dispatchKeyEvent(event) || executeKeyEvent(event);
    }


    //XXX

    /**
     * You can call this function yourself to have the scroll view perform
     * scrolling from a key event, just as if the event had been dispatched to
     * it by the view hierarchy.
     *
     * @param event The key event to execute.
     * @return Return true if the event was handled, else false.
     */
    public boolean executeKeyEvent(KeyEvent event) {
        boolean handled = false;
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    handled = arrowScroll(FOCUS_UP);
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    handled = arrowScroll(FOCUS_DOWN);
                    break;
                case KeyEvent.KEYCODE_TAB:
                    if (Build.VERSION.SDK_INT >= 11) {
                        // The focus finder had a bug handling FOCUS_FORWARD and FOCUS_BACKWARD
                        // before Android 3.0. Ignore the tab key on those devices.
                        if (KeyEventCompat.hasNoModifiers(event)) {
                            handled = arrowScroll(FOCUS_FORWARD);
                        } else if (KeyEventCompat.hasModifiers(event, KeyEvent.META_SHIFT_ON)) {
                            handled = arrowScroll(FOCUS_BACKWARD);
                        } /* end of if */
                    } /* end of if */
                    break;
            } /* end of switch */
        } /* end of if */
        return handled;
    }

    //XXX

    /**
     * Page keypad
     *
     * @param direction
     * @return handled
     */
    public boolean arrowScroll(int direction) {
        View currentFocused = findFocus();
        if (currentFocused == this) currentFocused = null;

        boolean handled = false;

        View nextFocused = FocusFinder.getInstance().findNextFocus(this, currentFocused,
                direction);
        if (nextFocused != null && nextFocused != currentFocused) {
            if (direction == View.FOCUS_UP) {
                // If there is nothing to the left, or this is causing us to
                // jump to the down, then what we really want to do is page up.
                if (currentFocused != null && nextFocused.getTop() >= currentFocused.getTop()) {
                    handled = pageUp();
                } else {
                    handled = nextFocused.requestFocus();
                } /* end of if */
            } else if (direction == View.FOCUS_DOWN) {
                // If there is nothing to the right, or this is causing us to
                // jump to the left, then what we really want to do is page right.
                if (currentFocused != null && nextFocused.getTop() <= currentFocused.getTop()) {
                    handled = pageDown();
                } else {
                    handled = nextFocused.requestFocus();
                } /* end of if */
            } /* end of if */
        } else if (direction == FOCUS_UP || direction == FOCUS_BACKWARD) {
            // Trying to move left and nothing there; try to page.
            handled = pageUp();
        } else if (direction == FOCUS_DOWN || direction == FOCUS_FORWARD) {
            // Trying to move right and nothing there; try to page.
            handled = pageDown();
        } /* end of if */
        if (handled) {
            playSoundEffect(SoundEffectConstants.getContantForFocusDirection(direction));
        } /* end of if */
        return handled;
    }

    //XXX
    boolean pageUp() {
        if (mCurItem > 0) {
            setCurrentItem(mCurItem - 1, true);
            return true;
        } /* end of if */
        return false;
    }

    //XXX
    boolean pageDown() {
        if (mAdapter != null && mCurItem < (mAdapter.getCount() - 1)) {
            setCurrentItem(mCurItem + 1, true);
            return true;
        } /* end of if */
        return false;
    }

    /**
     * We only want the current page that is being shown to be focusable.
     */
    @Override
    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        final int focusableCount = views.size();

        final int descendantFocusability = getDescendantFocusability();

        if (descendantFocusability != FOCUS_BLOCK_DESCENDANTS) {
            for (int i = 0; i < getChildCount(); i++) {
                final View child = getChildAt(i);
                if (child.getVisibility() == VISIBLE) {
                    ItemInfo ii = infoForChild(child);
                    if (ii != null && ii.position == mCurItem) {
                        child.addFocusables(views, direction, focusableMode);
                    } /* end of if */
                } /* end of if */
            } /* end of for */
        } /* end of if */

        // we add ourselves (if focusable) in all cases except for when we are
        // FOCUS_AFTER_DESCENDANTS and there are some descendants focusable.  this is
        // to avoid the focus search finding layouts when a more precise search
        // among the focusable children would be more interesting.
        if (
                descendantFocusability != FOCUS_AFTER_DESCENDANTS ||
                        // No focusable descendants
                        (focusableCount == views.size())) {
            // Note that we can't call the superclass here, because it will
            // add all views in.  So we need to do the same thing View does.
            if (!isFocusable()) {
                return;
            } /* end of if */
            if ((focusableMode & FOCUSABLES_TOUCH_MODE) == FOCUSABLES_TOUCH_MODE &&
                    isInTouchMode() && !isFocusableInTouchMode()) {
                return;
            } /* end of if */
            if (views != null) {
                views.add(this);
            } /* end of if */
        } /* end of if */
    }

    /**
     * We only want the current page that is being shown to be touchable.
     */
    @Override
    public void addTouchables(ArrayList<View> views) {
        // Note that we don't call super.addTouchables(), which means that
        // we don't call View.addTouchables().  This is okay because a ViewPager
        // is itself not touchable.
        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == VISIBLE) {
                ItemInfo ii = infoForChild(child);
                if (ii != null && ii.position == mCurItem) {
                    child.addTouchables(views);
                } /* end of if */
            } /* end of if */
        } /* end of for */
    }

    /**
     * We only want the current page that is being shown to be focusable.
     */
    @Override
    protected boolean onRequestFocusInDescendants(int direction,
                                                  Rect previouslyFocusedRect) {
        int index;
        int increment;
        int end;
        int count = getChildCount();
        if ((direction & FOCUS_FORWARD) != 0) {
            index = 0;
            increment = 1;
            end = count;
        } else {
            index = count - 1;
            increment = -1;
            end = -1;
        } /* end of if */
        for (int i = index; i != end; i += increment) {
            View child = getChildAt(i);
            if (child.getVisibility() == VISIBLE) {
                ItemInfo ii = infoForChild(child);
                if (ii != null && ii.position == mCurItem) {
                    if (child.requestFocus(direction, previouslyFocusedRect)) {
                        return true;
                    } /* end of if */
                } /* end of if */
            } /* end of if */
        } /* end of for */
        return false;
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        // ViewPagers should only report accessibility info for the current page,
        // otherwise things get very confusing.

        // TODO: Should this note something about the paging container?

        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == VISIBLE) {
                final ItemInfo ii = infoForChild(child);
                if (ii != null && ii.position == mCurItem &&
                        child.dispatchPopulateAccessibilityEvent(event)) {
                    return true;
                } /* end of if */
            } /* end of if */
        } /* end of for */

        return false;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return generateDefaultLayoutParams();
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams && super.checkLayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    private class PagerObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            dataSetChanged();
        }

        @Override
        public void onInvalidated() {
            dataSetChanged();
        }
    }

    /**
     * Layout parameters that should be supplied for views added to a
     * ViewPager.
     */
    public static class LayoutParams extends ViewGroup.LayoutParams {
        /**
         * true if this view is a decoration on the pager itself and not
         * a view supplied by the adapter.
         */
        public boolean isDecor;

        /**
         * Where to position the view page within the overall ViewPager
         * container; constants are defined in {@link Gravity}.
         */
        public int gravity;

        public LayoutParams() {
            super(FILL_PARENT, FILL_PARENT);
        }

        public LayoutParams(Context context, AttributeSet attrs) {
            super(context, attrs);

            final TypedArray a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
            gravity = a.getInteger(0, Gravity.NO_GRAVITY);
            a.recycle();
        }
    }
}
