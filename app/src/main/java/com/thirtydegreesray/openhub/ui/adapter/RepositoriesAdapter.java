

package com.thirtydegreesray.openhub.ui.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.R2;
import com.thirtydegreesray.openhub.common.GlideApp;
import com.thirtydegreesray.openhub.mvp.model.Repository;
import com.thirtydegreesray.openhub.ui.activity.ProfileActivity;
import com.thirtydegreesray.openhub.ui.adapter.base.BaseAdapter;
import com.thirtydegreesray.openhub.ui.adapter.base.BaseViewHolder;
import com.thirtydegreesray.openhub.ui.fragment.base.BaseFragment;
import com.thirtydegreesray.openhub.util.IgnoredRepoHelper;
import com.thirtydegreesray.openhub.util.LanguageColorsHelper;
import com.thirtydegreesray.openhub.util.PrefUtils;
import com.thirtydegreesray.openhub.util.StringUtils;
import com.thirtydegreesray.openhub.util.ViewUtils;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created on 2017/7/18.
 *
 * @author ThirtyDegreesRay
 */

public class RepositoriesAdapter extends BaseAdapter<RepositoriesAdapter.ViewHolder, Repository> {

    // Set by RepositoriesFragment only for Trending/Created (ignoreListEligible)
    // rows - dims a row still visible because the ignore-list toggle is off.
    // Every other screen this adapter is shared with (Bookmarks, Trace,
    // Starred, etc.) leaves this false and is unaffected.
    private boolean showIgnoredState = false;

    @Inject
    public RepositoriesAdapter(Context context, BaseFragment fragment){
        super(context, fragment);
    }

    public void setShowIgnoredState(boolean showIgnoredState) {
        this.showIgnoredState = showIgnoredState;
    }

    @Override
    protected int getLayoutId(int viewType) {
        return R.layout.layout_item_repository;
    }

    @NonNull
    @Override
    protected ViewHolder getViewHolder(@NonNull View itemView, int viewType) {
        ViewHolder holder = new ViewHolder(itemView);
        return holder;
    }

    public class ViewHolder extends BaseViewHolder {

        @BindView(R2.id.iv_user_avatar) ImageView ivUserAvatar;
        @BindView(R2.id.language_color) ImageView languageColor;
        @BindView(R2.id.tv_repo_name) TextView tvRepoName;
        @BindView(R2.id.tv_language) TextView tvLanguage;
        @BindView(R2.id.tv_repo_description) TextView tvRepoDescription;
        @BindView(R2.id.tv_star_num) TextView tvStarNum;
        @BindView(R2.id.tv_fork_num) TextView tvForkNum;
        @BindView(R2.id.tv_owner_name) TextView tvOwnerName;
        @BindView(R2.id.tv_since_star_num) TextView tvSinceStarNum;
        @BindView(R2.id.since_star_lay) LinearLayout sinceStarLay;
        @BindView(R2.id.owner_lay) LinearLayout ownerLay;
        @BindView(R2.id.fork_mark) View forkMark;
        @BindView(R2.id.topics_lay) LinearLayout topicsLay;
        @BindView(R2.id.tv_topics) TextView tvTopics;
        @BindView(R2.id.tv_pushed_at) TextView tvPushedAt;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        @OnClick(R2.id.iv_user_avatar)
        public void onUserClick(){
            if(getAdapterPosition() != RecyclerView.NO_POSITION){
                ProfileActivity.show((Activity) context, ivUserAvatar, data.get(getAdapterPosition()).getOwner().getLogin(),
                        data.get(getAdapterPosition()).getOwner().getAvatarUrl());
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        Repository repository = data.get(position);
        boolean hasOwnerAvatar = !StringUtils.isBlank(repository.getOwner().getAvatarUrl());
        holder.tvRepoName.setText(hasOwnerAvatar ? repository.getName(): repository.getFullName());
        ViewUtils.setTextView(holder.tvRepoDescription, repository.getDescription());
        holder.tvStarNum.setText(String.valueOf(repository.getStargazersCount()));
        holder.tvForkNum.setText(String.valueOf(repository.getForksCount()));
        holder.tvOwnerName.setText(repository.getOwner().getLogin());

        if(StringUtils.isBlank(repository.getLanguage())){
            holder.tvLanguage.setText("");
            holder.languageColor.setVisibility(View.INVISIBLE);
        } else {
            holder.languageColor.setVisibility(View.VISIBLE);
            holder.tvLanguage.setText(repository.getLanguage());
            int languageColor = LanguageColorsHelper.INSTANCE.getColor(context, repository.getLanguage());
            holder.languageColor.setImageTintList(ColorStateList.valueOf(languageColor));
        }


        if(hasOwnerAvatar){
            holder.ivUserAvatar.setVisibility(View.VISIBLE);
            holder.ownerLay.setVisibility(View.VISIBLE);
            holder.sinceStarLay.setVisibility(View.GONE);
            GlideApp.with(fragment)
                    .load(repository.getOwner().getAvatarUrl())
                    .onlyRetrieveFromCache(!PrefUtils.isLoadImageEnable())
                    .into(holder.ivUserAvatar);
        } else {
            holder.ivUserAvatar.setVisibility(View.GONE);
            holder.ownerLay.setVisibility(View.GONE);
            if (repository.getSinceStargazersCount() == 0) {
                holder.sinceStarLay.setVisibility(View.INVISIBLE);
            } else {
                holder.sinceStarLay.setVisibility(View.VISIBLE);
                switch (repository.getSince()) {
                    case Daily:
                        holder.tvSinceStarNum.setText(String.format(getString(R.string.star_num_today),
                                repository.getSinceStargazersCount()));
                        break;
                    case Weekly:
                        holder.tvSinceStarNum.setText(String.format(getString(R.string.star_num_this_week),
                                repository.getSinceStargazersCount()));
                        break;
                    case Monthly:
                        holder.tvSinceStarNum.setText(String.format(getString(R.string.star_num_this_month),
                                repository.getSinceStargazersCount()));
                        break;
                }
            }
        }

        holder.forkMark.setVisibility(repository.isFork() ? View.VISIBLE : View.GONE);

        if (repository.getPushedAt() != null) {
            holder.tvPushedAt.setVisibility(View.VISIBLE);
            holder.tvPushedAt.setText(String.format(getString(R.string.updated_at_format),
                    StringUtils.getNewsTimeStr(context, repository.getPushedAt())));
        } else {
            holder.tvPushedAt.setVisibility(View.GONE);
        }

        if(repository.getTopics() == null || repository.getTopics().isEmpty()){
            holder.topicsLay.setVisibility(View.GONE);
        } else {
            holder.topicsLay.setVisibility(View.VISIBLE);
            holder.tvTopics.setText(android.text.TextUtils.join(", ", repository.getTopics()));
        }

        holder.itemView.setAlpha(
                showIgnoredState && IgnoredRepoHelper.isIgnored(repository.getFullName()) ? 0.5f : 1f);
    }
}
