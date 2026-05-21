#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

SDK_DIR="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$ROOT_DIR/.android-sdk}}"
ADB="$SDK_DIR/platform-tools/adb"
APK="$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk"
PACKAGE="com.example.compositionhelper"
ACTIVITY="$PACKAGE/.MainActivity"
START_DESTINATION="camera"
SCREENSHOT_PATH=""
WAIT_SECONDS="3"

usage() {
  cat >&2 <<USAGE
Usage: ./view-android.sh [--camera|--gallery] [--serial SERIAL] [--screenshot PATH] [--wait SECONDS]

Examples:
  ./view-android.sh --gallery
  ./view-android.sh --camera --serial emulator-5554
  ./view-android.sh --gallery --screenshot /tmp/compositionhelper.png --wait 20
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --camera)
      START_DESTINATION="camera"
      shift
      ;;
    --gallery)
      START_DESTINATION="gallery"
      shift
      ;;
    --serial)
      if [[ $# -lt 2 ]]; then
        usage
        exit 1
      fi
      export ANDROID_SERIAL="$2"
      shift 2
      ;;
    --screenshot)
      if [[ $# -lt 2 ]]; then
        usage
        exit 1
      fi
      SCREENSHOT_PATH="$2"
      shift 2
      ;;
    --wait)
      if [[ $# -lt 2 ]]; then
        usage
        exit 1
      fi
      WAIT_SECONDS="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      usage
      exit 1
      ;;
  esac
done

if [[ ! -x "$ADB" ]]; then
  echo "adb not found at: $ADB" >&2
  echo "Set ANDROID_HOME or install the project-local SDK first." >&2
  exit 1
fi

if [[ ! -f "$APK" ]]; then
  echo "Debug APK not found. Building it first..."
  ./gradlew assembleDebug
fi

if [[ -n "${ANDROID_SERIAL:-}" ]]; then
  if ! "$ADB" get-state >/dev/null 2>&1; then
    echo "Selected Android target is not online: $ANDROID_SERIAL" >&2
    "$ADB" devices -l >&2
    exit 1
  fi
else
  DEVICE_COUNT="$($ADB devices | awk 'NR > 1 && $2 == "device" { count++ } END { print count + 0 }')"
  if [[ "$DEVICE_COUNT" -eq 0 ]]; then
    echo "No online Android device/emulator found." >&2
    echo "Start an emulator with hardware acceleration or connect a device with USB debugging enabled." >&2
    echo "Then rerun: ./view-android.sh --gallery" >&2
    exit 1
  fi

  if [[ "$DEVICE_COUNT" -gt 1 ]]; then
    echo "Multiple online devices found. Choose one with --serial:" >&2
    $ADB devices -l >&2
    exit 1
  fi
fi

echo "Installing $APK"
"$ADB" install -r "$APK"

echo "Granting runtime permissions when supported"
"$ADB" shell pm grant "$PACKAGE" android.permission.CAMERA >/dev/null 2>&1 || true
"$ADB" shell pm grant "$PACKAGE" android.permission.READ_MEDIA_IMAGES >/dev/null 2>&1 || true
"$ADB" shell pm grant "$PACKAGE" android.permission.READ_EXTERNAL_STORAGE >/dev/null 2>&1 || true

echo "Launching $ACTIVITY with startDestination=$START_DESTINATION"
"$ADB" shell am start -n "$ACTIVITY" -e startDestination "$START_DESTINATION"

if [[ -n "$SCREENSHOT_PATH" ]]; then
  echo "Waiting up to ${WAIT_SECONDS}s for $PACKAGE to take focus before screenshot"
  deadline=$((SECONDS + WAIT_SECONDS))
  while (( SECONDS < deadline )); do
    focus="$($ADB shell dumpsys window 2>/dev/null | grep -E 'mCurrentFocus|mFocusedApp' || true)"
    if echo "$focus" | grep -q "$PACKAGE" && ! echo "$focus" | grep -q 'Application Not Responding'; then
      break
    fi
    sleep 1
  done
  sleep 2
  mkdir -p "$(dirname "$SCREENSHOT_PATH")"
  "$ADB" exec-out screencap -p > "$SCREENSHOT_PATH"
  echo "Screenshot saved to $SCREENSHOT_PATH"
fi

echo "Done. The app should now be visible on the connected Android device/emulator."
