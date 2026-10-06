#!/usr/bin/env python3
"""Check this optimization against the supplied ZIP; not a compiler or a UI test."""
import argparse
import re
from pathlib import Path
from zipfile import ZipFile

parser = argparse.ArgumentParser()
parser.add_argument("baseline_zip", type=Path)
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
with ZipFile(args.baseline_zip) as archive:
    baseline = {
        str(Path(item.filename)): archive.read(item)
        for item in archive.infolist() if not item.is_dir()
    }

prefix = "app/src/main/java/com/flower/launcher/"
allowed_changes = {
    prefix + "apps/AppListViewModel.kt",
    prefix + "apps/AppNameSortKey.kt",
    prefix + "apps/AppRepository.kt",
    prefix + "ui/AllAppsContent.kt",
    prefix + "ui/LauncherRoot.kt",
}
unchanged = 0
for path, original in baseline.items():
    current = root / path
    assert current.is_file(), f"Original file removed: {path}"
    if path not in allowed_changes:
        assert current.read_bytes() == original, f"Unexpected change: {path}"
        unchanged += 1
print(f"PASS: {unchanged} original files unchanged; edits limited to five existing Kotlin files")

old_root = baseline[prefix + "ui/LauncherRoot.kt"].decode()
new_root = (root / (prefix + "ui/LauncherRoot.kt")).read_text()
marker = "private tailrec fun Context.findActivity()"
assert old_root[old_root.index(marker):] == new_root[new_root.index(marker):]
print("PASS: full transition controller, wallpaper blur host, easing and all tail helpers unchanged")

# These blocks contain no braces in string literals. Track nested braces so this
# also compares the formulas, branches and gesture cancellation bodies verbatim.
def blocks(source, pattern):
    result = []
    for match in re.finditer(pattern, source):
        start = match.start()
        opening = source.index("{", match.start())
        depth = 1
        end = opening + 1
        while depth and end < len(source):
            if source[end] == "{":
                depth += 1
            elif source[end] == "}":
                depth -= 1
            end += 1
        assert depth == 0, f"Unbalanced block near {start}"
        result.append(source[start:end])
    return result

patterns = {
    "render blocks": r"\.(?:graphicsLayer|drawBehind|drawWithCache)\s*\{",
    "pointer handlers": r"\.pointerInput\(",
}
for name in ("LauncherRoot", "AllAppsContent"):
    path = prefix + f"ui/{name}.kt"
    old = baseline[path].decode()
    new = (root / path).read_text()
    for label, pattern in patterns.items():
        previous, current = blocks(old, pattern), blocks(new, pattern)
        assert previous == current, f"Changed {label} in {name}"
        print(f"PASS: {name}: {len(previous)} {label} byte-identical")

old_drawer = baseline[prefix + "ui/AllAppsContent.kt"].decode()
new_drawer = (root / (prefix + "ui/AllAppsContent.kt")).read_text()
# Remove only the old indexing helpers, which now live in AppDrawerCatalog.
helper_start = old_drawer.index("private data class DrawerIndex")
helper_end = old_drawer.index("@Composable\nprivate fun AlphabetIndexBar", helper_start)
old_drawer = old_drawer[:helper_start] + old_drawer[helper_end:]
layout_marker = "    val showIndexBar = indexLetters.isNotEmpty()"
assert old_drawer[old_drawer.index(layout_marker):] == new_drawer[new_drawer.index(layout_marker):]
print("PASS: drawer layout, menu content, index gestures and item rendering unchanged")
print("LIMIT: source equivalence is not Android compilation, device rendering or a frame-time measurement")
