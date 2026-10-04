

package com.thirtydegreesray.openhub.mvp.presenter;

import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.AppConfig;
import com.thirtydegreesray.openhub.dao.DaoSession;
import com.thirtydegreesray.openhub.http.core.HttpObserver;
import com.thirtydegreesray.openhub.http.core.HttpResponse;
import com.thirtydegreesray.openhub.http.error.HttpPageNoFoundError;
import com.thirtydegreesray.openhub.mvp.contract.IRepoInfoContract;
import com.thirtydegreesray.openhub.mvp.model.Repository;
import com.thirtydegreesray.openhub.mvp.presenter.base.BasePagerPresenter;
import com.thirtydegreesray.openhub.util.ReadmeTranslationHelper;
import com.thirtydegreesray.openhub.util.StringUtils;

import java.io.IOException;

import javax.inject.Inject;

import okhttp3.ResponseBody;
import retrofit2.Response;
import rx.Observable;

/**
 * Created by ThirtyDegreesRay on 2017/8/11 11:34:31
 */

public class RepoInfoPresenter extends BasePagerPresenter<IRepoInfoContract.View>
        implements IRepoInfoContract.Presenter{

    @AutoAccess Repository repository;
    @AutoAccess String curBranch = "";
    private String readmeSource;
    private String readmeBaseUrl;
    private String translatedReadmeSource;
    private String detectedReadmeLanguage;
    private boolean showingTranslatedReadme = false;

    @Inject
    public RepoInfoPresenter(DaoSession daoSession) {
        super(daoSession);
    }

    @Override
    public void onViewInitialized() {
        super.onViewInitialized();
    }

    @Override
    protected void loadData() {
        mView.showRepoInfo(repository);
        if(readmeSource == null){
            loadReadMe();
        }
    }

    @Override
    public void loadReadMe() {
        final String readmeFileUrl = AppConfig.GITHUB_API_BASE_URL + "repos/" + repository.getFullName()
                + "/" + "readme" + (StringUtils.isBlank(curBranch) ? "" : "?ref=" + curBranch);

        String branch = StringUtils.isBlank(curBranch) ? repository.getDefaultBranch() : curBranch;
        final String baseUrl = AppConfig.GITHUB_BASE_URL + repository.getFullName()
                + "/blob/" + branch  + "/" + "README.md";

//        if(!StringUtils.isBlank(readmeSource)){
//            mView.showReadMe(readmeSource, baseUrl);
//            return;
//        }

        mView.showReadMeLoader();
        HttpObserver<ResponseBody> httpObserver = new HttpObserver<ResponseBody>() {
            @Override
            public void onError(Throwable error) {
                if(error instanceof HttpPageNoFoundError){
                    mView.showNoReadMe();
                } else {
                    mView.showErrorToast(getErrorTip(error));
                }
            }

            @Override
            public void onSuccess(HttpResponse<ResponseBody> response) {
                try {
                    readmeSource = response.body().string();
                    readmeBaseUrl = baseUrl;
                    translatedReadmeSource = null;
                    detectedReadmeLanguage = null;
                    showingTranslatedReadme = false;
                    mView.hideTranslateButton();
                    mView.showReadMe(readmeSource, baseUrl);
                    detectReadmeLanguage();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        generalRxHttpExecute(new IObservableCreator<ResponseBody>() {
            @Override
            public Observable<Response<ResponseBody>> createObservable(boolean forceNetWork) {
                return getRepoService().getFileAsHtmlStream(forceNetWork, readmeFileUrl);
            }
        }, httpObserver, true);

    }

    private void detectReadmeLanguage() {
        ReadmeTranslationHelper.detectLanguage(readmeSource, sourceLanguageCode -> {
            if (mView == null) return;
            detectedReadmeLanguage = sourceLanguageCode;
            if (sourceLanguageCode != null) {
                mView.showTranslateButton(false);
            }
        });
    }

    @Override
    public void toggleReadmeTranslation() {
        if (showingTranslatedReadme) {
            showingTranslatedReadme = false;
            mView.updateReadMe(readmeSource, readmeBaseUrl);
            mView.showTranslateButton(false);
            return;
        }
        if (translatedReadmeSource != null) {
            showingTranslatedReadme = true;
            mView.updateReadMe(translatedReadmeSource, readmeBaseUrl);
            mView.showTranslateButton(true);
            return;
        }
        if (detectedReadmeLanguage == null) return;
        mView.setTranslateButtonBusy(true);
        ReadmeTranslationHelper.translate(readmeSource, detectedReadmeLanguage,
                new ReadmeTranslationHelper.TranslateCallback() {
                    @Override
                    public void onTranslated(String translatedHtml) {
                        if (mView == null) return;
                        mView.setTranslateButtonBusy(false);
                        translatedReadmeSource = translatedHtml;
                        showingTranslatedReadme = true;
                        mView.updateReadMe(translatedReadmeSource, readmeBaseUrl);
                        mView.showTranslateButton(true);
                    }

                    @Override
                    public void onError(String message) {
                        if (mView == null) return;
                        mView.setTranslateButtonBusy(false);
                        mView.showTranslateError(message);
                    }
                });
    }

    public Repository getRepository() {
        return repository;
    }

    /**
     * check if the string size is too large to save
     */
    private void checkReadmeSourceSize(){
        if(readmeSource != null && readmeSource.getBytes().length > 128 * 1024){
            readmeSource = null;
        }
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setCurBranch(String curBranch) {
        this.curBranch = curBranch;
        readmeSource = null;
    }
}
