# Search settings (v7)

Added a Smart Launcher inspired Search Settings screen opened from the three-dot button beside the search field.

Implemented and wired:
- Persistent search settings.
- Search provider display (Baidu).
- Startup app count (0-12): controls apps shown when the search box is empty.
- Baidu web-search action for a non-empty query.
- Play Store search toggle and action.
- System Settings result toggle and action.
- Contacts / shortcuts / media / calendar toggles and startup contact count are persisted and represented in the settings model, ready for their platform-specific providers.

Not faked:
- Contacts, media and calendar data are not queried yet because those require runtime permissions and dedicated providers.
- Hidden-app filtering is stored but FlowerLauncher does not yet have a hidden-app database.
