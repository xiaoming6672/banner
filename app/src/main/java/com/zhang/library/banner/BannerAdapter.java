package com.zhang.library.banner;

import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.zhang.library.adapter.BaseRecyclerAdapter;
import com.zhang.library.adapter.viewholder.base.BaseRecyclerViewHolder;

/**
 * @author ZhangXiaoMing 2021-09-12 20:58 星期日
 */
public class BannerAdapter extends BaseRecyclerAdapter<String> {

    @Override
    protected BaseRecyclerViewHolder<String> onCreateVHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    protected void onBindData(BaseRecyclerViewHolder<String> viewHolder, String data, int position) {
    }

    private static final class ViewHolder extends BaseRecyclerViewHolder<String> {

        private TextView tvText;

        public ViewHolder(@NonNull ViewGroup parent) {
            super(parent, R.layout.item_baner);
        }

        @Override
        public void onInit() {
            tvText = itemView.findViewById(R.id.tv_text);
        }

        @Override
        public void onBindData(String item, int position) {
            tvText.setText(item);
        }
    }

}
