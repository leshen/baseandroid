package baseandroid.sl.sdk.analytics.data;

import android.content.ContentValues;
import android.content.Context;


import org.json.JSONException;
import org.json.JSONObject;

import baseandroid.sl.sdk.analytics.encrypt.SlDataEncrypt;
import baseandroid.sl.sdk.analytics.util.SlLog;

public class DbAdapter {
    private static DbAdapter instance;
    private final DbParams mDbParams;
    private DataOperation mTrackEventOperation;
    private DataOperation mPersistentOperation;

    private DbAdapter(Context context, String packageName, SlDataEncrypt slDataEncrypt) {
        mDbParams = DbParams.getInstance(packageName);
        if (slDataEncrypt != null) {
            mTrackEventOperation = new EncryptDataOperation(context.getApplicationContext(), slDataEncrypt);
        } else {
            mTrackEventOperation = new EventDataOperation(context.getApplicationContext());
        }
        mPersistentOperation = new PersistentDataOperation(context.getApplicationContext());
    }

    public static DbAdapter getInstance(Context context, String packageName,
                                        SlDataEncrypt slDataEncrypt) {
        if (instance == null) {
            instance = new DbAdapter(context, packageName, slDataEncrypt);
        }
        return instance;
    }

    public static DbAdapter getInstance() {
        if (instance == null) {
            throw new IllegalStateException("The static method getInstance(Context context, String packageName) should be called before calling getInstance()");
        }
        return instance;
    }

    /**
     * Adds a JSON string representing an event with properties or a person record
     * to the SQLiteDatabase.
     *
     * @param j the JSON to record
     * @return the number of rows in the table, or DB_OUT_OF_MEMORY_ERROR/DB_UPDATE_ERROR
     * on failure
     */
    public int addJSON(JSONObject j) {
        int code = mTrackEventOperation.insertData(mDbParams.getEventUri(), j);
        if (code == 0) {
            return mTrackEventOperation.queryDataCount(mDbParams.getEventUri());
        }
        return code;
    }

    /**
     * Removes all events from table
     */
    public void deleteAllEvents() {
        mTrackEventOperation.deleteData(mDbParams.getEventUri(), DbParams.DB_DELETE_ALL);
    }

    /**
     * Removes events with an _id &lt;= last_id from table
     *
     * @param last_id the last id to delete
     * @return the number of rows in the table
     */
    public int cleanupEvents(String last_id) {
        mTrackEventOperation.deleteData(mDbParams.getEventUri(), last_id);
        return mTrackEventOperation.queryDataCount(mDbParams.getEventUri());
    }

    /**
     * 保存启动的页面个数
     *
     * @param activityCount 页面个数
     */
    public void commitActivityCount(int activityCount) {
        try {
            mPersistentOperation.insertData(mDbParams.getActivityStartCountUri(), new JSONObject().put(DbParams.VALUE, activityCount));
        } catch (JSONException e) {
            SlLog.printStackTrace(e);
        }
    }

    /**
     * 获取存储的页面个数
     *
     * @return 存储的页面个数
     */
    public int getActivityCount() {
        String[] values = mPersistentOperation.queryData(mDbParams.getActivityStartCountUri(), 1);
        if (values != null && values.length > 0) {
            return Integer.parseInt(values[0]);
        }
        return 0;
    }

    /**
     * 设置 Activity Start 的时间戳
     *
     * @param appStartTime Activity Start 的时间戳
     */
    public void commitAppStartTime(long appStartTime) {
        try {
            mPersistentOperation.insertData(mDbParams.getAppStartTimeUri(), new JSONObject().put(DbParams.VALUE, appStartTime));
        } catch (JSONException e) {
            SlLog.printStackTrace(e);
        }
    }

    /**
     * 获取 Activity Start 的时间戳
     *
     * @return Activity Start 的时间戳
     */
    public long getAppStartTime() {
        try {
            String[] values = mPersistentOperation.queryData(mDbParams.getAppStartTimeUri(), 1);
            if (values != null && values.length > 0) {
                return Long.parseLong(values[0]);
            }
        } catch (Exception ex) {
            SlLog.printStackTrace(ex);
        }
        return 0;
    }

