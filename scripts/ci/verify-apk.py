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
badging = subprocess.check_output([aapt, "dump", "badging", str(apk)], text=True)
manifest = subprocess.check_output([aapt, "dump", "xmltree", str(apk), "AndroidManifest.xml"], text=True)
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
with zipfile.ZipFile(apk) as archive:
    names = archive.namelist()
    abis = sorted({name.split("/")[1] for name in names if name.startswith("lib/") and name.endswith(".so")})
    if abis != [abi]:
        raise SystemExit(f"Unexpected native ABIs: {abis}; expected {[abi]}")
    dex = [name for name in names if name.endswith(".dex")]
    if not any(b"force_ltr" in archive.read(name) for name in dex):
        raise SystemExit("Force LTR preference is missing from the compiled APK")
resources = subprocess.check_output([aapt, "dump", "--values", "resources", str(apk)], text=True)
for name in ["google_app_id", "gcm_defaultSenderId", "google_api_key", "project_id"]:
    if f"string/{name}" not in resources:
        raise SystemExit(f"Firebase configuration resource missing from the compiled APK: {name}")
package = re.search(r"^package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'", badging, re.M)
if not package:
    raise SystemExit("Could not read package metadata")
print(json.dumps({
    "file": apk.name,
    "label": label.group(1),
    "package": package.group(1),
    "versionCode": int(package.group(2)),
    "versionName": package.group(3),
    "abi": abi,
    "blueTelegramIcon": True,
    "oldEntinyGramIcon": False,
    "forceLtrPreference": True,
    "firebaseConfigured": True,
    "sha256": hashlib.sha256(apk.read_bytes()).hexdigest(),
}, indent=2))
