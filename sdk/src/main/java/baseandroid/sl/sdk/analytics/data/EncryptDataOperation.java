
package baseandroid.sl.sdk.analytics.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import baseandroid.sl.sdk.analytics.encrypt.SlDataEncrypt;
import baseandroid.sl.sdk.analytics.util.SlLog;

class EncryptDataOperation extends DataOperation {

    private SlDataEncrypt mSlDataEncrypt;

    EncryptDataOperation(Context context, SlDataEncrypt slDataEncrypt) {
        super(context);
        this.mSlDataEncrypt = slDataEncrypt;
    }

    @Override
    int insertData(Uri uri, JSONObject jsonObject) {
        try {
            if (deleteDataLowMemory(uri) != 0) {
                return DbParams.DB_OUT_OF_MEMORY_ERROR;
            }
            jsonObject = mSlDataEncrypt.encryptTrackData(jsonObject);
            ContentValues cv = new ContentValues();
            cv.put(DbParams.KEY_DATA, jsonObject.toString() + "\t" + jsonObject.toString().hashCode());
            cv.put(DbParams.KEY_CREATED_AT, System.currentTimeMillis());
            contentResolver.insert(uri, cv);
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
        return 0;
    }

    @Override
    int insertData(Uri uri, ContentValues contentValues) {
        try {
            if (deleteDataLowMemory(uri) != 0) {
                return DbParams.DB_OUT_OF_MEMORY_ERROR;
            }
            contentResolver.insert(uri, contentValues);
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
        return 0;
    }

    @Override
    String[] queryData(Uri uri, int limit) {
        Cursor cursor = null;
        String data = null;
        String last_id = null;
        String gzipType = DbParams.GZIP_DATA_ENCRYPT;
        try {
            Map<String, JSONArray> dataEncryptMap = new HashMap<>();
            JSONArray dataJsonArray = new JSONArray();
            cursor = contentResolver.query(uri, null, null, null, DbParams.KEY_CREATED_AT + " ASC LIMIT " + limit);
            if (cursor != null) {
                String keyData;
                JSONObject jsonObject;
                final String EKEY = "ekey";
                final String KEY_VER = "pkv";
                final String PAYLOADS = "payloads";
                while (cursor.moveToNext()) {
                    if (cursor.isLast()) {
                        last_id = cursor.getString(cursor.getColumnIndex("_id"));
                    }
                    try {
                        keyData = cursor.getString(cursor.getColumnIndex(DbParams.KEY_DATA));
                        keyData = parseData(keyData);
                        if (TextUtils.isEmpty(keyData)) {
                            continue;
                        }

                        jsonObject = new JSONObject(keyData);
                        boolean isHasEkey = jsonObject.has(EKEY);
                        if (!isHasEkey) { // 如果没有包含 Ekey 字段，则重新进行加密
                            jsonObject = mSlDataEncrypt.encryptTrackData(jsonObject);
                        }

                        if (jsonObject.has(EKEY)) {
                            String key = jsonObject.getString(EKEY) + "$" + jsonObject.getInt(KEY_VER);
                            if (dataEncryptMap.containsKey(key)) {
                                dataEncryptMap.get(key).put(jsonObject.getString(PAYLOADS));
                            } else {
                                JSONArray jsonArray = new JSONArray();
                                jsonArray.put(jsonObject.getString(PAYLOADS));
                                dataEncryptMap.put(key, jsonArray);
                            }
                        } else {
                            dataJsonArray.put(jsonObject);
                        }
                    } catch (Exception e) {
                        SlLog.printStackTrace(e);
                    }
                }
                JSONArray dataEncryptJsonArray = new JSONArray();
                for (String key : dataEncryptMap.keySet()) {
                    jsonObject = new JSONObject();
                    jsonObject.put(EKEY, key.substring(0, key.indexOf("$")));
                    jsonObject.put(KEY_VER, Integer.valueOf(key.substring(key.indexOf("$") + 1)));
                    jsonObject.put(PAYLOADS, dataEncryptMap.get(key));
                    jsonObject.put("flush_time", System.currentTimeMillis());
                    dataEncryptJsonArray.put(jsonObject);
                }
                if (dataEncryptJsonArray.length() > 0) {
                    data = dataEncryptJsonArray.toString();
                } else {
                    data = dataJsonArray.toString();
                    gzipType = DbParams.GZIP_DATA_EVENT;
                }
            }
        } catch (Exception ex) {
            SlLog.printStackTrace(ex);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (last_id != null) {
            return new String[]{last_id, data, gzipType};
        }
        return null;
    }

    @Override
    void deleteData(Uri uri, String id) {
        super.deleteData(uri, id);
    }
}
