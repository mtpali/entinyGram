"""Validate the registered Firebase client before producing a messaging APK."""

import json
import os
import re
import sys
from pathlib import Path

PACKAGE = "ua.entaytion.entinygram"
worktree = Path(sys.argv[1])
validate_only = "--validate-only" in sys.argv[2:]
raw = os.environ.get("GOOGLE_SERVICES_JSON", "")
if not raw:
    raise SystemExit("GOOGLE_SERVICES_JSON is required: this workflow does not publish APKs without Firebase configuration.")
try:
    config = json.loads(raw)
except (ValueError, TypeError):
    raise SystemExit("GOOGLE_SERVICES_JSON must contain a valid Firebase Android configuration export.") from None
if config.get("type") == "service_account" or "private_key" in config:
    raise SystemExit("Use the Android google-services.json export, not a Firebase service-account key.")
client = next((c for c in config.get("client", [])
               if c.get("client_info", {}).get("android_client_info", {}).get("package_name") == PACKAGE), None)
if not client:
    raise SystemExit(f"Firebase must contain a registered Android client for {PACKAGE}.")
project = config.get("project_info", {})
app_id = client.get("client_info", {}).get("mobilesdk_app_id", "")
keys = client.get("api_key", [])
if not (project.get("project_number") and project.get("project_id") and
        re.fullmatch(r"1:\d+:android:[0-9a-f]+", app_id) and
        any(k.get("current_key") for k in keys)):
    raise SystemExit("Firebase configuration is incomplete: project, Android app ID and API key are required.")
if app_id.split(":")[1] != str(project["project_number"]):
    raise SystemExit("Firebase app ID and project number do not match.")

# Optional personal Telegram credentials must belong to the API app with this FCM project registered.
api_id = os.environ.get("TELEGRAM_APP_ID", "")
api_hash = os.environ.get("TELEGRAM_APP_HASH", "")
if bool(api_id) != bool(api_hash):
    raise SystemExit("Set TELEGRAM_APP_ID and TELEGRAM_APP_HASH together.")
if api_id:
    if not api_id.isdecimal() or not 0 < int(api_id) < 2**31 or not re.fullmatch(r"[0-9a-fA-F]{32}", api_hash):
        raise SystemExit("Telegram API credentials have an invalid format.")
if validate_only:
    print("Required Firebase client configuration is available and valid.")
    sys.exit(0)

if api_id:
    build_vars = worktree / "TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java"
    source = build_vars.read_text()
    source, count_id = re.subn(r"public static int APP_ID = \d+;", f"public static int APP_ID = {int(api_id)};", source)
    source, count_hash = re.subn(r'public static String APP_HASH = "[^"]*";', f'public static String APP_HASH = "{api_hash}";', source)
    if (count_id, count_hash) != (1, 1):
        raise SystemExit("Could not locate the Telegram API configuration fields.")
    build_vars.write_text(source)

for module in ["TMessagesProj", "TMessagesProj_App"]:
    (worktree / module / "google-services.json").write_text(raw)
print("Firebase Android client configuration validated; FCM resources will be checked in each APK.")
