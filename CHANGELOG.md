# Changelog

## [1.1.0]

### Added
- **New Feature:** Added "Extract All Strings" action to batch extract all string literals from the current file.
- **Interactive Dialog:**
  - Edit keys directly in a table view.
  - Batch update keys for identical string values.
  - Support for multi-cell copy and paste.
  - Double-click original text to navigate to source code.
  - Exclude strings by deleting rows or clearing keys.
- **Configuration:** Added `lookup_arb_file` option in `pubspec.yaml` to prioritize a specific ARB file for key lookup.

### Fixed & Improved
- **Performance:** Optimized ARB file reading using streams.
- **Improvements:**
  - Prevent overwriting existing keys during batch extraction.
  - Improved UX for "Extract String" intention dialog.
  - Fixed file cache conflict issues by using Document API.
  - Support Undo (Ctrl+Z) for file modifications.

## [1.0.0]

### Added
- Initial release.
- Provides an Action and an Intention to extract strings to ARB files.
- Supports string interpolation and batch writing to all ARB files.