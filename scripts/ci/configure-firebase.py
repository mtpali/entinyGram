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
    root = Path(__file__).resolve().parents[2]
    local_config = root / "src/google-services.json"
    config_path = local_config if local_config.is_file() else root / "src/firebase/google-services.json"
    if not config_path.is_file():
        raise SystemExit("Firebase configuration is required: supply GOOGLE_SERVICES_JSON or a registered Android client file.")
    raw = config_path.read_text()
try:
    config = json.loads(raw)
except (ValueError, TypeError):
    raise SystemExit("Firebase configuration must contain a valid Android client JSON object.") from None
if not isinstance(config, dict):
    raise SystemExit("Firebase configuration must be a JSON object.")
if config.get("type") == "service_account" or "private_key" in config:
    raise SystemExit("Use the Android google-services.json export, not a Firebase service-account key.")
clients = config.get("client", [])
if not isinstance(clients, list) or any(not isinstance(c, dict) or
        not isinstance(c.get("client_info", {}), dict) or
        not isinstance(c.get("client_info", {}).get("android_client_info", {}), dict) for c in clients):
    raise SystemExit("Firebase Android client entries are invalid.")
client = next((c for c in clients
               if c.get("client_info", {}).get("android_client_info", {}).get("package_name") == PACKAGE), None)
if not client:
    raise SystemExit(f"Firebase must contain a registered Android client for {PACKAGE}.")
project = config.get("project_info", {})
app_id = client.get("client_info", {}).get("mobilesdk_app_id", "")
keys = client.get("api_key", [])
if not (isinstance(project, dict) and isinstance(app_id, str) and
        isinstance(keys, list) and all(isinstance(k, dict) for k in keys) and
        project.get("project_number") and project.get("project_id") and
        re.fullmatch(r"1:\d+:android:[0-9a-f]+", app_id) and
        any(k.get("current_key") for k in keys)):
    raise SystemExit("Firebase configuration is incomplete: project, Android app ID and API key are required.")
if app_id.split(":")[1] != str(project["project_number"]):
    raise SystemExit("Firebase app ID and project number do not match.")

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
    target = worktree / module / "google-services.json"
    if target.is_symlink():
        target.unlink()
    target.write_text(raw)
print("Firebase Android client configuration validated; FCM resources will be checked in each APK.")