    /**
     * 设置 Activity Pause 的时间戳
     *
     * @param appPausedTime Activity Pause 的时间戳
     */
    public void commitAppEndTime(long appPausedTime) {
        try {
            mPersistentOperation.insertData(mDbParams.getAppPausedUri(), new JSONObject().put(DbParams.VALUE, appPausedTime));
        } catch (JSONException e) {
            SlLog.printStackTrace(e);
        }
    }

    /**
     * 获取 Activity Pause 的时间戳
     *
     * @return Activity Pause 的时间戳
     */
    public long getAppEndTime() {
        try {
            String[] values = mPersistentOperation.queryData(mDbParams.getAppPausedUri(), 1);
            if (values != null && values.length > 0) {
                return Long.parseLong(values[0]);
            }
        } catch (Exception ex) {
            SlLog.printStackTrace(ex);
        }
        return 0;
    }

    /**
     * 设置 Activity End 的信息
     *
     * @param appEndData Activity End 的信息
     */
    public void commitAppEndData(String appEndData) {
        try {
            mPersistentOperation.insertData(mDbParams.getAppEndDataUri(), new JSONObject().put(DbParams.VALUE, appEndData));
        } catch (JSONException e) {
            SlLog.printStackTrace(e);
        }
    }

    /**
     * 获取 Activity End 的信息
     *
     * @return Activity End 的信息
     */
    public String getAppEndData() {
        String[] values = mPersistentOperation.queryData(mDbParams.getAppEndDataUri(), 1);
        if (values != null && values.length > 0) {
            return values[0];
        }
        return "";
    }

    /**
     * 存储 LoginId
     *
     * @param loginId 登录 Id
     */
    public void commitLoginId(String loginId) {
        try {
            mPersistentOperation.insertData(mDbParams.getLoginIdUri(), new JSONObject().put(DbParams.VALUE, loginId));
        } catch (JSONException e) {
            SlLog.printStackTrace(e);
        }
    }

    /**
     * 获取 LoginId
     *
     * @return LoginId
     */
    public String getLoginId() {
        String[] values = mPersistentOperation.queryData(mDbParams.getLoginIdUri(), 1);
        if (values != null && values.length > 0) {
            return values[0];
        }
        return "";
    }

    /**
     * 设置 Session 的时长
     *
     * @param sessionIntervalTime Session 的时长
     */
    public void commitSessionIntervalTime(int sessionIntervalTime) {
        try {
            mPersistentOperation.insertData(mDbParams.getSessionTimeUri(), new JSONObject().put(DbParams.VALUE, sessionIntervalTime));
        } catch (JSONException e) {
            SlLog.printStackTrace(e);
        }
    }

    /**
     * 获取 Session 的时长
     *
     * @return Session 的时长
     */
    public int getSessionIntervalTime() {
        try {
            String[] values = mPersistentOperation.queryData(mDbParams.getSessionTimeUri(), 1);
            if (values != null && values.length > 0) {
                return Integer.parseInt(values[0]);
            }
        } catch (Exception ex) {
            SlLog.printStackTrace(ex);
        }
        return 0;
    }

    /**
     * 查询表中是否有对应的事件
     *
     * @param eventName 事件名
     * @return false 表示已存在，true 表示不存在，是首次
     */
    public boolean isFirstChannelEvent(String eventName) {
        return mTrackEventOperation.queryDataCount(mDbParams.getChannelPersistentUri(), null, DbParams.KEY_CHANNEL_EVENT_NAME + " = ? ", new String[]{eventName}, null) <= 0;
    }

    /**
     * 添加渠道事件
     *
     * @param eventName 事件名
     */
    public void addChannelEvent(String eventName) {
        ContentValues values = new ContentValues();
        values.put(DbParams.KEY_CHANNEL_EVENT_NAME, eventName);
        values.put(DbParams.KEY_CHANNEL_RESULT, true);
        mTrackEventOperation.insertData(mDbParams.getChannelPersistentUri(), values);
    }

    /**
     * 从 Event 表中读取上报数据
     *
     * @param tableName 表名
     * @param limit 条数限制
     * @return 数据
     */
    public String[] generateDataString(String tableName, int limit) {
        return mTrackEventOperation.queryData(mDbParams.getEventUri(), limit);
    }
}