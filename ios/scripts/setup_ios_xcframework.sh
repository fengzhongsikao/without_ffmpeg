#!/bin/bash
# Converts device-only FFmpegKit .framework bundles into .xcframework with a
# simulator variant (arm64 @ IOSSIMULATOR). Required for iOS Simulator on Apple Silicon.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
IOS_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
FRAMEWORKS_DIR="${IOS_DIR}/Frameworks"
DOWNLOAD_URL="https://github.com/meetleev/flutter-ffmpeg-kit/releases/download/6.0.3/ffmpeg-kit-ios-min-gpl.zip"

cd "${IOS_DIR}"

if [ ! -d "${FRAMEWORKS_DIR}" ] || [ -z "$(ls -A "${FRAMEWORKS_DIR}" 2>/dev/null || true)" ]; then
  echo "Downloading FFmpegKit iOS frameworks..."
  mkdir -p "${FRAMEWORKS_DIR}"
  curl -L "${DOWNLOAD_URL}" -o /tmp/ffmpeg_kit_ios.zip
  unzip -o /tmp/ffmpeg_kit_ios.zip -d "${FRAMEWORKS_DIR}"
  rm -f /tmp/ffmpeg_kit_ios.zip
fi

# Already converted.
if compgen -G "${FRAMEWORKS_DIR}/*.xcframework" > /dev/null; then
  echo "XCFrameworks already present, skipping conversion."
  exit 0
fi

if ! compgen -G "${FRAMEWORKS_DIR}/*.framework" > /dev/null; then
  echo "error: no .framework found under ${FRAMEWORKS_DIR}" >&2
  exit 1
fi

echo "Converting .framework to .xcframework (device + simulator)..."

for fw in "${FRAMEWORKS_DIR}"/*.framework; do
  fwname="$(basename "${fw}" .framework)"
  fwpath="${fw}/${fwname}"

  echo "  -> ${fwname}"

  lipo "${fwpath}" -thin arm64 -output "/tmp/${fwname}_arm64"

  has_arm64e=false
  if lipo "${fwpath}" -thin arm64e -output "/tmp/${fwname}_arm64e" 2>/dev/null; then
    has_arm64e=true
  fi

  has_x86_64=false
  if lipo "${fwpath}" -thin x86_64 -output "/tmp/${fwname}_x86_64" 2>/dev/null; then
    has_x86_64=true
  fi

  # arm64 built for iOS device -> repackage for iOS Simulator (platform 7).
  xcrun vtool -set-build-version 7 12.1 18.5 -replace \
    -output "/tmp/${fwname}_arm64_sim" "/tmp/${fwname}_arm64"

  devdir="${IOS_DIR}/XCFramework/tmp/ios-arm64/${fwname}.framework"
  simdir="${IOS_DIR}/XCFramework/tmp/ios-arm64-simulator/${fwname}.framework"
  mkdir -p "${devdir}" "${simdir}"
  cp -R "${fw}/" "${devdir}/"
  cp -R "${fw}/" "${simdir}/"

  if [ "${has_arm64e}" = true ]; then
    lipo "/tmp/${fwname}_arm64" "/tmp/${fwname}_arm64e" \
      -create -output "${devdir}/${fwname}"
  else
    cp "/tmp/${fwname}_arm64" "${devdir}/${fwname}"
  fi

  if [ "${has_x86_64}" = true ]; then
    lipo "/tmp/${fwname}_arm64_sim" "/tmp/${fwname}_x86_64" \
      -create -output "${simdir}/${fwname}"
  else
    cp "/tmp/${fwname}_arm64_sim" "${simdir}/${fwname}"
  fi

  if [ -f "${simdir}/Info.plist" ]; then
    sed -i '' 's/iPhoneOS/iPhoneSimulator/g' "${simdir}/Info.plist" || true
  fi

  xcodebuild -create-xcframework \
    -framework "${devdir}" \
    -framework "${simdir}" \
    -output "${FRAMEWORKS_DIR}/${fwname}.xcframework"

  rm -f "/tmp/${fwname}_arm64" "/tmp/${fwname}_arm64e" \
    "/tmp/${fwname}_x86_64" "/tmp/${fwname}_arm64_sim"
done

rm -rf "${IOS_DIR}/XCFramework"
rm -rf "${FRAMEWORKS_DIR}"/*.framework
rm -rf "${FRAMEWORKS_DIR}/__MACOSX"

echo "Done. XCFrameworks are in ${FRAMEWORKS_DIR}"
