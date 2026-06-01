
import 'without_ffmpeg_platform_interface.dart';

class WithoutFfmpeg {
  Future<String?> getPlatformVersion() {
    return WithoutFfmpegPlatform.instance.getPlatformVersion();
  }

  Future<String?> test1() {
    return WithoutFfmpegPlatform.instance.test1();
  }
}
