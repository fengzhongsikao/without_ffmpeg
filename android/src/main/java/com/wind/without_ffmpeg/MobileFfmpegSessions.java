package com.wind.without_ffmpeg;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arthenica.mobileffmpeg.FFmpeg;
import com.arthenica.mobileffmpeg.FFprobe;
import com.arthenica.mobileffmpeg.MediaInformation;
import com.arthenica.mobileffmpeg.Statistics;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** In-memory session store backed by mobile-ffmpeg 4.4. */
final class MobileFfmpegSessions {

  static final int SESSION_TYPE_FFMPEG = 1;
  static final int SESSION_TYPE_FFPROBE = 2;
  static final int SESSION_TYPE_MEDIA_INFORMATION = 3;

  static final int STATE_CREATED = 0;
  static final int STATE_RUNNING = 1;
  static final int STATE_FAILED = 2;
  static final int STATE_COMPLETED = 3;

  static final int DEFAULT_LOG_WAIT_MS = 5000;

  private static final AtomicLong NEXT_ID = new AtomicLong(1);
  private static final ConcurrentHashMap<Long, Session> SESSIONS = new ConcurrentHashMap<>();
  private static final ConcurrentHashMap<Long, Long> EXECUTION_TO_SESSION = new ConcurrentHashMap<>();

  private static volatile long lastSessionId;
  private static volatile long lastCompletedSessionId;
  private static int historySize = 100;

  private MobileFfmpegSessions() {}

  static Session createFfmpeg(@NonNull List<String> arguments) {
    return register(new Session(SESSION_TYPE_FFMPEG, arguments));
  }

  static Session createFfprobe(@NonNull List<String> arguments) {
    return register(new Session(SESSION_TYPE_FFPROBE, arguments));
  }

  static Session createMediaInformation(@NonNull List<String> arguments) {
    return register(new Session(SESSION_TYPE_MEDIA_INFORMATION, arguments));
  }

  private static Session register(Session session) {
    SESSIONS.put(session.sessionId, session);
    lastSessionId = session.sessionId;
    trimHistory();
    return session;
  }

  private static void trimHistory() {
    if (SESSIONS.size() <= historySize) {
      return;
    }
    long oldest = Long.MAX_VALUE;
    for (Long id : SESSIONS.keySet()) {
      Session s = SESSIONS.get(id);
      if (s != null && s.createTime < oldest) {
        oldest = s.createTime;
      }
    }
  }

  @Nullable
  static Session get(long sessionId) {
    return SESSIONS.get(sessionId);
  }

  static void bindExecution(long executionId, long sessionId) {
    EXECUTION_TO_SESSION.put(executionId, sessionId);
  }

  @Nullable
  static Session resolveSession(long executionId) {
    Long sessionId = EXECUTION_TO_SESSION.get(executionId);
    if (sessionId != null) {
      return SESSIONS.get(sessionId);
    }
    return null;
  }

  static void appendLog(long sessionId, int level, @NonNull String message) {
    Session session = SESSIONS.get(sessionId);
    if (session != null) {
      session.logs.add(new LogEntry(sessionId, level, message));
    }
  }

  static void appendStatistics(long sessionId, @NonNull Statistics statistics) {
    Session session = SESSIONS.get(sessionId);
    if (session != null && session.type == SESSION_TYPE_FFMPEG) {
      session.statistics.add(statistics);
    }
  }

  static void markRunning(long sessionId) {
    Session session = SESSIONS.get(sessionId);
    if (session != null) {
      session.state = STATE_RUNNING;
      session.startTime = System.currentTimeMillis();
    }
  }

  static void markCompleted(long sessionId, int returnCode) {
    Session session = SESSIONS.get(sessionId);
    if (session != null) {
      session.returnCode = returnCode;
      session.endTime = System.currentTimeMillis();
      session.state =
          returnCode == com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS
              ? STATE_COMPLETED
              : (returnCode == com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL
                  ? STATE_FAILED
                  : STATE_FAILED);
      if (session.state == STATE_COMPLETED) {
        lastCompletedSessionId = sessionId;
      }
    }
  }

  static void setMediaInformation(long sessionId, @Nullable MediaInformation info) {
    Session session = SESSIONS.get(sessionId);
    if (session != null) {
      session.mediaInformation = info;
    }
  }

  static int getHistorySize() {
    return historySize;
  }

  static void setHistorySize(int size) {
    historySize = Math.max(1, size);
  }

  static void clear() {
    SESSIONS.clear();
    EXECUTION_TO_SESSION.clear();
    lastSessionId = 0;
    lastCompletedSessionId = 0;
  }

  @NonNull
  static List<Map<String, Object>> listByType(int type) {
    List<Map<String, Object>> list = new ArrayList<>();
    for (Session session : SESSIONS.values()) {
      if (session.type == type) {
        list.add(session.toMap());
      }
    }
    return list;
  }

  @NonNull
  static List<Map<String, Object>> listAll() {
    List<Map<String, Object>> list = new ArrayList<>();
    for (Session session : SESSIONS.values()) {
      list.add(session.toMap());
    }
    return list;
  }

  @NonNull
  static List<Map<String, Object>> listByState(int state) {
    List<Map<String, Object>> list = new ArrayList<>();
    for (Session session : SESSIONS.values()) {
      if (session.state == state) {
        list.add(session.toMap());
      }
    }
    return list;
  }

