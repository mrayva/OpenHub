package com.thirtydegreesray.openhub.mvp.contract.base;

/**
 * Created by ThirtyDegreesRay on 2017/9/22 10:47:52
 */

public interface IBaseListContract {

    interface View {
        void showLoadError(String errorMsg);

        void setCanLoadMore(boolean canLoadMore);

        /**
         * Syncs the fragment's own load-more page counter to a page a
         * presenter fetched directly (not via the fragment's own ++curPage
         * scroll-triggered increment) - needed by RepositoriesPresenter's
         * ignore-list auto-continue, which can silently consume many raw
         * pages in one reload/load-more call. Without this, the next
         * scroll-triggered load-more re-requests a page already consumed by
         * the auto-continue chain, which - thanks to dedup - appends zero
         * new items and looks completely stuck until enough further scrolls
         * happen to organically catch the counter back up.
         */
        void setCurPage(int page);
    }

}
