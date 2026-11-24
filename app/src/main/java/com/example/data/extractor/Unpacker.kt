package com.example.data.extractor

/**
 * Pure Kotlin Dean Edwards JS Unpacker (`eval(function(p,a,c,k,e,d){...})`).
 * Used by MixDropExtractor (and compatible packed embed scripts) without WebView or dynamic code loading.
 */
object Unpacker {

    private val PACKED_REGEX = Regex(
        """eval\(\s*function\s*\(\s*p\s*,\s*a\s*,\s*c\s*,\s*k\s*,\s*e\s*,\s*[dr]\s*\)\s*\{.*?\}\s*\(\s*(['"])(.*?)\1\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*(['"])(.*?)\5\.split\(\s*(['"])\|\7\s*\)""",
        setOf(RegexOption.DOT_MATCHES_ALL)
    )

    private val TOKEN_REGEX = Regex("""\b\w+\b""")

    fun unpack(script: String): String {
        if (!script.contains("eval(")) return script
        val matches = PACKED_REGEX.findAll(script).toList()
        if (matches.isEmpty()) return script

        val output = StringBuilder()
        for (match in matches) {
            val payloadQuote = match.groupValues[1]
            val rawPayload = unescapeJsString(match.groupValues[2], payloadQuote)
            val radix = match.groupValues[3].toIntOrNull() ?: 36
            val count = match.groupValues[4].toIntOrNull() ?: 0
            val symtabRaw = match.groupValues[6]
            val symtab = symtabRaw.split('|')

            val unpacked = unpackPayload(rawPayload, radix, count, symtab)
            output.append(unpacked).append('\n')
        }
        return output.toString().trim()
    }

    private fun unpackPayload(
        payload: String,
        radix: Int,
        count: Int,
        symtab: List<String>
    ): String {
        if (symtab.isEmpty()) return payload
        return TOKEN_REGEX.replace(payload) { matchResult ->
            val word = matchResult.value
            val index = unbase(word, radix)
            if (index != null && index in 0 until count && index < symtab.size) {
                val replacement = symtab[index]
                if (replacement.isNotEmpty()) replacement else word
            } else {
                word
            }
        }
    }

    private fun unbase(value: String, radix: Int): Int? {
        if (value.isEmpty() || radix < 2 || radix > 62) return null
        if (radix <= 36) {
            return value.toIntOrNull(radix)
        }
        val alphabet = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        var result = 0
        for (ch in value) {
            val digit = alphabet.indexOf(ch)
            if (digit < 0 || digit >= radix) return null
            result = result * radix + digit
        }
        return result
    }

    private fun unescapeJsString(input: String, quote: String): String {
        return input
            .replace("\\$quote", quote)
            .replace("\\\\", "\\")
            .replace("\\/", "/")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
    }
}
