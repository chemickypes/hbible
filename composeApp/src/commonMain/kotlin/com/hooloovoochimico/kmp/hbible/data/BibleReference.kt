package com.hooloovoochimico.kmp.hbible.data

import com.hooloovoochimico.kmp.hbible.platform.normalizeForRef

data class BibleRef(val book: Int, val chapter: Int, val verse: Int?, val verseEnd: Int? = null)

/** A Bible reference found inside free text, with the character range it spans. */
data class MatchedRef(val ref: BibleRef, val start: Int, val end: Int, val text: String)

/**
 * Parses Italian Bible references such as "1 GV 1:3", "Gv 3,16", "Sal 23",
 * "cantico dei cantici 2", "1cor13". Matches both full book names and
 * abbreviations (case- and accent-insensitive, separator-agnostic).
 */
object BibleReferenceParser {

  private val ALIASES: Map<Int, List<String>> =
    mapOf(
      1 to listOf("gen"),
      2 to listOf("es", "eso"),
      3 to listOf("lv", "lev"),
      4 to listOf("nm", "num"),
      5 to listOf("dt", "deu", "deut"),
      6 to listOf("gs", "gios"),
      7 to listOf("gdc", "giud"),
      8 to listOf("rt", "ru"),
      9 to listOf("1sam", "1sa", "1s"),
      10 to listOf("2sam", "2sa", "2s"),
      11 to listOf("1re", "1r", "re"),
      12 to listOf("2re", "2r"),
      13 to listOf("1cr", "1cro"),
      14 to listOf("2cr", "2cro"),
      15 to listOf("esd", "esr"),
      16 to listOf("ne"),
      17 to listOf("est", "ets"),
      18 to listOf("gb", "giob"),
      19 to listOf("sal", "salm", "salmo", "ps"),
      20 to listOf("pr", "prov", "pro"),
      21 to listOf("qo", "ecl", "ecc"),
      22 to listOf("ct", "cant", "cantico"),
      23 to listOf("is", "isa"),
      24 to listOf("ger", "gere"),
      25 to listOf("lam", "lm"),
      26 to listOf("ez", "eze"),
      27 to listOf("dn", "dan"),
      28 to listOf("os", "osa"),
      29 to listOf("gl", "gioe"),
      30 to listOf("am", "amo"),
      31 to listOf("ob", "oba"),
      32 to listOf("gio", "gion", "jon"),
      33 to listOf("mi", "mic"),
      34 to listOf("na"),
      35 to listOf("ab", "aba"),
      36 to listOf("sof", "so"),
      37 to listOf("ag", "agg"),
      38 to listOf("zc", "zac"),
      39 to listOf("ml", "mal"),
      40 to listOf("mt", "mat"),
      41 to listOf("mc", "mar"),
      42 to listOf("lc", "luc"),
      43 to listOf("gv", "giov"),
      44 to listOf("at", "att"),
      45 to listOf("rm", "rom"),
      46 to listOf("1cor", "1co", "1c"),
      47 to listOf("2cor", "2co", "2c"),
      48 to listOf("gal", "ga"),
      49 to listOf("ef", "efe"),
      50 to listOf("fil"),
      51 to listOf("col"),
      52 to listOf("1ts", "1te", "1tes"),
      53 to listOf("2ts", "2te", "2tes"),
      54 to listOf("1tm", "1ti", "1tim"),
      55 to listOf("2tm", "2ti", "2tim"),
      56 to listOf("tt", "tit"),
      57 to listOf("flm", "fm"),
      58 to listOf("eb", "ebr"),
      59 to listOf("gc", "giac"),
      60 to listOf("1pt", "1p", "1pi"),
      61 to listOf("2pt", "2p", "2pi"),
      62 to listOf("1gv", "1g"),
      63 to listOf("2gv", "2g"),
      64 to listOf("3gv", "3g"),
      65 to listOf("gd"),
      66 to listOf("ap", "apo", "apoc"),
    )

