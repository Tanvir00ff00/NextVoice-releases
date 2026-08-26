# NextVoice — releases

Download channel for the [NextVoice](https://github.com/Tanvir00ff00/NextVoice)
Android app. This repository holds **binaries only** — the source lives in a
private repository.

## Install

Grab the latest APK from the [releases page](../../releases/latest). Once it is
installed, the app checks here for new versions on its own and offers them from
the home screen; you should not need to come back.

## What is published

Every release carries two assets:

| File | Purpose |
|---|---|
| `nextvoice-<version>.apk` | The signed app |
| `update.json` | Version, size, SHA-256 and release notes, read by the in-app updater |

The app fetches `update.json` from
`releases/latest/download/update.json`, which GitHub always resolves to the
newest release, and verifies the APK's SHA-256 before handing it to the system
installer.

## Notes

Every APK is signed with the same key. Android will refuse an update whose
signature does not match what is installed, so if you previously side-loaded a
build signed with a different key, uninstall it once before installing from here.
