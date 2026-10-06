#!/usr/bin/env python3
"""Compare preserved source properties with the original ZIP. Not an Android build/UI test."""
import argparse
import re
from pathlib import Path
from zipfile import ZipFile

parser = argparse.ArgumentParser()
parser.add_argument('baseline_zip', type=Path)
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
with ZipFile(args.baseline_zip) as z:
    baseline = {str(Path(i.filename)): z.read(i) for i in z.infolist() if not i.is_dir()}

prefix = 'app/src/main/java/com/flower/launcher/'
root_path = prefix + 'ui/LauncherRoot.kt'
old = baseline[root_path].decode()
new = (root / root_path).read_text()
constants = re.findall(r'^private (?:const )?val ([A-Z][A-Z_0-9]*) = (.+)$', old, re.M)
for name, value in constants:
    assert re.search(r'^private (?:const )?val ' + name + ' = ' + re.escape(value) + '$', new, re.M), name
print(f'PASS: {len(constants)} transition/blur constants unchanged')

functions = ['easeOutCubic', 'smoothStep', 'rubberBandDistance', 'inverseRubberBandDistance',
             'signedRubberBandDistance', 'verticalTargetForRelease', 'composeBlurLevel',
             'wallpaperBlurLevel', 'Activity.setWallpaperBlurWithoutDim']
for name in functions:
    pattern = r'^private fun ' + re.escape(name) + r'\([\s\S]*?^}'
    a = re.search(pattern, old, re.M)
    b = re.search(pattern, new, re.M)
    assert a and b and a.group() == b.group(), name
curve = r'^private val FastStartSoftFinishEasing =[\s\S]*?^}'
assert re.search(curve, old, re.M).group() == re.search(curve, new, re.M).group()
print(f'PASS: {len(functions)} visual/gesture functions and easing curve unchanged')

untouched = []
for path, data in baseline.items():
    if (path.startswith('app/src/main/res/') or path.startswith('gradle/')
            or path.endswith('.gradle.kts') or path.endswith('.toml')
            or path.endswith('AndroidManifest.xml') or path.endswith('gradle.properties')
            or path.startswith(prefix + 'ui/theme/')
            or path.endswith('SettingsScreen.kt') or path.endswith('DesktopSettingsSheet.kt')):
        assert (root / path).read_bytes() == data, path
        untouched.append(path)
print(f'PASS: {len(untouched)} resource/build/settings files byte-identical')

# Check literal dp/sp dimensions in the modified UI files as an additional guard.
for name in ['HomeContent', 'LauncherRoot', 'SearchContent', 'AllAppsContent']:
    path = prefix + 'ui/' + name + '.kt'
    a = re.findall(r'\b\d+(?:\.\d+)?\.(?:dp|sp)\b', baseline[path].decode())
    b = re.findall(r'\b\d+(?:\.\d+)?\.(?:dp|sp)\b', (root/path).read_text())
    assert a == b, name
print('PASS: literal dp/sp dimensions unchanged in four edited UI files')

# Algebra check only: A(y) * fade equals interpolating endpoints * fade.
for step in range(1001):
    fade = step / 1000
    for a, b in [(0.12, 0.22), (0.22, 0.34)]:
        for y_step in range(101):
            t = y_step / 100
            original_alpha = (a * fade) * (1-t) + (b * fade) * t
            cached_alpha = (a * (1-t) + b * t) * fade
            assert abs(original_alpha-cached_alpha) < 1e-12
print('PASS: 202202 drawer alpha interpolation checks (real-number model, not GPU pixel comparison)')
print('LIMIT: this does not compile Kotlin or validate lifecycle, device rendering, power, or frame times.')
