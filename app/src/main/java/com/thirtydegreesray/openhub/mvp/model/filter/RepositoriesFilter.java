package com.thirtydegreesray.openhub.mvp.model.filter;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import com.google.android.material.navigation.NavigationView;
import android.view.MenuItem;

import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.R2;
import com.thirtydegreesray.openhub.ui.fragment.RepositoriesFragment;
import com.thirtydegreesray.openhub.util.ViewUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ThirtyDegreesRay on 2017/11/9 16:30:13
 */

public class RepositoriesFilter implements Parcelable {

    public final static RepositoriesFilter DEFAULT = new RepositoriesFilter();

    public final static RepositoriesFilter DEFAULT_STARRED_REPO
            = new RepositoriesFilter().setSort(Sort.Created).setSortDirection(SortDirection.Desc);

    /** Matches Manage Ignore List's pre-existing hardcoded order (most recently ignored first). */
    public final static RepositoriesFilter DEFAULT_IGNORED_REPO
            = new RepositoriesFilter().setSort(Sort.IgnoredAt).setSortDirection(SortDirection.Desc);

    private enum Type{
        All, Owner, Public, Private, Member
    }

    private enum Sort{
        Created, Updated, Pushed, Full_name,
        // Only ever selectable from menu_ignored_repos_filter.xml (Manage
        // Ignore List's own end-drawer content, never Owned/Public's
        // menu_repositories_filter.xml) - getSort()'s string form is sent
        // as a literal query param to GitHub's list-repos REST API for
        // Owned/Public, which has no stars/forks/ignored-date sort value,
        // so these three must never be reachable from that drawer.
        Stars, Forks, IgnoredAt
    }

    /**
     * Client-side only - GitHub's list-repos REST endpoints have no
     * fork/archived query param, so this is applied by RepositoriesPresenter
     * against the already-fetched Repository.isFork()/isArchived() fields,
     * unlike Type/Sort above which are sent to the server as-is.
     */
    public enum Kind{
        All, Sources, Forks, Archived
    }

    private final static Map<Integer, Type> TYPE_RELATION = new HashMap<>();
    static {
        TYPE_RELATION.put(R.id.nav_all, Type.All);
        TYPE_RELATION.put(R.id.nav_owner, Type.Owner);
        TYPE_RELATION.put(R.id.nav_public, Type.Public);
        TYPE_RELATION.put(R.id.nav_private, Type.Private);
        TYPE_RELATION.put(R.id.nav_member, Type.Member);
    }

    private final static Map<Integer, Kind> KIND_RELATION = new HashMap<>();
    static {
        KIND_RELATION.put(R.id.nav_kind_all, Kind.All);
        KIND_RELATION.put(R.id.nav_kind_sources, Kind.Sources);
        KIND_RELATION.put(R.id.nav_kind_forks, Kind.Forks);
        KIND_RELATION.put(R.id.nav_kind_archived, Kind.Archived);
    }

    private Type type = Type.All;
    private Sort sort = Sort.Full_name;
    private SortDirection sortDirection = SortDirection.Asc;
    private Kind kind = Kind.All;

    /**
     * Client-side only, same reasoning as Kind above - GitHub's plain
     * list-repos REST endpoints (unlike the Search API Trending/Created/My
     * Topics/Search use) have no language query param at all. Null means no
     * language filter. Holds the display NAME (e.g. "Python"), not a search
     * slug, since it's matched directly against Repository.getLanguage(),
     * which returns that same linguist display name.
     */
    private String languageName;

