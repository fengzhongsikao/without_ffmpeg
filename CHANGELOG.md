## 0.0.2

* Android: switch to `mobile-ffmpeg-full-gpl:4.4` so `libx264` is available.
* iOS: ship arm64 simulator xcframeworks; exclude simulator `x86_64` so Apple Silicon simulators link correctly.
* iOS: make `setup_ios_xcframework.sh` idempotent so `pod install` does not wipe existing frameworks.

## 0.0.1

* Initial release.
