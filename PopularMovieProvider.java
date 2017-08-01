package hu.bagitag.popularmovies.database;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class PopularMovieProvider extends ContentProvider {

    public static final int CODE_MOVIE = 100;
    public static final int CODE_MOVIE_WITH_ID = 101;

    public static final int CODE_TRAILER = 200;
    public static final int CODE_TRAILER_WITH_MOVIE_ID = 201;

    public static final int CODE_REVIEW = 300;
    public static final int CODE_REVIEW_WITH_MOVIE_ID = 301;

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private PopularMovieDBHelper mOpenHelper;

    /**
     * In onCreate, we initialize our content provider on startup. This method is called for all
     * registered content providers on the application main thread at application launch time.
     * It must not perform lengthy operations, or application startup will be delayed.
     */
    @Override
    public boolean onCreate() {
        mOpenHelper = new PopularMovieDBHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursor;
        final SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        switch (sUriMatcher.match(uri)) {
            case CODE_MOVIE: {
                cursor = db.query(
                        PopularMovieContract.MovieTableEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);

                break;
            }
            case CODE_MOVIE_WITH_ID: {

                String movieID = uri.getLastPathSegment();
                String[] selectionArguments = new String[]{movieID};

                cursor = db.query(
                        PopularMovieContract.MovieTableEntry.TABLE_NAME,
                        projection,
                        PopularMovieContract.MovieTableEntry.COLUMN_MOVIE_ID + " = ? ",
                        selectionArguments,
                        null,
                        null,
                        sortOrder);

                break;
            }
            case CODE_TRAILER: {
                cursor = db.query(
                        PopularMovieContract.TrailerTableEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);

                break;
            }
            case CODE_TRAILER_WITH_MOVIE_ID: {

                String movieID = uri.getLastPathSegment();
                String[] selectionArguments = new String[]{movieID};

                cursor = db.query(
                        PopularMovieContract.TrailerTableEntry.TABLE_NAME,
                        projection,
                        PopularMovieContract.TrailerTableEntry.COLUMN_MOVIE_ID + " = ? ",
                        selectionArguments,
                        null,
                        null,
                        sortOrder);

                break;
            }
            case CODE_REVIEW: {
                cursor = db.query(
                        PopularMovieContract.ReviewTableEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);

                break;
            }
            case CODE_REVIEW_WITH_MOVIE_ID: {

                String movieID = uri.getLastPathSegment();
                String[] selectionArguments = new String[]{movieID};

                cursor = db.query(
                        PopularMovieContract.ReviewTableEntry.TABLE_NAME,
                        projection,
                        PopularMovieContract.ReviewTableEntry.COLUMN_MOVIE_ID + " = ? ",
                        selectionArguments,
                        null,
                        null,
                        sortOrder);

                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int rowsInserted = 0;
        String tableName;

        switch (sUriMatcher.match(uri)) {
            case CODE_MOVIE:
                tableName = PopularMovieContract.MovieTableEntry.TABLE_NAME;
                break;
            case CODE_TRAILER:
                tableName = PopularMovieContract.TrailerTableEntry.TABLE_NAME;
                break;
            case CODE_REVIEW:
                tableName = PopularMovieContract.ReviewTableEntry.TABLE_NAME;
                break;
            default:
                return super.bulkInsert(uri, values);
        }

        db.beginTransaction();
        try {
            for (ContentValues value : values) {
                long _id = db.insert(tableName, null, value);

                if (_id != -1) {
                    rowsInserted++;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        if (rowsInserted > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsInserted;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String tableName;
        Uri resultUri;

        switch (sUriMatcher.match(uri)) {

            case CODE_MOVIE:
                tableName = PopularMovieContract.MovieTableEntry.TABLE_NAME;
                break;
            case CODE_TRAILER:
                tableName = PopularMovieContract.TrailerTableEntry.TABLE_NAME;
                break;
            case CODE_REVIEW:
                tableName = PopularMovieContract.ReviewTableEntry.TABLE_NAME;
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        long rowID = db.insert(tableName, null, contentValues);

        if (rowID > 0) {
            resultUri = ContentUris.withAppendedId(uri, rowID);
        } else {
            throw new RuntimeException("Failed to add a record into " + uri);
        }

        getContext().getContentResolver().notifyChange(resultUri, null);
        return resultUri;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        int numRowsDeleted;
        String tableName;
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        switch (sUriMatcher.match(uri)) {
            case CODE_MOVIE:
                tableName = PopularMovieContract.MovieTableEntry.TABLE_NAME;
                break;

            case CODE_TRAILER:
                tableName = PopularMovieContract.TrailerTableEntry.TABLE_NAME;
                break;

            case CODE_REVIEW:
                tableName = PopularMovieContract.ReviewTableEntry.TABLE_NAME;
                break;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        numRowsDeleted = db.delete(
                tableName,
                selection,
                selectionArgs);

        if (numRowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return numRowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        return 0;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        throw new RuntimeException("Not implemented.");
    }

    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = PopularMovieContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, PopularMovieContract.PATH_MOVIE, CODE_MOVIE);
        matcher.addURI(authority, PopularMovieContract.PATH_MOVIE + "/#", CODE_MOVIE_WITH_ID);

        matcher.addURI(authority, PopularMovieContract.PATH_TRAILER, CODE_TRAILER);
        matcher.addURI(authority, PopularMovieContract.PATH_TRAILER + "/#", CODE_TRAILER_WITH_MOVIE_ID);

        matcher.addURI(authority, PopularMovieContract.PATH_REVIEW, CODE_REVIEW);
        matcher.addURI(authority, PopularMovieContract.PATH_REVIEW + "/#", CODE_REVIEW_WITH_MOVIE_ID);

        return matcher;
    }
}
