package com.thirtydegreesray.openhub.mvp.contract;

import com.thirtydegreesray.openhub.mvp.contract.base.IBaseContract;
import com.thirtydegreesray.openhub.mvp.contract.base.IBaseListContract;
import com.thirtydegreesray.openhub.mvp.model.Gist;
import com.thirtydegreesray.openhub.mvp.model.GistFileEntry;

import java.util.ArrayList;

/**
 * Backs CreateGistActivity in both create and edit mode - see
 * CreateGistPresenter for how the two modes differ.
 */
public interface ICreateGistContract {

    interface View extends IBaseContract.View, IBaseListContract.View {
        void showFiles(ArrayList<GistFileEntry> files);
        void notifyItemInserted(int position);
        void onGistSaved(Gist gist);
    }

    interface Presenter extends IBaseContract.Presenter<ICreateGistContract.View> {
        void loadFiles();
        boolean isEditMode();
        String getInitialDescription();
        boolean addFile(String filename, String content);
        boolean updateFile(int position, String filename, String content);
        GistFileEntry removeFile(int position);
        void undoRemoveFile();
        void submitCreate(String description, boolean isPublic);
        void submitEdit(String description);
    }

}
