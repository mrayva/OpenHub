

package com.thirtydegreesray.openhub.ui.adapter;

import android.content.Context;
import androidx.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.R2;
import com.thirtydegreesray.openhub.common.GlideApp;
import com.thirtydegreesray.openhub.mvp.model.GistComment;
import com.thirtydegreesray.openhub.ui.adapter.base.BaseAdapter;
import com.thirtydegreesray.openhub.ui.adapter.base.BaseViewHolder;
import com.thirtydegreesray.openhub.ui.fragment.base.BaseFragment;
import com.thirtydegreesray.openhub.util.PrefUtils;
import com.thirtydegreesray.openhub.util.StringUtils;

import javax.inject.Inject;

import butterknife.BindView;

/**
 * Comment body is shown as plain (unrendered) markdown text this phase -
 * getGistComments() doesn't request GitHub's rendered-HTML preview media
 * type, so there's no HTML body to feed a WebView the way issue comments do.
 */
public class GistCommentsAdapter extends BaseAdapter<GistCommentsAdapter.ViewHolder, GistComment> {

    @Inject
    public GistCommentsAdapter(Context context, BaseFragment fragment) {
        super(context, fragment);
    }

    @Override
    protected int getLayoutId(int viewType) {
        return R.layout.layout_item_gist_comment;
    }

    @NonNull
    @Override
    protected ViewHolder getViewHolder(@NonNull View itemView, int viewType) {
        return new ViewHolder(itemView);
    }

    public class ViewHolder extends BaseViewHolder {
        @BindView(R2.id.iv_user_avatar) ImageView ivUserAvatar;
        @BindView(R2.id.tv_owner_name) TextView tvOwnerName;
        @BindView(R2.id.tv_updated_at) TextView tvUpdatedAt;
        @BindView(R2.id.tv_comment_body) TextView tvCommentBody;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        GistComment comment = data.get(position);
        holder.tvCommentBody.setText(comment.getBody());
        if (comment.getCreatedAt() != null) {
            holder.tvUpdatedAt.setText(StringUtils.getNewsTimeStr(context, comment.getCreatedAt()));
        }
        if (comment.getUser() != null) {
            holder.tvOwnerName.setText(comment.getUser().getLogin());
            GlideApp.with(fragment)
                    .load(comment.getUser().getAvatarUrl())
                    .onlyRetrieveFromCache(!PrefUtils.isLoadImageEnable())
                    .into(holder.ivUserAvatar);
        }
    }
}
