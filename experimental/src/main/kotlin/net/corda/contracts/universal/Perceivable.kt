package net.corda.contracts.universal

import net.corda.core.contracts.BusinessCalendar
import net.corda.core.contracts.Tenor
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.util.*

/**
 * Created by sofusmortensen on 23/05/16.
 */

interface Perceivable<T>

enum class Comparison {
    LT, LTE, GT, GTE
}

/**
 * Constant perceivable
 */
data class Const<T>(val value: T) : Perceivable<T> {
    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (value == null) {
            return false
        }
        if (other is Const<*>) {
            if (value is BigDecimal && other.value is BigDecimal) {
                return this.value.compareTo(other.value) == 0
            }
            return value.equals(other.value)
        }
        return false
    }

    override fun hashCode(): Int =
            if (value is BigDecimal)
                value.toDouble().hashCode()
            else
                value!!.hashCode()
}

fun<T> const(k: T) = Const(k)

data class Max(val args: Set<Perceivable<BigDecimal>>) : Perceivable<BigDecimal>
fun max(vararg args: Perceivable<BigDecimal>) = Max(args.toSet())

data class Min(val args: Set<Perceivable<BigDecimal>>) : Perceivable<BigDecimal>
fun min(vararg args: Perceivable<BigDecimal>) = Min(args.toSet())

//
class StartDate : Perceivable<Instant> {
    override fun hashCode(): Int {
        return 2
    }

    override fun equals(other: Any?): Boolean {
        return other is StartDate
    }
}

class EndDate : Perceivable<Instant> {
    override fun hashCode(): Int {
        return 3
    }

    override fun equals(other: Any?): Boolean {
        return other is EndDate
    }
}

/**
 * Perceivable based on time
 */
data class TimePerceivable(val cmp: Comparison, val instant: Perceivable<Instant>) : Perceivable<Boolean>

fun parseDate(str: String) = BusinessCalendar.parseDateFromString(str)

        // Instant.parse(str+"T00:00:00Z")!!

fun before(expiry: Perceivable<Instant>) = TimePerceivable(Comparison.LTE, expiry)
fun after(expiry: Perceivable<Instant>) = TimePerceivable(Comparison.GTE, expiry)
fun before(expiry: Instant) = TimePerceivable(Comparison.LTE, const(expiry))
fun after(expiry: Instant) = TimePerceivable(Comparison.GTE, const(expiry))
fun before(expiry: String) = TimePerceivable(Comparison.LTE, const(parseDate(expiry).toInstant()))
fun after(expiry: String) = TimePerceivable(Comparison.GTE, const(parseDate(expiry).toInstant()))

data class PerceivableAnd(val left: Perceivable<Boolean>, val right: Perceivable<Boolean>) : Perceivable<Boolean>
infix fun Perceivable<Boolean>.and(obs: Perceivable<Boolean>) = PerceivableAnd(this, obs)

data class PerceivableOr(val left: Perceivable<Boolean>, val right: Perceivable<Boolean>) : Perceivable<Boolean>
infix fun Perceivable<Boolean>.or(obs: Perceivable<Boolean>) = PerceivableOr(this, obs)

data class CurrencyCross(val foreign: Currency, val domestic: Currency) : Perceivable<BigDecimal>

operator fun Currency.div(currency: Currency) = CurrencyCross(this, currency)

data class PerceivableComparison<T>(val left: Perceivable<T>, val cmp: Comparison, val right: Perceivable<T>) : Perceivable<Boolean>

infix fun Perceivable<BigDecimal>.lt(n: BigDecimal) = PerceivableComparison(this, Comparison.LT, const(n))
infix fun Perceivable<BigDecimal>.gt(n: BigDecimal) = PerceivableComparison(this, Comparison.GT, const(n))
infix fun Perceivable<BigDecimal>.lt(n: Double) = PerceivableComparison(this, Comparison.LT, const( BigDecimal(n) ))
infix fun Perceivable<BigDecimal>.gt(n: Double) = PerceivableComparison(this, Comparison.GT, const( BigDecimal(n) ))

infix fun Perceivable<BigDecimal>.lte(n: BigDecimal) = PerceivableComparison(this, Comparison.LTE, const(n))
infix fun Perceivable<BigDecimal>.gte(n: BigDecimal) = PerceivableComparison(this, Comparison.GTE, const(n))
infix fun Perceivable<BigDecimal>.lte(n: Double) = PerceivableComparison(this, Comparison.LTE, const( BigDecimal(n) ))
infix fun Perceivable<BigDecimal>.gte(n: Double) = PerceivableComparison(this, Comparison.GTE, const( BigDecimal(n) ))

enum class Operation {
    PLUS, MINUS, TIMES, DIV
}

data class UnaryPlus<T>(val arg: Perceivable<T>) : Perceivable<T>

data class PerceivableOperation<T>(val left: Perceivable<T>, val op: Operation, val right: Perceivable<T>) : Perceivable<T>

