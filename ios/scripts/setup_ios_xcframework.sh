#!/bin/bash
# Vendored xcframeworks must already be in ios/Frameworks.
# Each one needs an iphoneos arm64 slice and an iphonesimulator slice
# that contains x86_64. This script only checks that; it does not download
# or rewrite binaries.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
IOS_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
FRAMEWORKS_DIR="${IOS_DIR}/Frameworks"

required=(
  ffmpegkit
  libavcodec
  libavdevice
  libavfilter
  libavformat
  libavutil
  libswresample
  libswscale
)

missing=0
for name in "${required[@]}"; do
  xc="${FRAMEWORKS_DIR}/${name}.xcframework"
  device="${xc}/ios-arm64/${name}.framework"
  simulator="${xc}/ios-arm64_x86_64-simulator/${name}.framework"
  if [[ ! -d "${device}" || ! -d "${simulator}" ]]; then
    echo "error: ${name}.xcframework is missing ios-arm64 or ios-arm64_x86_64-simulator" >&2
    missing=1
  fi
done

if [[ "${missing}" -ne 0 ]]; then
  echo "error: ios/Frameworks must contain the vendored min-gpl xcframeworks." >&2
  exit 1
fi

echo "XCFrameworks present (device arm64 + simulator arm64/x86_64)."
