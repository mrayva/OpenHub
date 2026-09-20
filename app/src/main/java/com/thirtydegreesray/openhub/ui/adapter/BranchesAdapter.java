

package com.thirtydegreesray.openhub.ui.adapter;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.R2;
import com.thirtydegreesray.openhub.mvp.model.Branch;
import com.thirtydegreesray.openhub.ui.adapter.base.BaseAdapter;
import com.thirtydegreesray.openhub.ui.adapter.base.BaseViewHolder;
import com.thirtydegreesray.openhub.util.StringUtils;
import com.thirtydegreesray.openhub.util.ViewUtils;

import java.util.Date;

import butterknife.BindView;

/**
 * Created by ThirtyDegreesRay on 2017/8/15 22:50:29
 */

public class BranchesAdapter extends BaseAdapter<BaseViewHolder, Branch> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private final String curBranch;

    public BranchesAdapter(Context context, String curBranch) {
        super(context);
        this.curBranch = curBranch;
    }

    @Override
    public int getItemViewType(int position) {
        return data.get(position).isHeader() ? TYPE_HEADER : TYPE_ITEM;
    }

    @Override
    protected int getLayoutId(int viewType) {
        return viewType == TYPE_HEADER ? R.layout.layout_item_branch_header : R.layout.layout_item_branch;
    }

    @Override
    protected BaseViewHolder getViewHolder(View itemView, int viewType) {
        return viewType == TYPE_HEADER ? new HeaderViewHolder(itemView) : new ItemViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull BaseViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        Branch branch = data.get(position);
        if (getItemViewType(position) == TYPE_HEADER) {
            ((HeaderViewHolder) holder).label.setText(branch.getHeaderLabel());
            return;
        }
        ItemViewHolder itemHolder = (ItemViewHolder) holder;
        itemHolder.icon.setImageResource(branch.isBranch() ? R.drawable.ic_branch : R.drawable.ic_tag);
        itemHolder.name.setText(branch.getName());
        Date updatedAt = branch.getUpdatedAt();
        if (updatedAt != null) {
            itemHolder.updatedAt.setText(String.format(getString(R.string.updated_at_format),
                    StringUtils.getNewsTimeStr(context, updatedAt)));
            itemHolder.updatedAt.setVisibility(View.VISIBLE);
        } else {
            itemHolder.updatedAt.setVisibility(View.GONE);
        }
        if(branch.getName().equals(curBranch)){
            itemHolder.rootLayout.setBackgroundColor(ViewUtils.getSelectedColor(context));
        }else{
            itemHolder.rootLayout.setBackground(null);
        }
    }

    class ItemViewHolder extends BaseViewHolder {

        @BindView(R2.id.root_layout) LinearLayout rootLayout;
        @BindView(R2.id.icon) AppCompatImageView icon;
        @BindView(R2.id.name) TextView name;
        @BindView(R2.id.updated_at) TextView updatedAt;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    class HeaderViewHolder extends BaseViewHolder {

        @BindView(R2.id.header_label) TextView label;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

}
