#!/usr/bin/env bash
set -euo pipefail
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
cd "$project_dir"

# A small compatibility smoke check. This does NOT replace the behavioral tests.
python3 verification/verify_v61_gesture_coordinator.py
python3 verification/verify_v70_unified_search_pull.py
python3 verification/verify_v73_serialized_ime.py
bash ./gradlew :app:testDebugUnitTest :app:assembleDebug
