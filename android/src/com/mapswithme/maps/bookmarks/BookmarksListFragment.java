package com.mapswithme.maps.bookmarks;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.cocosw.bottomsheet.BottomSheet;
import com.mapswithme.maps.Framework;
import com.mapswithme.maps.MWMActivity;
import com.mapswithme.maps.R;
import com.mapswithme.maps.base.BaseMwmListFragment;
import com.mapswithme.maps.bookmarks.data.Bookmark;
import com.mapswithme.maps.bookmarks.data.BookmarkCategory;
import com.mapswithme.maps.bookmarks.data.BookmarkManager;
import com.mapswithme.maps.bookmarks.data.Track;
import com.mapswithme.maps.widget.placepage.EditBookmarkFragment;
import com.mapswithme.util.BottomSheetHelper;
import com.mapswithme.util.sharing.ShareAction;
import com.mapswithme.util.sharing.SharingHelper;

public class BookmarksListFragment extends BaseMwmListFragment
                                implements AdapterView.OnItemLongClickListener,
                                           MenuItem.OnMenuItemClickListener
{
  public static final String TAG = BookmarksListFragment.class.getSimpleName();

  private BookmarkCategory mCategory;
  private int mCategoryIndex;
  private int mSelectedPosition;
  private BookmarkListAdapter mAdapter;

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    mCategoryIndex = getArguments().getInt(ChooseBookmarkCategoryActivity.BOOKMARK_CATEGORY_INDEX, -1);
    mCategory = BookmarkManager.INSTANCE.getCategoryById(mCategoryIndex);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
  {
    return inflater.inflate(R.layout.simple_list, container, false);
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState)
  {
    super.onViewCreated(view, savedInstanceState);
    initList();
    setHasOptionsMenu(true);
    ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(mCategory.getName());
  }

  @Override
  public void onResume()
  {
    super.onResume();

    mAdapter.startLocationUpdate();
    mAdapter.notifyDataSetChanged();
  }

  @Override
  public void onPause()
  {
    super.onPause();

    mAdapter.stopLocationUpdate();
  }

  private void initList()
  {
    mAdapter = new BookmarkListAdapter(getActivity(), mCategory);
    mAdapter.startLocationUpdate();
    setListAdapter(mAdapter);
    getListView().setOnItemLongClickListener(this);
  }

  @Override
  public void onListItemClick(ListView l, View v, int position, long id)
  {
    switch (mAdapter.getItemViewType(position))
    {
    case BookmarkListAdapter.TYPE_SECTION:
      return;
    case BookmarkListAdapter.TYPE_BOOKMARK:
      final Bookmark bookmark = (Bookmark) mAdapter.getItem(position);
      BookmarkManager.INSTANCE.showBookmarkOnMap(mCategoryIndex, bookmark.getBookmarkId());
      break;
    case BookmarkListAdapter.TYPE_TRACK:
      final Track track = (Track) mAdapter.getItem(position);
      Framework.nativeShowTrackRect(track.getCategoryId(), track.getTrackId());
      break;
    }

    final Intent i = new Intent(getActivity(), MWMActivity.class);
    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    startActivity(i);
  }

  @Override
  public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
  {
    mSelectedPosition = position;
    final Object item = mAdapter.getItem(mSelectedPosition);
    int type = mAdapter.getItemViewType(mSelectedPosition);

    switch (type)
    {
    case BookmarkListAdapter.TYPE_SECTION:
      // Do nothing here?
      break;

    case BookmarkListAdapter.TYPE_BOOKMARK:
      BottomSheet bs = BottomSheetHelper.create(getActivity())
                                        .title(((Bookmark) item).getName())
                                        .sheet(R.menu.menu_bookmarks)
                                        .listener(this)
                                        .build();

      if (!ShareAction.SMS_SHARE.isSupported(getActivity()))
        bs.getMenu().removeItem(R.id.share_message);

      if (!ShareAction.EMAIL_SHARE.isSupported(getActivity()))
        bs.getMenu().removeItem(R.id.share_email);

      bs.show();
      break;

    case BookmarkListAdapter.TYPE_TRACK:
      BottomSheetHelper.create(getActivity())
                       .title(((Track) item).getName())
                       .sheet(Menu.NONE, R.drawable.ic_delete, R.string.delete)
                       .listener(new DialogInterface.OnClickListener()
                       {
                         @Override
                         public void onClick(DialogInterface dialog, int which)
                         {
                           BookmarkManager.INSTANCE.deleteTrack((Track) item);
                           mAdapter.notifyDataSetChanged();
                         }
                       }).show();
      break;
    }

    return true;
  }

  @Override
  public boolean onMenuItemClick(MenuItem menuItem)
  {
    Bookmark item = (Bookmark) mAdapter.getItem(mSelectedPosition);

    switch (menuItem.getItemId())
    {
    case R.id.share_message:
      ShareAction.SMS_SHARE.shareMapObject(getActivity(), item);
      break;

    case R.id.share_email:
      ShareAction.EMAIL_SHARE.shareMapObject(getActivity(), item);
      break;

    case R.id.share:
      ShareAction.ANY_SHARE.shareMapObject(getActivity(), item);
      break;

    case R.id.edit:
      editBookmark(mCategory.getId(), item.getBookmarkId());
      break;

    case R.id.delete:
      BookmarkManager.INSTANCE.deleteBookmark(item);
      mAdapter.notifyDataSetChanged();
      break;
    }
    return false;
  }

  private void editBookmark(int cat, int bmk)
  {
    final Bundle args = new Bundle();
    args.putInt(EditBookmarkFragment.EXTRA_CATEGORY_ID, cat);
    args.putInt(EditBookmarkFragment.EXTRA_BOOKMARK_ID, bmk);
    final EditBookmarkFragment fragment = (EditBookmarkFragment) Fragment.instantiate(getActivity(), EditBookmarkFragment.class.getName(), args);
    fragment.setArguments(args);
    fragment.show(getActivity().getSupportFragmentManager(), null);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
  {
    inflater.inflate(R.menu.option_menu_bookmarks, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    if (item.getItemId() == R.id.set_share)
    {
      SharingHelper.shareBookmarksCategory(getActivity(), mCategory.getId());
      return true;
    }

    return super.onOptionsItemSelected(item);
  }
}
