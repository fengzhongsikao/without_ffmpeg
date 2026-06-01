import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'without_ffmpeg_platform_interface.dart';

/// An implementation of [WithoutFfmpegPlatform] that uses method channels.
class MethodChannelWithoutFfmpeg extends WithoutFfmpegPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('without_ffmpeg');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