  @Nullable
  static Map<String, Object> lastSessionMap() {
    return lastSessionId > 0 ? toMap(get(lastSessionId)) : null;
  }

  @Nullable
  static Map<String, Object> lastCompletedSessionMap() {
    return lastCompletedSessionId > 0 ? toMap(get(lastCompletedSessionId)) : null;
  }

  @Nullable
  static Map<String, Object> toMap(@Nullable Session session) {
    return session == null ? null : session.toMap();
  }

  @NonNull
  static String joinArguments(@NonNull List<String> arguments) {
    return joinArgumentsArray(arguments.toArray(new String[0]));
  }

  private static String joinArgumentsArray(@NonNull String[] args) {
    StringBuilder sb = new StringBuilder();
    for (String arg : args) {
      if (sb.length() > 0) {
        sb.append(' ');
      }
      if (arg.indexOf(' ') >= 0) {
        sb.append('\"').append(arg.replace("\"", "\\\"")).append('\"');
      } else {
        sb.append(arg);
      }
    }
    return sb.toString();
  }

  @Nullable
  static String extractInputPath(@NonNull List<String> arguments) {
    for (int i = 0; i < arguments.size() - 1; i++) {
      if ("-i".equals(arguments.get(i))) {
        return arguments.get(i + 1);
      }
    }
    if (!arguments.isEmpty()) {
      return arguments.get(arguments.size() - 1);
    }
    return null;
  }

  @NonNull
  static List<Map<String, Object>> toLogMaps(long sessionId, @NonNull List<LogEntry> logs) {
    List<Map<String, Object>> list = new ArrayList<>();
    for (LogEntry log : logs) {
      list.add(log.toMap());
    }
    return list;
  }

  @NonNull
  static List<Map<String, Object>> toStatisticsMaps(
      long sessionId, @NonNull List<Statistics> statistics) {
    List<Map<String, Object>> list = new ArrayList<>();
    for (Statistics s : statistics) {
      list.add(toStatisticsMap(sessionId, s));
    }
    return list;
  }

  @NonNull
  static Map<String, Object> toStatisticsMap(long sessionId, @NonNull Statistics statistics) {
    Map<String, Object> map = new HashMap<>();
    map.put("sessionId", sessionId);
    map.put("videoFrameNumber", statistics.getVideoFrameNumber());
    map.put("videoFps", statistics.getVideoFps());
    map.put("videoQuality", statistics.getVideoQuality());
    long size = statistics.getSize();
    map.put("size", size < Integer.MAX_VALUE ? (int) size : (int) (size % Integer.MAX_VALUE));
    map.put("time", statistics.getTime());
    map.put("bitrate", statistics.getBitrate());
    map.put("speed", statistics.getSpeed());
    return map;
  }

  @Nullable
  static Map<String, Object> mediaInformationToMap(@Nullable MediaInformation info) {
    if (info == null) {
      return null;
    }
    JSONObject json = info.getAllProperties();
    if (json == null) {
      return null;
    }
    return jsonObjectToMap(json);
  }

  @Nullable
  static Map<String, Object> jsonObjectToMap(@Nullable JSONObject jsonObject) {
    if (jsonObject == null) {
      return null;
    }
    Map<String, Object> map = new HashMap<>();
    Iterator<String> keys = jsonObject.keys();
    while (keys.hasNext()) {
      String key = keys.next();
      Object value = jsonObject.opt(key);
      if (value != null) {
        map.put(key, value);
      }
    }
    return map;
  }

  static final class Session {
    final long sessionId;
    final int type;
    final List<String> arguments;
    final long createTime;
    final List<LogEntry> logs = new ArrayList<>();
    final List<Statistics> statistics = new ArrayList<>();

    long startTime;
    long endTime;
    int state = STATE_CREATED;
    int returnCode = com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;
    @Nullable MediaInformation mediaInformation;

    Session(int type, List<String> arguments) {
      this.sessionId = NEXT_ID.getAndIncrement();
      this.type = type;
      this.arguments = new ArrayList<>(arguments);
      this.createTime = System.currentTimeMillis();
      this.startTime = this.createTime;
    }

    String command() {
      return joinArguments(arguments);
    }

    Map<String, Object> toMap() {
      Map<String, Object> map = new HashMap<>();
      map.put("sessionId", sessionId);
      map.put("createTime", createTime);
      map.put("startTime", startTime);
      map.put("command", command());
      map.put("type", type);
      if (type == SESSION_TYPE_MEDIA_INFORMATION && mediaInformation != null) {
        Map<String, Object> mi = mediaInformationToMap(mediaInformation);
        if (mi != null) {
          map.put("mediaInformation", mi);
        }
      }
      return map;
    }

    String allLogsAsString() {
      StringBuilder sb = new StringBuilder();
      for (LogEntry log : logs) {
        sb.append(log.message);
      }
      String fromConfig = com.arthenica.mobileffmpeg.Config.getLastCommandOutput();
      if (sb.length() == 0 && fromConfig != null) {
        return fromConfig;
      }
      return sb.toString();
    }
  }

  static final class LogEntry {
    final long sessionId;
    final int level;
    final String message;

    LogEntry(long sessionId, int level, String message) {
      this.sessionId = sessionId;
      this.level = level;
      this.message = message;
    }

    Map<String, Object> toMap() {
      Map<String, Object> map = new HashMap<>();
      map.put("sessionId", sessionId);
      map.put("level", level);
      map.put("message", message);
      return map;
    }
  }
}
