package com.wind.without_ffmpeg;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arthenica.mobileffmpeg.AbiDetect;
import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.arthenica.mobileffmpeg.FFprobe;
import com.arthenica.mobileffmpeg.Level;
import com.arthenica.mobileffmpeg.LogMessage;
import com.arthenica.mobileffmpeg.MediaInformation;
import com.arthenica.mobileffmpeg.Statistics;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

/** Android FFmpegKit-compatible bridge over mobile-ffmpeg-full:4.4 */
public class WithoutFfmpegPlugin implements FlutterPlugin, MethodChannel.MethodCallHandler, EventChannel.StreamHandler {

  private static final String METHOD_CHANNEL = "flutter.arthenica.com/ffmpeg_kit";
  private static final String EVENT_CHANNEL = "flutter.arthenica.com/ffmpeg_kit_event";

  private static final String EVENT_LOG = "FFmpegKitLogCallbackEvent";
  private static final String EVENT_STATISTICS = "FFmpegKitStatisticsCallbackEvent";
  private static final String EVENT_COMPLETE = "FFmpegKitCompleteCallbackEvent";

  private MethodChannel methodChannel;
  private EventChannel eventChannel;
  private EventChannel.EventSink eventSink;
  private final ExecutorService executor = Executors.newFixedThreadPool(4);
  private final Handler mainHandler = new Handler(Looper.getMainLooper());
  private final AtomicBoolean logsEnabled = new AtomicBoolean(false);
  private final AtomicBoolean statisticsEnabled = new AtomicBoolean(false);
  private final ThreadLocal<Long> activeSessionId = new ThreadLocal<>();

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
    methodChannel = new MethodChannel(binding.getBinaryMessenger(), METHOD_CHANNEL);
    methodChannel.setMethodCallHandler(this);
    eventChannel = new EventChannel(binding.getBinaryMessenger(), EVENT_CHANNEL);
    eventChannel.setStreamHandler(this);
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    if (methodChannel != null) {
      methodChannel.setMethodCallHandler(null);
      methodChannel = null;
    }
    if (eventChannel != null) {
      eventChannel.setStreamHandler(null);
      eventChannel = null;
    }
    executor.shutdownNow();
  }

  @Override
  public void onListen(Object arguments, EventChannel.EventSink events) {
    eventSink = events;
    registerCallbacks();
  }

  @Override
  public void onCancel(Object arguments) {
    eventSink = null;
  }

  private void registerCallbacks() {
    Config.enableLogCallback(message -> {
      if (!logsEnabled.get()) {
        return;
      }
      long sessionId = resolveSessionId(message.getExecutionId());
      int level = message.getLevel() != null ? message.getLevel().getValue() : Level.AV_LOG_INFO.getValue();
      String text = message.getText() != null ? message.getText() : "";
      MobileFfmpegSessions.appendLog(sessionId, level, text);
      emitLog(sessionId, level, text);
    });

    Config.enableStatisticsCallback(statistics -> {
      if (!statisticsEnabled.get() || statistics == null) {
        return;
      }
      long sessionId = resolveSessionId(statistics.getExecutionId());
      MobileFfmpegSessions.appendStatistics(sessionId, statistics);
      emitStatistics(sessionId, statistics);
    });
  }

  private long resolveSessionId(long executionId) {
    MobileFfmpegSessions.Session session = MobileFfmpegSessions.resolveSession(executionId);
    if (session != null) {
      return session.sessionId;
    }
    Long active = activeSessionId.get();
    if (active != null) {
      return active;
    }
    return executionId > 0 ? executionId : 0;
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
    try {
      handleMethodCall(call, result);
    } catch (Exception e) {
      result.error("PLUGIN_ERROR", e.getMessage(), null);
    }
  }

  private void handleMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
    Integer sessionId = call.argument("sessionId");
    Integer waitTimeout = call.argument("waitTimeout");
    List<String> arguments = call.argument("arguments");
    String ffprobeJsonOutput = call.argument("ffprobeJsonOutput");

    switch (call.method) {
      case "abstractSessionGetEndTime":
        requireSession(sessionId, result, s -> result.success(s.endTime > 0 ? s.endTime : null));
        break;
      case "abstractSessionGetDuration":
        requireSession(sessionId, result, s -> {
          long duration = s.endTime > 0 ? s.endTime - s.startTime : 0;
          result.success(duration);
        });
        break;
      case "abstractSessionGetAllLogs":
        requireSession(sessionId, result, s -> result.success(MobileFfmpegSessions.toLogMaps(s.sessionId, s.logs)));
        break;
      case "abstractSessionGetLogs":
        requireSession(sessionId, result, s -> result.success(MobileFfmpegSessions.toLogMaps(s.sessionId, s.logs)));
        break;
      case "abstractSessionGetAllLogsAsString":
        requireSession(sessionId, result, s -> result.success(s.allLogsAsString()));
        break;
      case "abstractSessionGetState":
        requireSession(sessionId, result, s -> result.success(s.state));
        break;
      case "abstractSessionGetReturnCode":
        requireSession(sessionId, result, s -> result.success(s.returnCode));
        break;
      case "abstractSessionGetFailStackTrace":
        result.success(null);
        break;
      case "thereAreAsynchronousMessagesInTransmit":
        result.success(false);
        break;
      case "getArch":
        result.success(AbiDetect.getAbi());
        break;
      case "ffmpegSession":
        if (arguments == null) {
          result.error("INVALID_ARGUMENTS", "Invalid arguments array.", null);
        } else {
          result.success(MobileFfmpegSessions.toMap(MobileFfmpegSessions.createFfmpeg(arguments)));
        }
        break;
      case "ffmpegSessionGetAllStatistics":
      case "ffmpegSessionGetStatistics":
        requireSession(sessionId, result, s -> {
          if (s.type != MobileFfmpegSessions.SESSION_TYPE_FFMPEG) {
            result.error("NOT_FFMPEG_SESSION", "A session is found but it does not have the correct type.", null);
          } else {
            result.success(MobileFfmpegSessions.toStatisticsMaps(s.sessionId, s.statistics));
          }
        });
        break;
      case "ffprobeSession":
        if (arguments == null) {
          result.error("INVALID_ARGUMENTS", "Invalid arguments array.", null);
        } else {
          result.success(MobileFfmpegSessions.toMap(MobileFfmpegSessions.createFfprobe(arguments)));
        }
        break;
      case "mediaInformationSession":
        if (arguments == null) {
          result.error("INVALID_ARGUMENTS", "Invalid arguments array.", null);
        } else {
          result.success(MobileFfmpegSessions.toMap(MobileFfmpegSessions.createMediaInformation(arguments)));
        }
        break;
      case "getMediaInformation":
        requireSession(sessionId, result, s ->
            result.success(MobileFfmpegSessions.mediaInformationToMap(s.mediaInformation)));
        break;
      case "mediaInformationJsonParserFrom":
        parseMediaInformationJson(ffprobeJsonOutput, result, false);
        break;
      case "mediaInformationJsonParserFromWithError":
        parseMediaInformationJson(ffprobeJsonOutput, result, true);
        break;
      case "enableRedirection":
        enableRedirection();
        result.success(null);
        break;
      case "disableRedirection":
        Config.disableRedirection();
        result.success(null);
        break;
      case "enableLogs":
        logsEnabled.set(true);
        result.success(null);
        break;
      case "disableLogs":
        logsEnabled.set(false);
        result.success(null);
        break;
      case "enableStatistics":
        statisticsEnabled.set(true);
        result.success(null);
        break;
      case "disableStatistics":
        statisticsEnabled.set(false);
        result.success(null);
        break;
      case "setFontconfigConfigurationPath":
        Config.setFontconfigConfigurationPath(call.argument("path"));
        result.success(null);
        break;
      case "setFontDirectory":
        result.success(null);
        break;
      case "setFontDirectoryList":
        result.success(null);
        break;
      case "registerNewFFmpegPipe":
        result.success(null);
        break;
      case "closeFFmpegPipe":
        result.success(null);
        break;
      case "getFFmpegVersion":
        result.success(Config.getFFmpegVersion());
        break;
      case "isLTSBuild":
        result.success(false);
        break;
      case "getBuildDate":
        result.success("");
        break;
      case "setEnvironmentVariable":
        result.success(null);
        break;
      case "ignoreSignal":
        result.success(null);
        break;
      case "ffmpegSessionExecute":
        executeFfmpegSession(sessionId, result, false);
        break;
      case "ffprobeSessionExecute":
        executeFfprobeSession(sessionId, result, false);
        break;
      case "mediaInformationSessionExecute":
        executeMediaInformationSession(sessionId, result);
        break;
      case "asyncFFmpegSessionExecute":
        executeFfmpegSession(sessionId, result, true);
        break;
      case "asyncFFprobeSessionExecute":
        executeFfprobeSession(sessionId, result, true);
        break;
      case "asyncMediaInformationSessionExecute":
        executeMediaInformationSessionAsync(sessionId, result);
        break;
      case "getLogLevel":
        result.success(Config.getLogLevel().getValue());
        break;
      case "setLogLevel": {
        Integer level = call.argument("level");
        if (level != null) {
          Config.setLogLevel(Level.from(level));
          result.success(null);
        } else {
          result.error("INVALID_LEVEL", "Invalid level value.", null);
        }
        break;
      }
      case "getSessionHistorySize":
        result.success(MobileFfmpegSessions.getHistorySize());
        break;
      case "setSessionHistorySize": {
        Integer size = call.argument("sessionHistorySize");
        if (size != null) {
          MobileFfmpegSessions.setHistorySize(size);
          result.success(null);
        } else {
          result.error("INVALID_SIZE", "Invalid session history size value.", null);
        }
        break;
      }
      case "getSession":
        requireSession(sessionId, result, s -> result.success(s.toMap()));
        break;
      case "getLastSession":
        result.success(MobileFfmpegSessions.lastSessionMap());
        break;
      case "getLastCompletedSession":
        result.success(MobileFfmpegSessions.lastCompletedSessionMap());
        break;
      case "getSessions":
        result.success(MobileFfmpegSessions.listAll());
        break;
      case "clearSessions":
        MobileFfmpegSessions.clear();
        result.success(null);
        break;
      case "getSessionsByState": {
        Integer state = call.argument("state");
        if (state != null) {
          result.success(MobileFfmpegSessions.listByState(state));
        } else {
          result.error("INVALID_SESSION_STATE", "Invalid session state value.", null);
        }
        break;
      }
      case "getLogRedirectionStrategy":
        result.success(4);
        break;
      case "setLogRedirectionStrategy":
        result.success(null);
        break;
      case "messagesInTransmit":
        result.success(0);
        break;
      case "getPlatform":
        result.success("android");
        break;
      case "writeToPipe":
        result.error("NOT_SUPPORTED", "Not supported on Android with mobile-ffmpeg.", null);
        break;
      case "selectDocument":
      case "getSafParameter":
        result.error("NOT_SUPPORTED", "Not supported on Android with mobile-ffmpeg.", null);
        break;
      case "cancel":
        FFmpeg.cancel();
        result.success(null);
        break;
      case "cancelSession":
        if (sessionId != null) {
          FFmpeg.cancel(sessionId.longValue());
        }
        result.success(null);
        break;
      case "getFFmpegSessions":
        result.success(MobileFfmpegSessions.listByType(MobileFfmpegSessions.SESSION_TYPE_FFMPEG));
        break;
      case "getFFprobeSessions":
        result.success(MobileFfmpegSessions.listByType(MobileFfmpegSessions.SESSION_TYPE_FFPROBE));
        break;
      case "getMediaInformationSessions":
        result.success(MobileFfmpegSessions.listByType(MobileFfmpegSessions.SESSION_TYPE_MEDIA_INFORMATION));
        break;
      case "getPackageName":
        result.success("mobile-ffmpeg-full");
        break;
      case "getExternalLibraries":
        result.success(new ArrayList<String>());
        break;
      default:
        result.notImplemented();
        break;
    }
  }

  private interface SessionConsumer {
    void accept(MobileFfmpegSessions.Session session);
  }

  private void requireSession(
      @Nullable Integer sessionId,
      @NonNull MethodChannel.Result result,
      @NonNull SessionConsumer consumer) {
    if (sessionId == null) {
      result.error("INVALID_SESSION", "Invalid session id.", null);
      return;
    }
    MobileFfmpegSessions.Session session = MobileFfmpegSessions.get(sessionId.longValue());
    if (session == null) {
      result.error("SESSION_NOT_FOUND", "Session not found.", null);
    } else {
      consumer.accept(session);
    }
  }

  private void enableRedirection() {
    logsEnabled.set(true);
    statisticsEnabled.set(true);
    Config.enableRedirection();
    registerCallbacks();
  }

  private void executeFfmpegSession(
      @Nullable Integer sessionId, @NonNull MethodChannel.Result result, boolean async) {
    if (sessionId == null) {
      result.error("INVALID_SESSION", "Invalid session id.", null);
      return;
    }
    MobileFfmpegSessions.Session session = MobileFfmpegSessions.get(sessionId.longValue());
    if (session == null || session.type != MobileFfmpegSessions.SESSION_TYPE_FFMPEG) {
      result.error("NOT_FFMPEG_SESSION", "A session is found but it does not have the correct type.", null);
      return;
    }
  final String command = session.command();
    if (async) {
      MobileFfmpegSessions.markRunning(session.sessionId);
      long executionId =
          FFmpeg.executeAsync(
              command,
              (executionId1, returnCode) -> {
                MobileFfmpegSessions.bindExecution(executionId1, session.sessionId);
                MobileFfmpegSessions.markCompleted(session.sessionId, returnCode);
                emitComplete(session);
              });
      MobileFfmpegSessions.bindExecution(executionId, session.sessionId);
      result.success(null);
    } else {
      executor.execute(
          () -> {
            MobileFfmpegSessions.markRunning(session.sessionId);
            activeSessionId.set(session.sessionId);
            try {
              int rc = FFmpeg.execute(command);
              MobileFfmpegSessions.markCompleted(session.sessionId, rc);
            } finally {
              activeSessionId.remove();
            }
            mainHandler.post(
                () -> {
                  emitComplete(session);
                  result.success(null);
                });
          });
    }
  }

  private void executeFfprobeSession(
      @Nullable Integer sessionId, @NonNull MethodChannel.Result result, boolean async) {
    if (sessionId == null) {
      result.error("INVALID_SESSION", "Invalid session id.", null);
      return;
    }
    MobileFfmpegSessions.Session session = MobileFfmpegSessions.get(sessionId.longValue());
    if (session == null || session.type != MobileFfmpegSessions.SESSION_TYPE_FFPROBE) {
      result.error("NOT_FFPROBE_SESSION", "A session is found but it does not have the correct type.", null);
      return;
    }
    final String command = session.command();
    if (async) {
      MobileFfmpegSessions.markRunning(session.sessionId);
      executor.execute(
          () -> {
            activeSessionId.set(session.sessionId);
            try {
              int rc = FFprobe.execute(command);
              MobileFfmpegSessions.markCompleted(session.sessionId, rc);
            } finally {
              activeSessionId.remove();
            }
            mainHandler.post(() -> emitComplete(session));
          });
      result.success(null);
    } else {
      executor.execute(
          () -> {
            MobileFfmpegSessions.markRunning(session.sessionId);
            activeSessionId.set(session.sessionId);
            try {
              int rc = FFprobe.execute(command);
              MobileFfmpegSessions.markCompleted(session.sessionId, rc);
            } finally {
              activeSessionId.remove();
            }
            mainHandler.post(
                () -> {
                  emitComplete(session);
                  result.success(null);
                });
          });
    }
  }

  private void executeMediaInformationSession(
      @Nullable Integer sessionId, @NonNull MethodChannel.Result result) {
    if (sessionId == null) {
      result.error("INVALID_SESSION", "Invalid session id.", null);
      return;
    }
    MobileFfmpegSessions.Session session = MobileFfmpegSessions.get(sessionId.longValue());
    if (session == null || session.type != MobileFfmpegSessions.SESSION_TYPE_MEDIA_INFORMATION) {
      result.error("NOT_MEDIA_INFORMATION_SESSION", "A session is found but it does not have the correct type.", null);
      return;
    }
    executor.execute(
        () -> {
          MobileFfmpegSessions.markRunning(session.sessionId);
          activeSessionId.set(session.sessionId);
          int rc;
          try {
            String path = MobileFfmpegSessions.extractInputPath(session.arguments);
            MediaInformation info = path != null ? FFprobe.getMediaInformation(path) : null;
            MobileFfmpegSessions.setMediaInformation(session.sessionId, info);
            rc = info != null ? Config.RETURN_CODE_SUCCESS : Config.getLastReturnCode();
          } finally {
            activeSessionId.remove();
          }
          MobileFfmpegSessions.markCompleted(session.sessionId, rc);
          mainHandler.post(
              () -> {
                emitComplete(session);
                result.success(null);
              });
        });
  }

  private void executeMediaInformationSessionAsync(
      @Nullable Integer sessionId, @NonNull MethodChannel.Result result) {
    executeMediaInformationSession(sessionId, result);
  }

  private void parseMediaInformationJson(
      @Nullable String ffprobeJsonOutput,
      @NonNull MethodChannel.Result result,
      boolean throwOnError) {
    if (ffprobeJsonOutput == null) {
      result.error("INVALID_FFPROBE_JSON_OUTPUT", "Invalid ffprobe json output.", null);
      return;
    }
    try {
      JSONObject json = new JSONObject(ffprobeJsonOutput);
      result.success(MobileFfmpegSessions.jsonObjectToMap(json));
    } catch (Exception e) {
      if (throwOnError) {
        result.error("PARSE_FAILED", "Parsing MediaInformation failed with JSON error.", null);
      } else {
        result.success(null);
      }
    }
  }

  private void emitLog(long sessionId, int level, String message) {
    Map<String, Object> log = new HashMap<>();
    log.put("sessionId", sessionId);
    log.put("level", level);
    log.put("message", message);
    emitEvent(EVENT_LOG, log);
  }

  private void emitStatistics(long sessionId, Statistics statistics) {
    emitEvent(EVENT_STATISTICS, MobileFfmpegSessions.toStatisticsMap(sessionId, statistics));
  }

  private void emitComplete(@NonNull MobileFfmpegSessions.Session session) {
    emitEvent(EVENT_COMPLETE, session.toMap());
  }

  private void emitEvent(@NonNull String key, @Nullable Map<String, Object> payload) {
    if (eventSink == null || payload == null) {
      return;
    }
    Map<String, Object> wrapper = new HashMap<>();
    wrapper.put(key, payload);
    mainHandler.post(() -> eventSink.success(wrapper));
  }
}
