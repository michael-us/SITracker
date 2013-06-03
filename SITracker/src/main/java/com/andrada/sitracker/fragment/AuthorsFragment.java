package com.andrada.sitracker.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.andrada.sitracker.R;
import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.db.manager.SiSQLiteHelper;
import com.andrada.sitracker.task.AddAuthorTask;
import com.andrada.sitracker.util.DateFormatterUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AuthorsFragment extends ListFragment implements AddAuthorTask.IAuthorTaskCallback {

	OnAuthorSelectedListener mCallback;

    public interface OnAuthorSelectedListener {
		public void onAuthorSelected(long id);
        public void onAuthorAdded();
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

	@Override
	public void onStart() {
		super.onStart();

		// When in two-pane layout, set the listview to highlight the selected
		// list item
		// (We do this during onStart because at the point the listview is
		// available.)
		if (getFragmentManager().findFragmentById(R.id.fragment_publications) != null) {
			getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            getListView().setSelector(R.drawable.authors_list_selector);
        }
        getListView().setBackgroundResource(R.drawable.authors_list_background);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		try {
			updateView();
			// This makes sure that the container activity has implemented
			// the callback interface. If not, it throws an exception.
			mCallback = (OnAuthorSelectedListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnAuthorSelectedListener");
		}
	}

    @Override
    public void onDetach() {
        super.onDetach();
        mCallback = null;
    }

	public void updateView() {
		try {
			SiSQLiteHelper helper = new SiSQLiteHelper(getActivity());
			List<Author> authors = helper.getAuthorDao().queryForAll();
			setListAdapter(new AuthorsAdapter(authors, getActivity()));
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	public void updateViewAtPosition(int position) {
		updateView();
		getListView().setItemChecked(position, true);
		getListView().setSelection(position);
	}

    public void tryAddAuthor(String url) {
        new AddAuthorTask((Context)mCallback, this).execute(url);
    }

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		// Notify the parent activity of selected item
		mCallback.onAuthorSelected(id);

		// Set the item as checked to be highlighted when in two-pane layout
		getListView().setItemChecked(position, true);
	}

    @Override
    public void deliverResults(String message) {
        if (message.length() == 0) {
            //This is success
            mCallback.onAuthorAdded();
        } else {
            Toast.makeText((Context)mCallback, message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void operationStart() {

    }

    @Override
    public void onProgressUpdate(int percent) {

    }

	
	private class AuthorsAdapter extends BaseAdapter {
		List<Author> authors;
		private LayoutInflater inflater;
		
		public AuthorsAdapter(List<Author> authors, Context context) {
			this.authors = new ArrayList<Author>();
			this.authors.addAll(authors);
			this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return authors.size();
		}

		@Override
		public Object getItem(int position) {
			return authors.get(position);
		}

		@Override
		public long getItemId(int position) {
			return authors.get(position).getId();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = inflater.inflate(R.layout.authors_list_item, parent,false);
            }
            if (authors.get(position).isUpdated()) {
                view.setBackgroundResource(R.drawable.authors_list_item_selector_new);
            } else {
                view.setBackgroundResource(R.drawable.authors_list_item_selector_normal);
            }

			TextView authorTitle = (TextView) view.findViewById(R.id.author_title);
            authorTitle.setText(authors.get(position).getName());
            CheckBox updated = (CheckBox)view.findViewById(R.id.author_updated);
            updated.setChecked(authors.get(position).isUpdated());
            TextView authorUpdateDate = (TextView) view.findViewById(R.id.author_update_date);
            authorUpdateDate.setText(DateFormatterUtil.getFriendlyDateRelativeToToday(authors.get(position).getUpdateDate()));

            return view;
		}
		
	}
}
