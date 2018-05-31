package com.example.xyzreader.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

    private static final String TAG = Adapter.class.getSimpleName();

    private Cursor mCursor;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    Adapter(Cursor cursor) {
        mCursor = cursor;
    }

    @Override
    public long getItemId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getLong(ArticleLoader.Query._ID);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.list_item_article, parent, false);
        return new ViewHolder(view);
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

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Context context = holder.itemView.getContext();

        mCursor.moveToPosition(position);
        holder.mTitleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
        Date publishedDate = parsePublishedDate();
        if (!publishedDate.before(START_OF_EPOCH.getTime())) {

            holder.mSubtitleView.setText(Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            publishedDate.getTime(),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + "<br/>" + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR)));
        } else {
            holder.mSubtitleView.setText(Html.fromHtml(
                    outputFormat.format(publishedDate)
                            + "<br/>" + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR)));
        }

        Glide.with(context).load(mCursor.getString(ArticleLoader.Query.THUMB_URL))
                .into(holder.mThumbnailView);

    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.thumbnail)ImageView mThumbnailView;
        @BindView(R.id.article_title)TextView mTitleView;
        @BindView(R.id.article_subtitle)TextView mSubtitleView;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);

            final Context context = view.getContext();
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    context.startActivity(new Intent(Intent.ACTION_VIEW,
                            ItemsContract.Items.buildItemUri(getAdapterPosition())));
                }
            });

        }
    }
}


