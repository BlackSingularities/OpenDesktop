package com.opendesktop.launcher.domain

import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.abs
import kotlin.math.pow

/**
 * Natychmiastowe odpowiedzi w wyszukiwarce: kalkulator i przeliczanie jednostek.
 * Czyste funkcje bez Androida – testowane w [QuickAnswersTest].
 */
object QuickAnswers {

    data class Answer(val result: String, val expression: String)

    fun answer(query: String): Answer? = convert(query) ?: calculate(query)

    // ------------------------------------------------------------------ kalkulator

    /** Wynik wyrażenia albo null, gdy to nie jest działanie (samą liczbę też pomijamy). */
    fun calculate(query: String): Answer? {
        val q = query.trim().removeSuffix("=").trim()
        if (q.isEmpty() || q.none { it in OPERATORS } || q.none { it.isDigit() }) return null
        if (q.trimStart('-', '+').all { it.isDigit() || it == '.' || it == ',' }) return null
        val value = runCatching { Parser(q).parse() }.getOrNull() ?: return null
        if (value.isNaN() || value.isInfinite()) return null
        return Answer(format(value), q)
    }

    private const val OPERATORS = "+-*/×÷:^%()"

    private class Parser(text: String) {
        private val s = text.replace(" ", "")
        private var i = 0

        fun parse(): Double {
            val v = sum()
            require(i == s.length)
            return v
        }

        private fun sum(): Double {
            var v = product()
            while (i < s.length) {
                v = when (s[i]) {
                    '+' -> { i++; v + product() }
                    '-' -> { i++; v - product() }
                    else -> return v
                }
            }
            return v
        }

        private fun product(): Double {
            var v = power()
            while (i < s.length) {
                v = when (s[i]) {
                    '*', '×' -> { i++; v * power() }
                    '/', '÷', ':' -> { i++; val d = power(); require(d != 0.0); v / d }
                    else -> return v
                }
            }
            return v
        }

        private fun power(): Double {
            val base = unary()
            if (i < s.length && s[i] == '^') {
                i++
                return base.pow(power())
            }
            return base
        }

        private fun unary(): Double = when {
            i < s.length && s[i] == '-' -> { i++; -unary() }
            i < s.length && s[i] == '+' -> { i++; unary() }
            else -> postfix()
        }

        private fun postfix(): Double {
            var v = atom()
            while (i < s.length && s[i] == '%') { i++; v /= 100 }
            return v
        }

        private fun atom(): Double {
            require(i < s.length)
            if (s[i] == '(') {
                i++
                val v = sum()
                require(i < s.length && s[i] == ')')
                i++
                return v
            }
            val start = i
            while (i < s.length && (s[i].isDigit() || s[i] == '.' || s[i] == ',')) i++
            require(i > start)
            return s.substring(start, i).replace(',', '.').toDouble()
        }
    }

    // ------------------------------------------------------------------ jednostki

    private enum class Dim { Length, Mass, Volume, Speed, Data, Time, Area, Temperature }

    private data class Unit(val dim: Dim, val factor: Double, val symbol: String)

