package person.cyl.flutterstringextractor

/**
 * 负责 Dart 字符串的格式化处理工具类。
 * 主要功能包括去除引号和将 Dart 插值转换为 ARB 参数格式。
 */
object ArbFormatter {

    /**
     * 转换结果数据类。
     * @property suggestedKey 根据内容生成的建议 Key 名称 (CamelCase)。
     * @property arbValue 转换为 ARB 格式后的字符串值。
     */
    data class ArbConversionResult(val suggestedKey: String, val arbValue: String)

    /**
     * 去除 Dart 字符串字面量的引号。
     * 支持处理：
     * - 三引号: """...""" 或 '''...'''
     * - 普通引号: "..." 或 '...'
     * - Raw 字符串: r"..." 或 r'...'
     *
     * @param text Dart 源码中的原始字符串文本。
     * @return 去除引号后的内容。
     */
    fun stripQuotes(text: String): String {
        if (text.length < 2) return text
        
        // 处理三引号 (Triple quotes)
        if (text.startsWith("\"\"\"") && text.endsWith("\"\"\"")) return text.substring(3, text.length - 3)
        if (text.startsWith("'''") && text.endsWith("'''")) return text.substring(3, text.length - 3)

        val first = text.first()
        val last = text.last()
        
        // 处理普通引号 (Simple quotes)
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return text.substring(1, text.length - 1)
        }

        // 处理 Raw 字符串 (Raw strings)
        if (text.startsWith("r\"") && text.endsWith("\"")) return text.substring(2, text.length - 1)
        if (text.startsWith("r'") && text.endsWith("'")) return text.substring(2, text.length - 1)

        return text
    }

    /**
     * 将包含插值的 Dart 字符串转换为 ARB 格式。
     *
     * 示例: "Hello ${user.name}" -> "Hello {userName}"
     *
     * @param dartContent 不包含引号的 Dart 字符串内容。
     * @return [ArbConversionResult] 包含格式化后的值和建议的 Key。
     */
    fun convertDartToArbFormat(dartContent: String): ArbConversionResult {
        // 匹配 ${expression} 或 $variable
        val regex = Regex("""(\$\{([^}]+)})|(\$([a-zA-Z_]\w*))""")
        val usedParamNames = mutableMapOf<String, Int>()

        val arbValue = regex.replace(dartContent) { matchResult ->
            // Group 2 是 ${...} 内部的内容, Group 4 是 $... 的变量名
            val rawBaseName = matchResult.groups[2]?.value ?: matchResult.groups[4]?.value ?: "param"
            val baseArbName = generateArbParamName(rawBaseName)

            // 处理重复参数名，添加计数后缀
            val count = usedParamNames.getOrDefault(baseArbName, 0)
            usedParamNames[baseArbName] = count + 1

            val finalArbName = if (count == 0) baseArbName else "$baseArbName${count + 1}"
            "{$finalArbName}"
        }

        // 生成建议 Key 的逻辑：
        // 如果字符串较短且只包含字母和下划线，则直接将其转为小写作为 Key。
        // 这对于简单的词汇（如 "Cancel", "Title"）非常有用。
        val suggestedKey = if (arbValue.length < 20 && arbValue.matches(Regex("^[a-zA-Z_]+$"))) {
            arbValue.lowercase()
        } else {
            ""
        }

        return ArbConversionResult(suggestedKey, arbValue)
    }

    /**
     * 将 Dart 表达式转换为符合 ARB 规范的参数名 (CamelCase)。
     * 例如: "user.firstName" -> "userFirstName"
     */
    private fun generateArbParamName(dartExpression: String): String {
        // 1. 清洗非法字符，只保留字母、数字、下划线和点号
        var cleaned = dartExpression.replace("[^a-zA-Z0-9_.]".toRegex(), "")
        if (cleaned.isEmpty()) cleaned = "param"
        
        // 2. 确保不以数字开头
        if (cleaned.first().isDigit()) cleaned = "var$cleaned"

        val parts = cleaned.split('.')
        if (parts.isNotEmpty()) {
            return parts.last().replaceFirstChar { it.lowercase() }
        }
        return cleaned
    }
}