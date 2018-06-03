package com.example.xyzreader.ui;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.Group;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * An activity representing a single Article detail screen.
 */
public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = ArticleDetailActivity.class.getSimpleName();
    private static final String EXTRA_ARTICLE_ID = "article_id";

    private Cursor mCursor;
    private long mArticleId;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    @BindView(R.id.coordinator_layout)CoordinatorLayout mCoordinatorLayout;
    @BindView(R.id.article_byline)TextView mBylineView;
    @BindView(R.id.article_body)TextView mBodyView;
    @BindView(R.id.toolbar)Toolbar mToolbar;
    @BindView(R.id.toolbar_layout)CollapsingToolbarLayout mToolbarLayout;
    @BindView(R.id.share_fab)FloatingActionButton mShareFab;
    @BindView(R.id.photo)ImageView mPhotoView;
    @BindView(R.id.progress_bar)ProgressBar mProgressBar;
    @BindView(R.id.content)Group mContentGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail);

        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mShareFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(ArticleDetailActivity.this)
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });

        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                Log.d(TAG, "URL: " + getIntent().getData());
                mArticleId = ItemsContract.Items.getItemId(getIntent().getData());
                Log.d(TAG, "Article ID: " + mArticleId);
            }
        }
        else {
            mArticleId = savedInstanceState.getLong(EXTRA_ARTICLE_ID);
        }

        getSupportLoaderManager().initLoader(0, null, this);

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(EXTRA_ARTICLE_ID, mArticleId);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        showProgressBar();
        return ArticleLoader.newInstanceForItemId(this, mArticleId);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor cursor) {

        hideProgressBar();

        mCursor = cursor;

        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();

    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> cursorLoader) {
        hideProgressBar();
        mCursor = null;
    }


    private void bindViews() {

        if (mCursor != null) {

            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                mBylineView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + " by <font color='#ffffff'>"
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));

            } else {
                // If date is before 1902, just show the string
                mBylineView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate) + " by <font color='#ffffff'>"
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));

            }
            mBodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)
                    .replaceAll("(\r\n|\n)", "<br />")));

            mToolbarLayout.setTitle(mCursor.getString(ArticleLoader.Query.TITLE));
            Glide.with(this)
                    .asBitmap()
                    .load(mCursor.getString(ArticleLoader.Query.PHOTO_URL))
                    .into(mPhotoView);

            mCursor.close();

        } else {
            showEmptyState();
        }
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    private void showProgressBar() {
        mContentGroup.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgressBar() {
        mProgressBar.setVisibility(View.GONE);
        mContentGroup.setVisibility(View.VISIBLE);
    }

    private void showEmptyState() {
        mBylineView.setText("N/A" );
        mBodyView.setText("N/A");
        Snackbar.make(mCoordinatorLayout, getString(R.string.empty_article), Snackbar.LENGTH_LONG)
        .show();
    }

}
