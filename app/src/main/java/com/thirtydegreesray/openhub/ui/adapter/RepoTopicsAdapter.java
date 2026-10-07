package com.thirtydegreesray.openhub.ui.adapter;

import android.content.Context;
import androidx.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.R2;
import com.thirtydegreesray.openhub.ui.adapter.base.BaseAdapter;
import com.thirtydegreesray.openhub.ui.adapter.base.BaseViewHolder;
import com.thirtydegreesray.openhub.ui.fragment.base.BaseFragment;
import com.thirtydegreesray.openhub.util.MyTopicHelper;

import butterknife.BindView;

/**
 * Created by ThirtyDegreesRay on 2017/12/29 11:11:24
 */

public class RepoTopicsAdapter extends BaseAdapter<RepoTopicsAdapter.ViewHolder, String> {

    public RepoTopicsAdapter(Context context, BaseFragment fragment) {
        super(context, fragment);
    }

    @Override
    protected int getLayoutId(int viewType) {
        return R.layout.layout_item_repo_topic;
    }

    @Override
    protected ViewHolder getViewHolder(View itemView, int viewType) {
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        String slug = data.get(position);
        holder.topicName.setText(slug);
        holder.ivMyTopic.setVisibility(MyTopicHelper.isMyTopic(slug) ? View.VISIBLE : View.GONE);
    }

    class ViewHolder extends BaseViewHolder {
        @BindView(R2.id.topic_name) TextView topicName;
        @BindView(R2.id.iv_my_topic) ImageView ivMyTopic;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

}
