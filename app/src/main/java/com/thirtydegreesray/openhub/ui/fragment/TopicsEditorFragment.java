package com.thirtydegreesray.openhub.ui.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.snackbar.Snackbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.View;

import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.dao.MyTopic;
import com.thirtydegreesray.openhub.inject.component.AppComponent;
import com.thirtydegreesray.openhub.inject.component.DaggerFragmentComponent;
import com.thirtydegreesray.openhub.inject.module.FragmentModule;
import com.thirtydegreesray.openhub.mvp.contract.ITopicsEditorContract;
import com.thirtydegreesray.openhub.mvp.presenter.TopicsEditorPresenter;
import com.thirtydegreesray.openhub.ui.adapter.TopicsEditorAdapter;
import com.thirtydegreesray.openhub.ui.adapter.base.ItemTouchHelperCallback;
import com.thirtydegreesray.openhub.ui.fragment.base.ListFragment;
import com.thirtydegreesray.openhub.util.PrefUtils;

import java.util.ArrayList;

public class TopicsEditorFragment extends ListFragment<TopicsEditorPresenter, TopicsEditorAdapter>
        implements ITopicsEditorContract.View, ItemTouchHelperCallback.ItemGestureListener {

    public static TopicsEditorFragment create() {
        return new TopicsEditorFragment();
    }

    @Override
    protected void initFragment(Bundle savedInstanceState) {
        super.initFragment(savedInstanceState);
        setCanLoadMore(false);
        ItemTouchHelperCallback callback = new ItemTouchHelperCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, this);
        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
        addVerticalDivider();
        if (PrefUtils.isTopicsEditorTipAble()) {
            showOperationTip(R.string.topics_editor_tip);
            PrefUtils.set(PrefUtils.TOPICS_EDITOR_TIP_ABLE, false);
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_list;
    }

    @Override
    protected void setupFragmentComponent(AppComponent appComponent) {
        DaggerFragmentComponent.builder()
                .appComponent(appComponent)
                .fragmentModule(new FragmentModule(this))
                .build()
                .inject(this);
    }

    @Override
    protected void onReLoadData() {
        mPresenter.loadTopics();
    }

    @Override
    protected String getEmptyTip() {
        return getString(R.string.no_topics_added);
    }

    @Override
    public void showTopics(ArrayList<MyTopic> topics) {
        adapter.setData(topics);
        postNotifyDataSetChanged();
    }

    @Override
    public void notifyItemInserted(int position) {
        adapter.notifyItemInserted(position);
        recyclerView.scrollToPosition(position);
    }

    @Override
    public void onItemClick(int position, @NonNull View view) {
        super.onItemClick(position, view);
        mPresenter.toggleSelected(position);
        adapter.notifyItemChanged(position);
    }

    @Override
    public boolean onItemMoved(int fromPosition, int toPosition) {
        return false;
    }

    @Override
    public void onItemSwiped(int position, int direction) {
        MyTopic removedTopic = mPresenter.removeTopic(position);
        if (adapter.getData().size() == 0) {
            postNotifyDataSetChanged();
        } else {
            adapter.notifyItemRemoved(position);
        }
        String tip = String.format(getString(R.string.topic_removed), removedTopic.getSlug());
        Snackbar.make(recyclerView, tip, Snackbar.LENGTH_SHORT)
                .setAction(R.string.undo, v -> mPresenter.undoRemoveTopic())
                .show();
    }

    public boolean addTopic(String slug) {
        return mPresenter.addTopic(slug);
    }

    public int getSelectedCount() {
        return mPresenter.getSelectedCount();
    }

}
