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

  # Same download as ffmpeg_kit_min_gpl, then convert to xcframework for simulator.
  s.prepare_command = <<-CMD
    download_url="https://github.com/meetleev/flutter-ffmpeg-kit/releases/download/6.0.3/ffmpeg-kit-ios-min-gpl.zip"
    if [ ! -d "./Frameworks/ffmpegkit.framework" ]; then
        echo "Downloading from ${download_url}..."
        rm -rf Frameworks
        mkdir -p Frameworks
        curl -L "${download_url}" -o /tmp/ffmpeg_kit_ios.zip
        unzip -o /tmp/ffmpeg_kit_ios.zip -d Frameworks
        rm -f /tmp/ffmpeg_kit_ios.zip
        rm -rf Frameworks/__MACOSX
    fi
    bash scripts/setup_ios_xcframework.sh
  CMD

  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES',
    'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386 x86_64',
  }
end
