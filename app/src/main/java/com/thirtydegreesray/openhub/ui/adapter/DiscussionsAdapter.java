package com.thirtydegreesray.openhub.ui.adapter;

import android.content.Context;
import androidx.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.R2;
import com.thirtydegreesray.openhub.common.GlideApp;
import com.thirtydegreesray.openhub.mvp.model.graphql.Discussion;
import com.thirtydegreesray.openhub.mvp.model.graphql.DiscussionCategory;
import com.thirtydegreesray.openhub.ui.adapter.base.BaseAdapter;
import com.thirtydegreesray.openhub.ui.adapter.base.BaseViewHolder;
import com.thirtydegreesray.openhub.ui.fragment.base.BaseFragment;
import com.thirtydegreesray.openhub.util.PrefUtils;
import com.thirtydegreesray.openhub.util.StringUtils;

import javax.inject.Inject;

import butterknife.BindView;

public class DiscussionsAdapter extends BaseAdapter<DiscussionsAdapter.ViewHolder, Discussion> {

    @Inject
    public DiscussionsAdapter(Context context, BaseFragment fragment) {
        super(context, fragment);
    }

    @Override
    protected int getLayoutId(int viewType) {
        return R.layout.layout_item_discussion;
    }

    @NonNull
    @Override
    protected ViewHolder getViewHolder(@NonNull View itemView, int viewType) {
        return new ViewHolder(itemView);
    }

    public class ViewHolder extends BaseViewHolder {

        @BindView(R2.id.iv_user_avatar) ImageView ivUserAvatar;
        @BindView(R2.id.tv_discussion_title) TextView tvDiscussionTitle;
        @BindView(R2.id.tv_category) TextView tvCategory;
        @BindView(R2.id.tv_owner_name) TextView tvOwnerName;
        @BindView(R2.id.tv_comments_num) TextView tvCommentsNum;
        @BindView(R2.id.tv_updated_at) TextView tvUpdatedAt;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        Discussion discussion = data.get(position);
        holder.tvDiscussionTitle.setText(discussion.getTitle());
        holder.tvCommentsNum.setText(String.valueOf(discussion.getCommentsCount()));
        if (discussion.getUpdatedAt() != null) {
            holder.tvUpdatedAt.setText(StringUtils.getNewsTimeStr(context, discussion.getUpdatedAt()));
        }

        DiscussionCategory category = discussion.getCategory();
        if (category != null) {
            String emoji = StringUtils.isBlank(category.getEmoji()) ? "" : category.getEmoji().concat(" ");
            holder.tvCategory.setText(emoji.concat(category.getName()));
        }

        if (discussion.getAuthor() != null) {
            holder.tvOwnerName.setText(discussion.getAuthor().getLogin());
            GlideApp.with(fragment)
                    .load(discussion.getAuthor().getAvatarUrl())
                    .onlyRetrieveFromCache(!PrefUtils.isLoadImageEnable())
                    .into(holder.ivUserAvatar);
        }
    }
}
