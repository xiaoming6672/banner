package com.zhang.lib.banner.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.zhang.lib.banner.R;

/**
 * Banner控件，数据只需要用RecyclerView.Adapter来控制数据列表显示
 *
 * <br>可以通过设置{#XMBannerView_gapInterval}来控制Banner每次切换的间隔时间
 * <br>可以通过设置{#XMBannerView_scrollDuration}来控制Banner每次切换时候的消耗时间，可以视为切换的速率
 * <br>可以通过设置{#XMBannerView_autoStart}来控制是否自动开始轮播
 * <br>可以通过设置{#android_orientation}来控制Banner列表的方向
 * <p>
 * Tips:Adapter的ItemView的宽高必须是match_parent，否则会报错
 *
 * @author ZhangXiaoMing 2021-09-11 00:14 星期六
 */
public class XMBannerView extends FrameLayout implements Runnable {

    protected final String TAG = getClass().getSimpleName();

    /** 默认轮播间隔，单位：毫秒 */
    private static final int DEFAULT_GAP_INTERVAL = 3000;
    /** 默认轮播滑动耗时，单位：毫秒 */
    private static final int DEFAULT_SCROLL_DURATION = 500;
    /** 一个完整的Item的滚动，分成若干次的滑动，每次滑动的时间间隔定义，单位：毫秒 */
    private static final int PER_SCROLL_TIME_MILLIS = 10;

    /** 用RecyclerView实现Banner轮播，RecyclerView方向可由android:orientation声明，或者方法设置 */
    private RecyclerView mBanner;

    /** 当前轮播位置 */
    private int mCurrentIndex;
    /** 自动开始轮播 */
    private boolean autoStart;
    /** 轮播间隔时间，单位：毫秒 */
    private int mGapInterval;
    /** 轮播滑动耗时，单位：毫秒 */
    private int mScrollDuration;
    /** 适配器包裹层 */
    private XMAdapterWrapper mAdapterWrapper;
    /** Banner轮播运行中 */
    private boolean isRunning;
    /** Banner轮播切换滑动中 */
    private boolean isWorking;
    /** Banner列表方向 */
    private int mOrientation;
    /** Banner轮播开始滑动时候已滑动的距离 */
    private int mHasScrolledDistance;


    public XMBannerView(@NonNull Context context) {
        this(context, null);
    }

