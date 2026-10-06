# Telegram personal fork

Based on [entaytion/entinyGram v12.10.6-202610040](https://github.com/entaytion/entinyGram/releases/tag/v12.10.6-202610040), commit `77b9c206998cc4bdaf8d24cc55ae312daf426c24`. Telegram Android is pinned by `upstream-commit`.

The app is named **Telegram**. Its default black-and-white launcher icon comes from [mtpali/NagramXF](https://github.com/mtpali/NagramXF), commit `3c9bd86df5200008b00b998b51d262f7c6b8a64d`. The blue stock Telegram icon remains selectable. The old entinyGram launcher and notification icons are removed. Source attribution and the original icon project's license are in `src/res/launcher/nagramxf/`.

**Force LTR:** Settings → Telegram Settings → Appearance → Force LTR. The setting defaults to off. Enable it and tap Restart now. It changes interface layout direction while preserving Persian/Arabic strings, locale selection and text shaping. Disable it and restart to restore the selected language's normal direction.

## Build and downloads

The **Build Telegram APKs** workflow builds two signed APKs:

| File | ABI | Devices |
| --- | --- | --- |
| `Telegram-12.10.6-armv7.apk` | `armeabi-v7a` | 32-bit ARM |
| `Telegram-12.10.6-armv8.apk` | `arm64-v8a` | 64-bit ARM |

Both require Android 8.0 or newer. Pushes to `main` and the development branch run the workflow; it can also be run manually. Successful builds are published as GitHub preview releases, with SHA-256 checksums and per-ABI build metadata. Version codes start at `2026100600` plus the workflow run number.

The package ID remains `ua.entaytion.entinygram`, so account/data identity is retained. APKs are signed with the development keystore already provided in the Telegram source. For long-term public distribution, configure a private signing key and keep it consistent across builds. Android will accept an update only when its signing certificate matches the installed app.

Firebase configuration is required before CI publishes either APK. Set the repository secret `GOOGLE_SERVICES_JSON` to a real Firebase Android export for `ua.entaytion.entinygram`. The build rejects missing configuration, a different package, incomplete project fields, or a service-account key. Google SDK configuration strings are explicitly retained during resource shrinking and checked in the compiled APK.

For personal Firebase credentials, also configure your own Telegram API app at [my.telegram.org/apps](https://my.telegram.org/apps), register that Firebase project's FCM credentials there, and supply the matching `TELEGRAM_APP_ID` and `TELEGRAM_APP_HASH` repository secrets together. Those values override the inherited Telegram API configuration only during CI. A new Firebase project alone does not configure Telegram's push sender. A Firebase service-account key belongs in the Telegram API app's FCM configuration, never in this repository or APK. If using the original maintainer's configuration, it must correspond to the inherited API app.

The workflow does not post to Telegram channels. Automatic upstream update checks default to off to keep personal branding; configure your own update source before enabling them.

## Validation

The workflow checks search-registry slugs, string usage, translations, patch consistency and reproducible generated icons. Each built APK is checked for its localized app label, single ABI, preserved stock icon, removed old alias, compiled Force LTR preference, Firebase configuration resources and valid signature. Actual push delivery must also be tested on a device with Google Play services and a Telegram account.

Device acceptance checks: select Persian, enable Force LTR and restart; check chat list, drawer, settings, chats and dialogs; rotate the screen and change font size; switch languages; disable Force LTR and restart. Persian text must remain readable and the selected language must remain unchanged.
