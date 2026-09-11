# Demo content packages

Two real, installable content packages for demoing the P2P/backend content pipeline —
P0 item "Create two real demo content ZIPs".

- `math-grade5-decimals/` (+ `.zip`) — Mathematics, Grade 5, English + Hindi
- `science-grade5-water-cycle/` (+ `.zip`) — Science, Grade 5, English + Hindi

Each `.zip` is the source directory zipped flat (manifest.json, checksums.json, lessons/,
questions/ at the archive root — no wrapping folder), matching the format
`ContentPackageReader`/`ContentInstallerImpl` expect. The `checksum` field in each
`manifest.json` is a real SHA-256 over the concatenated bytes of `lessons/*.json` then
`questions/*.json` in sorted filename order — the exact algorithm
`ContentPackageReader.computeContentChecksum()` uses — verified by
`app/src/test/java/.../DemoContentPackageTest.kt`, which extracts each zip and runs it
through the actual `ContentInstallerImpl.install()` path (mocked DAOs only).

## How to use for a demo

- **P2P store-and-forward**: `adb push math-grade5-decimals.zip /data/data/com.hackx.ruraledtech/files/packages/math-grade5-decimals.zip`
  on a device (app must have created that dir once — launch it first), restart the app, and
  it will appear in that device's local manifest as an installed package other devices can
  request over the mesh. (Simpler alternative: extract one zip's contents into a directory
  and call `ContentInstaller.install(path)` from a debug entry point once one exists.)
- **Backend upload**: multipart-POST the zip to `POST /api/v1/content` with the fields
  `RuralEdTechApi.uploadContent` expects (`package_id`, `version`, `subject`, `grade`,
  `language`, `checksum`, `manifest`, `file`) — there is no in-app "teacher upload" screen
  yet, so this currently needs a manual client (curl/Postman) or a teammate's tooling.
