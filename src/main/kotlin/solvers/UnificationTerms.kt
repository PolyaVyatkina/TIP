package solvers

/**
 * A generic term: a variable [[Var]], a constructor [[Cons]], or a recursive term [[Mu]].
 *
 * @tparam A type parameter describing the kind of constraint system
 */

interface Term<A> {

    /**
     * Returns the set of free variables in this term.
     */
    val fv: Set<Var<A>>

    /**
     * Produces a new term from this term by substituting the variable `v` with the term `t`.
     */
    fun subst(v: Var<A>, t: Term<A>): Term<A>
}

/**
 * A constraint variable.
 */
abstract class Var<A> : Term<A> {

    override val fv: Set<Var<A>> = setOf(this)

    override fun subst(v: Var<A>, t: Term<A>) = if (v == this) t else this
}

/**
 * An n-ary term constructor.
 * 0-ary constructors are constants.
 */
abstract class Cons<A> : Term<A> {
    /**
     * The sub-terms.
     */
    val args = mutableListOf<Term<A>>()

    /**
     * The arity of the constructor.
     */
    val arity = args.size

    override val fv: Set<Var<A>> by lazy { args.flatMap { it.fv }.toSet() }

    /**
     * Checks whether the term `t` matches this term, meaning that it has the same constructor class and the same arity.
     */
    fun doMatch(t: Term<A>): Boolean = this.javaClass == t.javaClass && arity == (t as Cons<A>).arity
}

/**
 * Recursive term.
 * Whenever a term is such that v = t[v] where v appears free in t[v], then we represent it finitely as \u03bc v. t[v].
 * v is a binder in the term, and the copy rule holds: \u03bc v. t[v] == t [ \u03bc v. t[v] ]
 */
abstract class Mu<A> : Term<A> {

    /**
     * The variable.
     */
    abstract val v: Var<A>

    /**
     * The term.
     */
    abstract val t: Term<A>

    override val fv: Set<Var<A>> by lazy { t.fv - v }

    override fun toString(): String = "mu$v.$t"
}

/**
 * Special operations on terms.
 */
abstract class TermOps<A> {

    /**
     * Constructor for [[tip.solvers.Mu]] terms.
     */
    abstract fun makeMu(v: Var<A>, t: Term<A>): Mu<A>

    /**
     * Constructor for fresh variables.
     * The identity of the variable is uniquely determined by `x`.
     */
    abstract fun makeAlpha(x: Var<A>): Var<A>

    /**
     * Closes the term by replacing each free variable with its value in the given environment.
     * Whenever a recursive type is detected, a [[Mu]] term is generated.
     * Remaining free variables are replaced by fresh variables that are implicitly universally quantified.
     *
     * @param t       the term to close
     * @param env     environment, map from variables to terms
     */
    fun close(t: Term<A>, env: Map<Var<A>, Term<A>>): Term<A> = closeRec(t, env)

    /**
     * Closes the term by replacing each variable that appears as a subterm of with its value in the given environment.
     * Whenever a recursive type is detected, a [[Mu]] term is generated.
     *
     * @param t       the term to close
     * @param env     environment, map from variables to terms
     * @param visited the set of already visited variables (empty by default)
     */
    private fun closeRec(t: Term<A>?, env: Map<Var<A>, Term<A>>, visited: Set<Var<A>> = setOf()): Term<A> =
        when (t) {
            is Var<A> ->
                if (!visited.contains(t) && env[t] != t) {
                    // no cycle found, and the variable does not map to itself
                    val cterm = closeRec(env[t], env, visited + t)
                    val newT = makeAlpha(t)
                    if (cterm.fv.contains(newT))
                    // recursive term found, make a [[Mu]]
                        makeMu(newT, cterm.subst(t, newT))
                    else cterm
                } else
                // an unconstrained (i.e. universally quantified) variable
                    makeAlpha(t)
            is Cons<A> ->
                // substitute each free variable with its closed term
                t.fv.fold(t as Term<A>) { acc, v ->
                    acc.subst(v, closeRec(v, env, visited))
                }
            is Mu<A> -> makeMu(t.v, closeRec(t.t, env, visited))
            else -> throw IllegalArgumentException()
        }
}
