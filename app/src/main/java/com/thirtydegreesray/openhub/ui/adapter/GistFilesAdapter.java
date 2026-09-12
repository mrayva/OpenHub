

package com.thirtydegreesray.openhub.ui.adapter;

import android.content.Context;
import androidx.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.R2;
import com.thirtydegreesray.openhub.mvp.model.GistFile;
import com.thirtydegreesray.openhub.ui.adapter.base.BaseAdapter;
import com.thirtydegreesray.openhub.ui.adapter.base.BaseViewHolder;
import com.thirtydegreesray.openhub.ui.fragment.base.BaseFragment;
import com.thirtydegreesray.openhub.util.StringUtils;

import javax.inject.Inject;

import butterknife.BindView;

public class GistFilesAdapter extends BaseAdapter<GistFilesAdapter.ViewHolder, GistFile> {

    @Inject
    public GistFilesAdapter(Context context, BaseFragment fragment) {
        super(context, fragment);
    }

    @Override
    protected int getLayoutId(int viewType) {
        return R.layout.layout_item_gist_file;
    }

    @NonNull
    @Override
    protected ViewHolder getViewHolder(@NonNull View itemView, int viewType) {
        return new ViewHolder(itemView);
    }

    public class ViewHolder extends BaseViewHolder {
        @BindView(R2.id.tv_file_name) TextView tvFileName;
        @BindView(R2.id.tv_file_language) TextView tvFileLanguage;
        @BindView(R2.id.tv_file_size) TextView tvFileSize;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        GistFile file = data.get(position);
        holder.tvFileName.setText(file.getFilename());
        holder.tvFileLanguage.setText(StringUtils.isBlank(file.getLanguage()) ? "" : file.getLanguage());
        holder.tvFileSize.setText(StringUtils.getSizeString(file.getSize()));
    }
}
