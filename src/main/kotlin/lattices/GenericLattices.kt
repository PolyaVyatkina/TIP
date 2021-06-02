package lattices

import utils.MapWithDefault
import utils.withDefaultValue

/**
 * A (semi-)lattice.
 * @param T is the type of the elements of this lattice.
 */
interface Lattice<T> {

    /**
     * The characteristic function of the set of lattice elements.
     * Default implementation: returns true for all elements of the right type.
     */
    fun ch(e: T) = true

    /**
     * The bottom element of this lattice.
     */
    val bottom: T

    /**
     * The top element of this lattice.
     * Default: not implemented.
     */
    val top: T?
        get() = TODO()


    /**
     * The least upper bound of `x` and `y`.
     */
    fun lub(x: T, y: T): T

    /**
     * Returns true whenever `x` <= `y`.
     */
    fun leq(x: T, y: T): Boolean = lub(x, y) == y // rarely used, but easy to implement :-)
}

/**
 * The `n`-th product lattice made of `sublattice` lattices.
 * @param T is the type of the elements of this lattice.
 */
class UniformProductLattice<T, L : Lattice<T>>(val sublattice: L, n: Int) : Lattice<List<T>> {

    override val bottom = (1..n).map { sublattice.bottom }

    override fun lub(x: List<T>, y: List<T>): List<T> {
        if (x.size != y.size)
            error()
        return (x zip y).map { (xc, yc) -> sublattice.lub(xc, yc) }
    }

    private fun error() {
        throw IllegalArgumentException("products not of same length")
    }
}

/**
 * The flat lattice made of element of `X`.
 * Top is greater than every other element, and Bottom is less than every other element.
 * No additional ordering is defined.
 */
open class FlatLattice<X> : Lattice<FlatLattice.FlatElement> {

    interface FlatElement

    data class FlatEl<X>(val el: X) : FlatElement {
        override fun toString(): String = el.toString()
    }

    object Top : FlatElement {
        override fun toString() = "Top"
    }

    object Bot : FlatElement {
        override fun toString() = "Bot"
    }

    /**
     * Lift an element of `X` into an element of the flat lattice.
     */
    fun lift(a: X): FlatEl<X> = FlatEl(a)

    /**
     * Un-lift an element of the lattice to an element of `X`.
     * If the element is Top or Bot then IllegalArgumentException is thrown.
     * Note that this method is declared as implicit, so the conversion can be done automatically.
     */
    fun unlift(a: FlatEl<X>): X = a.el

    override val top: FlatElement = Top

    override val bottom: FlatElement = Bot

    override fun lub(x: FlatElement, y: FlatElement): FlatElement =
        if (x == Bot || y == Top || x == y)
            y
        else if (y == Bot || x == Top)
            x
        else
            Top
}

/**
 * The product lattice made by `l1` and `l2`.
 * @param A is the type of the elements of `l1`.
 * @param B is the type of the elements of `l2`.
 */
class PairLattice<A, L1 : Lattice<A>, B, L2 : Lattice<B>>(val sublattice1: L1, val sublattice2: L2) : Lattice<Pair<A, B>> {

    override val bottom: Pair<A, B> = sublattice1.bottom to sublattice2.bottom

    override fun lub(x: Pair<A, B>, y: Pair<A, B>) =
        sublattice1.lub(x.first, y.first) to sublattice2.lub(x.second, y.second)

}

/**
 * A lattice of maps from the set `X` to the lattice `sublattice`.
 * @param T is the type of the elements of `sublattice`.
 * The set `X` is a subset of `A` and it is defined by the characteristic function `ch`, i.e. `a` is in `X` if and only if `ch(a)` returns true.
 * Bottom is the default value.
 */
class MapLattice<A, T, out L : Lattice<T>>(ch: (A) -> Boolean, val sublattice: L) : Lattice<MapWithDefault<A, T>> {
    // note: 'ch' isn't used in the class, but having it as a class parameter avoids a lot of type annotations

    override val bottom: MapWithDefault<A, T> = mutableMapOf<A, T>().withDefaultValue(sublattice.bottom)

    override fun lub(x: MapWithDefault<A, T>, y: MapWithDefault<A, T>): MapWithDefault<A, T> =
        x.keys.fold(y) { m, a -> m + (a to sublattice.lub(x[a], y[a])) }.withDefaultValue(sublattice.bottom)
}

/**
 * The powerset lattice of `X`, where `X` is the subset of `A` defined by the characteristic function `ch`, with subset ordering.
 */
class PowersetLattice<A>(ch: (A) -> Boolean) : Lattice<Set<A>> {
    // note: 'ch' isn't used in the class, but having it as a class parameter avoids a lot of type annotations

    override val bottom: Set<A> = emptySet()

    override fun lub(x: Set<A>, y: Set<A>): Set<A> = x union y
}

/**
 * The powerset lattice of `X`, where `X` is the subset of `A` defined by the characteristic function `ch`, with superset ordering.
 */
class ReversePowersetLattice<A>(s: Set<A>) : Lattice<Set<A>> {

    override val bottom: Set<A> = s

    override fun lub(x: Set<A>, y: Set<A>) = x intersect y
}

/**
 * The lift lattice for `sublattice`.
 * @param T is the type of the elements of this lattice.
 * Supports lifting and unlifting.
 */
class LiftLattice<T, out L : Lattice<T>>(val sublattice: L) : Lattice<LiftLattice.Lifted<T>> {

    interface Lifted<T>

    class Bottom<T> : Lifted<T> {
        override fun toString() = "LiftBot"
    }

    data class Lift<T>(val n: T) : Lifted<T>

    override val bottom: Lifted<T> = Bottom()

    override fun lub(x: Lifted<T>, y: Lifted<T>): Lifted<T> =
        when {
            x is Bottom -> y
            y is Bottom -> x
            x is Lift && y is Lift -> Lift(sublattice.lub(x.n, y.n))
            else -> throw IllegalArgumentException()
        }

    /**
     * Lift elements of the sublattice to this lattice.
     */
    fun lift(x: T): Lifted<T> = Lift(x)

    /**
     * Un-lift elements of this lattice to the sublattice.
     * Throws an IllegalArgumentException if trying to unlift the bottom element
     */
    fun unlift(x: Lifted<T>): T = when (x) {
        is Lift -> x.n
        is Bottom -> throw IllegalArgumentException("Cannot unlift bottom")
        else -> throw IllegalArgumentException("Cannot unlift $x")
    }
}

