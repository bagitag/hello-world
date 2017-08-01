package hu.bagitag.popularmovies;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import hu.bagitag.popularmovies.database.PopularMovieContract;
import hu.bagitag.popularmovies.database.PopularMovieDBHelper;
import hu.bagitag.popularmovies.model.Movie;
import hu.bagitag.popularmovies.utilities.MovieDBJsonUtils;
import hu.bagitag.popularmovies.utilities.NetworkUtils;

public class MovieAsyncTaskLoader extends AsyncTaskLoader<List<Movie>> {

    private Bundle mBundleArgs;

    private List<Movie> cacheMovieList;

    public MovieAsyncTaskLoader(Context context, Bundle args) {
        super(context);
        mBundleArgs = args;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();

        if(mBundleArgs == null || mBundleArgs.size() == 0) {
            return;
        }

        if(cacheMovieList != null) {
            deliverResult(cacheMovieList);
            return;
        }
        forceLoad();
    }

    @Override
    public List<Movie> loadInBackground() {
        String urlString = mBundleArgs.getString(MainActivity.MOVIES_SEARCH_LOADER_URL_BUNDLE);
        URL moviesRequestUrl = NetworkUtils.buildUrl(urlString);

        try {
            if(moviesRequestUrl != null) {
                String jsonMoviesResponse = NetworkUtils.getResponseFromHttpUrl(moviesRequestUrl);
                cacheMovieList = MovieDBJsonUtils.getMovieListFromJSON(jsonMoviesResponse);
            } else {
                if(PopularMovieContract.MovieTableEntry.CONTENT_URI.toString().equals(urlString)) {
                    ContentResolver popularMovieContentResolver = getContext().getContentResolver();
                    Cursor cursor = popularMovieContentResolver.query(Uri.parse(urlString), null, null, null, null);

                    try {
                        if (cursor != null && cursor.getCount() > 0) {
                            cacheMovieList = new ArrayList<>();
                            cursor.moveToFirst();

                            while (!cursor.isAfterLast()) {
                                Movie data = PopularMovieDBHelper.getMovieFromCursor(cursor);
                                cacheMovieList.add(data);
                                cursor.moveToNext();
                            }
                        }
                    } finally {
                        if(cursor != null) {
                            cursor.close();
                        }
                    }
                }
            }
            return cacheMovieList;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
