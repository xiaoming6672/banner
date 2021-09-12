package com.zhang.lib.banner.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.zhang.lib.banner.R;

import java.lang.ref.WeakReference;

/**
 * Banner
 *
 * @author ZhangXiaoMing 2021-09-11 00:14 星期六
 */
public class XMBannerView extends FrameLayout {

    private static final int DEFAULT_DURATION = 1000;

    /** 用ViewPager2实现Banner轮播，ViewPager2方向可由android:orientation声明，或者方法设置 */
    private ViewPager2 mVpgBanner;

    /** 自动开始轮播 */
    private boolean autoStart;
    /** 轮播间隔时间 */
    private int mGapInterval;
    /** 适配器包裹层 */
    private XMAdapterWrapper mAdapterWrapper;
    private XMBannerRunnable mRunnable;
    /** Banner轮播运行中 */
    private boolean isRunning;

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

    private void init(@NonNull Context context, @Nullable AttributeSet attrs) {
        initAttributeSet(context, attrs);

        mVpgBanner = new ViewPager2(context, attrs);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mVpgBanner.setLayoutParams(params);
        this.addView(mVpgBanner);

        mVpgBanner.setAdapter(getAdapterWrapper());
        mVpgBanner.registerOnPageChangeCallback(mOnPageChangeCallback);

        if (autoStart)
            start();
    }

    private void initAttributeSet(@NonNull Context context, @Nullable AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.XMBannerView);

        mGapInterval = a.getInteger(R.styleable.XMBannerView_gapInterval, DEFAULT_DURATION);
        autoStart = a.getBoolean(R.styleable.XMBannerView_autoStart, true);

        a.recycle();
    }

    public void setOrientation(@ViewPager2.Orientation int orientation) {
        mVpgBanner.setOrientation(orientation);
    }

    @ViewPager2.Orientation
    public int getOrientation() {
        return mVpgBanner.getOrientation();
    }

    public int getGapInterval() {
        return mGapInterval;
    }

    public void setGapInterval(int mDuration) {
        this.mGapInterval = mDuration;
    }

    public void setAdapter(RecyclerView.Adapter adapter) {
        getAdapterWrapper().setAdapter(adapter);
    }

    public void registerOnPageChangeCallback(ViewPager2.OnPageChangeCallback callback) {
        mVpgBanner.registerOnPageChangeCallback(callback);
    }

    public void unregisterOnPageChangeCallback(ViewPager2.OnPageChangeCallback callback) {
        mVpgBanner.unregisterOnPageChangeCallback(callback);
    }

    public void stop() {
        if (!isRunning)
            return;

        removeCallbacks(getRunnable());
        isRunning = false;
    }

    public void start() {
        if (isRunning)
            return;

        stop();

        postDelayed(getRunnable(), mGapInterval);
        isRunning = true;
    }

    public boolean isRunning() {
        return isRunning;
    }

    /** 销毁，如果有添加注册{@link ViewPager2.OnPageChangeCallback}，须自行在调用本方法前注销 */
    public void destroy() {
        if (mRunnable != null) {
            stop();
            mRunnable.destroy();
            mRunnable = null;
        }

        mVpgBanner.unregisterOnPageChangeCallback(mOnPageChangeCallback);
    }

    private XMBannerRunnable getRunnable() {
        if (mRunnable == null) {
            mRunnable = new XMBannerRunnable(this);
        }

        return mRunnable;
    }

    private XMAdapterWrapper getAdapterWrapper() {
        if (mAdapterWrapper == null) {
            mAdapterWrapper = new XMAdapterWrapper();
        }

        return mAdapterWrapper;
    }

    private final ViewPager2.OnPageChangeCallback mOnPageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);
        }
    };


    private static final class XMBannerRunnable implements Runnable {

        private WeakReference<XMBannerView> mReference;

        public XMBannerRunnable(XMBannerView view) {
            this.mReference = new WeakReference<>(view);
        }

        public void destroy() {
            if (mReference != null) {
                mReference.clear();
                mReference = null;
            }
        }

        @Override
        public void run() {
            if (mReference == null || mReference.get() == null)
                return;

            XMBannerView bannerView = mReference.get();
            if (bannerView.getAdapterWrapper().getItemCount() <= 1) {
                bannerView.postDelayed(bannerView.getRunnable(), bannerView.getGapInterval());
                return;
            }

            try {
                int currentItem = bannerView.mVpgBanner.getCurrentItem();
                bannerView.mVpgBanner.setCurrentItem(currentItem + 1);
            } catch (Exception e) {
                e.printStackTrace();
            }

            bannerView.postDelayed(bannerView.getRunnable(), bannerView.getGapInterval());
        }
    }

    private static final class XMAdapterWrapper extends RecyclerView.Adapter {

        private static final String TAG = XMAdapterWrapper.class.getSimpleName();

        private RecyclerView.Adapter mAdapter;

        public void setAdapter(RecyclerView.Adapter adapter) {
            this.mAdapter = adapter;

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

        public RecyclerView.Adapter getAdapter() {
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
