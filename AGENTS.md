# AGENTS.md

## Windows / Gradle

When running Gradle commands on Windows:

- Always pass `--no-daemon`.
- Prefer `--console=plain` for non-interactive builds.
- Do not leave Gradle daemons or other background processes running after shell commands.

Examples:

```
.\gradlew.bat :app:assembleDebug --no-daemon --console=plain
.\gradlew.bat test --no-daemon --console=plain
```

## Source update workflow

When given a new optimized source zip (e.g. `F:\Projects\software\FlowerLauncher-optimized-*-source.zip`):

- Extract to a temp staging dir, diff against the current project, and copy over the project (keep `local.properties`).
- Build (`assembleDebug --offline --no-daemon --console=plain`). Fix any compile errors minimally so it builds.
- Two request types (Chinese instructions):
  - 「更新 / 更新一下」 = apply + build only. Do NOT verify on device / adb screenshots / device checks.
  - 「执行更新」 = apply + build AND verify on device (install via adb, screenshots).
- Always install the built APK to the phone after a successful build (`adb install -r app\build\outputs\apk\debug\app-debug.apk`), unless asked not to. This applies to every update.
- Report: files changed, new docs, build result, and any compile fixes applied.

## Source package

After every change (feature, fix, or docs), refresh the source zip at:

```
F:\Projects\software\shepherd_launcher_source.zip
```

Rules:

- Copy the shepherd project into a temp staging dir, excluding `build`, `.gradle`, `.kotlin`, `.idea`, `.git`, `local.properties`, `*.log`, `.DS_Store`.
- Create the zip so entries use forward slashes (`/`), not backslashes. Use `tar.exe -a -cf <zip> -C <staging> .` (do NOT use PowerShell `Compress-Archive` / `[System.IO.Compression.ZipFile]::CreateFromDirectory`, they emit `\` separators on Windows).
- Overwrite the existing zip.

## Git

- Repository root: `F:\助手\FlowerLauncher` (branch `main`, remote `origin` = `https://github.com/justsosdsds/flowerlauncher.git`).
- **After every completed update, commit AND push.** This applies to features, fixes, docs and source-zip refreshes: `git add -A` → `git commit -m "<concise message>"` → `git push`.
- Before committing, review with `git status` / `git diff --stat`. Never commit `build/`, `.gradle/`, `.kotlin/`, `.idea/`, `local.properties`, `*.apk`, `*.zip`, `*.log` (most are already covered by `.gitignore`).
- Only skip the push when the user explicitly asks not to (e.g. “只提交” / “不要推送”).
