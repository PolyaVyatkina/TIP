//package lattices
//
//import java.lang.IllegalArgumentException
//
///**
// * The lattice of edge functions, used by [[solvers.IDEAnalysis]].
// * Technically a map lattice, but we don't bother implementing it as extension of MapLattice.
// */
//class EdgeLattice<E, L: Lattice<E>>(val valuelattice: L) : Lattice<EdgeLattice.Edge<E>> {
//
//    override val bottom: ConstEdge<E> = ConstEdge(valuelattice.bottom)
//
//    override fun lub(x: Edge<E>, y: Edge<E>): Edge<E> = x.joinWith(y)
//
//    /**
//     * An "edge" represents a function L -> L where L is the value lattice.
//     */
//    interface Edge<L> : (L) -> L {
//
//        /**
//         * Applies the function to the given lattice element.
//         */
//        override fun invoke(x: L): L
//
//        /**
//         * Composes this function with the given one.
//         * The resulting function first applies `e` then this function.
//         */
//        fun composeWith(e: Edge<L>): Edge<L>
//
//        /**
//         * Finds the least upper bound of this function and the given one.
//         */
//        fun joinWith(e: Edge<L>): Edge<L>
//    }
//
//    /**
//     * Edge labeled with identity function.
//     */
//    inner class IdEdge<L> : Edge<L> {
//
//        override fun invoke(x: L) = x
//
//        override fun composeWith(e: Edge<L>) = e
//
//        override fun joinWith(e: Edge<L>): Edge<L> =
//            if (e == this) this
//            else e.joinWith(this)
//
//        override fun toString() = "IdEdge()"
//    }
//
//    /**
//     * Edge labeled with constant function.
//     */
//     inner class ConstEdge<L>(val c: E) : Edge<L> {
//
//        override fun invoke(x: L) = c
//
//        override fun composeWith(e: Edge<L>) = this
//
//        override fun joinWith(e: Edge<E>) =
//            if (e == this || c == valuelattice.top) this
//            else if (c == valuelattice.bottom) e
//            else when (e) {
//                is EdgeLattice<*, *>.IdEdge<E> -> throw IllegalArgumentException() // never reached with the currently implemented analyses
//                is EdgeLattice<*, *>.ConstEdge<E> -> ConstEdge(valuelattice.lub(c, e.c))
//                else -> e.joinWith(this)
//            }
//
//        override fun toString() = "ConstEdge($c)"
//    }
//}