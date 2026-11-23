Pod::Spec.new do |s|
  s.name             = 'without_ffmpeg'
  s.version          = '0.0.2'
  s.summary          = 'FFmpeg Kit for Flutter (without_ffmpeg)'
  s.description      = 'A Flutter plugin for running FFmpeg and FFprobe commands on iOS using FFmpegKit.'
  s.homepage         = 'https://github.com/arthenica/ffmpeg-kit'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Your Company' => 'email@example.com' }
  s.source           = { :path => '.' }

  s.platform         = :ios, '12.1'
  s.requires_arc     = true
  s.static_framework = true

  s.dependency 'Flutter'

  s.source_files        = 'Classes/**/*'
  s.public_header_files = 'Classes/**/*.h'

  s.vendored_frameworks = 'Frameworks/*.xcframework'
  s.ios.frameworks      = 'VideoToolbox'
  s.libraries           = 'z', 'bz2', 'c++', 'iconv'

  # Frameworks already include ios-arm64 (device) and
  # ios-arm64_x86_64-simulator. Do not re-download or relabel them.
  s.prepare_command = 'bash scripts/setup_ios_xcframework.sh'

  # Simulator builds may be arm64 or x86_64. This only drops 32-bit i386.
  # iphoneos (real device) is not affected.
  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES',
    'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386',
  }
end
