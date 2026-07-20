# chla-ios

NDD Resource Map — SwiftUI iOS app (KiNDD brand) in the CHLA monorepo, project generated via XcodeGen.

## Stack

- Swift 5.9, SwiftUI, XcodeGen-managed Xcode project (`project.yml`)
- iOS 18 deployment target (`project.yml`); `Package.swift` separately declares iOS 17 / macOS 14 for the SPM library shim
- SPM dependency: `Textual` (gonzalezreal/textual)
- Bundle ID `com.nddresources.map`; includes a `KiNDDWidgetsExtension` widget target

## Commands

| Task | Command |
| --- | --- |
| Generate project | `xcodegen generate` (or `./setup.sh`) |
| Build | `xcodebuild -project CHLA-iOS.xcodeproj -scheme CHLA-iOS -sdk iphonesimulator build` |
| Run | Open `CHLA-iOS.xcodeproj` in Xcode, run the `CHLA-iOS` scheme |
| Test | (no test target defined in `project.yml`) |

Defer to the repo root `AGENTS.md` for working agreement; log changes in root `FIXES.md`.
