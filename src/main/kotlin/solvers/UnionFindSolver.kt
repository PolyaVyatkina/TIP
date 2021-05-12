package solvers

import utils.withDefault

/**
 * Unification solver based on union-find.
 * @tparam A type parameter describing the kind of constraint system
 */
class UnionFindSolver<A> {

    /**
     * This map holds the graph structure for the union-find algorithm.
     * Each term has a "parent". Two terms are equivalent (unified) if one is reachable from the other along zero or more parent links.
     * The parent of a term may be the term itself, in which case it is the representative for its equivalence class.
     */
    private val parent = mutableMapOf<Term<A>, Term<A>>()

    /**
     * Performs the unification of the two terms `t1` and `t2`.
     * When unifying a variable and a non-variable term, the non-variable term has higher priority for becoming the representative.
     */
    fun unify(t1: Term<A>, t2: Term<A>) {
        mkSet(t1)
        mkSet(t2)
        val rep1 = find(t1)
        val rep2 = find(t2)

        if (rep1 == rep2) return
        when {
            rep1 is Var<A> && rep2 is Var<A> -> mkUnion(rep1, rep2)
            rep1 is Var<A> -> mkUnion(rep1, rep2)
            rep2 is Var<A> -> mkUnion(rep2, rep1)
            rep1 is Cons<A> && rep2 is Cons<A> ->
                if (rep1.doMatch(rep2)) {
                    mkUnion(rep1, rep2)
                    rep1.args.zip(rep2.args).forEach { unify(it.first, it.second) }
                }
            else -> throw UnificationFailure("Can't unify $t1 and $t2 (with representatives $rep1 and $rep2)")
        }
    }


    /**
     * Returns the canonical element of the equivalence class of the term `t`.
     * The term is added as a new equivalence class if it has not been encountered before.
     * Uses path compression.
     */
    fun find(t: Term<A>): Term<A> {
        mkSet(t)
        if (parent[t] != t)
            parent[t] = find(parent[t]!!)
        return parent[t]!!
    }

    /**
     * Perform the union of the equivalence classes of `t1` and `t2`, such that `t2` becomes the new canonical element.
     * We assume `t1` and `t2` to be distinct canonical elements.
     * This implementation does not use [[https://en.wikipedia.org/wiki/Disjoint-set_data_structure union-by-rank]].
     */
    private fun mkUnion(t1: Term<A>, t2: Term<A>) {
        parent[t1] = t2
    }

    /**
     * Creates an equivalence class for the term `t`, if it does not exists already.
     */
    private fun mkSet(t: Term<A>) {
        if (!parent.contains(t))
            parent[t] = t
    }

    /**
     * Returns the solution of the solver.
     * Note that the terms in the solution have not yet been closed, i.e. they may contain constraint variables.
     * @return a map associating to each variable the representative of its equivalence class
     */
    fun solution(): Map<Var<A>, Term<A>> {
        // for each constraint variable, find its canonical representative (using the variable itself as default)
        val m = mutableMapOf<Var<A>, Term<A>>()
        parent.keys.forEach {
            if (it is Var<A>)
                m[it] = parent[it]!!
        }
        return m.withDefault { find(it) }
    }

    /**
     * Returns all the unifications of the solution.
     * @return a map from representative to equivalence class
     */
    fun unifications(): Map<Term<A>, Collection<Term<A>>> =
        parent.keys.groupBy { find(it) }.withDefault { setOf(it) }

    /**
     * Produces a string representation of the solution.
     */
    override fun toString(): String = solution().map { p -> "${p.key} = ${p.value}" }.toString()
}

/**
 * Exception thrown in case of unification failure.
 */
class UnificationFailure(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)
