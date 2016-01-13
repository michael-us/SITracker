/*
 * Copyright 2016 Gleb Godonoga.
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

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.andrada.sitracker.contracts.AuthorItemListener;
import com.andrada.sitracker.contracts.IsNewItemTappedListener;
import com.andrada.sitracker.contracts.SIPrefs_;
import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.db.dao.AuthorDao;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.andrada.sitracker.events.AuthorMarkedAsReadEvent;
import com.andrada.sitracker.events.AuthorSelectedEvent;
import com.andrada.sitracker.ui.components.AuthorItemView;
import com.andrada.sitracker.ui.components.AuthorItemView_;
import com.andrada.sitracker.ui.widget.AuthorMultiSelector;
import com.andrada.sitracker.util.AnalyticsHelper;
import com.bignerdranch.android.multiselector.MultiSelectorBindingHolder;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.OrmLiteDao;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import de.greenrobot.event.EventBus;

@EBean
public class AuthorsAdapter extends RecyclerView.Adapter<AuthorsAdapter.ViewHolder> implements IsNewItemTappedListener {

    List<Author> authors = new ArrayList<Author>();
    long mNewAuthors;

    @OrmLiteDao(helper = SiDBHelper.class)
    AuthorDao authorDao;

    @Pref
    SIPrefs_ prefs;

    @RootContext
    Context context;

    /**
     * This is a singletone bean
     */
    @Bean
    AuthorMultiSelector mMultiSelector;

    WeakReference<AuthorItemListener> mAuthorItemListener;

    @AfterInject
    void initAdapter() {
        setHasStableIds(true);
        reloadAuthors();
    }

    public void updateContext(Context context) {
        this.context = context;
    }

    public void setAuthorItemListener(AuthorItemListener mAuthorItemListener) {
        this.mAuthorItemListener = new WeakReference<>(mAuthorItemListener);
    }

    /**
     * Reloads authors in background posting change set notification to UI Thread
     */
    @Background
    public void reloadAuthors() {
        try {
            int sortType = Integer.parseInt(prefs.authorsSortType().get());
            List<Author> newList;
            if (sortType == 0) {
                newList = authorDao.getAllAuthorsSortedAZ();
            } else {
                newList = authorDao.getAllAuthorsSortedNew();
            }
            mNewAuthors = authorDao.getNewAuthorsCount();
            postDataSetChanged(newList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @UiThread
    protected void postDataSetChanged(List<Author> newAuthors) {
        authors.clear();
        authors.addAll(newAuthors);
        notifyDataSetChanged();
    }

    public Author getItem(int position) {
        return authors.get(position);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewHolder vh = new ViewHolder(AuthorItemView_.build(context));
        vh.view.setListener(this);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (position < authors.size()) {
            holder.view.bind(authors.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return authors.size();
    }

    @Override
    public long getItemId(int position) {
        if (position > authors.size() - 1) {
            return -1;
        }
        return authors.get(position).getId();
    }

    @Override
    @Background
    public void onIsNewItemTapped(@NotNull View starButton) {
        Author auth = (Author) starButton.getTag();
        dismissAuthor(auth);
    }

    @Background
    public void markAuthorsRead(@NotNull List<Long> authorsToMarkAsRead) {
        for (long authId : authorsToMarkAsRead) {
            dismissAuthor(this.getAuthorById(authId));
        }
    }

    /**
     * Should be called on background thread only
     *
     * @param auth Author to mark as read
     */
    private void dismissAuthor(@Nullable Author auth) {
        if (auth != null) {
            auth.markRead();
            try {
                authorDao.update(auth);
                EventBus.getDefault().post(new AuthorMarkedAsReadEvent(auth));
            } catch (SQLException e) {
                AnalyticsHelper.getInstance().sendException("Author mark as read: ", e);
            }
        }
    }

    @Background
    public void removeAuthors(@NotNull final List<Long> authorsToRemove) {
        try {
            authorDao.callBatchTasks(new Callable<Object>() {
                @Nullable
                @Override
                public Object call() throws Exception {
                    for (Long anAuthorsToRemove : authorsToRemove) {
                        authorDao.removeAuthor(anAuthorsToRemove);
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            AnalyticsHelper.getInstance().sendException("Author Remove thread: ", e);
        }

        List<Author> authorCopy = new ArrayList<Author>(authors);
        for (Long anAuthorToRemove : authorsToRemove) {
            authorCopy.remove(getAuthorById(anAuthorToRemove));
        }
        postDataSetChanged(authorCopy);
    }

    @Nullable
    private Author getAuthorById(long authorId) {
        for (Author author : authors) {
            if (author.getId() == authorId) {
                return author;
            }
        }
        return null;
    }

    public int getItemPositionByAuthorId(long authorId) {
        for (int i = 0; i < authors.size(); i++) {
            if (authors.get(i).getId() == authorId) {
                return i;
            }
        }
        return -1;
    }

    public class ViewHolder extends MultiSelectorBindingHolder implements View.OnClickListener, View.OnLongClickListener {

        private boolean mIsSelectable;

        public AuthorItemView getView() {
            return view;
        }

        final AuthorItemView view;

        public ViewHolder(AuthorItemView itemView) {
            super(itemView, mMultiSelector);
            view = itemView;
            itemView.setOnClickListener(this);
            itemView.setLongClickable(true);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {

            if (mMultiSelector.tapSelection(this)) {
                if (mAuthorItemListener != null && mAuthorItemListener.get() != null) {
                    mAuthorItemListener.get().onAuthorItemClick();
                }
                // Selection is on, so tapSelection() toggled item selection.
            } else {
                // Selection is off; handle normal item click here.
                Author auth = getItem(getAdapterPosition());
                EventBus.getDefault().post(new AuthorSelectedEvent(auth.getId(), auth.getName()));
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (mAuthorItemListener != null && mAuthorItemListener.get() != null) {
                mAuthorItemListener.get().onAuthorItemLongClick(this);
                return true;
            }
            return false;
        }

        @Override
        public void setSelectable(boolean isSelectable) {
            boolean changed = isSelectable != this.mIsSelectable;
            this.mIsSelectable = isSelectable;
            if (changed && !isSelectable) {
                view.setChecked(false);
            }

        }

        @Override
        public boolean isSelectable() {
            return this.mIsSelectable;
        }

        @Override
        public void setActivated(boolean activated) {
            if (mIsSelectable) {
                view.setChecked(activated);
            } else if (!activated) {
                view.setChecked(false);
            }
        }

        @Override
        public boolean isActivated() {
            return view.isChecked();
        }
    }
}
