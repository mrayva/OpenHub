package com.thirtydegreesray.openhub.mvp.model.request;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * Body for POST/PATCH gists. GitHub's real PATCH semantics: a filename key
 * omitted from files is left unchanged; an explicit null value deletes that
 * file; a new or changed key adds/updates it - so an edit request must include
 * a null entry for every file that was removed, not just the ones that remain.
 * Renaming a file is done here as delete-old + add-new rather than GitHub's
 * dedicated rename-via-old-key-with-new-filename-field mechanism, since the
 * end result is the same and it keeps this model - and the diffing logic that
 * builds it - much simpler.
 */
public class CreateGistModel {

    private String description;

    @SerializedName("public")
    private boolean isPublic;

    private Map<String, GistFileContent> files;

    public CreateGistModel(String description, boolean isPublic, Map<String, GistFileContent> files) {
        this.description = description;
        this.isPublic = isPublic;
        this.files = files;
    }

    public String getDescription() {
        return description;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public Map<String, GistFileContent> getFiles() {
        return files;
    }

}