    public XMBannerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public XMBannerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context, attrs);
    }

    /** 初始化属性 */
    private void initAttributeSet(@NonNull Context context, @Nullable AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.XMBannerView);

        mGapInterval = a.getInteger(R.styleable.XMBannerView_gapInterval, DEFAULT_GAP_INTERVAL);
        mGapInterval = DEFAULT_GAP_INTERVAL;
        mScrollDuration = DEFAULT_SCROLL_DURATION;
        autoStart = a.getBoolean(R.styleable.XMBannerView_autoStart, true);
        mOrientation = a.getInteger(R.styleable.XMBannerView_android_orientation, RecyclerView.HORIZONTAL);

        a.recycle();
    }

    /** 初始化 */
    private void init(@NonNull Context context, @Nullable AttributeSet attrs) {
        initAttributeSet(context, attrs);

        mBanner = new RecyclerView(context, attrs);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mBanner.setLayoutParams(params);
        this.addView(mBanner);

        mBanner.setLayoutManager(new LinearLayoutManager(context, mOrientation, false));
//        mBanner.registerOnPageChangeCallback(mOnPageChangeCallback);
        mBanner.addOnScrollListener(createOnScrollListener());
        mBanner.addOnChildAttachStateChangeListener(createChildAttachStateChangeListener());
        mBanner.setAdapter(getAdapterWrapper());


        if (autoStart)
            start();
    }

    /**
     * 设置Banner列表方向
     *
     * @param orientation 列表方向
     */
    public void setOrientation(@RecyclerView.Orientation int orientation) {
        if (mBanner.getLayoutManager() == null)
            return;

        ((LinearLayoutManager) mBanner.getLayoutManager()).setOrientation(orientation);
    }

    /** 获取Banner列表方向 */
    @RecyclerView.Orientation
    public int getOrientation() {
        return ((LinearLayoutManager) mBanner.getLayoutManager()).getOrientation();
    }

    /** 获取Banner轮播间隔时间，单位：毫秒 */
    public int getGapInterval() {
        return mGapInterval;
    }

    /** 设置轮播间隔时间，单位：毫秒 */
    public void setGapInterval(int mDuration) {
        this.mGapInterval = mDuration;
    }

    /** 获取轮播滑动耗时，单位：毫秒 */
    public int getScrollDuration() {
        return mScrollDuration;
    }

    /** 设置轮播滑动耗时，单位：毫秒 */
    public void setScrollDuration(int mScrollDuration) {
        this.mScrollDuration = mScrollDuration;
    }

    /** 设置数据适配器 */
    public void setAdapter(RecyclerView.Adapter<?> adapter) {
        getAdapterWrapper().setAdapter(adapter);
        mBanner.scrollToPosition(0);
        mCurrentIndex = 0;
    }

    /** 停止轮播 */
    public void stop() {
        if (!isRunning)
            return;

        removeCallbacks(this);
        isRunning = false;
    }

    /** 开始轮播 */
    public void start() {
        if (isRunning)
            return;

        stop();

        postDelayed(this, mGapInterval);
        isRunning = true;
    }

    /** 轮播是否正在进行中 */
    public boolean isRunning() {
        return isRunning;
    }

    /** 销毁 */
    public void destroy() {
        mBanner.clearOnChildAttachStateChangeListeners();
        mBanner.removeOnScrollListener(createOnScrollListener());
    }

    /** 滑动监听 */
    private RecyclerView.OnScrollListener createOnScrollListener() {
        return new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (newState == RecyclerView.SCROLL_STATE_DRAGGING)
                    removeCallbacks(XMBannerView.this);

                if (newState == RecyclerView.SCROLL_STATE_DRAGGING || recyclerView.getLayoutManager() == null)
                    return;

                LinearLayoutManager manager = (LinearLayoutManager) recyclerView.getLayoutManager();

                if (newState == RecyclerView.SCROLL_STATE_SETTLING) {
                    recyclerView.removeOnScrollListener(this);
                    recyclerView.stopScroll();
                    removeCallbacks(XMBannerView.this);

                    if (manager.findFirstCompletelyVisibleItemPosition() != RecyclerView.NO_POSITION)
                        return;

                    int position = manager.findFirstVisibleItemPosition();

                    View previousView = manager.findViewByPosition(position);

                    boolean isHorizontal = getOrientation() == RecyclerView.HORIZONTAL;

                    if (isHorizontal) {
                        if (previousView.getRight() < getMeasuredWidth() * 0.45)
                            recyclerView.smoothScrollToPosition(mCurrentIndex = position + 1);
                        else
                            recyclerView.smoothScrollToPosition(mCurrentIndex = position);
                    } else {
                        if (previousView.getBottom() < getMeasuredHeight() * 0.45)
                            recyclerView.smoothScrollToPosition(mCurrentIndex = position + 1);
                        else
                            recyclerView.smoothScrollToPosition(mCurrentIndex = position);
                    }

                    recyclerView.addOnScrollListener(this);
                    if (isWorking)
                        post(XMBannerView.this);
                    else
                        postDelayed(XMBannerView.this, getGapInterval());

                    return;
                }


                int position = manager.findLastCompletelyVisibleItemPosition();
                if (position == RecyclerView.NO_POSITION)
                    position = mCurrentIndex;
                else
                    mCurrentIndex = position;

                recyclerView.smoothScrollToPosition(position);

                if (isWorking)
                    post(XMBannerView.this);
                else
                    postDelayed(XMBannerView.this, getGapInterval());

            }
        };
    }

    /** 监听ItemView的宽高是否都设置match_parent */
    private RecyclerView.OnChildAttachStateChangeListener createChildAttachStateChangeListener() {
        return new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull View view) {
                RecyclerView.LayoutParams layoutParams =
                        (RecyclerView.LayoutParams) view.getLayoutParams();
                if (layoutParams.width != ViewGroup.LayoutParams.MATCH_PARENT
                        || layoutParams.height != ViewGroup.LayoutParams.MATCH_PARENT) {
                    throw new IllegalStateException(
                            "Pages must fill the whole XMBannerView (use match_parent)");
                }
            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull View view) {
            }
        };
    }


    private XMAdapterWrapper getAdapterWrapper() {
        if (mAdapterWrapper == null) {
            mAdapterWrapper = new XMAdapterWrapper();
        }

        return mAdapterWrapper;
    }


    @Override
    public void run() {
        if (getAdapterWrapper().getItemCount() <= 1) {
            postDelayed(this, getGapInterval());
            return;
        }

        boolean isHorizontal = getOrientation() == RecyclerView.HORIZONTAL;

        int needScrollDistance = isHorizontal ? mBanner.getMeasuredWidth() : mBanner.getMeasuredHeight();

        //每10毫秒滑动一次，一整个Item移动完，需要移动的次数
        int needScrollCount = getScrollDuration() / PER_SCROLL_TIME_MILLIS;
        int perScrollDistance = needScrollDistance / needScrollCount;

        isWorking = true;
        if (isHorizontal)
            mBanner.scrollBy(perScrollDistance, 0);
        else
            mBanner.scrollBy(0, perScrollDistance);

        mHasScrolledDistance += perScrollDistance;
        if (mHasScrolledDistance < needScrollDistance) {
            postDelayed(this, PER_SCROLL_TIME_MILLIS);
        } else {
            postDelayed(this, getGapInterval());
            mHasScrolledDistance = 0;
            mCurrentIndex++;
            isWorking = false;
        }
    }

    private static final class XMAdapterWrapper extends RecyclerView.Adapter {

        private static final String TAG = XMAdapterWrapper.class.getSimpleName();

        private RecyclerView.Adapter mAdapter;

        public void setAdapter(RecyclerView.Adapter adapter) {
            this.mAdapter = adapter;

            if (mAdapter == null)
                return;

            mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                @Override
                public void onChanged() {
                    Log.i(TAG, "registerAdapterDataObserver>>>onChanged()");
                    super.onChanged();

                    notifyDataSetChanged();
                }

                @Override
                public void onItemRangeChanged(int positionStart, int itemCount) {
                    Log.i(TAG, "registerAdapterDataObserver>>>onItemRangeChanged()");
                    super.onItemRangeChanged(positionStart, itemCount);

                    notifyItemRangeChanged(positionStart, itemCount);
                }

                @Override
                public void onItemRangeChanged(int positionStart, int itemCount, @Nullable Object payload) {
                    Log.i(TAG, "registerAdapterDataObserver>>>onItemRangeChanged()");
                    super.onItemRangeChanged(positionStart, itemCount, payload);

                    notifyItemRangeChanged(positionStart, itemCount, payload);
                }

                @Override
                public void onItemRangeInserted(int positionStart, int itemCount) {
                    Log.i(TAG, "registerAdapterDataObserver>>>onItemRangeInserted()");
                    super.onItemRangeInserted(positionStart, itemCount);

                    notifyItemRangeInserted(positionStart, itemCount);
                }

                @Override
                public void onItemRangeRemoved(int positionStart, int itemCount) {
                    Log.i(TAG, "registerAdapterDataObserver>>>onItemRangeRemoved()");
                    super.onItemRangeRemoved(positionStart, itemCount);

                    notifyItemRangeRemoved(positionStart, itemCount);
                }

                @Override
                public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                    Log.i(TAG, "registerAdapterDataObserver>>>onItemRangeMoved()");
                    super.onItemRangeMoved(fromPosition, toPosition, itemCount);

                    notifyItemMoved(fromPosition, toPosition);
                }

            });

            notifyDataSetChanged();
        }

        public RecyclerView.Adapter<?> getAdapter() {
            return mAdapter;
        }

        /** 获取真实的位置 */
        private int getRealPosition(int position) {
            if (mAdapter == null || mAdapter.getItemCount() == 0)
                return position;

            int itemCount = mAdapter.getItemCount();
            return position % itemCount;
        }

        @Override
        public int getItemCount() {
            if (mAdapter == null || mAdapter.getItemCount() == 0)
                return 0;

            int itemCount = mAdapter.getItemCount();
            return itemCount == 1 ? 1 : Integer.MAX_VALUE;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return mAdapter.onCreateViewHolder(parent, viewType);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            mAdapter.onBindViewHolder(holder, getRealPosition(position));
        }

    }

}
