package io.github.arpankapoor.db;

import android.provider.BaseColumns;

public class MessageContract {
    public static final String TEXT_TYPE = " TEXT";
    public static final String INTEGER_TYPE = " INTEGER";
    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + MessageEntry.TABLE_NAME;
    private static final String COMMA_SEP = ",";
    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + MessageEntry.TABLE_NAME + " (" +
                    MessageEntry._ID + INTEGER_TYPE + " PRIMARY KEY" + COMMA_SEP +
                    MessageEntry.COLUMN_NAME_SENDER_ID + INTEGER_TYPE + COMMA_SEP +
                    MessageEntry.COLUMN_NAME_STATUS + INTEGER_TYPE + COMMA_SEP +
                    MessageEntry.COLUMN_NAME_KEY + TEXT_TYPE + COMMA_SEP +
                    MessageEntry.COLUMN_NAME_FILENAME + TEXT_TYPE +
                    " )";

    public MessageContract() {
    }

    public static abstract class MessageEntry implements BaseColumns {
        public static final String TABLE_NAME = "message";
        public static final String COLUMN_NAME_SENDER_ID = "sender_id";
        public static final String COLUMN_NAME_STATUS = "status";
        public static final String COLUMN_NAME_KEY = "key";
        public static final String COLUMN_NAME_FILENAME = "filename";
        public static final int STATUS_KEY_RCVD = 0;
        public static final int STATUS_KEY_SENT = 1;
        public static final int STATUS_IMG_RCVD = 2;
    }
}
