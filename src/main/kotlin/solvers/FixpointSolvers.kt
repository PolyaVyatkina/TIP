package solvers

import analysis.Dependencies
import lattices.*
import lattices.LiftLattice.*
import utils.*

object FixpointSolvers {

    val log = Log.logger(this.javaClass)
}

/**
 * Base trait for lattice solvers.
 * @param T is the type of the element of the lattice
 */
interface LatticeSolver<T> {

    /**
     * The lattice used by the solver.
     */
    val lattice: Lattice<T>

    /**
     * The analyze function.
     */
    fun analyze(): T
}

/**
 * Simple fixpoint solver.
 * @param T is the type of the element of the lattice
 */
interface SimpleFixpointSolver<T> : LatticeSolver<T> {

    /**
     * The constraint function for which the least fixpoint is to be computed.
     * @param x the input lattice element
     * @return the output lattice element
     */
    fun function(x: T): T

    /**
     * The basic Kleene fixpoint solver.
     */
    override fun analyze(): T {
        var x = lattice.bottom
        var t: T
        do {
            t = x
            x = function(x)
        } while (x != t)
        return x
    }
}

/**
 * Base trait for map lattice solvers.
 * @tparam N type of the elements in the map domain.
 */
interface MapLatticeSolver<N, T> : LatticeSolver<MapWithDefault<N, T>>, Dependencies<N> {

    /**
     * Must be a map lattice.
     */
    override val lattice: MapLattice<N, T, Lattice<T>>

    /**
     * The transfer function.
     */
    fun transfer(n: N, s: T): T

    /**
     * The constraint function for individual elements in the map domain.
     * First computes the join of the incoming elements and then applies the transfer function.
     * @param n the current location in the map domain
     * @param x the current lattice element for all locations
     * @return the output sublattice element
     */
    fun funsub(n: N, x: Map<N, T>): T = transfer(n, join(n, x))

    /**
     * Computes the least upper bound of the incoming elements.
     */
    fun join(n: N, o: Map<N, T>): T {
        val states = indep(n).map { o[it]!! }
        return states.fold(lattice.sublattice.bottom) { acc, pred -> lattice.sublattice.lub(acc, pred) }
    }
}

/**
 * Simple fixpoint solver for map lattices where the constraint function is defined pointwise.
 * @tparam N type of the elements in the map domain.
 */
interface SimpleMapLatticeFixpointSolver<N, T> : SimpleFixpointSolver<MapWithDefault<N, T>>, MapLatticeSolver<N, T> {

    /**
     * The map domain.
     */
    val domain: Set<N>

    /**
     * The function for which the least fixpoint is to be computed.
     * Applies the sublattice constraint function pointwise to each entry.
     * @param x the input lattice element
     * @return the output lattice element
     */
    override fun function(x: MapWithDefault<N, T>): MapWithDefault<N, T> {
        FixpointSolvers.log.verb("In state $x")
        return domain.fold(lattice.bottom) { m, a ->
            FixpointSolvers.log.verb("Processing $a")
            m.plus(a to funsub(a, x))
        }
    }
}

/**
 * Base interface for solvers for map lattices with lifted co-domains.
 * @tparam N type of the elements in the map domain.
 */
interface MapLiftLatticeSolver<N, T> : MapLatticeSolver<N, Lifted<T>>, Dependencies<N> {

    override val lattice: MapLattice<N, Lifted<T>, LiftLattice<T, Lattice<T>>>

    /**
     * The transfer function for the sub-sub-lattice.
     */
    fun transferUnlifted(n: N, s: T): T

    override fun transfer(n: N, s: Lifted<T>): Lifted<T> =
        when (s) {
            is Bottom<T> -> Bottom()
            is Lift<T> -> {
                lattice.sublattice.lift(transferUnlifted(n, s.n))
            }
            else -> throw IllegalArgumentException()
        }

}

/**
 * An abstract worklist algorithm.
 * @tparam N type of the elements in the worklist.
 */
interface Worklist<N> {

    /**
     * Called by [run] to process an item from the worklist.
     */
    fun process(n: N)

    /**
     * Adds an item to the worklist.
     */
    fun add(n: N)

    /**
     * Adds a set of items to the worklist.
     */
    fun add(ns: Set<N>)

    /**
     * Iterates until there is no more work to do.
     * @param first the initial contents of the worklist
     */
    fun run(first: Set<N>)
}

/**
 * A simple worklist algorithm.
 * (Using a priority queue would typically be faster.)
 * @tparam N type of the elements in the worklist.
 */
interface ListSetWorklist<N> : Worklist<N> {

    var worklist: Set<N>

    override fun add(n: N) {
        FixpointSolvers.log.verb("Adding $n to worklist")
        worklist = setOf(n) + worklist
    }

    override fun add(ns: Set<N>) {
        FixpointSolvers.log.verb("Adding $ns to worklist")
        for (el in ns) {
            worklist = setOf(el) + worklist
        }
    }

    override fun run(first: Set<N>) {
        worklist = first
        while (worklist.isNotEmpty()) {
            val n = worklist.first()
            worklist = worklist - worklist.first()
            process(n)
        }
    }
}

/**
 * Base trait for worklist-based fixpoint solvers.
 * @tparam N type of the elements in the worklist.
 */
interface WorklistFixpointSolver<N, T> : MapLatticeSolver<N, T>, ListSetWorklist<N>, Dependencies<N> {

    /**
     * The current lattice element.
     */
    var x: MapWithDefault<N, T>

