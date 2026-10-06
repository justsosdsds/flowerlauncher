from pathlib import Path

root = Path(__file__).resolve().parents[1] / "app/src/main/java/com/flower/launcher/ui"
launcher = (root / "LauncherRoot.kt").read_text()
search = (root / "SearchCoordinator.kt").read_text()
content = (root / "SearchContent.kt").read_text()
gesture = (root / "GestureCoordinator.kt").read_text()
interaction = (root / "SearchInteractionState.kt").read_text()

checks = {
    "GestureCoordinator exists": "class GestureCoordinator" in gesture,
    "Root local handoff latches removed": all(x not in launcher for x in ("returnArmed", "handoffStarted")),
    "Search pointer-held latch removed": "pageReturnPointerHeld" not in search,
    "release intent latch removed": "PageReturnPointerReleased" not in search,
    "threshold transfer is accepted before Returning": (
        "commitSearchBottomReturn" in launcher and "SearchIntent.PageReturnStarted" in launcher
    ),
    "Search threshold callback returns acceptance": "onReturnHomeThreshold: () -> Boolean" in content,
    "same-frame release path exists": (
        "if (!pressed)" in gesture and "finishTarget = SearchReturnSettleTarget.Home" in gesture
    ),
    "reversal settle is coordinator-owned": (
        "downwardAfterCommitPx" in gesture and "settleTargetForSearchReturn" in gesture
    ),
    "Root applies coordinator frames": "onSearchPointerFrame" in launcher,
    "SearchInteractionState beginInteractive is non-suspend": (
        "fun beginInteractive()" in interaction and "suspend fun beginInteractive" not in interaction
    ),
    "SearchCoordinator beginInteractive is non-suspend": (
        "fun beginInteractive()" in search and "suspend fun beginInteractive" not in search
    ),
    "cancelAndJoin absent": "cancelAndJoin" not in interaction and "cancelAndJoin" not in search,
}

failed = [name for name, ok in checks.items() if not ok]
for name, ok in checks.items():
    print(("PASS" if ok else "FAIL"), name)

if failed:
    raise SystemExit("V61 verification failed: " + ", ".join(failed))
