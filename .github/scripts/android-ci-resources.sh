#!/usr/bin/env bash
set -euo pipefail

report_resources() {
  date -u
  free -h || true
  df -h "$RUNNER_TEMP" "$GITHUB_WORKSPACE" "$ANDROID_HOME" || true
  local cgroup_path memory_events
  if [[ -r /proc/self/cgroup ]]; then
    cgroup_path=$(sed -n 's/^0:://p' /proc/self/cgroup) || cgroup_path=''
    memory_events="/sys/fs/cgroup${cgroup_path%/}/memory.events"
    if [[ -r "$memory_events" ]]; then
      cat "$memory_events" || true
    fi
  fi
  ps -eo pid,comm,rss | awk 'NR == 1 || $2 == "emulator" || $2 ~ /^qemu-system/' || true
}

report_resources
(
  pause=''
  trap 'if [[ -n "$pause" ]]; then kill "$pause" 2>/dev/null || true; fi; exit' TERM
  while :; do
    sleep 30 &
    pause=$!
    wait "$pause"
    report_resources
  done
) &
monitor=$!
trap 'kill "$monitor" 2>/dev/null || true; wait "$monitor" 2>/dev/null || true; report_resources' EXIT

"$@"
