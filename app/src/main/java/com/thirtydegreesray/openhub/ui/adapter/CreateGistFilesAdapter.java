package com.thirtydegreesray.openhub.ui.adapter;

import android.content.Context;
import androidx.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.R2;
import com.thirtydegreesray.openhub.mvp.model.GistFileEntry;
import com.thirtydegreesray.openhub.ui.adapter.base.BaseAdapter;
import com.thirtydegreesray.openhub.ui.adapter.base.BaseViewHolder;
import com.thirtydegreesray.openhub.ui.fragment.base.BaseFragment;

import javax.inject.Inject;

import butterknife.BindView;

public class CreateGistFilesAdapter extends BaseAdapter<CreateGistFilesAdapter.ViewHolder, GistFileEntry> {

    @Inject
    public CreateGistFilesAdapter(Context context, BaseFragment fragment) {
        super(context, fragment);
    }

    @Override
    protected int getLayoutId(int viewType) {
        return R.layout.layout_item_create_gist_file;
    }

    @NonNull
    @Override
    protected ViewHolder getViewHolder(@NonNull View itemView, int viewType) {
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        GistFileEntry entry = data.get(position);
        holder.tvFileName.setText(entry.getFilename());
    }

    class ViewHolder extends BaseViewHolder {
        @BindView(R2.id.tv_file_name) TextView tvFileName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

}
