package com.thirtydegreesray.openhub.ui.fragment;

import android.os.Bundle;
import android.view.View;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.R2;
import com.thirtydegreesray.openhub.inject.component.AppComponent;
import com.thirtydegreesray.openhub.mvp.model.Repository;
import com.thirtydegreesray.openhub.ui.activity.TopicRepositoriesActivity;
import com.thirtydegreesray.openhub.ui.adapter.RepoTopicsAdapter;
import com.thirtydegreesray.openhub.ui.adapter.base.AddTopicSwipeCallback;
import com.thirtydegreesray.openhub.ui.fragment.base.BaseFragment;
import com.thirtydegreesray.openhub.util.BundleHelper;
import com.thirtydegreesray.openhub.util.MyTopicHelper;

import butterknife.BindView;

/**
 * Created by ThirtyDegreesRay on 2017/8/11 11:35:39
 */

public class RepoTopicsFragment extends BaseFragment {

    public static RepoTopicsFragment create(Repository repository) {
        RepoTopicsFragment fragment = new RepoTopicsFragment();
        fragment.setArguments(BundleHelper.builder().put("repository", repository).build());
        return fragment;
    }

    @AutoAccess Repository repository;

    @BindView(R2.id.recycler_view) RecyclerView recyclerView;
    @BindView(R2.id.empty_lay) View emptyLay;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_repo_topics;
    }

    @Override
    protected void setupFragmentComponent(AppComponent appComponent) {

    }

    @Override
    protected void initFragment(Bundle savedInstanceState) {
        RepoTopicsAdapter adapter = new RepoTopicsAdapter(getContext(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener((position, view) ->
                TopicRepositoriesActivity.show(getContext(), adapter.getData().get(position)));

        new ItemTouchHelper(new AddTopicSwipeCallback(getContext(), position -> {
            String slug = adapter.getData().get(position);
            boolean added = MyTopicHelper.add(slug);
            adapter.notifyItemChanged(position);
            Snackbar snackbar = Snackbar.make(recyclerView, String.format(getString(added
                    ? R.string.topic_added_to_my_topics : R.string.topic_already_in_my_topics), slug),
                    Snackbar.LENGTH_LONG);
            if (added) {
                snackbar.setAction(R.string.undo, v -> MyTopicHelper.remove(slug));
            }
            snackbar.show();
        })).attachToRecyclerView(recyclerView);

        if (repository.getTopics() == null || repository.getTopics().isEmpty()) {
            emptyLay.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyLay.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.setData(new java.util.ArrayList<>(repository.getTopics()));
            adapter.notifyDataSetChanged();
        }
    }

}
