

package com.thirtydegreesray.openhub.ui.fragment.base;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.R2;
import com.thirtydegreesray.openhub.mvp.contract.base.IBaseContract;
import com.thirtydegreesray.openhub.ui.adapter.base.BaseAdapter;
import com.thirtydegreesray.openhub.ui.adapter.base.BaseViewHolder;
import com.thirtydegreesray.openhub.ui.adapter.base.CatchableLinearLayoutManager;
import com.thirtydegreesray.openhub.util.NetHelper;
import com.thirtydegreesray.openhub.util.PrefUtils;
import com.thirtydegreesray.openhub.util.ViewUtils;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created on 2017/7/20.
 *
 * @author ThirtyDegreesRay
 */

public abstract class ListFragment <P extends IBaseContract.Presenter, A extends BaseAdapter>
        extends BaseFragment<P> implements IBaseContract.View,
        BaseViewHolder.OnItemClickListener,
        BaseViewHolder.OnItemLongClickListener,
        SwipeRefreshLayout.OnRefreshListener{

    @BindView(R2.id.refresh_layout) protected SwipeRefreshLayout refreshLayout;
    @BindView(R2.id.recycler_view) protected RecyclerView recyclerView;
    @Inject protected A adapter;
    private RecyclerView.AdapterDataObserver observer;

    @BindView(R2.id.lay_tip) LinearLayout layTip;
    @BindView(R2.id.tv_tip) TextView tvTip;
    @BindView(R2.id.error_image) AppCompatImageView errorImage;

    private int curPage = 1;

    private boolean refreshEnable = true;
    private boolean loadMoreEnable = false;
    private boolean canLoadMore = false;
    private boolean autoJudgeCanLoadMoreEnable = true;
    private boolean isLoading = false;
    private final int DEFAULT_PAGE_SIZE = 30;

    @Override
    protected void initFragment(Bundle savedInstanceState) {
        refreshLayout.setOnRefreshListener(this);
        refreshLayout.setColorSchemeColors(ViewUtils.getRefreshLayoutColors(getContext()));

        recyclerView.setLayoutManager(new CatchableLinearLayoutManager(getActivity()));
        // Every ListFragment subclass's recycler_view is declared
        // match_parent/match_parent (fragment_list.xml / fragment_list_with_search.xml),
        // so its own size never depends on adapter content - safe to skip
        // the layout pass RecyclerView would otherwise run to check.
        recyclerView.setHasFixedSize(true);
        adapter.setOnItemLongClickListener(this);
        adapter.setOnItemClickListener(this);
        recyclerView.setAdapter(adapter);

        layTip.setVisibility(View.GONE);

        //adapter 数据观察者，当数据为空时，显示空提示
        observer = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                onListDataUpdated();
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                onListDataUpdated();
            }
        };
        adapter.registerAdapterDataObserver(observer);
        recyclerView.setOnScrollListener(new ScrollListener());
        refreshLayout.setRefreshing(true);
        initScrollListener();
    }

    private class ScrollListener extends RecyclerView.OnScrollListener{
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            //only LinearLayoutManager can find last visible
            if(layoutManager instanceof LinearLayoutManager){
                LinearLayoutManager linearManager = (LinearLayoutManager) layoutManager;
                int lastPosition = linearManager.findLastVisibleItemPosition();
                boolean atBottom = lastPosition == adapter.getItemCount() - 1;
                if (atBottom) {
                    // Only logged once actually scrolled to the bottom edge
                    // (not on every scroll tick) - if this ever prints
                    // "BLOCKED" with isLoading=true and no matching
                    // hideLoading() nearby in logcat, that's a stuck
                    // in-flight request silently eating every further
                    // scroll-triggered load-more attempt.
                    if(!loadMoreEnable || !canLoadMore || isLoading || !NetHelper.INSTANCE.getNetEnabled()){
                        android.util.Log.d("SEARCH_DEBUG", "load-more BLOCKED at bottom: loadMoreEnable="
                                + loadMoreEnable + " canLoadMore=" + canLoadMore + " isLoading=" + isLoading
                                + " netEnabled=" + NetHelper.INSTANCE.getNetEnabled());
                    } else {
                        onLoadMore(++curPage);
                    }
                }
            }
        }
    }

    /**
     * Shared by onChanged() (full rebind) and onItemRangeInserted()
     * (targeted load-more append) - both need the same empty-state/
     * canLoadMore bookkeeping, they just fire from different adapter
     * notify calls.
     */
    private void onListDataUpdated(){
        int itemCount = adapter.getItemCount();
        if (itemCount == 0) {
            refreshLayout.setVisibility(View.GONE);
            layTip.setVisibility(View.VISIBLE);
            tvTip.setText(getEmptyTip());
            errorImage.setVisibility(View.GONE);
        } else {
            refreshLayout.setVisibility(View.VISIBLE);
            layTip.setVisibility(View.GONE);
            itemCount -= getHeaderSize();
            if(loadMoreEnable && autoJudgeCanLoadMoreEnable){
                canLoadMore = itemCount % getPagerSize() == 0 ;
//                        curPage = itemCount % getPagerSize() == 0 ?
//                                itemCount / getPagerSize() : (itemCount / getPagerSize()) + 1;
            }
        }
    }

    public int getVisibleItemCount(){
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        //only LinearLayoutManager can find last visible
        if(layoutManager instanceof LinearLayoutManager){
            LinearLayoutManager linearManager = (LinearLayoutManager) layoutManager;
            int firstPosition = linearManager.findFirstVisibleItemPosition();
            int lastPosition = linearManager.findLastVisibleItemPosition();
            return lastPosition - firstPosition + 1;
        }else {
            throw new UnsupportedOperationException("only for Linear RecyclerView ");
        }
    }

    public int getItemCount(){
        return adapter.getItemCount();
    }

    @Override
    public void onItemClick(int position, @NonNull View view) {

    }

    @Override
    public boolean onItemLongClick(int position, @NonNull View view) {
        return false;
    }

    @Override
    public void onRefresh() {
        refreshLayout.setRefreshing(true);
        curPage = 1;
        onReLoadData();
    }

    // Removed retry button click handler since button was removed from layout

    protected void setErrorTip(String errorTip){
        refreshLayout.setVisibility(View.GONE);
        errorImage.setVisibility(View.VISIBLE);
        layTip.setVisibility(View.VISIBLE);
        tvTip.setText(errorTip);
    }

    protected void addVerticalDivider(){
        recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));
    }

    /**
     * load more switch
     * @param loadMoreEnable flag
     */
    public void setLoadMoreEnable(boolean loadMoreEnable) {
        this.loadMoreEnable = loadMoreEnable;
    }

    public void setCanLoadMore(boolean canLoadMore) {
        this.canLoadMore = canLoadMore;
    }

    public void setRefreshEnable(boolean refreshEnable) {
        this.refreshEnable = refreshEnable;
        refreshLayout.setEnabled(refreshEnable);
    }

    public void setAutoJudgeCanLoadMoreEnable(boolean autoJudgeLoadMoreEnable) {
        this.autoJudgeCanLoadMoreEnable = autoJudgeLoadMoreEnable;
        canLoadMore = !autoJudgeLoadMoreEnable;
    }

    public int getCurPage() {
        return curPage;
    }

    public void setCurPage(int page) {
        curPage = page;
    }

    @Override
    public void showLoading() {
        android.util.Log.d("SEARCH_DEBUG", "showLoading() this=" + System.identityHashCode(this));
        isLoading = true;
        refreshLayout.setRefreshing(true);
    }

    @Override
    public void hideLoading() {
        android.util.Log.d("SEARCH_DEBUG", "hideLoading() this=" + System.identityHashCode(this));
        isLoading = false;
        refreshLayout.setRefreshing(false);
    }

    protected abstract void onReLoadData();

    protected abstract String getEmptyTip();

    protected int getPagerSize(){
        return DEFAULT_PAGE_SIZE;
    }

    protected int getHeaderSize(){
        return 0;
    }

    protected void onLoadMore(int page){
        if(page == 3 && PrefUtils.isDoubleClickTitleTipAble()){
            showOperationTip(R.string.double_click_toolbar_tip);
            PrefUtils.set(PrefUtils.DOUBLE_CLICK_TITLE_TIP_ABLE, false);
        }
    }

    public void showLoadError(String error) {
        setErrorTip(error);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(adapter != null && observer != null)
            adapter.unregisterAdapterDataObserver(observer);
    }

    @Override
    public void scrollToTop() {
        super.scrollToTop();
        if(recyclerView != null) recyclerView.scrollToPosition(0);
    }

    protected void postNotifyDataSetChanged(){
        adapter.notifyDataSetChanged();
    }

    /**
     * Use only when the newly-added items were purely appended to the end of
     * an otherwise-unchanged list (e.g. a load-more page) - it skips
     * rebinding every already-visible row (each of which may re-trigger a
     * Glide load) that a full notifyDataSetChanged() would force. Not safe
     * if existing items could have moved/changed too (e.g. a client-side
     * re-sort across old+new items) - use postNotifyDataSetChanged() there.
     */
    protected void postNotifyItemRangeInserted(int positionStart, int itemCount){
        adapter.notifyItemRangeInserted(positionStart, itemCount);
    }

    private ListScrollListener mListScrollListener;

    public void setListScrollListener(ListScrollListener listScrollListener){
        mListScrollListener = listScrollListener;
        initScrollListener();
    }

    private void initScrollListener(){
        if(recyclerView == null || mListScrollListener == null) return;
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if(dy > 0){
                    mListScrollListener.onScrollUp();
                } else {
                    mListScrollListener.onScrollDown();
                }
            }
        });
    }

    public interface ListScrollListener{
        void onScrollUp();

        void onScrollDown();
    }

}
