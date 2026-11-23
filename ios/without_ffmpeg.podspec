Pod::Spec.new do |s|
  s.name             = 'without_ffmpeg'
  s.version          = '0.0.1'
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

  # Download device frameworks if missing, then convert to xcframework
  # (device arm64/arm64e + Apple Silicon simulator arm64).
  s.prepare_command = 'bash scripts/setup_ios_xcframework.sh'

  # Upstream min-gpl binaries have no x86_64 simulator slice.
  # Apple Silicon simulators use arm64; exclude x86_64 so link succeeds.
  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES',
    'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386 x86_64',
  }
  s.user_target_xcconfig = {
    'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386 x86_64',
  }
end