    override fun process(n: N) {
        val xn = x[n]
        FixpointSolvers.log.verb("Processing $n in state $xn")
        val y = funsub(n, x)
        if (y != xn) {
            x.plusAssign(n to y)
            add(outdep(n))
        }
    }
}

/**
 * Worklist-based fixpoint solver.
 * @tparam N type of the elements in the worklist.
 */
interface SimpleWorklistFixpointSolver<N, T> : WorklistFixpointSolver<N, T> {

    /**
     * The map domain.
     */
    val domain: Set<N>

    override fun analyze(): MapWithDefault<N, T> {
        x = lattice.bottom
        run(domain)
        return x
    }
}

/**
 * The worklist-based fixpoint solver with initialization.
 */
interface WorklistFixpointSolverWithInit<N, T> : WorklistFixpointSolver<N, Lifted<T>>, MapLiftLatticeSolver<N, T> {

    /**
     * The start locations.
     */
    val first: Set<N>

    override fun analyze(): MapWithDefault<N, Lifted<T>> {
        x = lattice.bottom
        run(first)
        return x
    }
}

/**
 * Functions for solvers that perform propagation after transfer instead of join before transfer.
 */
interface WorklistFixpointPropagationFunctions<N, T> : ListSetWorklist<N> {

    /**
     * Must be a map lattice.
     */
    val lattice: MapLattice<N, T, Lattice<T>>

    /**
     * The current lattice element.
     */
    var x: MapWithDefault<N, T>

    /**
     * The start locations.
     */
    val first: Set<N>

    /**
     * The initial lattice element at the start locations.
     */
    val init: T

    /**
     * Propagates lattice element y to node m.
     */
    fun propagate(y: T, m: N) {
        FixpointSolvers.log.verb("Propagating $y to $m")
        val xm = x[m]
        val t = lattice.sublattice.lub(xm ?: lattice.sublattice.bottom, y)
        if (t != xm) {
            add(m)
            x = x + (m to t)
        }
    }

    fun analyze(): Map<N, T> {
        x = first.fold(lattice.bottom) { l, cur ->
            l + (cur to init)
        }
        run(first)
        return x
    }
}

/**
 * Worklist-based fixpoint solver that performs propagation after transfer instead of join before transfer.
 * This results in fewer join operations when nodes have many dependencies.
 * Note that with this approach, each abstract state represents the program point *after* the node
 * (for a forward analysis, and opposite for a backward analysis).
 */
interface WorklistFixpointPropagationSolver<N, T> : WorklistFixpointSolverWithInit<N, T>, WorklistFixpointPropagationFunctions<N, Lifted<T>> {

    override val lattice: MapLattice<N, Lifted<T>, LiftLattice<T, Lattice<T>>>

    /**
     * The initial lattice element at the start locations.
     * Default: lift(bottom).
     */
    override val init: Lifted<T>
        get() = lattice.sublattice.lift(lattice.sublattice.sublattice.bottom)

    /**
     * This method overrides the one from [WorklistFixpointSolver].
     * Called by the worklist solver when a node is visited.
     */
    override fun process(n: N) {
        // read the current lattice element
        val xn = x[n]
        // apply the transfer function
        FixpointSolvers.log.verb("Processing $n in state $xn")
        val y = transfer(n, xn)
        // propagate to all nodes that depend on this one
        for (m in outdep(n)) propagate(y, m)
    }

    override fun analyze(): MapWithDefault<N, Lifted<T>> {
        x = first.fold(lattice.bottom) { l, cur -> l + (cur to init)  }
        run(first)
        return x
    }

    override var x: MapWithDefault<N, Lifted<T>>
}

/**
 * Worklist-based fixpoint solver with initialization and simple widening.
 */
interface WorklistFixpointSolverWithInitAndSimpleWidening<N, T> : WorklistFixpointSolverWithInit<N, T> {

    /**
     * Set widening function.
     * @param s input lattice element
     * @return output lattice element
     */
    fun widen(s: Lifted<T>) : Lifted<T>

    /**
     * Tells whether (src,dst) is a back-edge.
     */
    fun backedge(src: N, dst: N): Boolean

    override fun process(n: N) {
        val xn = x[n]
        FixpointSolvers.log.verb("Processing $n in state $xn")
        val y = funsub(n, x)
        if (y != xn) {
            x.plusAssign((n to if (outdep(n).any { backedge(n, it) } ) widen(y) else y))
            add(outdep(n))
        }
    }
}

/**
 * The worklist-based fixpoint solver with initialization, simple widening, and narrowing.
 */
interface WorklistFixpointSolverWithInitAndSimpleWideningAndNarrowing<N, T> : WorklistFixpointSolverWithInitAndSimpleWidening<N, T>, SimpleMapLatticeFixpointSolver<N, Lifted<T>> {

    /**
     * Number of narrowing steps.
     */
    val narrowingSteps: Int

    /**
     * Performs narrowing on the given lattice element
     * @param x the lattice element
     * @param i number of iterations
     */
    fun narrow(x: MapWithDefault<N, Lifted<T>>, i: Int): MapWithDefault<N, Lifted<T>> =
        if (i <= 0) x else narrow(function(x), i - 1) // uses the simple definition of 'fun' from SimpleMapLatticeFixpointSolver

    override fun analyze(): MapWithDefault<N, Lifted<T>> =
        narrow(super<WorklistFixpointSolverWithInitAndSimpleWidening>.analyze(), narrowingSteps)

}
