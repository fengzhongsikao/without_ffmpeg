import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'without_ffmpeg_method_channel.dart';

abstract class WithoutFfmpegPlatform extends PlatformInterface {
  /// Constructs a WithoutFfmpegPlatform.
  WithoutFfmpegPlatform() : super(token: _token);

  static final Object _token = Object();

  static WithoutFfmpegPlatform _instance = MethodChannelWithoutFfmpeg();

  /// The default instance of [WithoutFfmpegPlatform] to use.
  ///
  /// Defaults to [MethodChannelWithoutFfmpeg].
  static WithoutFfmpegPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [WithoutFfmpegPlatform] when
  /// they register themselves.
  static set instance(WithoutFfmpegPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
