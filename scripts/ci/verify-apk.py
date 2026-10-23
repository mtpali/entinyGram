import hashlib
import json
import re
import subprocess
import sys
import zipfile
from pathlib import Path

apk = Path(sys.argv[1])
abi = sys.argv[2]
aapt = sys.argv[3]
firebase_config = json.loads(Path(sys.argv[4]).read_text())
firebase_client = next(c for c in firebase_config["client"]
                       if c["client_info"]["android_client_info"]["package_name"] == "ua.entaytion.entinygram")
firebase_expected = {
    "google_app_id": firebase_client["client_info"]["mobilesdk_app_id"],
    "gcm_defaultSenderId": str(firebase_config["project_info"]["project_number"]),
    "google_api_key": next(k["current_key"] for k in firebase_client["api_key"] if k.get("current_key")),
    "project_id": firebase_config["project_info"]["project_id"],
}
if firebase_config["project_info"].get("storage_bucket"):
    firebase_expected["google_storage_bucket"] = firebase_config["project_info"]["storage_bucket"]
badging = subprocess.check_output([aapt, "dump", "badging", str(apk)], text=True, errors="replace")
manifest = subprocess.check_output([aapt, "dump", "xmltree", str(apk), "AndroidManifest.xml"], text=True, errors="replace")
label = re.search(r"^application-label:'([^']*)'", badging, re.M)
if not label or label.group(1) != "Telegram":
    raise SystemExit("APK application label must be Telegram")
labels = re.findall(r"^application-label(?:-[^:]+)?:'([^']*)'", badging, re.M)
if any(value != "Telegram" for value in labels):
    raise SystemExit("A localized application label is not Telegram")
if "org.telegram.messenger.StockIcon" not in manifest:
    raise SystemExit("Blue stock Telegram launcher alias is missing")
if "org.telegram.messenger.OldIcon" in manifest:
    raise SystemExit("Old entinyGram launcher alias is still present")
for alias in ("AquaIcon", "VintageIcon", "DisguiseIcon"):
    if f"org.telegram.messenger.{alias}" in manifest:
        raise SystemExit(f"Removed launcher alias is still present: {alias}")
if "desu.inugram.helpers.update." in manifest:
    raise SystemExit("Removed updater component is still registered")
locales_line = re.search(r"^locales: (.*)$", badging, re.M)
if not locales_line:
    raise SystemExit("Could not inspect APK locales")
locales = re.findall(r"'([^']+)'", locales_line.group(1))
languages = {re.split(r"[-_]", value)[0] for value in locales if re.match(r"[a-z]", value)}
if languages - {"en", "fa"} or "fa" not in languages:
    raise SystemExit(f"Unexpected UI languages: {sorted(languages)}")
with zipfile.ZipFile(apk) as archive:
    names = archive.namelist()
    abis = sorted({name.split("/")[1] for name in names if name.startswith("lib/") and name.endswith(".so")})
    if abis != [abi]:
        raise SystemExit(f"Unexpected native ABIs: {abis}; expected {[abi]}")
    if any(re.search(r"/(?:libinu_native|liblsplant|libshadowhook[^/]*)\.so$", name) for name in names):
        raise SystemExit("Plugin native engine is still packaged")
    dex = [name for name in names if name.endswith(".dex")]
    dex_data = b"".join(archive.read(name) for name in dex)
    if b"force_ltr" not in dex_data:
        raise SystemExit("Force LTR preference is missing from the compiled APK")
    if b"inu_saved_texts" not in dex_data:
        raise SystemExit("Prepared-text storage is missing from the compiled APK")
    if b"inu_composer_tab" not in dex_data:
        raise SystemExit("Composer tab selection is missing from the compiled APK")
    for preference in (
        b"material3_switches", b"material3_fabs", b"m3_sections_style", b"material3_avatars",
        b"material_profile_actions", b"m3_navigation_animation", b"m3_bottom_tabs", b"pill_stack_enabled",
        b"material3_sliders", b"m3_accent_icons", b"md3_player", b"drawer_m3_sections",
        b"plugins_enabled", b"plugins_state", b"plugins_dev_mode", b"market_index_url",
        b"message_plugin_actions", b"chat_plugin_actions",
    ):
        if preference in dex_data:
            raise SystemExit(f"Removed preference is still compiled: {preference.decode()}")
    removed = [
        b"Ldesu/inugram/helpers/plugins/", b"Ldesu/inugram/core/plugins/",
        b"Ldesu/inugram/ui/settings/PluginsActivity;",
        b"Ldesu/inugram/helpers/theme/M3SliderHelper;",
        b"Ldesu/inugram/helpers/player/Md3PlayerHelper;",
        b"Ldesu/inugram/helpers/ai/", b"Ldesu/inugram/helpers/update/",
        b"Ldesu/inugram/helpers/LocalPremiumHelper;",
        b"Ldesu/inugram/helpers/security/ArchiveLockHelper;",
        b"Ldesu/inugram/helpers/security/ParanoiaHelper;",
        b"Ldesu/inugram/ui/settings/ParanoiaActivity;",
        b"Ldesu/inugram/helpers/icons/SolarIconPack;",
        b"Ldesu/inugram/helpers/icons/VkIconPack;",
        b"Ldesu/inugram/helpers/icons/PhosphorIconPack;",
        b"Ldesu/inugram/ui/settings/IosStyleSettingsActivity;",
        b"Ldesu/inugram/ui/settings/AiSettingsActivity;",
        b"Ldesu/inugram/helpers/pillstack/",
        b"Ldesu/inugram/ui/settings/PillStackSettingsActivity;",
        b"Ldesu/inugram/ui/settings/WeatherLocationActivity;",
        b"Ldesu/inugram/ui/settings/RatePairEditActivity;",
        b"Ldesu/inugram/ui/settings/CurrencyPickerActivity;",
        b"Ldesu/inugram/helpers/theme/M3SwitchHelper;",
        b"Ldesu/inugram/helpers/theme/M3FabHelper;",
        b"Ldesu/inugram/helpers/theme/M3SectionsHelper;",
        b"Ldesu/inugram/helpers/theme/M3MainTabsHelper;",
        b"Ldesu/inugram/helpers/theme/Material3NavigationAnimation;",
    ]
    for descriptor in removed:
        if descriptor in dex_data:
            raise SystemExit(f"Removed feature is still compiled: {descriptor.decode()}")
    if b"Ldesu/inugram/helpers/AccountRoute;" in dex_data:
        raise SystemExit("Channel route helper was not obfuscated")
    if any(re.search(r"(?:^|/)icon_[46]_", name) for name in names):
        raise SystemExit("Removed launcher artwork is still packaged")
