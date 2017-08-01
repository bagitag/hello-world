package hu.bagitag.popularmovies;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

import hu.bagitag.popularmovies.database.PopularMovieContract;
import hu.bagitag.popularmovies.model.Movie;
import hu.bagitag.popularmovies.utilities.NetworkUtils;

public class MainActivity extends AppCompatActivity implements MoviesAdapter.MovieAdapterOnClickHandler,
        LoaderManager.LoaderCallbacks<List<Movie>> {

    public static final String INTENT_EXTRA_SELECTED_MOVIE = "selectedMovie";
    private static final String INSTANCE_STATE_SELECTED_URL = "selectedUrl";

    private static final int MOVIES_SEARCH_LOADER_ID = 98;
    public static final String MOVIES_SEARCH_LOADER_URL_BUNDLE = "loaderURL";

    private RecyclerView mRecyclerView;
    private TextView mErrorTextView;
    private TextView mTitleTextView;
    private ProgressBar mLoadingIndicator;

    private MoviesAdapter mMoviesAdapter;

    private String mMovieListUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            mMovieListUrl = savedInstanceState.getString(INSTANCE_STATE_SELECTED_URL);
        }

        mRecyclerView = (RecyclerView) findViewById(R.id.main_recyclerview);
        mErrorTextView = (TextView) findViewById(R.id.main_error_tv);
        mTitleTextView = (TextView) findViewById(R.id.main_title_tv);
        mLoadingIndicator = (ProgressBar) findViewById(R.id.main_progressbar);

        GridLayoutManager layoutManager = new GridLayoutManager(MainActivity.this, calculateColumnNumberToGridLayout());
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);

        mMoviesAdapter = new MoviesAdapter(this);
        mRecyclerView.setAdapter(mMoviesAdapter);

        if(mMovieListUrl == null && !NetworkUtils.isNetworkAvailable(this)) {
            mMovieListUrl = PopularMovieContract.MovieTableEntry.CONTENT_URI.toString();
        } else {
            downloadMoviesData(mMovieListUrl != null ? mMovieListUrl : NetworkUtils.POPULAR_MOVIEDB_URL);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        /**
         * Refreshing movie list only if the selected type is "Favorites" because users can remove on DetailActivity.
         *
         *
         * Can I replace this code with registering a ContentObserver to this CONTENT_URI?
         * Because I call:
         *      getContext().getContentResolver().notifyChange(uri, null);
         *  after deleting a movie in PopularMovieProvider.java.
         */
        if(mMovieListUrl != null && mMovieListUrl.equals(PopularMovieContract.MovieTableEntry.CONTENT_URI.toString())) {
            mMoviesAdapter.setMoviesList(null);
            downloadMoviesData(mMovieListUrl);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(INSTANCE_STATE_SELECTED_URL, mMovieListUrl);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mMovieListUrl = savedInstanceState.getString(INSTANCE_STATE_SELECTED_URL);
    }

    private int calculateColumnNumberToGridLayout() {
        int screenSize = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        boolean isScreenSizeLarge = screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE;
        boolean isOrientationPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

        if(isScreenSizeLarge) {
            return isOrientationPortrait ? 3 : 4;
        } else {
            return isOrientationPortrait ? 2 : 3;
        }
    }

    /**
     * Downloading movie data from web or from DB
     *
     * @param urlString
     */
    private void downloadMoviesData(String urlString) {
        mMovieListUrl = urlString;
        showData();

        LoaderManager loaderManager = getSupportLoaderManager();
        Loader<List<Movie>> movieSearchLoader = loaderManager.getLoader(MOVIES_SEARCH_LOADER_ID);

        Bundle queryBundle = new Bundle();
        queryBundle.putString(MOVIES_SEARCH_LOADER_URL_BUNDLE, mMovieListUrl);

        if (movieSearchLoader == null) {
            loaderManager.initLoader(MOVIES_SEARCH_LOADER_ID, queryBundle, this);
        } else {
            loaderManager.restartLoader(MOVIES_SEARCH_LOADER_ID, queryBundle, this);
        }
    }

    @Override
    public Loader<List<Movie>> onCreateLoader(int id, final Bundle args) {
        mLoadingIndicator.setVisibility(View.VISIBLE);

        return new MovieAsyncTaskLoader(this, args);
    }

    @Override
    public void onLoadFinished(Loader<List<Movie>> loader, List<Movie> moviesList) {
        mLoadingIndicator.setVisibility(View.INVISIBLE);

        if (moviesList != null && moviesList.size() > 0) {
            showData();
            mMoviesAdapter.setMoviesList(moviesList);
        } else {
            showError();
        }
    }

    @Override
    public void onLoaderReset(Loader<List<Movie>> loader) {
    }

    private void showData() {
        String title;

        switch (mMovieListUrl) {
            case NetworkUtils.POPULAR_MOVIEDB_URL:
                title = getString(R.string.main_activity_title_popularity_label);
                break;
            case NetworkUtils.TOP_RATED_MOVIEDB_URL:
                title = getString(R.string.main_activity_title_top_rated_label);
                break;
            default:
                title = getString(R.string.main_activity_title_favorites_label);
                break;
        }

        mTitleTextView.setText(title);
        mErrorTextView.setVisibility(View.INVISIBLE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    private void showError() {
        mRecyclerView.setVisibility(View.INVISIBLE);
        mErrorTextView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(Movie selectedMovie) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra(INTENT_EXTRA_SELECTED_MOVIE, selectedMovie);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_refresh) {
            mMoviesAdapter.setMoviesList(null);
            downloadMoviesData(mMovieListUrl);
            return true;
        } else if (id == R.id.action_sort_by_popularity) {
            mMoviesAdapter.setMoviesList(null);
            downloadMoviesData(NetworkUtils.POPULAR_MOVIEDB_URL);
            return true;
        } else if(id == R.id.action_sort_by_rating) {
            mMoviesAdapter.setMoviesList(null);
            downloadMoviesData(NetworkUtils.TOP_RATED_MOVIEDB_URL);
            return true;
        } else if(id == R.id.action_favorites) {
            mMoviesAdapter.setMoviesList(null);
            downloadMoviesData(PopularMovieContract.MovieTableEntry.CONTENT_URI.toString());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}
