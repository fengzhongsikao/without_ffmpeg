import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:without_ffmpeg/without_ffmpeg_method_channel.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  MethodChannelWithoutFfmpeg platform = MethodChannelWithoutFfmpeg();
  const MethodChannel channel = MethodChannel('without_ffmpeg');

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
      channel,
      (MethodCall methodCall) async {
        return '42';
      },
    );
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel, null);
  });

  test('getPlatformVersion', () async {
    expect(await platform.getPlatformVersion(), '42');
  });
}
