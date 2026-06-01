import 'package:flutter_test/flutter_test.dart';
import 'package:without_ffmpeg/without_ffmpeg.dart';
import 'package:without_ffmpeg/without_ffmpeg_platform_interface.dart';
import 'package:without_ffmpeg/without_ffmpeg_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockWithoutFfmpegPlatform
    with MockPlatformInterfaceMixin
    implements WithoutFfmpegPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final WithoutFfmpegPlatform initialPlatform = WithoutFfmpegPlatform.instance;

  test('$MethodChannelWithoutFfmpeg is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelWithoutFfmpeg>());
  });

  test('getPlatformVersion', () async {
    WithoutFfmpeg withoutFfmpegPlugin = WithoutFfmpeg();
    MockWithoutFfmpegPlatform fakePlatform = MockWithoutFfmpegPlatform();
    WithoutFfmpegPlatform.instance = fakePlatform;

    expect(await withoutFfmpegPlugin.getPlatformVersion(), '42');
  });
}
