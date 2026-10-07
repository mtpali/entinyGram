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
for alias in ("AquaIcon", "VintageIcon"):
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
    dex = [name for name in names if name.endswith(".dex")]
    dex_data = b"".join(archive.read(name) for name in dex)
    if b"force_ltr" not in dex_data:
        raise SystemExit("Force LTR preference is missing from the compiled APK")
    removed = [
        b"Ldesu/inugram/helpers/ai/", b"Ldesu/inugram/helpers/update/",
        b"Ldesu/inugram/helpers/LocalPremiumHelper;",
        b"Ldesu/inugram/helpers/security/ArchiveLockHelper;",
        b"Ldesu/inugram/helpers/icons/SolarIconPack;",
        b"Ldesu/inugram/helpers/icons/VkIconPack;",
        b"Ldesu/inugram/helpers/icons/PhosphorIconPack;",
        b"Ldesu/inugram/ui/settings/IosStyleSettingsActivity;",
        b"Ldesu/inugram/ui/settings/AiSettingsActivity;",
    ]
    for descriptor in removed:
        if descriptor in dex_data:
            raise SystemExit(f"Removed feature is still compiled: {descriptor.decode()}")
    if b"Ldesu/inugram/helpers/AccountRoute;" in dex_data:
        raise SystemExit("Channel route helper was not obfuscated")
    if any(re.search(r"(?:^|/)icon_[46]_", name) for name in names):
        raise SystemExit("Removed launcher artwork is still packaged")
resources = subprocess.check_output([aapt, "dump", "--values", "resources", str(apk)], text=True, errors="replace")
if re.search(r":drawable/(?:phosphor_|vkui_)", resources):
    raise SystemExit("Removed icon-pack artwork is still packaged")
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
    "firebaseConfigured": True,
    "sha256": hashlib.sha256(apk.read_bytes()).hexdigest(),
}, indent=2))
