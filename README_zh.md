# Flutter String Extractor 插件使用指南

这是一个用于 IntelliJ IDEA / Android Studio 的 Flutter 插件，旨在帮助开发者快速将硬编码的字符串提取到 ARB 文件中，并自动替换为本地化代码调用。

# ✨ 核心特性

- **智能提取**：支持提取普通字符串和包含插值的字符串（例如 `"Hello $name"`）。
- **批量提取**：支持一次性提取当前文件中的所有字符串，并提供强大的预览对话框。
- **自动转换**：自动将 Dart 插值语法（`$name`）转换为 ARB 占位符格式（`{name}`）。
- **批量更新**：自动识别并更新指定目录下的所有 `.arb` 文件（例如同时更新 `app_en.arb` 和 `app_zh.arb`），省去手动复制 Key 的麻烦。
- **高度可配置**：支持自定义 ARB 文件目录、生成的本地化类名以及首选查找文件。

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
  
  # [可选] 指定一个首选的 ARB 文件用于查找已存在的 Key
  # 如果设置了此项，插件将优先在该文件中查找 Value 是否已存在，以复用 Key
  # 如果未设置，默认在找到的第一个 ARB 文件中查找
  lookup_arb_file: app_zh.arb
```

# 🚀 如何使用

插件提供了三种便捷的使用方式：

### 方式一：Intention Action (推荐 👍)

这是提取单个字符串最快的方式，符合 IDE 原生习惯。

1. 将光标移动到 Dart 代码中的字符串字面量内部（例如 `"Click me"`）。
2. 按下 `Alt + Enter` (Windows/Linux) 或 `Option + Enter` (macOS)。
3. 在弹出的菜单中选择 **"Extract string to ARB file"**。
4. 在弹出的对话框中输入 Key 名称（插件会自动根据字符串内容提供建议）。
5. 回车确认，插件将自动替换代码并更新 ARB 文件。

### 方式二：Extract All Strings (新功能 ✨)

批量提取当前文件中的所有字符串。

1. 在编辑器中右键单击，选择 **"Extract All Strings to ARB"**。
2. 此时会弹出一个对话框，列出文件中所有发现的字符串。
3. **预览与编辑**：
    - **编辑 Key**：直接在表格中修改 Key。
    - **批量更新**：勾选 "Batch update keys for same values"，修改一个 Key 会自动同步更新所有 Value 相同的条目。
    - **复制/粘贴**：支持多单元格的复制和粘贴（兼容 Excel/表格格式）。
    - **跳转源码**：双击 "Original Text" 列可直接跳转到对应的源代码位置。
    - **排除条目**：选中行并按下 `Backspace` 键清空 Key（这些条目将被跳过），或使用工具栏移除。
4. 点击 **OK**，插件将批量写入 ARB 文件并替换代码。

### 方式三：菜单 Action

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

### 3. 智能 Key 查找

通过配置 `lookup_arb_file`，你可以指定一个主语言文件（例如 `app_zh.arb`）。在提取字符串时，插件会优先检查该文件中是否已存在相同的 Value。如果存在，插件会自动复用已有的 Key，避免产生重复的条目。

### 4. 撤销支持

所有的文件修改（包括 ARB 文件和 Dart 代码）都支持标准的 IDE 撤销操作 (`Ctrl+Z` / `Cmd+Z`)。

# 许可证

本项目基于 MIT 许可证开源 - 详情请参阅 [LICENSE](LICENSE) 文件。
