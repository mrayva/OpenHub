package com.thirtydegreesray.openhub.mvp.presenter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.dao.DaoSession;
import com.thirtydegreesray.openhub.http.core.HttpObserver;
import com.thirtydegreesray.openhub.http.core.HttpResponse;
import com.thirtydegreesray.openhub.mvp.contract.ICreateGistContract;
import com.thirtydegreesray.openhub.mvp.model.Gist;
import com.thirtydegreesray.openhub.mvp.model.GistFile;
import com.thirtydegreesray.openhub.mvp.model.GistFileEntry;
import com.thirtydegreesray.openhub.mvp.model.request.CreateGistModel;
import com.thirtydegreesray.openhub.mvp.model.request.GistFileContent;
import com.thirtydegreesray.openhub.mvp.presenter.base.BasePresenter;
import com.thirtydegreesray.openhub.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Response;
import rx.Observable;

/**
 * Backs both create and edit gist. In edit mode (editingGist != null),
 * originalFilenames snapshots what the gist had when the screen opened, so
 * submitEdit() can diff the current file list against it and send an
 * explicit null for every filename that was removed - GitHub's real PATCH
 * semantics: an omitted filename is left alone, a null deletes it, a new/
 * changed key adds/updates it. Renaming a file is handled as delete-old +
 * add-new (see GistFileEntry) rather than GitHub's dedicated rename
 * mechanism - same end result, much simpler diff.
 */
public class CreateGistPresenter extends BasePresenter<ICreateGistContract.View>
        implements ICreateGistContract.Presenter {

    @AutoAccess Gist editingGist;

    private ArrayList<GistFileEntry> files;
    private Set<String> originalFilenames;

    private GistFileEntry removedEntry;
    private int removedPosition;

    @Inject
    public CreateGistPresenter(DaoSession daoSession) {
        super(daoSession);
    }

    @Override
    public void onViewInitialized() {
        super.onViewInitialized();
        loadFiles();
    }

    @Override
    public void loadFiles() {
        if (files == null) {
            files = new ArrayList<>();
            originalFilenames = new HashSet<>();
            if (editingGist != null) {
                for (GistFile file : editingGist.getFileList()) {
                    files.add(new GistFileEntry(file.getFilename(), file.getContent()));
                    originalFilenames.add(file.getFilename());
                }
            }
        }
        mView.showFiles(files);
        mView.hideLoading();
    }

    @Override
    public boolean isEditMode() {
        return editingGist != null;
    }

    @Override
    public String getInitialDescription() {
        return editingGist == null ? null : editingGist.getDescription();
    }

    @Override
    public boolean addFile(String filename, String content) {
        if (StringUtils.isBlank(filename) || isDuplicateFilename(filename, -1)) return false;
        files.add(new GistFileEntry(filename, content));
        mView.notifyItemInserted(files.size() - 1);
        return true;
    }

    @Override
    public boolean updateFile(int position, String filename, String content) {
        if (StringUtils.isBlank(filename) || isDuplicateFilename(filename, position)) return false;
        GistFileEntry entry = files.get(position);
        entry.setFilename(filename);
        entry.setContent(content);
        mView.showFiles(files);
        return true;
    }

    private boolean isDuplicateFilename(String filename, int exceptPosition) {
        for (int i = 0; i < files.size(); i++) {
            if (i != exceptPosition && files.get(i).getFilename().equals(filename)) return true;
        }
        return false;
    }

    @Override
    public GistFileEntry removeFile(int position) {
        removedEntry = files.remove(position);
        removedPosition = position;
        return removedEntry;
    }

    @Override
    public void undoRemoveFile() {
        files.add(removedPosition, removedEntry);
        mView.notifyItemInserted(removedPosition);
    }

    @Override
    public void submitCreate(final String description, final boolean isPublic) {
        if (!checkFiles()) return;
        Map<String, GistFileContent> fileMap = new HashMap<>();
        for (GistFileEntry entry : files) {
            fileMap.put(entry.getFilename(), new GistFileContent(entry.getContent()));
        }
        CreateGistModel model = new CreateGistModel(description, isPublic, fileMap);
        HttpObserver<Gist> httpObserver = new HttpObserver<Gist>() {
            @Override
            public void onError(Throwable error) {
                mView.showErrorToast(getErrorTip(error));
            }

            @Override
            public void onSuccess(HttpResponse<Gist> response) {
                mView.showSuccessToast(getString(R.string.create_gist_success));
                mView.onGistSaved(response.body());
            }
        };
        generalRxHttpExecute(new IObservableCreator<Gist>() {
            @Override
            public Observable<Response<Gist>> createObservable(boolean forceNetWork) {
                return getGistService().createGist(model);
            }
        }, httpObserver, false, mView.getProgressDialog(getLoadTip()));
    }

    @Override
    public void submitEdit(final String description) {
        if (!checkFiles()) return;
        Map<String, GistFileContent> fileMap = new HashMap<>();
        Set<String> currentFilenames = new HashSet<>();
        for (GistFileEntry entry : files) {
            fileMap.put(entry.getFilename(), new GistFileContent(entry.getContent()));
            currentFilenames.add(entry.getFilename());
        }
        for (String originalFilename : originalFilenames) {
            if (!currentFilenames.contains(originalFilename)) {
                fileMap.put(originalFilename, null);
            }
        }
        CreateGistModel model = new CreateGistModel(description, false, fileMap);
        // isPublic is ignored server-side on PATCH (GitHub doesn't allow
        // changing a gist's visibility after creation) - kept false here
        // purely because the field is non-null on CreateGistModel.
        Gson gson = new GsonBuilder().serializeNulls().create();
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"), gson.toJson(model));

        HttpObserver<Gist> httpObserver = new HttpObserver<Gist>() {
            @Override
            public void onError(Throwable error) {
                mView.showErrorToast(getErrorTip(error));
            }

            @Override
            public void onSuccess(HttpResponse<Gist> response) {
                mView.showSuccessToast(getString(R.string.edit_gist_success));
                mView.onGistSaved(response.body());
            }
        };
        generalRxHttpExecute(new IObservableCreator<Gist>() {
            @Override
            public Observable<Response<Gist>> createObservable(boolean forceNetWork) {
                return getGistService().editGist(editingGist.getId(), body);
            }
        }, httpObserver, false, mView.getProgressDialog(getLoadTip()));
    }

    private boolean checkFiles() {
        if (StringUtils.isBlankList(files)) {
            mView.showWarningToast(getString(R.string.gist_needs_a_file));
            return false;
        }
        return true;
    }

}
