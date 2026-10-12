# VYBE App Update & Remote Configuration Control

This directory contains the central versioned control file (`app-update.json`) used by the VYBE Android app to handle updates and remote configuration.

## Update Modes (`update_mode`)

| Mode | Behavior | Use Case |
| :--- | :--- | :--- |
| `none` | No update screen is shown. App functions normally with default settings or cached remote config. | Normal operation, no active update campaign. |
| `silent_config` | App silently applies non-breaking remote configurations (feature flags, announcements, optional API endpoint override) without interrupting playback or requiring an APK download. | Instant server-side configuration changes, banner announcements, feature flag toggles. |
| `mandatory` | Displays a non-dismissible, full-screen update screen on devices where the installed `versionName` is strictly less than `minimum_supported_version`. Back navigation is disabled. Directs user to the official channel: `https://t.me/VyomaOfficial`. | Major native releases, breaking API migrations, mandatory security or engine patches. |

## Schema Fields

- **`schema_version`** (integer): Version of this JSON schema format (currently `1`).
- **`enabled`** (boolean): Master kill-switch. When `false`, remote update checks and mandatory update blocks are bypassed.
- **`update_mode`** (string): One of `"none"`, `"silent_config"`, `"mandatory"`.
- **`latest_release_version`** (string): SemVer formatted version of the latest available public APK release (e.g., `"1.1.0"`).
- **`minimum_supported_version`** (string): SemVer formatted lowest version required for app access when `update_mode` is `"mandatory"`. Devices running a lower version are blocked from the main UI.
- **`config_version`** (integer): Monotonically increasing revision number for silent configuration caches.
- **`release_notes`** (string): User-facing summary of improvements, fixes, and changes in the latest release.
- **`config_url`** (string, optional): Canonical raw HTTPS URL for polling config updates.
- **`remote_config`** (object, optional):
  - **`custom_api_base_url`** (string or null): Optional override for the backend endpoint (must use HTTPS).
  - **`announcement_banner`** (object): In-app banner notice with `id`, `message`, `type` (`info`, `promo`, `warning`), `action_url`, and `enabled`.
  - **`feature_flags`** (map): Key-value flags to dynamically toggle client features.

## Safety & Security Guarantees

1. **No Dynamic Code Loading (DCL):** Remote configs only deliver declarative data (strings, numbers, flags). No executable DEX/JAR, reflection hooks, or downloaded code is ever executed.
2. **Offline & Network Resilience:** Network errors during update checks never lock users out if there is no pre-existing validated mandatory update. The app falls back to safe defaults with an exponential backoff retry.
3. **SemVer Compliance:** Version numbers are parsed as integer components (e.g. `1.10.0` is correctly evaluated as newer than `1.2.0`).
