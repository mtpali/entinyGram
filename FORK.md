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

The default Firebase Android client is restored in `src/firebase/google-services.json` from entinyGram APK resources. All five client fields were independently verified against the official ARM64 release APKs from [2026-09-10](https://github.com/entaytion/entinyGram/releases/tag/v12.10.1-202609100) (`874a8ade`) and [2026-09-20](https://github.com/entaytion/entinyGram/releases/tag/v12.10.1-202609200) (`8ec637ff`), with each APK's SHA-256 checked against its GitHub release digest. The resources are absent from the 2026-09-26 release. The client uses project `entinygram` and package `ua.entaytion.entinygram` together with the inherited Telegram API configuration. These public client identifiers do not grant Firebase administrative access. This reconstruction contains the FCM client fields; it does not reconstruct OAuth clients or a Realtime Database URL.

CI uses this client by default. A `GOOGLE_SERVICES_JSON` repository secret overrides it; an ignored local `src/google-services.json` file overrides it for local setup. The build rejects a different package, incomplete project fields or a service-account key. Google SDK configuration strings are explicitly retained during resource shrinking and checked against the supplied client in each compiled APK. The original Firebase project remains under its maintainer's control, so restoring the client alone cannot guarantee future push delivery.

For personal Firebase credentials, also configure your own Telegram API app at [my.telegram.org/apps](https://my.telegram.org/apps), register that Firebase project's FCM credentials there, and supply the matching `TELEGRAM_APP_ID` and `TELEGRAM_APP_HASH` repository secrets together. Those values override the inherited Telegram API configuration only during CI. A new Firebase project alone does not configure Telegram's push sender. A Firebase service-account key belongs in the Telegram API app's FCM configuration, never in this repository or APK. If using the original maintainer's configuration, it must correspond to the inherited API app.

The workflow does not post to Telegram channels. The in-app updater and its channel integration have been removed.

## Validation

The workflow checks search-registry slugs, string usage, translations, patch consistency and reproducible generated icons. Each built APK is checked for its localized app label, single ABI, preserved stock icon, removed old alias, compiled Force LTR preference, matching Firebase configuration resources and valid signature. Actual push delivery must also be tested on a device with Google Play services and a Telegram account: allow notifications, sign in, close the app normally, lock the screen and send a message from another account. Android Force stop is a separate state that blocks delivery until the app is reopened.

Device acceptance checks: select Persian, enable Force LTR and restart; check chat list, drawer, settings, chats and dialogs; rotate the screen and change font size; switch languages; disable Force LTR and restart. Persian text must remain readable and the selected language must remain unchanged.

## VPN963 customization

Only English and Persian UI languages are shipped or offered by the language picker. Other source translations and non-default icon packs are removed during source materialization. Existing unsupported interface languages fall back to English or the supported device language. Persian uses Telegram's official language pack for stock UI strings; fork strings are bundled.

The fork header reads VPN963. Settings Help contains only `Telegram : VPN963`, opening `vpn963` in the selected account. Its title and route are encoded against the registered package identifier; R8 renames only that helper, retaining the existing naming behavior elsewhere. Open source remains editable. Attribution and Firebase package/project identifiers remain unchanged.

Custom AI providers, transcription, editor and summaries, iOS layout controls and previews, local Premium/custom emoji/local names, archive locking and the internal updater are removed. Stock protocol types required for Telegram interoperability remain. Aqua and Vintage launcher aliases and assets are removed; Default, the blue Telegram icon and the remaining stock choices are preserved. Package replacement restores Default if a removed launcher choice had been selected.

Solar Hijri is the only calendar choice. ICU formats Persian dates, including old timestamps, and uses the actual date pattern. The history grid also uses Persian month lengths, leap years and weekday positions, with standard Telegram timestamps. Calendar display was compared with NagramXF at `3c9bd86df5200008b00b998b51d262f7c6b8a64d`; no additional calendar dependency is needed.

Numbers default to unrounded. Downloads use the Telegram folder. Power-saving flags and battery threshold, pinned-message notifications, raise-to-listen and pause-music-on-recording default to off. Automatic media downloads are initialized off once per account, including upgrades from the previous build, and later user changes are retained. Other saved choices are retained.
