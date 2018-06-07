package com.example.xyzreader.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.UpdaterService;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LOADER_ID = 0;

    private boolean mIsRefreshing = false;

    @BindView(R.id.toolbar) Toolbar mToolbar;
    @BindView(R.id.swipe_refresh_layout) SwipeRefreshLayout mSwipeRefreshLayout;
    @BindView(R.id.recycler_view) RecyclerView mRecyclerView;
    @BindView(R.id.coordinator_layout)CoordinatorLayout mCoordinatorLayout;
    @BindView(R.id.empty_state_tv)TextView mEmptyStateTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        if (savedInstanceState == null) {
            refresh();
        } else {
            getSupportLoaderManager().initLoader(LOADER_ID, null, this);
        }

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int itemId = item.getItemId();

        switch (itemId) {
            case R.id.action_refresh:
                refresh();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void refresh() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
            startService(new Intent(this, UpdaterService.class));
        } else {
            Snackbar.make(mCoordinatorLayout, getString(R.string.no_internet_state), Snackbar.LENGTH_LONG)
                    .show();
            showEmptyState();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
            if (!mIsRefreshing) {
                getSupportLoaderManager().restartLoader(LOADER_ID, null, ArticleListActivity.this);
            }
        }
    };

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursor == null || !cursor.moveToFirst()) {
            showEmptyState();
            return;
        }
        hideEmptyState();
        Adapter adapter = new Adapter(cursor);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        showEmptyState();
        mRecyclerView.setAdapter(null);
    }

    private void showEmptyState() {
        mRecyclerView.setVisibility(View.GONE);
        mEmptyStateTv.setVisibility(View.VISIBLE);
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void hideEmptyState() {
        mEmptyStateTv.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }

}
