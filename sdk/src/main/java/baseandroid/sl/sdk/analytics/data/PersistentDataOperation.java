package baseandroid.sl.sdk.analytics.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import org.json.JSONObject;

import baseandroid.sl.sdk.analytics.util.SlLog;

class PersistentDataOperation extends DataOperation {

    PersistentDataOperation(Context context) {
        super(context);
    }

    @Override
    String[] queryData(Uri uri, int limit) {
        return handleQueryUri(uri);
    }

    @Override
    int insertData(Uri uri, JSONObject jsonObject) {
        return handleInsertUri(uri, jsonObject);
    }

    @Override
    int insertData(Uri uri, ContentValues contentValues) {
        return 0;
    }

    private int handleInsertUri(Uri uri, JSONObject jsonObject) {
        if (uri == null) return -1;
        ContentValues contentValues = new ContentValues();
        String path = uri.getPath();
        if (!TextUtils.isEmpty(path)) {
            path = path.substring(1);
            switch (path) {
                case DbParams.TABLE_ACTIVITY_START_COUNT:
                    contentValues.put(DbParams.TABLE_ACTIVITY_START_COUNT, jsonObject.optInt(DbParams.VALUE));
                    break;
                case DbParams.TABLE_APP_END_DATA:
                    contentValues.put(DbParams.TABLE_APP_END_DATA, jsonObject.optString(DbParams.VALUE));
                    break;
                case DbParams.TABLE_APP_END_TIME:
                    contentValues.put(DbParams.TABLE_APP_END_TIME, jsonObject.optLong(DbParams.VALUE));
                    break;
                case DbParams.TABLE_APP_START_TIME:
                    contentValues.put(DbParams.TABLE_APP_START_TIME, jsonObject.optLong(DbParams.VALUE));
                    break;
                case DbParams.TABLE_SESSION_INTERVAL_TIME:
                    contentValues.put(DbParams.TABLE_SESSION_INTERVAL_TIME, jsonObject.optLong(DbParams.VALUE));
                    break;
                case DbParams.TABLE_LOGIN_ID:
                    contentValues.put(DbParams.TABLE_LOGIN_ID, jsonObject.optString(DbParams.VALUE));
                    break;
                default:
                    return -1;
            }
            contentResolver.insert(uri, contentValues);
        }
        return 0;
    }

    private String[] handleQueryUri(Uri uri) {
        if (uri == null) return null;
        String path = uri.getPath();
        if (TextUtils.isEmpty(path)) return null;
        Cursor cursor = null;
        try {
            path = path.substring(1);
            cursor = contentResolver.query(uri, null, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToNext();
                switch (path) {
                    case DbParams.TABLE_ACTIVITY_START_COUNT:
                        return new String[]{String.valueOf(cursor.getInt(0))};
                    case DbParams.TABLE_APP_END_DATA:
                    case DbParams.TABLE_LOGIN_ID:
                        return new String[]{cursor.getString(0)};
                    case DbParams.TABLE_APP_END_TIME:
                    case DbParams.TABLE_SESSION_INTERVAL_TIME:
                    case DbParams.TABLE_APP_START_TIME:
                        return new String[]{String.valueOf(cursor.getLong(0))};
                    default:
                        return null;
                }
            }
        } catch (Exception ex) {
            SlLog.printStackTrace(ex);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }
}