    private val units: Map<String, Unit> = buildMap {
        fun add(dim: Dim, factor: Double, symbol: String, vararg names: String) {
            val u = Unit(dim, factor, symbol)
            put(symbol.lowercase(), u)
            names.forEach { put(it.lowercase(), u) }
        }
        add(Dim.Length, 0.001, "mm")
        add(Dim.Length, 0.01, "cm")
        add(Dim.Length, 1.0, "m", "metr", "metry", "metrów")
        add(Dim.Length, 1000.0, "km")
        add(Dim.Length, 0.0254, "in", "cal", "cale", "inch", "\"")
        add(Dim.Length, 0.3048, "ft", "stopa", "stopy", "stóp", "feet")
        add(Dim.Length, 0.9144, "yd", "jard", "jardy")
        add(Dim.Length, 1609.344, "mi", "mila", "mile", "mil", "mile")
        add(Dim.Length, 1852.0, "nmi", "mila morska")
        add(Dim.Mass, 0.001, "mg")
        add(Dim.Mass, 1.0, "g", "gram", "gramy")
        add(Dim.Mass, 10.0, "dag", "dkg")
        add(Dim.Mass, 1000.0, "kg")
        add(Dim.Mass, 1_000_000.0, "t", "tona", "tony")
        add(Dim.Mass, 28.349523125, "oz", "uncja", "uncje")
        add(Dim.Mass, 453.59237, "lb", "lbs", "funt", "funty")
        add(Dim.Volume, 0.001, "ml")
        add(Dim.Volume, 0.1, "dl")
        add(Dim.Volume, 1.0, "l", "litr", "litry")
        add(Dim.Volume, 1000.0, "m3", "m³")
        add(Dim.Volume, 3.785411784, "gal", "galon", "galony")
        add(Dim.Volume, 0.0295735295625, "floz")
        add(Dim.Volume, 0.2365882365, "cup", "szklanka")
        add(Dim.Speed, 1 / 3.6, "km/h", "kmh", "kph")
        add(Dim.Speed, 1.0, "m/s")
        add(Dim.Speed, 0.44704, "mph")
        add(Dim.Speed, 0.514444, "kn", "węzeł", "węzły")
        add(Dim.Data, 1.0, "B", "bajt", "bajty")
        add(Dim.Data, 1024.0, "KB", "kib")
        add(Dim.Data, 1024.0.pow(2), "MB", "mib")
        add(Dim.Data, 1024.0.pow(3), "GB", "gib")
        add(Dim.Data, 1024.0.pow(4), "TB", "tib")
        add(Dim.Time, 0.001, "ms")
        add(Dim.Time, 1.0, "s", "sek", "sekundy")
        add(Dim.Time, 60.0, "min", "minuty", "minut")
        add(Dim.Time, 3600.0, "h", "godz", "godziny", "godzin")
        add(Dim.Time, 86400.0, "d", "dzień", "dni", "doba")
        add(Dim.Time, 604800.0, "tydz", "tydzień", "tygodnie", "tygodni")
        add(Dim.Area, 0.0001, "cm2", "cm²")
        add(Dim.Area, 1.0, "m2", "m²")
        add(Dim.Area, 100.0, "a", "ar")
        add(Dim.Area, 10_000.0, "ha", "hektar", "hektary")
        add(Dim.Area, 1_000_000.0, "km2", "km²")
        add(Dim.Area, 4046.8564224, "ac", "akr", "akry")
        add(Dim.Temperature, 0.0, "°C", "c", "celsjusz", "celsjusza")
        add(Dim.Temperature, 1.0, "°F", "f", "fahrenheit", "fahrenheita")
        add(Dim.Temperature, 2.0, "K", "kelwin", "kelwina")
    }

    private val conversion = Regex(
        """^\s*(-?\d+(?:[.,]\d+)?)\s*(.+?)\s+(?:to|in|na|w|do|->|=)\s+(.+?)\s*$""",
        RegexOption.IGNORE_CASE,
    )

    /** „10 km na mi”, „72 f to c”, „1,5 l w ml”. */
    fun convert(query: String): Answer? {
        val m = conversion.matchEntire(query) ?: return null
        val amount = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        val from = unit(m.groupValues[2]) ?: return null
        val to = unit(m.groupValues[3]) ?: return null
        if (from.dim != to.dim) return null
        val value = if (from.dim == Dim.Temperature) {
            fromKelvin(toKelvin(amount, from.factor), to.factor)
        } else amount * from.factor / to.factor
        return Answer("${format(value)} ${to.symbol}", "${format(amount)} ${from.symbol}")
    }

    private fun unit(text: String): Unit? {
        val t = text.trim().removePrefix("°").ifEmpty { text.trim() }
        return units[text.trim().lowercase()] ?: units[t.lowercase()]
    }

    private fun toKelvin(v: Double, kind: Double) = when (kind) {
        0.0 -> v + 273.15
        1.0 -> (v - 32) * 5 / 9 + 273.15
        else -> v
    }

    private fun fromKelvin(v: Double, kind: Double) = when (kind) {
        0.0 -> v - 273.15
        1.0 -> (v - 273.15) * 9 / 5 + 32
        else -> v
    }

    /** Do 10 cyfr znaczących, bez zbędnych zer, z przecinkiem dziesiętnym. */
    fun format(v: Double): String {
        if (v == 0.0 || abs(v) < 1e-12) return "0"
        val bd = BigDecimal(v).round(MathContext(10)).stripTrailingZeros()
        val text = if (abs(v) >= 1e15 || abs(v) < 1e-6) bd.toString() else bd.toPlainString()
        return text.replace('.', ',')
    }
}
