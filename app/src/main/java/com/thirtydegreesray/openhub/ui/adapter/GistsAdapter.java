

package com.thirtydegreesray.openhub.ui.adapter;

import android.content.Context;
import androidx.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.R2;
import com.thirtydegreesray.openhub.common.GlideApp;
import com.thirtydegreesray.openhub.mvp.model.Gist;
import com.thirtydegreesray.openhub.ui.adapter.base.BaseAdapter;
import com.thirtydegreesray.openhub.ui.adapter.base.BaseViewHolder;
import com.thirtydegreesray.openhub.ui.fragment.base.BaseFragment;
import com.thirtydegreesray.openhub.util.PrefUtils;
import com.thirtydegreesray.openhub.util.StringUtils;
import com.thirtydegreesray.openhub.util.ViewUtils;

import javax.inject.Inject;

import butterknife.BindView;

public class GistsAdapter extends BaseAdapter<GistsAdapter.ViewHolder, Gist> {

    @Inject
    public GistsAdapter(Context context, BaseFragment fragment) {
        super(context, fragment);
    }

    @Override
    protected int getLayoutId(int viewType) {
        return R.layout.layout_item_gist;
    }

    @NonNull
    @Override
    protected ViewHolder getViewHolder(@NonNull View itemView, int viewType) {
        return new ViewHolder(itemView);
    }

    public class ViewHolder extends BaseViewHolder {

        @BindView(R2.id.iv_user_avatar) ImageView ivUserAvatar;
        @BindView(R2.id.tv_gist_title) TextView tvGistTitle;
        @BindView(R2.id.tv_gist_description) TextView tvGistDescription;
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
        Gist gist = data.get(position);
        holder.tvGistTitle.setText(gist.getDisplayTitle());
        ViewUtils.setTextView(holder.tvGistDescription, gist.getDescription());
        holder.tvCommentsNum.setText(String.valueOf(gist.getComments()));
        if (gist.getUpdatedAt() != null) {
            holder.tvUpdatedAt.setText(StringUtils.getNewsTimeStr(context, gist.getUpdatedAt()));
        }

        if (gist.getOwner() != null) {
            holder.tvOwnerName.setText(gist.getOwner().getLogin());
            GlideApp.with(fragment)
                    .load(gist.getOwner().getAvatarUrl())
                    .onlyRetrieveFromCache(!PrefUtils.isLoadImageEnable())
                    .into(holder.ivUserAvatar);
        }
    }
}
