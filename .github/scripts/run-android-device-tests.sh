#!/usr/bin/env bash
set -euo pipefail

tools="$ANDROID_HOME/cmdline-tools/latest/bin"
adb="$ANDROID_HOME/platform-tools/adb"
device=$(jq -er '[.name, .avdProfile, .systemImageApi, .systemImageSource] | @tsv' \
    gradle/android-test-device.json)
IFS=$'\t' read -r avd profile image_api image_source <<< "$device"
image="system-images;android-$image_api;$image_source;x86_64"
port=5554
log="$RUNNER_TEMP/android-emulator.log"
logcat="$RUNNER_TEMP/android-logcat.log"

"$tools/sdkmanager" --install emulator "$image"
# Keep avdmanager and emulator on the same AVD lookup path.
ANDROID_AVD_HOME=$(mktemp -d "$RUNNER_TEMP/android-avd.XXXXXX")
export ANDROID_AVD_HOME
"$tools/avdmanager" create avd --name "$avd" --package "$image" --device "$profile" <<< no
"$adb" start-server

"$ANDROID_HOME/emulator/emulator" -avd "$avd" -port "$port" -no-window -gpu software \
    -no-snapshot -noaudio -no-boot-anim > "$log" 2>&1 &
emulator_pid=$!

cleanup() {
    local status=$?
    if (( status != 0 )); then
        timeout 15s "$adb" logcat -d -v threadtime > "$logcat" 2>&1 || true
        tail -n 80 "$log"
    fi
    kill "$emulator_pid" 2>/dev/null || true
    wait "$emulator_pid" 2>/dev/null || true
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

export ANDROID_SERIAL="emulator-$port"
boot_deadline=$((SECONDS + 300))
until [[ $(timeout 10s "$adb" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r') == 1 ]]; do
    if ! kill -0 "$emulator_pid" 2>/dev/null; then
        echo 'Android emulator exited before finishing boot.' >&2
        exit 1
    fi
    if (( SECONDS >= boot_deadline )); then
        echo 'Android emulator did not finish booting within five minutes.' >&2
        exit 1
    fi
    sleep 2
done
"$adb" shell input keyevent 82

bash .github/scripts/android-ci-resources.sh \
    ./gradlew --console=plain connectedAndroidDeviceTest --continue --max-workers=1