operator fun Perceivable<BigDecimal>.plus(n: BigDecimal) = PerceivableOperation(this, Operation.PLUS, const(n))
fun<T> Perceivable<T>.plus() = UnaryPlus(this)
operator fun Perceivable<BigDecimal>.minus(n: Perceivable<BigDecimal>) = PerceivableOperation(this, Operation.MINUS, n)
operator fun Perceivable<BigDecimal>.minus(n: BigDecimal) = PerceivableOperation(this, Operation.MINUS, const(n))
operator fun Perceivable<BigDecimal>.plus(n: Double) = PerceivableOperation(this, Operation.PLUS, const(BigDecimal(n)))
operator fun Perceivable<BigDecimal>.minus(n: Double) = PerceivableOperation(this, Operation.MINUS, const(BigDecimal(n)))
operator fun Perceivable<BigDecimal>.times(n: BigDecimal) = PerceivableOperation(this, Operation.TIMES, const(n))
operator fun Perceivable<BigDecimal>.div(n: BigDecimal) = PerceivableOperation(this, Operation.DIV, const(n))
operator fun Perceivable<BigDecimal>.times(n: Double) = PerceivableOperation(this, Operation.TIMES, const(BigDecimal(n)))
operator fun Perceivable<BigDecimal>.div(n: Double) = PerceivableOperation(this, Operation.DIV, const(BigDecimal(n)))

operator fun Perceivable<Int>.plus(n: Int) = PerceivableOperation(this, Operation.PLUS, const(n))
operator fun Perceivable<Int>.minus(n: Int) = PerceivableOperation(this, Operation.MINUS, const(n))

class DummyPerceivable<T> : Perceivable<T>


// todo: holidays
data class Interest(val amount: Perceivable<BigDecimal>, val dayCountConvention: String,
               val interest: Perceivable<BigDecimal>, val start: Perceivable<Instant>, val end: Perceivable<Instant>) : Perceivable<BigDecimal>

fun libor(@Suppress("UNUSED_PARAMETER") amount: BigDecimal, @Suppress("UNUSED_PARAMETER") start: String, @Suppress("UNUSED_PARAMETER") end: String) : Perceivable<BigDecimal> = DummyPerceivable()
fun libor(@Suppress("UNUSED_PARAMETER") amount: BigDecimal, @Suppress("UNUSED_PARAMETER") start: Perceivable<Instant>, @Suppress("UNUSED_PARAMETER") end: Perceivable<Instant>) : Perceivable<BigDecimal> = DummyPerceivable()

fun interest(@Suppress("UNUSED_PARAMETER") amount: BigDecimal, @Suppress("UNUSED_PARAMETER") dayCountConvention: String, @Suppress("UNUSED_PARAMETER") interest: BigDecimal /* todo -  appropriate type */,
             @Suppress("UNUSED_PARAMETER") start: String, @Suppress("UNUSED_PARAMETER") end: String) : Perceivable<BigDecimal> = Interest(Const(amount), dayCountConvention, Const(interest), const(parseDate(start).toInstant()), const(parseDate(end).toInstant()))

fun interest(@Suppress("UNUSED_PARAMETER") amount: BigDecimal, @Suppress("UNUSED_PARAMETER") dayCountConvention: String, @Suppress("UNUSED_PARAMETER") interest: Perceivable<BigDecimal> /* todo -  appropriate type */,
             @Suppress("UNUSED_PARAMETER") start: String, @Suppress("UNUSED_PARAMETER") end: String) : Perceivable<BigDecimal> =
        Interest(Const(amount), dayCountConvention, interest, const(parseDate(start).toInstant()), const(parseDate(end).toInstant()))

fun interest(@Suppress("UNUSED_PARAMETER") amount: BigDecimal, @Suppress("UNUSED_PARAMETER") dayCountConvention: String, @Suppress("UNUSED_PARAMETER") interest: BigDecimal /* todo -  appropriate type */,
             @Suppress("UNUSED_PARAMETER") start: Perceivable<Instant>, @Suppress("UNUSED_PARAMETER") end: Perceivable<Instant>) : Perceivable<BigDecimal> = Interest(const(amount), dayCountConvention, const(interest), start, end )

fun interest(@Suppress("UNUSED_PARAMETER") amount: BigDecimal, @Suppress("UNUSED_PARAMETER") dayCountConvention: String, @Suppress("UNUSED_PARAMETER") interest: Perceivable<BigDecimal> /* todo -  appropriate type */,
             @Suppress("UNUSED_PARAMETER") start: Perceivable<Instant>, @Suppress("UNUSED_PARAMETER") end: Perceivable<Instant>) : Perceivable<BigDecimal> = Interest(const(amount), dayCountConvention, interest, start, end)

data class Fixing(val source: String, val date: Perceivable<Instant>, val tenor: Tenor) : Perceivable<BigDecimal>

fun fix(source: String, date: Perceivable<Instant>, tenor: Tenor): Perceivable<BigDecimal> = Fixing(source, date, tenor)
fun fix(source: String, date: LocalDate, tenor: Tenor): Perceivable<BigDecimal> = Fixing(source, const(date.toInstant()), tenor)