package com.thirtydegreesray.openhub.ui.adapter;

import android.content.Context;
import androidx.annotation.NonNull;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.R2;
import com.thirtydegreesray.openhub.dao.MyTopic;
import com.thirtydegreesray.openhub.ui.adapter.base.BaseAdapter;
import com.thirtydegreesray.openhub.ui.adapter.base.BaseViewHolder;
import com.thirtydegreesray.openhub.ui.fragment.base.BaseFragment;

import javax.inject.Inject;

import butterknife.BindView;

public class TopicsEditorAdapter extends BaseAdapter<TopicsEditorAdapter.ViewHolder, MyTopic> {

    @Inject
    public TopicsEditorAdapter(Context context, BaseFragment fragment) {
        super(context, fragment);
    }

    @Override
    protected int getLayoutId(int viewType) {
        return R.layout.layout_item_topic_editor;
    }

    @Override
    protected ViewHolder getViewHolder(View itemView, int viewType) {
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        MyTopic topic = data.get(position);
        holder.topicName.setText(topic.getSlug());
        holder.topicCheckbox.setChecked(topic.getSelected());
    }

    class ViewHolder extends BaseViewHolder {
        @BindView(R2.id.topic_name) TextView topicName;
        @BindView(R2.id.topic_checkbox) CheckBox topicCheckbox;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

}
