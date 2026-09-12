package com.thirtydegreesray.openhub.ui.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.Html;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.R2;
import com.thirtydegreesray.openhub.common.GlideApp;
import com.thirtydegreesray.openhub.mvp.model.graphql.DiscussionCategory;
import com.thirtydegreesray.openhub.mvp.model.graphql.DiscussionComment;
import com.thirtydegreesray.openhub.ui.adapter.base.BaseAdapter;
import com.thirtydegreesray.openhub.ui.adapter.base.BaseViewHolder;
import com.thirtydegreesray.openhub.ui.fragment.base.BaseFragment;
import com.thirtydegreesray.openhub.util.PrefUtils;
import com.thirtydegreesray.openhub.util.StringUtils;
import com.thirtydegreesray.openhub.util.ViewUtils;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Row 0 is always the discussion's own body (see
 * DiscussionComment.forHeader()/isHeader()) - shown with its title/category
 * visible; every other row is a real comment, with a reply
 * (DiscussionComment.isReply()) given extra start padding as its only visual
 * distinction from a top-level comment (GitHub discussions only nest one
 * level deep).
 */
public class DiscussionCommentsAdapter extends BaseAdapter<DiscussionCommentsAdapter.ViewHolder, DiscussionComment> {

    private OnUpvoteClickListener upvoteClickListener;

    @Inject
    public DiscussionCommentsAdapter(Context context, BaseFragment fragment) {
        super(context, fragment);
    }

    public void setUpvoteClickListener(OnUpvoteClickListener listener) {
        this.upvoteClickListener = listener;
    }

    @Override
    protected int getLayoutId(int viewType) {
        return R.layout.layout_item_discussion_comment;
    }

    @NonNull
    @Override
    protected ViewHolder getViewHolder(@NonNull View itemView, int viewType) {
        return new ViewHolder(itemView);
    }

    public class ViewHolder extends BaseViewHolder {
        @BindView(R2.id.lay_root) View layRoot;
        @BindView(R2.id.iv_user_avatar) ImageView ivUserAvatar;
        @BindView(R2.id.tv_discussion_title) TextView tvDiscussionTitle;
        @BindView(R2.id.tv_category) TextView tvCategory;
        @BindView(R2.id.tv_owner_name) TextView tvOwnerName;
        @BindView(R2.id.tv_updated_at) TextView tvUpdatedAt;
        @BindView(R2.id.tv_comment_body) TextView tvCommentBody;
        @BindView(R2.id.iv_upvote) ImageView ivUpvote;
        @BindView(R2.id.tv_upvote_count) TextView tvUpvoteCount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        @OnClick(R2.id.lay_upvote)
        public void onUpvoteClick() {
            if (upvoteClickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                upvoteClickListener.onUpvoteClick(getAdapterPosition());
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        DiscussionComment comment = data.get(position);

        if (comment.isHeader()) {
            holder.tvDiscussionTitle.setVisibility(View.VISIBLE);
            holder.tvDiscussionTitle.setText(comment.getDiscussionTitle());
            DiscussionCategory category = comment.getDiscussionCategory();
            if (category != null) {
                holder.tvCategory.setVisibility(View.VISIBLE);
                String emoji = StringUtils.isBlank(category.getEmoji()) ? "" : category.getEmoji().concat(" ");
                holder.tvCategory.setText(emoji.concat(category.getName()));
            } else {
                holder.tvCategory.setVisibility(View.GONE);
            }
        } else {
            holder.tvDiscussionTitle.setVisibility(View.GONE);
            holder.tvCategory.setVisibility(View.GONE);
        }

        // Computed fresh from resources (not read back off the recycled
        // view's current padding) so it stays correct no matter how many
        // times this ViewHolder gets reused for a differently-indented row.
        int basePadding = context.getResources().getDimensionPixelSize(R.dimen.spacing_normal);
        int indent = comment.isReply() ?
                context.getResources().getDimensionPixelSize(R.dimen.spacing_x_large) : 0;
        holder.layRoot.setPadding(basePadding + indent, holder.layRoot.getPaddingTop(),
                holder.layRoot.getPaddingRight(), holder.layRoot.getPaddingBottom());

        if (!StringUtils.isBlank(comment.getBodyHTML())) {
            holder.tvCommentBody.setText(Html.fromHtml(comment.getBodyHTML()));
        }
        if (comment.getCreatedAt() != null) {
            holder.tvUpdatedAt.setText(StringUtils.getNewsTimeStr(context, comment.getCreatedAt()));
        }
        if (comment.getAuthor() != null) {
            holder.tvOwnerName.setText(comment.getAuthor().getLogin());
            GlideApp.with(fragment)
                    .load(comment.getAuthor().getAvatarUrl())
                    .onlyRetrieveFromCache(!PrefUtils.isLoadImageEnable())
                    .into(holder.ivUserAvatar);
        }

        holder.tvUpvoteCount.setText(String.valueOf(comment.getUpvoteCount()));
        int tintColor = comment.isViewerHasUpvoted() ?
                ViewUtils.getAccentColor(context) : ViewUtils.getSecondaryTextColor(context);
        ColorStateList tint = ColorStateList.valueOf(tintColor);
        holder.ivUpvote.setImageTintList(tint);
        holder.tvUpvoteCount.setTextColor(tintColor);
    }

    public interface OnUpvoteClickListener {
        void onUpvoteClick(int position);
    }
}
