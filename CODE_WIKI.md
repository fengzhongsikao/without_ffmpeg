# Without FFmpeg 项目 Code Wiki

## 项目概述

**without_ffmpeg** 是一个 Flutter 插件，用于在移动设备上进行音视频处理。该项目基于 FFmpegKit 的 API 设计，但针对 Android 使用 **mobile-ffmpeg-full** 库（版本 4.4），iOS 则使用 **ffmpegkit** 库。

项目名称 "without_ffmpeg" 暗示了它在 Android 端实际上并不使用 FFmpegKit，而是使用 mobile-ffmpeg 作为底层实现。

---

## 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                      Flutter/Dart 层                        │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  without_ffmpeg.dart (入口)                          │   │
│  │  ├── FFmpegKit (FFmpeg 命令执行)                      │   │
│  │  ├── FFprobeKit (媒体信息探测)                        │   │
│  │  ├── FFmpegKitConfig (全局配置)                       │   │
│  │  └── ArchDetect (架构检测)                            │   │
│  └─────────────────────────────────────────────────────┘   │
│                            │                                │
│              MethodChannel / EventChannel                   │
│                            ▼                                │
├─────────────────────────────────────────────────────────────┤
│                    平台特定实现层                             │
│  ┌───────────────────────┬───────────────────────────────┐ │
│  │    Android Plugin     │       iOS Plugin              │ │
│  │  WithoutFfmpegPlugin  │    WithoutFfmpegPlugin        │ │
│  │    (Java/Kotlin)      │        (Objective-C)          │ │
│  │                       │                               │ │
│  │  底层库:              │    底层库:                     │ │
│  │  mobile-ffmpeg-full   │    ffmpegkit (Swift/ObjC)     │ │
│  └───────────────────────┴───────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

---

## 核心目录结构

```
without_ffmpeg/
├── lib/                          # Dart 核心代码
│   ├── without_ffmpeg.dart      # 入口导出文件
│   ├── ffmpeg_kit.dart          # FFmpeg 命令执行主类
│   ├── ffprobe_kit.dart         # FFprobe 媒体信息类
│   ├── ffmpeg_kit_config.dart   # 全局配置类
│   ├── arch_detect.dart         # 架构检测类
│   ├── ffmpeg_session.dart      # FFmpeg 会话
│   ├── ffprobe_session.dart     # FFprobe 会话
│   ├── media_information_session.dart  # 媒体信息会话
│   ├── return_code.dart         # 返回码定义
│   ├── session.dart             # 会话基类
│   ├── abstract_session.dart    # 抽象会话类
│   ├── log.dart                 # 日志类
│   ├── statistics.dart          # 统计类
│   ├── stream_information.dart  # 流信息类
│   ├── media_information.dart   # 媒体信息类
│   ├── chapter.dart             # 章节信息类
│   ├── level.dart               # 日志级别
│   ├── packages.dart            # 包信息
│   ├── signal.dart              # 信号定义
│   ├── session_state.dart       # 会话状态
│   └── src/                     # 内部实现
│       ├── ffmpeg_kit_factory.dart
│       └── ffmpeg_kit_flutter_initializer.dart
│
├── android/                      # Android 原生代码
│   └── src/main/java/com/wind/without_ffmpeg/
│       ├── WithoutFfmpegPlugin.java    # Android 插件主类
│       └── MobileFfmpegSessions.java  # 会话管理
│
├── ios/                          # iOS 原生代码
│   └── Classes/
│       ├── WithoutFfmpegPlugin.h  # iOS 插件头文件
│       └── WithoutFfmpegPlugin.m  # iOS 插件实现
│
└── pubspec.yaml                  # 项目配置
```

---

## Android 与 iOS 平台区分机制 ⭐

这是本项目的核心设计亮点。以下是平台区分的完整机制：

### 1. Flutter Plugin 注册区分

在 `pubspec.yaml` 中通过 `plugin` 配置声明平台实现：

```yaml
flutter:
  plugin:
    platforms:
      android:
        package: com.wind.without_ffmpeg
        pluginClass: WithoutFfmpegPlugin
      ios:
        pluginClass: WithoutFfmpegPlugin
```

