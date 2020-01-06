/*
 * Copyright 2014 Gleb Godonoga.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andrada.sitracker.ui.fragment.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.R;
import com.andrada.sitracker.analytics.AnalyticsManager;
import com.andrada.sitracker.analytics.PublicationOpenedEvent;
import com.andrada.sitracker.contracts.IsNewItemTappedListener;
import com.andrada.sitracker.contracts.SIPrefs_;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.db.dao.PublicationDao;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.andrada.sitracker.events.PublicationMarkedAsReadEvent;
import com.andrada.sitracker.ui.components.PublicationCategoryItemView;
import com.andrada.sitracker.ui.components.PublicationCategoryItemView_;
import com.andrada.sitracker.ui.components.PublicationItemView;
import com.andrada.sitracker.ui.components.PublicationItemView_;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.OrmLiteDao;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;

import static com.andrada.sitracker.util.LogUtils.LOGE;

@EBean
public class PublicationsAdapter extends BaseExpandableListAdapter implements
        IsNewItemTappedListener, AdapterView.OnItemLongClickListener {
    private final Map<Long, Publication> mDownloadingPublications = new HashMap<>();
    private List<CategoryValue> mCategories = new ArrayList<>();
    @OrmLiteDao(helper = SiDBHelper.class)
    PublicationDao publicationsDao;
    @RootContext
    Context context;
    @Pref
    SIPrefs_ prefs;
    @Nullable
    ListView listView = null;
    boolean shouldShowImages;
    private PublicationShareAttemptListener listener;
    private boolean showcaseViewShown = false;

    @Background
    public void reloadPublicationsForAuthorId(long id) {
        try {
            shouldShowImages = prefs.displayPubImages().get();

            final List<CategoryValue> categories = new ArrayList<>();
            final Map<CategoryInfo, CategoryValue> categoriesByInfo = new HashMap<>();

            final List<Publication> publications = publicationsDao.getSortedPublicationsForAuthorId(id);
            for (Publication publication : publications) {
                final CategoryInfo info = new CategoryInfo(publication.getCategory(), false);
                CategoryValue category = categoriesByInfo.get(info);
                if (category == null) {
                    category = new CategoryValue(info);
                    categoriesByInfo.put(info, category);
                    categories.add(category);
                }
                category.add(publication);
            }

            updateAdapterDataSet(categories);
        } catch (SQLException e) {
            LOGE("SiTracker", "Exception while reloading pubs", e);
        }
    }

    @UiThread(propagation = UiThread.Propagation.REUSE)
    void updateAdapterDataSet(List<CategoryValue> categories) {
        mCategories = categories;
        postDataSetChanged();
    }

    @UiThread(delay = 300)
    void createAndShowShowcaseView(View view) {
        if (context instanceof Activity) {
            new ShowcaseView.Builder((Activity) context)
                    .setTarget(new ViewTarget(view))
                    .setContentTitle(context.getString(R.string.showcase_pub_quick_title))
                    .setContentText(context.getString(R.string.showcase_pub_quick_detail))
                    .setStyle(R.style.ShowcaseView_Base)
                    .singleShot(Constants.SHOWCASE_PUBLICATION_QUICK_ACCESS_SHOT_ID)
                    .build();
        }
    }

    @UiThread(propagation = UiThread.Propagation.REUSE)
    void postDataSetChanged() {
        notifyDataSetChanged();
    }

    @Override
    public int getGroupCount() {
        return mCategories.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return mCategories.get(groupPosition).getPublications().size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mCategories.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        final List<Publication> publications = mCategories.get(groupPosition).getPublications();
        return publications.get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        final List<Publication> publications = mCategories.get(groupPosition).getPublications();
        return publications.get(childPosition).getId();
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @NotNull
    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             @Nullable View convertView, ViewGroup parent) {
        if (listView == null) {
            listView = (ListView) parent;
        }
        PublicationCategoryItemView publicationCategoryView;
        //For some weird reason, convertView is PublicationItemView instead of PublicationCategoryItemView_
        if (!(convertView instanceof PublicationCategoryItemView)) {
            publicationCategoryView = PublicationCategoryItemView_.build(context);
        } else {
            publicationCategoryView = (PublicationCategoryItemView) convertView;
        }
        final CategoryValue category = mCategories.get(groupPosition);
        publicationCategoryView.bind(category.getName(), category.getPublications().size(),
                category.getNewCount());
        return publicationCategoryView;
    }

    @NotNull
    @Override
    public View getChildView(int groupPosition, int childPosition,
                             boolean isLastChild, @Nullable View convertView, ViewGroup parent) {

        Publication pub = (Publication) getChild(groupPosition, childPosition);

        PublicationItemView publicationItemView;
        if (convertView == null) {
            publicationItemView = PublicationItemView_.build(context);
            publicationItemView.setListener(this);
        } else {
            publicationItemView = (PublicationItemView) convertView;
        }
        publicationItemView.bind(pub, shouldShowImages);

        if (!showcaseViewShown) {
            showcaseViewShown = true;
            createAndShowShowcaseView(publicationItemView);
        }

        return publicationItemView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public void onIsNewItemTapped(@NotNull View checkBox) {
        if (listView != null) {
            Publication pub = (Publication) checkBox.getTag();
            if (pub != null) {
                updateStatusOfPublication(pub);
            }
        }
    }

    public void stopProgressOnPublication(long id, boolean success) {
        Publication loadingPub = mDownloadingPublications.get(id);
        if (loadingPub != null) {
            loadingPub.setLoading(false);
            mDownloadingPublications.remove(id);
            if (success) {
                updateStatusOfPublication(loadingPub);
            }
        }
        notifyDataSetChanged();
    }

    public void setShareListener(PublicationShareAttemptListener listener) {
        this.listener = listener;
    }

    @Background
    protected void updateStatusOfPublication(@NotNull Publication pub) {
        if (pub.getNew()) {
            try {
                final CategoryInfo info = new CategoryInfo(pub.getCategory(), false);
                for (CategoryValue category: mCategories) {
                    if (category.info.equals(info)) {
                        category.decrementNewCount();
                        break;
                    }
                }
                boolean authorNewChanged = publicationsDao.markPublicationRead(pub);
                EventBus.getDefault().post(new PublicationMarkedAsReadEvent(authorNewChanged));
            } catch (SQLException e) {
                AnalyticsManager.getInstance().sendException("Publication Set update", e);
            }
            postDataSetChanged();
        }
    }

    @Override
    public boolean onItemLongClick(@NotNull AdapterView<?> parent, View view, int position, long id) {
        if (ExpandableListView.getPackedPositionType(id) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            long packedPosition = ((ExpandableListView) parent).getExpandableListPosition(position);
            int groupPosition = ExpandableListView.getPackedPositionGroup(packedPosition);
            int childPosition = ExpandableListView.getPackedPositionChild(packedPosition);
            final List<Publication> publications = mCategories.get(groupPosition).getPublications();
            Publication pub = publications.get(childPosition);

            if (pub.getLoading()) {
                //Ignore if it is loading now
                return true;
            }
            if (listener != null) {
                //Mark item as loading
                mDownloadingPublications.put(pub.getId(), pub);
                pub.setLoading(true);
                notifyDataSetChanged();

                //Attempt to open or download publication
                listener.publicationShare(pub, pub.getNew());

                AnalyticsManager.getInstance().logEvent(new PublicationOpenedEvent(pub.getName(), false));
            }
            // Return true as we are handling the event.
            return true;
        }

        return false;
    }

    public interface PublicationShareAttemptListener {
        void publicationShare(Publication pub, boolean forceDownload);
    }

    private class CategoryInfo {
        public final String name;
        public final boolean isCustom;

        private CategoryInfo(@NotNull String categoryName, boolean isCustom) {
            this.name = categoryName;
            this.isCustom = isCustom;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            final CategoryInfo that = (CategoryInfo) o;
            return name.equals(that.name) && isCustom == that.isCustom;
        }

        @Override
        public int hashCode() {
            return name.hashCode() * 31 + (isCustom ? 1 : 0);
        }
    }

    public class CategoryValue {
        private final CategoryInfo info;
        private final List<Publication> publications;
        private int newCount;

        private CategoryValue(CategoryInfo info) {
            this.info = info;
            publications = new ArrayList<>();
            newCount = 0;
        }

        public String getName(){
            return info.name;
        }

        public boolean isCustom() {
            return info.isCustom;
        }

        private void decrementNewCount() {
            --newCount;
        }

        public int getNewCount() {
            return newCount;
        }

        public List<Publication> getPublications() {
            return publications;
        }

        public void add(Publication publication) {
            publications.add(publication);
            if (publication.getNew())
                ++newCount;
        }
    }
}
