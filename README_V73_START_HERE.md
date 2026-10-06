# Shepherd Launcher V73 — Serialized IME State Machine

V73 fixes the Search-entry IME oscillation reported after V72: entering Search from Home and then reversing upward could make the keyboard repeatedly show/hide.

## Root cause

V72 still allowed independent IME command sources to race:

- the 5.5% Home → Search presentation gate could drive `requestKeyboard` true/false;
- page-return suppression could independently request Hide;
- `programmaticImeHidePending` could be cleared by a newer Show before an older Hide acknowledgement arrived;
- a late `imeVisible=true` acknowledgement could overwrite a newer presentation target.

Because Android IME visibility is asynchronous, an old Hide/Show acknowledgement could arrive after the desired state had already reversed and feed back into the presentation state.

## V73 architecture

Search keeps two separate concepts:

- `SearchPresentationMode.Input / Expanded`: desired Search presentation;
- `SearchImePhase.Hidden / Showing / Visible / Hiding`: acknowledged/in-flight IME phase.

`SearchCoordinator.reconcileIme()` is now the only normal Search path that emits `Show` / `Hide` commands.

Serialization rules:

1. `Hidden -> Showing -> Visible` completes before a newer Hide can be emitted.
2. `Visible -> Hiding -> Hidden` completes before a newer Show can be emitted.
3. The 5.5% transition progress is only a **Show permission gate**. Dropping below it does not hide the IME.
4. Hide policy comes from Expanded mode, page-return suppression, hidden Search session, or Search-settings force-hide.
5. IME visibility callbacks acknowledge actual state; they no longer blindly rewrite the desired presentation.

## Preserved behavior

- Every fresh Home → Search session still targets Input.
- `KEYBOARD_TRIGGER_PROGRESS = 0.055f` is unchanged.
- V70 unified empty/non-empty top pull remains unchanged.
- Empty-query upward Search → Home ownership remains in LauncherRoot.
- V59–V61 same-pointer handoff logic is unchanged.
- IME reveal/collapse thresholds and haptics are unchanged.

## Verification

- V61 gesture coordinator static checks: PASS.
- V70 unified Search pull static checks: PASS.
- V73 serialized IME checks: PASS (21 checks).
- Kotlin parser/new-symbol scan: no V73 syntax/new-symbol errors found.
- Full Gradle test/build could not run in the execution environment because the project wrapper downloads Gradle 8.13 from `mirrors.aliyun.com`, which is unreachable here.