    public static RepositoriesFilter generateFromDrawer(@NonNull NavigationView navView){
        RepositoriesFilter filter = new RepositoriesFilter();
        // getSelectedMenu() dereferences its argument's getSubMenu() with no
        // null check of its own - findItem() returns null outright for an id
        // a given drawer's menu resource doesn't declare at all (not just
        // hidden), which menu_ignored_repos_filter.xml doesn't for Type/Kind
        // (Manage Ignore List has no equivalent concepts), so this has to be
        // checked before calling it, same as the Language chooser below
        // already does.
        MenuItem typeChooserItem = navView.getMenu().findItem(R.id.nav_type_chooser);
        if (typeChooserItem != null) {
            MenuItem typeItem = ViewUtils.getSelectedMenu(typeChooserItem);
            if (typeItem != null) {
                filter.type = TYPE_RELATION.get(typeItem.getItemId());
            }
        }

        MenuItem sortItem = ViewUtils.getSelectedMenu(navView.getMenu().findItem(R.id.nav_sort));
        Sort sort = Sort.Full_name;
        SortDirection sortDirection = SortDirection.Asc;
        if(sortItem != null){
            switch (sortItem.getItemId()){
                case R.id.nav_full_name_asc:
                    sort = Sort.Full_name;
                    sortDirection = SortDirection.Asc;
                    break;
                case R.id.nav_full_name_desc:
                    sort = Sort.Full_name;
                    sortDirection = SortDirection.Desc;
                    break;
                case R.id.nav_recently_created:
                    sort = Sort.Created;
                    sortDirection = SortDirection.Desc;
                    break;
                case R.id.nav_previously_created:
                    sort = Sort.Created;
                    sortDirection = SortDirection.Asc;
                    break;

                case R.id.nav_recently_updated:
                    sort = Sort.Updated;
                    sortDirection = SortDirection.Desc;
                    break;
                case R.id.nav_least_recently_updated:
                    sort = Sort.Updated;
                    sortDirection = SortDirection.Asc;
                    break;
                case R.id.nav_most_pushed:
                    sort = Sort.Pushed;
                    sortDirection = SortDirection.Desc;
                    break;
                case R.id.nav_fewest_pushed:
                    sort = Sort.Pushed;
                    sortDirection = SortDirection.Asc;
                    break;

                // Manage Ignore List's own sort options (menu_ignored_repos_filter.xml) -
                // see the Sort enum's comment for why these three are safe
                // to add here despite not being valid GitHub API sort values.
                case R.id.nav_most_stars:
                    sort = Sort.Stars;
                    sortDirection = SortDirection.Desc;
                    break;
                case R.id.nav_fewest_stars:
                    sort = Sort.Stars;
                    sortDirection = SortDirection.Asc;
                    break;
                case R.id.nav_most_forks:
                    sort = Sort.Forks;
                    sortDirection = SortDirection.Desc;
                    break;
                case R.id.nav_fewest_forks:
                    sort = Sort.Forks;
                    sortDirection = SortDirection.Asc;
                    break;
                case R.id.nav_recently_ignored:
                    sort = Sort.IgnoredAt;
                    sortDirection = SortDirection.Desc;
                    break;
                case R.id.nav_previously_ignored:
                    sort = Sort.IgnoredAt;
                    sortDirection = SortDirection.Asc;
                    break;
            }
        }
        filter.sort = sort;
        filter.sortDirection = sortDirection;

        MenuItem kindChooserItem = navView.getMenu().findItem(R.id.nav_kind_chooser);
        MenuItem kindItem = kindChooserItem == null ? null : ViewUtils.getSelectedMenu(kindChooserItem);
        filter.kind = kindItem != null && KIND_RELATION.containsKey(kindItem.getItemId())
                ? KIND_RELATION.get(kindItem.getItemId()) : Kind.All;

        MenuItem languageChooserItem = navView.getMenu().findItem(R.id.nav_language_chooser);
        if (languageChooserItem != null) {
            MenuItem languageItem = ViewUtils.getSelectedMenu(languageChooserItem);
            filter.languageName = (languageItem != null && languageItem.getItemId() != R.id.nav_language_all)
                    ? languageItem.getTitle().toString() : null;
        }

        return filter;
    }

    public static void initDrawer(NavigationView navView,
                                  @NonNull RepositoriesFragment.RepositoriesType type){
        if(navView == null) return;
        if(RepositoriesFragment.RepositoriesType.OWNED.equals(type)){
            //do nothing
        } else if(RepositoriesFragment.RepositoriesType.PUBLIC.equals(type)){
            navView.getMenu().findItem(R.id.nav_private).setVisible(false);
            navView.getMenu().findItem(R.id.nav_public).setVisible(false);
        } else if(RepositoriesFragment.RepositoriesType.STARRED.equals(type)){
            navView.getMenu().findItem(R.id.nav_type_chooser).setVisible(false);
            navView.getMenu().findItem(R.id.nav_full_name_asc).setVisible(false);
            navView.getMenu().findItem(R.id.nav_full_name_desc).setVisible(false);
            navView.getMenu().findItem(R.id.nav_most_pushed).setVisible(false);
            navView.getMenu().findItem(R.id.nav_fewest_pushed).setVisible(false);

            navView.getMenu().findItem(R.id.nav_full_name_asc).setChecked(false);
            navView.getMenu().findItem(R.id.nav_recently_created).setChecked(true);
            navView.getMenu().findItem(R.id.nav_recently_created).setTitle(R.string.recently_starred);
            navView.getMenu().findItem(R.id.nav_previously_created).setTitle(R.string.previously_starred);
        }
    }

    public String getType() {
        return type.name().toLowerCase();
    }

    public String getSort() {
        return sort.name().toLowerCase();
    }

    public String getSortDirection() {
        return sortDirection.name().toLowerCase();
    }

    public Kind getKind() {
        return kind == null ? Kind.All : kind;
    }

    public String getLanguageName() {
        return languageName;
    }

    private RepositoriesFilter setType(Type type) {
        this.type = type;
        return this;
    }

    private RepositoriesFilter setSort(Sort sort) {
        this.sort = sort;
        return this;
    }

    private RepositoriesFilter setSortDirection(SortDirection sortDirection) {
        this.sortDirection = sortDirection;
        return this;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.type == null ? -1 : this.type.ordinal());
        dest.writeInt(this.sort == null ? -1 : this.sort.ordinal());
        dest.writeInt(this.sortDirection == null ? -1 : this.sortDirection.ordinal());
        dest.writeInt(this.kind == null ? -1 : this.kind.ordinal());
        dest.writeString(this.languageName);
    }

    public RepositoriesFilter() {
    }

    protected RepositoriesFilter(Parcel in) {
        int tmpType = in.readInt();
        this.type = tmpType == -1 ? null : Type.values()[tmpType];
        int tmpSort = in.readInt();
        this.sort = tmpSort == -1 ? null : Sort.values()[tmpSort];
        int tmpSortDirection = in.readInt();
        this.sortDirection = tmpSortDirection == -1 ? null : SortDirection.values()[tmpSortDirection];
        int tmpKind = in.readInt();
        this.kind = tmpKind == -1 ? null : Kind.values()[tmpKind];
        this.languageName = in.readString();
    }

    public static final Parcelable.Creator<RepositoriesFilter> CREATOR = new Parcelable.Creator<RepositoriesFilter>() {
        @Override
        public RepositoriesFilter createFromParcel(Parcel source) {
            return new RepositoriesFilter(source);
        }

        @Override
        public RepositoriesFilter[] newArray(int size) {
            return new RepositoriesFilter[size];
        }
    };

}