Flutter 框架会根据运行平台自动加载对应的原生插件实现。

### 2. 底层库的差异

| 特性 | Android | iOS |
|------|---------|-----|
| **底层库** | mobile-ffmpeg-full:4.4 | ffmpegkit |
| **API 来源** | com.arthenica.mobileffmpeg | ffmpegkit (C/Swift) |
| **架构检测** | `AbiDetect.getAbi()` | `ArchDetect.getArch()` |
| **平台标识** | `"android"` | `"ios"` |

### 3. MethodChannel 方法调用差异

Dart 层通过 `FFmpegKitPlatform` 调用平台方法，两个平台响应相同的接口，但内部实现不同：

#### 获取平台标识

**Dart 调用：**
```dart
await FFmpegKitConfig.getPlatform();
```

**Android 实现** ([WithoutFfmpegPlugin.java:341-343](file:///Users/hd/development/without_ffmpeg/android/src/main/java/com/wind/without_ffmpeg/WithoutFfmpegPlugin.java#L341-L343)):
```java
case "getPlatform":
    result.success("android");
    break;
```

**iOS 实现** ([WithoutFfmpegPlugin.m:964-966](file:///Users/hd/development/without_ffmpeg/ios/Classes/WithoutFfmpegPlugin.m#L964-L966)):
```objc
- (void)getPlatform:(FlutterResult)result {
  result(PLATFORM_NAME);  // PLATFORM_NAME = @"ios"
}
```

#### 获取架构信息

**Dart 调用：**
```dart
await ArchDetect.getArch();
```

**Android 实现** ([WithoutFfmpegPlugin.java:164-166](file:///Users/hd/development/without_ffmpeg/android/src/main/java/com/wind/without_ffmpeg/WithoutFfmpegPlugin.java#L164-L166)):
```java
case "getArch":
    result.success(AbiDetect.getAbi());
    break;
```

**iOS 实现** ([WithoutFfmpegPlugin.m:569-571](file:///Users/hd/development/without_ffmpeg/ios/Classes/WithoutFfmpegPlugin.m#L569-L571)):
```objc
- (void)getArch:(FlutterResult)result {
  result([ArchDetect getArch]);
}
```

### 4. SAF (Storage Access Framework) 支持差异

Android 独有的 SAF 支持：

```dart
// 仅在 Android 上可用
Future<String?> getSafParameterForRead(String uriString);
Future<String?> getSafParameterForWrite(String uriString);
```

**Android 实现** ([WithoutFfmpegPlugin.java:347-349](file:///Users/hd/development/without_ffmpeg/android/src/main/java/com/wind/without_ffmpeg/WithoutFfmpegPlugin.java#L347-L349)):
```java
case "selectDocument":
case "getSafParameter":
    result.error("NOT_SUPPORTED", "Not supported on Android with mobile-ffmpeg.", null);
    break;
```

**iOS 实现** ([WithoutFfmpegPlugin.m:1021-1027](file:///Users/hd/development/without_ffmpeg/ios/Classes/WithoutFfmpegPlugin.m#L1021-L1027)):
```objc
- (void)selectDocument:(FlutterResult)result {
  result([FlutterError errorWithCode:@"NOT_SUPPORTED" 
                              message:@"Not supported on iOS platform." 
                              details:nil]);
}

- (void)getSafParameter:(FlutterResult)result {
  result([FlutterError errorWithCode:@"NOT_SUPPORTED" 
                              message:@"Not supported on iOS platform." 
                              details:nil]);
}
```

### 5. 平台特定功能汇总

| 方法 | Android | iOS | 说明 |
|------|---------|-----|------|
| `getPlatform()` | ✅ | ✅ | 获取平台名称 |
| `getArch()` | ✅ | ✅ | 获取 CPU 架构 |
| `getSafParameter()` | ✅ | ❌ | SAF 参数转换 (Android 19+) |
| `selectDocument()` | ✅ | ❌ | 文档选择器 (Android) |
| `writeToPipe()` | ❌ | ✅ | 管道写入 |
| `isLTSBuild()` | ❌ | ✅ | 是否 LTS 构建 |
| `setFontDirectory()` | ⚠️ | ✅ | 字体目录设置 |
| `registerNewFFmpegPipe()` | ❌ | ✅ | 注册命名管道 |

### 6. 用户使用示例

```dart
import 'package:without_ffmpeg/without_ffmpeg.dart';

// 检测当前平台
String platform = await FFmpegKitConfig.getPlatform();
print("Running on: $platform");  // "android" 或 "ios"

// 检测架构
String arch = await ArchDetect.getArch();
print("Architecture: $arch");

// 根据平台执行不同逻辑
if (platform == "android") {
  // Android 特定处理
  // 例如使用 SAF
  String safPath = await FFmpegKitConfig.getSafParameterForRead(uriString);
} else {
  // iOS 特定处理
  // 例如使用管道
  String pipe = await FFmpegKitConfig.registerNewFFmpegPipe();
}
```

---

## 核心类详解

### 1. FFmpegKit - FFmpeg 命令执行

**文件**: [ffmpeg_kit.dart](file:///Users/hd/development/without_ffmpeg/lib/ffmpeg_kit.dart)

#### 主要方法

| 方法 | 说明 |
|------|------|
| `execute(String command)` | 同步执行 FFmpeg 命令 |
| `executeWithArguments(List<String> args)` | 同步执行带参数列表的命令 |
| `executeAsync(String command, callbacks...)` | 异步执行 FFmpeg 命令 |
| `cancel([int? sessionId])` | 取消执行 |
| `listSessions()` | 列出所有会话 |

#### 使用示例

```dart
// 同步执行
final session = await FFmpegKit.execute(
  "-i input.mp4 -c:v libx264 output.mp4"
);

// 异步执行（推荐用于长任务）
final session = await FFmpegKit.executeAsync(
  "-i input.mp4 -c:v libx264 output.mp4",
  (session) {
    print("Complete: ${session.getReturnCode()}");
  },
  (log) {
    print("Log: ${log.getMessage()}");
  },
  (statistics) {
    print("Progress: ${statistics.getTime()}");
  }
);

// 取消执行
await FFmpegKit.cancel();
```

### 2. FFprobeKit - 媒体信息探测

**文件**: [ffprobe_kit.dart](file:///Users/hd/development/without_ffmpeg/lib/ffprobe_kit.dart)

#### 主要方法

| 方法 | 说明 |
|------|------|
| `execute(String command)` | 同步执行 FFprobe 命令 |
| `getMediaInformation(String path)` | 获取文件媒体信息 |
| `getMediaInformationAsync(String path)` | 异步获取媒体信息 |

#### 使用示例

```dart
// 获取媒体信息
final session = await FFprobeKit.getMediaInformation("video.mp4");
final info = await session.getMediaInformation();
print("Duration: ${info.getDuration()}");
print("Format: ${info.getFormat()}");

// 获取所有流信息
final streams = info.getStreams();
for (final stream in streams) {
  print("Stream: ${stream.getCodecType()} - ${stream.getCodecName()}");
}
```

### 3. FFmpegKitConfig - 全局配置

**文件**: [ffmpeg_kit_config.dart](file:///Users/hd/development/without_ffmpeg/lib/ffmpeg_kit_config.dart)

#### 主要配置方法

```dart
// 初始化
await FFmpegKitConfig.init();

// 日志配置
await FFmpegKitConfig.enableRedirection();
await FFmpegKitConfig.enableLogs();
await FFmpegKitConfig.setLogLevel(Level.avLogInfo);

// 会话历史管理
await FFmpegKitConfig.setSessionHistorySize(100);
await FFmpegKitConfig.clearSessions();

// 全局回调
FFmpegKitConfig.enableLogCallback((log) {
  print(log.getMessage());
});
FFmpegKitConfig.enableStatisticsCallback((stats) {
  print("FPS: ${stats.getVideoFps()}");
});

// 平台检测
final platform = await FFmpegKitConfig.getPlatform();
final arch = await ArchDetect.getArch();
```

### 4. FFmpegSession - FFmpeg 执行会话

**文件**: [ffmpeg_session.dart](file:///Users/hd/development/without_ffmpeg/lib/ffmpeg_session.dart)

#### 会话状态

```dart
enum SessionState {
  created,    // 创建但未执行
  running,    // 执行中
  failed,     // 执行失败
  completed   // 执行完成
}
```

#### 常用方法

```dart
final session = await FFmpegKit.executeAsync(command);

// 获取会话信息
int sessionId = session.getSessionId();
SessionState state = await session.getState();
ReturnCode? returnCode = await session.getReturnCode();
Duration duration = await session.getDuration();

// 获取日志
List<Log> logs = await session.getAllLogs();
String logString = await session.getAllLogsAsString();

// 获取统计信息
List<Statistics> stats = await session.getAllStatistics();
Statistics? lastStat = await session.getLastReceivedStatistics();

// 获取媒体信息
MediaInformation? mediaInfo = await session.getMediaInformation();
```

### 5. ReturnCode - 返回码

**文件**: [return_code.dart](file:///Users/hd/development/without_ffmpeg/lib/return_code.dart)

```dart
class ReturnCode {
  static const int success = 0;   // 执行成功
  static const int cancel = 255;  // 被取消

  // 判断方法
  bool isValueSuccess() => this._value == ReturnCode.success;
  bool isValueError() => this._value != ReturnCode.success && this._value != ReturnCode.cancel;
  bool isValueCancel() => this._value == ReturnCode.cancel;
}
```

---

## 依赖关系

### pubspec.yaml 依赖

```yaml
dependencies:
  flutter:
    sdk: flutter
  ffmpeg_kit_flutter_platform_interface: ^0.2.1  # 平台接口定义
  plugin_platform_interface: ^2.0.2               # 插件接口

dev_dependencies:
  flutter_test:
    sdk: flutter
  flutter_lints: ^5.0.0
```

### Android 依赖

**build.gradle**:
```groovy
dependencies {
    implementation 'com.arthenica:mobile-ffmpeg-full:4.4'
}
```

### iOS 依赖

**without_ffmpeg.podspec**:
```ruby
s.dependency 'ffmpegkit-ios-full', '6.0'
```

---

## 项目运行方式

### 1. 安装插件

```yaml
# pubspec.yaml
dependencies:
  without_ffmpeg: ^0.0.1
```

### 2. 基本使用流程

```dart
import 'package:without_ffmpeg/without_ffmpeg.dart';

void main() async {
  // 1. 初始化
  await FFmpegKitConfig.init();
  
  // 2. 配置（可选）
  await FFmpegKitConfig.enableRedirection();
  await FFmpegKitConfig.enableStatistics();
  
  // 3. 执行命令
  final session = await FFmpegKit.execute(
    "-i input.mp4 -c:v libx264 -preset fast output.mp4"
  );
  
  // 4. 检查结果
  final returnCode = await session.getReturnCode();
  if (ReturnCode.isSuccess(returnCode)) {
    print("Video processed successfully!");
  } else if (ReturnCode.isCancel(returnCode)) {
    print("Processing was cancelled.");
  } else {
    print("Error occurred.");
  }
  
  // 5. 获取日志（如需要）
  final logs = await session.getAllLogsAsString();
  print(logs);
}
```

### 3. 异步处理进度

```dart
await FFmpegKit.executeAsync(
  "-i input.mp4 -c:v libx264 output.mp4",
  (session) {
    // 完成回调
    final rc = session.getReturnCode();
    print("Done: ${rc?.getValue()}");
  },
  (log) {
    // 日志回调
    print("LOG: ${log.getMessage()}");
  },
  (statistics) {
    // 进度回调
    print("Time: ${statistics.getTime()}s, "
          "FPS: ${statistics.getVideoFps()}, "
          "Speed: ${statistics.getSpeed()}x");
  }
);
```

### 4. 运行示例项目

```bash
cd example

# Android
flutter run -d android

# iOS
flutter run -d ios
```

---

## Android 原生层详解

### WithoutFfmpegPlugin.java

**职责**:
- 实现 FlutterPlugin 接口
- 处理 MethodChannel 方法调用
- 管理 EventChannel 事件流
- 集成 mobile-ffmpeg-full 库

**关键方法**:

| 方法 | 说明 |
|------|------|
| `onAttachedToEngine()` | 插件绑定到 Flutter 引擎 |
| `handleMethodCall()` | 处理 Dart 层的方法调用 |
| `executeFfmpegSession()` | 执行 FFmpeg 会话（同步/异步） |
| `executeFfprobeSession()` | 执行 FFprobe 会话 |
| `emitEvent()` | 发送事件到 Dart 层 |

### MobileFfmpegSessions.java

**职责**:
- 管理 FFmpeg 会话的生命周期
- 存储会话状态、日志、统计信息
- 提供会话查询接口

**内部结构**:

```java
// 会话类型
static final int SESSION_TYPE_FFMPEG = 1;
static final int SESSION_TYPE_FFPROBE = 2;
static final int SESSION_TYPE_MEDIA_INFORMATION = 3;

// 会话状态
static final int STATE_CREATED = 0;
static final int STATE_RUNNING = 1;
static final int STATE_FAILED = 2;
static final int STATE_COMPLETED = 3;
```

---

## iOS 原生层详解

### WithoutFfmpegPlugin.m

**职责**:
- 实现 Flutter 插件接口
- 处理 MethodChannel 方法调用
- 管理 EventChannel 事件流
- 集成 ffmpegkit 库

**关键差异** (相对于 Android):

| 特性 | iOS 实现 |
|------|----------|
| 并发处理 | `dispatch_async(asyncDispatchQueue, ...)` |
| 主线程回调 | `dispatch_async(dispatch_get_main_queue(), ...)` |
| SAF 支持 | 不支持，返回 `NOT_SUPPORTED` 错误 |
| 管道支持 | 支持 `writeToPipe()` 方法 |

---

## 常见使用场景

### 1. 视频转码

```dart
Future<void> transcodeVideo(String input, String output) async {
  final session = await FFmpegKit.execute(
    "-i $input -c:v libx264 -preset fast -crf 23 $output"
  );
  
  final rc = await session.getReturnCode();
  if (ReturnCode.isSuccess(rc)) {
    print("Transcoding complete!");
  }
}
```

### 2. 提取音频

```dart
Future<void> extractAudio(String videoPath, String audioPath) async {
  final session = await FFmpegKit.execute(
    "-i $videoPath -vn -acodec copy $audioPath"
  );
  // vn = no video, acodec copy = 复制音频流
}
```

### 3. 获取媒体信息

```dart
Future<void> showMediaInfo(String path) async {
  final info = await FFprobeKit.getMediaInformation(path);
  
  print("Format: ${info.getFormat()}");
  print("Duration: ${info.getDuration()}");
  print("Bitrate: ${info.getBitrate()}");
  
  for (final stream in info.getStreams()) {
    print("${stream.getCodecType()}: ${stream.getCodecName()}");
  }
}
```

### 4. 视频压缩

```dart
Future<void> compressVideo(String input, String output) async {
  await FFmpegKit.executeAsync(
    "-i $input -vf scale=1280:-1 -c:v libx264 -preset veryfast -crf 28 $output",
    (session) => print("Compress done"),
    null,
    (stats) => print("Progress: ${stats.getTime()}")
  );
}
```

### 5. 平台检测与差异化处理

```dart
Future<void> platformAwareProcessing() async {
  final platform = await FFmpegKitConfig.getPlatform();
  
  if (platform == "android") {
    // Android: 使用 SAF 处理 Content URI
    final uri = await pickDocument();
    final safPath = await FFmpegKitConfig.getSafParameterForRead(uri);
    await FFmpegKit.execute("-i $safPath output.mp4");
  } else {
    // iOS: 直接使用文件路径
    final path = await pickDocument();
    await FFmpegKit.execute("-i $path output.mp4");
  }
}
```

---

## 注意事项

1. **错误处理**: 始终检查 `ReturnCode` 以确定命令执行结果
2. **异步操作**: 长时间运行的任务使用 `executeAsync()` 避免阻塞 UI
3. **进度监控**: 使用 `StatisticsCallback` 监控转码进度
4. **资源释放**: Flutter 插件会自动管理资源，但取消执行后需等待回调完成
5. **平台差异**: 某些功能（如 SAF、管道）在不同平台有不同支持程度

---

## 文档版本

- **版本**: 1.0.0
- **生成日期**: 2026-06-02
- **项目**: without_ffmpeg
- **作者**: Wind