  fun parse(input: String, books: List<BookRef>): BibleRef? {
    val tokens =
      normalize(input).trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return null

    var prefix: Int? = null
    var chapter: Int? = null
    var verse: Int? = null
    var verseEnd: Int? = null
    val letters = StringBuilder()

    for (raw in tokens) {
      var tok = raw.trim(':')
      if (tok.isEmpty()) continue

      val leading = Regex("^(\\d{1,2})(?=[a-z])(.*)$").find(tok)
      if (leading != null && prefix == null && letters.isEmpty() && chapter == null) {
        prefix = leading.groupValues[1].toIntOrNull()
        tok = leading.groupValues[2]
      }

      // "23:4-20" / "re23:4-20": intervallo di versetti; "23:4-24:3" anche
      // tra capitoli diversi (si punta al versetto iniziale).
      val verseRange =
        Regex("^([a-z]*)(\\d{1,3}):(\\d{1,3})-(\\d{1,3})(?::(\\d{1,3}))?$").matchEntire(tok)
      val letterRef = Regex("^([a-z]+):?(\\d{1,3})(?::(\\d{1,3}))?$").matchEntire(tok)
      when {
        verseRange != null -> {
          letters.append(verseRange.groupValues[1])
          if (chapter == null) chapter = verseRange.groupValues[2].toInt()
          if (verse == null) verse = verseRange.groupValues[3].toInt()
          if (verseEnd == null && verseRange.groupValues[5].isEmpty()) {
            verseEnd = verseRange.groupValues[4].toInt()
          }
        }
        // "23-24": intervallo di capitoli, si punta al capitolo iniziale.
        tok.matches(Regex("\\d{1,3}-\\d{1,3}")) && chapter == null -> {
          chapter = tok.split('-')[0].toInt()
        }
        letterRef != null -> {
          letters.append(letterRef.groupValues[1])
          if (chapter == null) chapter = letterRef.groupValues[2].toInt()
          if (letterRef.groupValues[3].isNotEmpty()) verse = letterRef.groupValues[3].toInt()
        }
        tok.matches(Regex("\\d{1,3}:\\d{1,3}")) -> {
          val parts = tok.split(':')
          chapter = parts[0].toInt()
          verse = parts[1].toInt()
        }
        tok.matches(Regex("\\d{1,3}")) -> {
          val n = tok.toInt()
          when {
            prefix == null && letters.isEmpty() && chapter == null -> prefix = n
            chapter == null -> chapter = n
            verse == null -> verse = n
          }
        }
        else -> letters.append(tok.filter { it.isLetter() })
      }
    }

    val key = (prefix?.toString() ?: "") + letters.toString()
    if (key.isEmpty()) return null

    val nameKeys =
      books.associate { it.n to normalize(it.name).replace(" ", "") }
    fun matches(n: Int, predicate: (String) -> Boolean): Boolean =
      predicate(nameKeys[n].orEmpty()) || ALIASES[n].orEmpty().any(predicate)

    val exact = books.filter { b -> matches(b.n) { it == key } }
    val resolved =
      when {
        exact.size == 1 -> exact[0].n
        exact.isEmpty() -> {
          // Partial book names ("1gio" → 1 Giovanni) only for keys with letters:
          // a bare number ("3") must never fuzzy-match "3Giovanni".
          val prefixed =
            if (key.any { it.isLetter() }) {
              books.filter { b -> matches(b.n) { it.startsWith(key) } }.map { it.n }.distinct()
            } else {
              emptyList()
            }
          if (prefixed.size == 1) prefixed[0] else return null
        }
        else -> return null
      }

    val book = books.first { it.n == resolved }
    val ch = chapter ?: 1
    if (ch < 1 || ch > book.chapters) return null
    if (verse != null && verse < 1) return null
    // Un intervallo è valido solo con un versetto iniziale e una fine >= inizio.
    val end = verseEnd
    if (end != null && (verse == null || end < verse)) return BibleRef(resolved, ch, verse)
    return BibleRef(resolved, ch, verse, verseEnd)
  }

  // La parte Unicode (NFD + strip combining marks + lowercase) è nel seam
  // platform.normalizeForRef; qui restano solo separatori e caratteri ammessi.
  private fun normalize(s: String): String =
    normalizeForRef(s)
      .replace(',', ':')
      .replace(';', ':')
      .replace('.', ':')
      .replace(Regex("\\s+"), " ")
      .filter { it.isLetterOrDigit() || it == ' ' || it == ':' || it == '-' }

  /** Punctuation stripped from the edges of a candidate reference in free text. */
  private val EDGE_PUNCT = charArrayOf('.', ',', ';', ':', '!', '?', '"', '\'', '(', ')', '[', ']', '«', '»', '“', '”', '’', '…')

  /** Longest candidate reference: up to 4 tokens ("cantico dei cantici 2"). */
  private const val MAX_WINDOW = 4

  /**
   * Finds Bible references inside free text (used to turn mentions in notes into
   * chips). At each position the longest matching window wins; a candidate must
   * contain at least one digit (a chapter) to avoid matching bare book names
   * that collide with common words ("at", "am", "na").
   */
  fun findReferences(text: String, books: List<BookRef>): List<MatchedRef> {
    val tokens = Regex("\\S+").findAll(text).toList()
    val matches = mutableListOf<MatchedRef>()
    var i = 0
    while (i < tokens.size) {
      var found: MatchedRef? = null
      var matchedLen = 0
      for (len in minOf(MAX_WINDOW, tokens.size - i) downTo 1) {
        val start = tokens[i].range.first
        val end = tokens[i + len - 1].range.last + 1
        val raw = text.substring(start, end)
        val trimmed = raw.trim(*EDGE_PUNCT).trim()
        // A candidate needs a book name (letters) and a chapter (digits):
        // bare numbers like "3." must never become chips.
        if (trimmed.isEmpty() || trimmed.none { it.isDigit() } || trimmed.none { it.isLetter() }) continue
        val lead = raw.indexOf(trimmed[0])
        val ref = parse(trimmed, books) ?: continue
        val label = trimmed.replace(Regex("\\s+"), " ")
        found = MatchedRef(ref, start + lead, start + lead + trimmed.length, label)
        matchedLen = len
        break
      }
      if (found != null) {
        matches += found
        i += matchedLen
      } else {
        i += 1
      }
    }
    return matches
  }
}
