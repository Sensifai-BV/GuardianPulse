package com.guardianpulse.prototype.data;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Float;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@SuppressWarnings({"unchecked", "deprecation"})
public final class EventLogDao_Impl implements EventLogDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<EventLog> __insertionAdapterOfEventLog;

  private final SharedSQLiteStatement __preparedStmtOfDeleteOlderThan;

  public EventLogDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfEventLog = new EntityInsertionAdapter<EventLog>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `event_logs` (`id`,`timestamp`,`hrValue`,`audioLevel`,`hrFlag`,`audioFlag`,`alertLevel`,`isAcknowledged`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final EventLog entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getTimestamp());
        if (entity.getHrValue() == null) {
          statement.bindNull(3);
        } else {
          statement.bindDouble(3, entity.getHrValue());
        }
        if (entity.getAudioLevel() == null) {
          statement.bindNull(4);
        } else {
          statement.bindDouble(4, entity.getAudioLevel());
        }
        final int _tmp = entity.getHrFlag() ? 1 : 0;
        statement.bindLong(5, _tmp);
        final int _tmp_1 = entity.getAudioFlag() ? 1 : 0;
        statement.bindLong(6, _tmp_1);
        statement.bindLong(7, entity.getAlertLevel());
        final int _tmp_2 = entity.isAcknowledged() ? 1 : 0;
        statement.bindLong(8, _tmp_2);
      }
    };
    this.__preparedStmtOfDeleteOlderThan = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM event_logs WHERE timestamp < ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final EventLog log, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfEventLog.insert(log);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteOlderThan(final long thresholdTime,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteOlderThan.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, thresholdTime);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteOlderThan.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<EventLog>> getRecentLogs() {
    final String _sql = "SELECT * FROM event_logs ORDER BY timestamp DESC LIMIT 100";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"event_logs"}, new Callable<List<EventLog>>() {
      @Override
      @NonNull
      public List<EventLog> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfHrValue = CursorUtil.getColumnIndexOrThrow(_cursor, "hrValue");
          final int _cursorIndexOfAudioLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "audioLevel");
          final int _cursorIndexOfHrFlag = CursorUtil.getColumnIndexOrThrow(_cursor, "hrFlag");
          final int _cursorIndexOfAudioFlag = CursorUtil.getColumnIndexOrThrow(_cursor, "audioFlag");
          final int _cursorIndexOfAlertLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "alertLevel");
          final int _cursorIndexOfIsAcknowledged = CursorUtil.getColumnIndexOrThrow(_cursor, "isAcknowledged");
          final List<EventLog> _result = new ArrayList<EventLog>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final EventLog _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final Float _tmpHrValue;
            if (_cursor.isNull(_cursorIndexOfHrValue)) {
              _tmpHrValue = null;
            } else {
              _tmpHrValue = _cursor.getFloat(_cursorIndexOfHrValue);
            }
            final Float _tmpAudioLevel;
            if (_cursor.isNull(_cursorIndexOfAudioLevel)) {
              _tmpAudioLevel = null;
            } else {
              _tmpAudioLevel = _cursor.getFloat(_cursorIndexOfAudioLevel);
            }
            final boolean _tmpHrFlag;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHrFlag);
            _tmpHrFlag = _tmp != 0;
            final boolean _tmpAudioFlag;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfAudioFlag);
            _tmpAudioFlag = _tmp_1 != 0;
            final int _tmpAlertLevel;
            _tmpAlertLevel = _cursor.getInt(_cursorIndexOfAlertLevel);
            final boolean _tmpIsAcknowledged;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsAcknowledged);
            _tmpIsAcknowledged = _tmp_2 != 0;
            _item = new EventLog(_tmpId,_tmpTimestamp,_tmpHrValue,_tmpAudioLevel,_tmpHrFlag,_tmpAudioFlag,_tmpAlertLevel,_tmpIsAcknowledged);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
