import 'package:flutter_test/flutter_test.dart';
import 'package:without_ffmpeg/media_information.dart';
import 'package:without_ffmpeg/return_code.dart';

void main() {
  test('ReturnCode.isSuccess / isCancel', () {
    expect(ReturnCode.isSuccess(ReturnCode(0)), isTrue);
    expect(ReturnCode.isSuccess(ReturnCode(1)), isFalse);
    expect(ReturnCode.isCancel(ReturnCode(255)), isTrue);
    expect(ReturnCode.isCancel(ReturnCode(0)), isFalse);
  });

  test('MediaInformation.getDuration', () {
    final info = MediaInformation({
      'format': {'duration': '12.345'},
    });
    expect(info.getDuration(), '12.345');
  });
}
