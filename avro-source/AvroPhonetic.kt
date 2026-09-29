/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * This file is a Kotlin port of PhoneticParser.java from JAvroPhonetic
 * (https://github.com/omicronlab/JAvroPhonetic, branch "xml").
 *
 * The Original Code is JAvroPhonetic.
 * The Initial Developer of the Original Code is Rifat Nabi <to.rifat@gmail.com>.
 * Copyright (C) OmicronLab (http://www.omicronlab.com). All Rights Reserved.
 *
 * The rules it runs are OmicronLab's phonetic.xml, converted to
 * assets/avro_rules.json by scripts/build_avro_rules.py. The full text of the
 * licence is in assets/licenses/MPL-1.1.txt.
 */
package com.nextvoice.app.ime

import org.json.JSONObject

/**
 * Types Bengali with a Latin keyboard: `ami` becomes আমি, `bangla` becomes বাংলা.
 *
 * This is the real Avro Phonetic: the same rules file and the same algorithm as
 * OmicronLab's own implementations, so it agrees with what Bengali users already
 * know from Avro. The rules are data ([Rules], loaded from the app's assets);
 * this object only applies them. The conversion is a pure function of the letters
 * typed so far, applied to the whole word on every keystroke, so backspace is
 * simply "remove the last Latin letter and convert again".
 *
 * Until [install] has been given the rules, text is returned unchanged.
 */
object AvroPhonetic {

    private class Match(val prefix: Boolean, val scope: String, val negative: Boolean, val value: String)

    private class Rule(val matches: List<Match>, val replace: String)

    private class Pattern(val replace: String, val rules: List<Rule>)

    /** The rules file, read into memory. */
    class Rules private constructor(
        internal val vowel: String,
        internal val consonant: String,
        internal val caseSensitive: String,
        internal val patterns: Map<String, Any>,
        internal val maxLength: Int,
    ) {
        companion object {
            fun parse(json: String): Rules {
                val root = JSONObject(json)
                val patterns = HashMap<String, Any>()
                val list = root.getJSONArray("patterns")
                var max = 0
                for (i in 0 until list.length()) {
                    val p = list.getJSONObject(i)
                    val find = p.getString("f")
                    if (find.isEmpty() || patterns.containsKey(find)) continue
                    val rules = ArrayList<Rule>()
                    val rs = p.getJSONArray("u")
                    for (j in 0 until rs.length()) {
                        val r = rs.getJSONObject(j)
                        val ms = r.getJSONArray("m")
                        val matches = ArrayList<Match>()
                        for (k in 0 until ms.length()) {
                            val m = ms.getJSONArray(k)
                            matches += Match(m.getString(0) == "prefix", m.getString(1), m.getInt(2) == 1, m.getString(3))
                        }
                        rules += Rule(matches, r.getString("r"))
                    }
                    patterns[find] = Pattern(p.getString("r"), rules)
                    if (find.length > max) max = find.length
                }
                return Rules(
                    root.getString("vowel"), root.getString("consonant"), root.getString("caseSensitive"),
                    patterns, max,
                )
            }
        }
    }

    @Volatile
    private var rules: Rules? = null

    /** Gives the engine its rules (once, when the keyboard starts). */
    fun install(rules: Rules) {
        this.rules = rules
    }

    val isInstalled: Boolean get() = rules != null

    private const val BENGALI_DIGITS = "০১২৩৪৫৬৭৮৯"

    fun toBengaliDigits(text: String): String {
        if (text.none { it in '0'..'9' }) return text
        val out = StringBuilder(text.length)
        for (c in text) out.append(if (c in '0'..'9') BENGALI_DIGITS[c - '0'] else c)
        return out.toString()
    }

    private fun toAsciiDigits(text: String): String {
        if (text.none { it in BENGALI_DIGITS }) return text
        val out = StringBuilder(text.length)
        for (c in text) {
            val i = BENGALI_DIGITS.indexOf(c)
            out.append(if (i >= 0) ('0' + i) else c)
        }
        return out.toString()
    }

    /** Letters and the engine's own symbols (backtick for special forms, ^ for chandrabindu, : for visarga). */
    fun isInputChar(c: Char): Boolean = c.isLetter() && c.code < 128 || c == '`' || c == '^' || c == ':'

    /**
     * @param bengaliDigits write 0-9 as ০-৯. Off, digits stay as typed.
     */
    fun convert(roman: String, bengaliDigits: Boolean = false): String {
        val r = rules ?: return roman
        if (roman.isEmpty()) return ""
        val text = parse(r, roman)
        return if (bengaliDigits) text else toAsciiDigits(text)
    }

    // ------------------------------------------------------------- the algorithm (from PhoneticParser.java)

    private fun isVowel(r: Rules, c: Char) = r.vowel.indexOf(c.lowercaseChar()) >= 0

    private fun isConsonant(r: Rules, c: Char) = r.consonant.indexOf(c.lowercaseChar()) >= 0

    private fun isPunctuation(r: Rules, c: Char) = !(isVowel(r, c) || isConsonant(r, c))

    private fun isExact(needle: String, haystack: String, start: Int, end: Int, not: Boolean): Boolean =
        (start >= 0 && end < haystack.length && haystack.substring(start, end) == needle) xor not

    private fun parse(r: Rules, input: String): String {
        val fixed = StringBuilder(input.length)
        for (c in input) {
            fixed.append(if (r.caseSensitive.indexOf(c.lowercaseChar()) >= 0) c else c.lowercaseChar())
        }
        val text = fixed.toString()

        val output = StringBuilder()
        var cur = 0
        while (cur < text.length) {
            val start = cur
            var matched = false
            var len = r.maxLength
            while (len > 0) {
                val end = start + len
                if (end <= text.length) {
                    val pattern = r.patterns[text.substring(start, end)] as Pattern?
                    if (pattern != null) {
                        val rule = pattern.rules.firstOrNull { applies(r, it, text, start, end) }
                        output.append(rule?.replace ?: pattern.replace)
                        cur = end - 1
                        matched = true
                        break
                    }
                }
                len--
            }
            if (!matched) output.append(text[cur])
            cur++
        }
        return output.toString()
    }

    /** True when every condition of the rule holds around the text being replaced. */
    private fun applies(r: Rules, rule: Rule, text: String, start: Int, end: Int): Boolean {
        for (m in rule.matches) {
            val at = if (m.prefix) start - 1 else end
            val held = when (m.scope) {
                "punctuation" ->
                    (m.prefix && at < 0) || (!m.prefix && at >= text.length) ||
                        (at in text.indices && isPunctuation(r, text[at]))
                "vowel" -> at in text.indices && isVowel(r, text[at])
                "consonant" -> at in text.indices && isConsonant(r, text[at])
                "exact" -> {
                    val s = if (m.prefix) start - m.value.length else end
                    val e = if (m.prefix) start else end + m.value.length
                    // The original folds "not" into the comparison itself.
                    if (isExact(m.value, text, s, e, m.negative)) continue else return false
                }
                else -> true
            }
            if (held == m.negative) return false
        }
        return true
    }
}
