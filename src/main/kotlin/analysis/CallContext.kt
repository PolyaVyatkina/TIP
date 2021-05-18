package analysis

import cfg.CfgCallNode
import cfg.CfgFunEntryNode
import lattices.Lattice

/**
 * Base trait for call contexts.
 */
interface CallContext {

    /**
     * Functions for creating call contexts.
     * @tparam C the type for call contexts
     * @tparam L the type for the abstract state lattice
     */
    interface CallContextFunctions<C : CallContext, T, L : Lattice<T>> {

        val statelattice: L

        /**
         * Initial context, for the main function.
         */
        val initialContext: C

        /**
         * Makes a context for the callee at a function call site.
         * @param c the caller context
         * @param n the current call node
         * @param x the callee entry abstract state
         * @param f the callee function
         * @return the context for the callee
         */
        fun makeCallContext(c: C, n: CfgCallNode, x: T, f: CfgFunEntryNode): C
    }
}

/**
 * Call context for call strings.
 */
data class CallStringContext(val cs: List<CfgCallNode>) : CallContext {

    /**
     * Creates string representation using the source locations of the calls in the call string.
     */
    override fun toString(): String = cs.map { it.data.loc }.joinToString("[", ",", "]")
}

/**
 * Call context construction for call strings.
 */
interface CallStringFunctions<T, L : Lattice<T>> : CallContext.CallContextFunctions<CallStringContext, T, L> {

    /**
     * Default maximum length for call strings: 1.
     */
    val maxCallStringLength: Int
        get() = 1

    /**
     * Creates a context as the empty list.
     */
    override val initialContext: CallStringContext
        get() = CallStringContext(emptyList())

    /**
     * Creates a context as the singleton list consisting of the call node (and ignoring the other arguments).
     */
    override fun makeCallContext(c: CallStringContext, n: CfgCallNode, x: T, f: CfgFunEntryNode): CallStringContext =
        CallStringContext((listOf(n) + c.cs).slice(IntRange(0, maxCallStringLength)))
}

/**
 * Call context for functional approach.
 * @param x a lattice element
 */
data class FunctionalContext(val x: Any?) : CallContext { // TODO: find some way to make Scala type check that x is indeed of type statelattice.Element

    override fun toString(): String = x.toString()
}

/**
 * Call context construction for functional approach.
 */
interface FunctionalFunctions<T, L : Lattice<T>> : CallContext.CallContextFunctions<FunctionalContext, T, L> {

    /**
     * Creates a context as the empty abstract state.
     */
    override val initialContext: FunctionalContext
        get() = FunctionalContext(statelattice.bottom)

    /**
     * Creates a context as the singleton list consisting of the call node (and ignoring the other arguments).
     */
    override fun makeCallContext(c: FunctionalContext, n: CfgCallNode, x: T, f: CfgFunEntryNode): FunctionalContext =
        FunctionalContext(x)
}