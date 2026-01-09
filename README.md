[中文文档](README_zh.md)

# Flutter String Extractor Plugin Usage Guide

This is a Flutter plugin for IntelliJ IDEA / Android Studio designed to help developers quickly extract hard-coded strings into ARB files and automatically replace them with localization code calls.

# ✨ Core Features

- **Smart Extraction**: Supports extracting both simple strings and strings with interpolation (e.g., `"Hello $name"`).
- **Batch Extraction**: Extract all strings in the current file at once with a powerful review dialog.
- **Automatic Conversion**: Automatically converts Dart interpolation syntax (`$name`) into ARB placeholder format (`{name}`).
- **Batch Update**: Automatically identifies and updates all `.arb` files in the specified directory (e.g., updating both `app_en.arb` and `app_zh.arb` simultaneously), saving you the trouble of manually copying keys.
- **Highly Configurable**: Supports customizing the ARB file directory, the generated localization class name, and the preferred lookup file.

# ⚙️ Project Configuration (pubspec.yaml)

To let the plugin know where your ARB files are located and which localization class you are using (e.g., `S` or `AppLocalizations`), please add a `flutter_string_extractor` configuration section to your project's `pubspec.yaml`.

If not configured, the plugin will use the following default values:
- Directory: `lib/l10n`
- Class Name: `S`

### Complete Configuration Example

```yaml
# pubspec.yaml

# ... other dependencies ...

# Plugin Configuration
flutter_string_extractor:
  # [Optional] The directory path where ARB files are located (relative to the project root)
  # The plugin will scan all .arb files in this directory and write to them simultaneously
  arb_dir: lib/src/l10n

  # [Optional] The class name used when generating code
  # For example, if set to AppLocalizations, the replacement code will be: AppLocalizations.of(context).keyName
  localizations_class_name: S
  
  # [Optional] Specify a preferred ARB file to lookup existing keys.
  # If set, the plugin will only search this file for existing values to reuse keys.
  # If not set, it defaults to searching the first ARB file found.
  lookup_arb_file: app_zh.arb
```

# 🚀 How to Use

The plugin provides three convenient ways to use it:

### Method 1: Intention Action (Recommended 👍)

This is the fastest way for single string extraction.

1. Place your cursor inside a string literal in your Dart code (e.g., `"Click me"`).
2. Press `Alt + Enter` (Windows/Linux) or `Option + Enter` (macOS).
3. Select **"Extract string to ARB file"** from the popup menu.
4. Enter the Key name in the dialog box (the plugin will automatically suggest a name based on the string content).
5. Press Enter to confirm. The plugin will automatically replace the code and update the ARB files.

### Method 2: Extract All Strings (New ✨)

Batch extract all strings in the current file.

1. Right-click in the editor and select **"Extract All Strings to ARB"**.
2. A dialog will appear listing all string literals found in the file.
3. **Review & Edit**:
    - **Edit Keys**: Directly modify the keys in the table.
    - **Batch Update**: Check "Batch update keys for same values" to update keys for identical strings simultaneously.
    - **Copy/Paste**: Supports multi-cell copy and paste (compatible with Excel/Sheets).
    - **Navigate**: Double-click on the "Original Text" to jump to the source code.
    - **Exclude**: Select rows and press `Backspace` to clear the key (these entries will be skipped) or use the toolbar to remove them.
4. Click **OK**. The plugin will batch write to ARB files and replace the code.

### Method 3: Menu Action

1. Select the string you want to extract (or simply place the cursor on the string).
2. Right-click in the editor, or look for the Action provided by the plugin in the top menu bar (usually under the **Refactor** menu, or use the shortcut `Alt + S`).
3. Enter the Key name and confirm.

# 💡 Feature Details

### 1. Automatic Interpolation Handling

The plugin intelligently handles Dart string interpolation.

- **Source Code**: `"Total cost: ${price * count}"`
- **Extracted ARB Value**: `"Total cost: {priceCount}"`
- **Generated Dart Code**: `S.of(context).total_cost` (assuming the Key is `total_cost`)

*(Note: The generated ARB placeholder names are camelCase names automatically generated based on the variable names)*

### 2. Multi-language Synchronization

If multiple files exist in your `arb_dir` directory:
- `app_en.arb`
- `app_zh.arb`

When you extract a string, the plugin will write the same Key and Value to both files simultaneously (as a base for translation). This ensures that keys in your translation files are always synchronized, avoiding omissions.

### 3. Smart Key Lookup

With the `lookup_arb_file` configuration, you can specify a primary language file (e.g., `app_zh.arb`). When extracting strings, the plugin will prioritize checking this file. If the string value already exists, it will automatically reuse the existing Key, preventing duplicate entries.

### 4. Undo Support

All file modifications (ARB files and Dart code) support the standard IDE Undo operation (`Ctrl+Z` / `Cmd+Z`).

# License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