resources = subprocess.check_output([aapt, "dump", "--values", "resources", str(apk)], text=True, errors="replace")
if re.search(r":drawable/(?:phosphor_|vkui_|pillstack_)", resources):
    raise SystemExit("Removed icon-pack or Pill Stack artwork is still packaged")
if re.search(r":(?:string/Inu(?:Paranoia|DisguiseName)|drawable/inu_shortcut_paranoia)", resources):
    raise SystemExit("Removed Paranoia Mode resources are still packaged")
if re.search(r":string/InuPlugins", resources):
    raise SystemExit("Plugin interface resources are still packaged")
for component in ("org.telegram.messenger.GcmPushListenerService", "com.google.firebase.MESSAGING_EVENT", "FirebaseInitProvider"):
    if component not in manifest:
        raise SystemExit(f"Firebase component missing: {component}")
resource_sections = re.split(r"(?m)^\s*resource ", resources)
for name, expected in firebase_expected.items():
    section = next((s for s in resource_sections if s.strip() and
                    re.search(rf":string/{name}(?=\s|:|$)", s.splitlines()[0])), "")
    if not section or json.dumps(expected, ensure_ascii=False) not in section:
        raise SystemExit(f"Firebase configuration resource missing or mismatched in the compiled APK: {name}")
package = re.search(r"^package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'", badging, re.M)
if not package:
    raise SystemExit("Could not read package metadata")
if package.group(1) != "ua.entaytion.entinygram":
    raise SystemExit("APK package does not match the Firebase Android client")
if int(package.group(2)) <= 2026100714:
    raise SystemExit("Version code must increase beyond the previous VPN963 builds")
certificate = subprocess.check_output([str(Path(aapt).with_name("apksigner")), "verify", "--print-certs", str(apk)], text=True)
match = re.search(r"Signer #1 certificate SHA-256 digest: ([0-9a-fA-F]+)", certificate)
expected_certificate = "a08d7dc323ddf71ef3201944397e0d3cce7d40847263e11f328b68bbe19229ab"
if not match or match.group(1).lower() != expected_certificate:
    raise SystemExit("APK signing certificate changed")
print(json.dumps({
    "file": apk.name,
    "label": label.group(1),
    "package": package.group(1),
    "versionCode": int(package.group(2)),
    "versionName": package.group(3),
    "abi": abi,
    "blueTelegramIcon": True,
    "oldEntinyGramIcon": False,
    "aquaVintageIcons": False,
    "uiLanguages": sorted(languages),
    "removedFeaturesAbsent": True,
    "channelRouteObfuscated": True,
    "forceLtrPreference": True,
    "preparedTextsPreference": True,
    "materialDesignOptionsRemoved": True,
    "pillStackRemoved": True,
    "paranoiaModeRemoved": True,
    "firebaseConfigured": True,
    "pluginsRemoved": True,
    "certificateSha256": expected_certificate,
    "upstreamTag": "v13.0.2-202610160",
    "sha256": hashlib.sha256(apk.read_bytes()).hexdigest(),
}, indent=2))
