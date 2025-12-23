# Flutter String Extractor 插件使用指南

这是一个用于 IntelliJ IDEA / Android Studio 的 Flutter 插件，旨在帮助开发者快速将硬编码的字符串提取到 ARB 文件中，并自动替换为本地化代码调用。

# ✨ 核心特性

- **智能提取**：支持提取普通字符串和包含插值的字符串（例如 `"Hello $name"`）。
- **自动转换**：自动将 Dart 插值语法（`$name`）转换为 ARB 占位符格式（`{name}`）。
- **批量更新**：自动识别并更新指定目录下的所有 `.arb` 文件（例如同时更新 `app_en.arb` 和 `app_zh.arb`），省去手动复制 Key 的麻烦。
- **高度可配置**：支持自定义 ARB 文件目录和生成的本地化类名。

# ⚙️ 项目配置 (pubspec.yaml)

为了让插件知道 ARB 文件在哪里以及你使用的是哪个本地化类（如 `S` 或 `AppLocalizations`），请在项目的 `pubspec.yaml` 中添加 `flutter_string_extractor` 配置段。

如果未配置，插件将使用默认值：
- 目录：`lib/l10n`
- 类名：`S`

### 完整配置示例

```yaml
# pubspec.yaml

# ... 其他依赖 ...

# 插件配置
flutter_string_extractor:
  # [可选] ARB 文件所在的目录路径 (相对于项目根目录)
  # 插件会扫描该目录下所有的 .arb 文件并同时写入
  arb_dir: lib/src/l10n

  # [可选] 生成代码时使用的类名
  # 例如设置为 AppLocalizations 后，替换代码将变为: AppLocalizations.of(context).keyName
  localizations_class_name: S
```

# 🚀 如何使用

插件提供了两种便捷的使用方式：

### 方式一：Intention Action (推荐 👍)

这是最快的方式，符合 IDE 原生习惯。

1. 将光标移动到 Dart 代码中的字符串字面量内部（例如 `"Click me"`）。
2. 按下 `Alt + Enter` (Windows/Linux) 或 `Option + Enter` (macOS)。
3. 在弹出的菜单中选择 **"Extract string to ARB file"**。
4. 在弹出的对话框中输入 Key 名称（插件会自动根据字符串内容提供建议）。
5. 回车确认，插件将自动替换代码并更新 ARB 文件。

### 方式二：菜单 Action

1. 选中想要提取的字符串（或者直接将光标放在字符串上）。
2. 在编辑器中右键单击，或者在顶部菜单栏查找插件提供的 Action（通常在 **Refactor** 菜单下，或者使用快捷键 `Alt + S`）。
3. 输入 Key 名称并确认。

# 💡 功能细节

### 1. 插值自动处理

插件能智能处理 Dart 字符串插值。

- **源代码**: `"Total cost: ${price * count}"`
- **提取后的 ARB Value**: `"Total cost: {priceCount}"`
- **生成的 Dart 代码**: `S.of(context).total_cost` (假设 Key 为 `total_cost`)

*(注意：生成的 ARB 占位符名称是基于变量名自动生成的驼峰命名)*

### 2. 多语言同步

如果你的 `arb_dir` 目录下存在多个文件：
- `app_en.arb`
- `app_zh.arb`

当你提取一个字符串时，插件会同时向这两个文件写入相同的 Key 和 Value（作为待翻译的基础）。这确保了你的翻译文件 Key 值始终保持同步，避免遗漏。
