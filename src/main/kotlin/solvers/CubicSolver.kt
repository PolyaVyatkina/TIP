package solvers

import utils.Log
import java.util.*
import kotlin.streams.toList

class CubicSolver<V, T>(val cycleElimination: Boolean = true) {

    val log = Log.logger(this.javaClass)

    var lastTknId = -1

    val nextTokenId: Int
        get() {
            lastTknId++
            return lastTknId
        }

    inner class Node(
        val succ: MutableSet<V> = mutableSetOf(), // note: the edges between nodes go via the variables
        val tokenSol: BitSet = BitSet(), // the current solution bitvector
        val conditionals: MutableMap<Int, MutableSet<Pair<V, V>>> = mutableMapOf(), // the pending conditional constraints
        val vars: MutableSet<V> = mutableSetOf() // the variables belonging to this node
    ) {

        /*fun this(x: V) {
            this()
            vars += x
        }*/

        override fun toString(): String = this.hashCode().toString()
    }

    /**
     * The map from variables to nodes.
     */
    val varToNode: MutableMap<V, Node> = mutableMapOf()

    /**
     * Provides an index for each token that we have seen.
     */
    val tokenToInt: MutableMap<T, Int> = mutableMapOf()

    /**
     * Returns the index associated with the given token.
     * Allocates a fresh index if the token hasn't been seen before.
     */
    private fun getTokenInt(tkn: T): Int = tokenToInt[tkn] ?: tokenToInt.put(tkn, nextTokenId)!!

    /**
     * Retrieves the node associated with the given variable.
     * Allocates a fresh node if the variable hasn't been seen before.
     */
    private fun getOrPutNode(x: V): Node = varToNode[x] ?: varToNode.put(x, Node(mutableSetOf(x)))!!

    /**
     * Attempts to detect a path from `from` to `to` in the graph.
     * @return the list of variables in the path if such path is found, an empty list otherwise.
     */
    private fun detectPath(from: Node, to: Node): List<Node> {
        val visited: MutableSet<Node> = mutableSetOf()

        fun detectPathRec(current: Node): List<Node> =
            if (current == to)
            // Detected a path from from to to
                listOf(current)
            else {
                visited += current
                // Search for the first cycle we can find, and save it
                // If no cycle is found, return the empty list
                var toReturn: List<Node> = listOf()
                current.succ
                    .map { varToNode[it] }
                    .toSet()
                    .filter { !visited.contains(it) }
                    .any { n ->
                        val cycleVisited = detectPathRec(n!!)
                        if (!cycleVisited.isNullOrEmpty()) {
                            // Cycle found
                            toReturn = listOf(current) + cycleVisited
                            true
                        } else false //
                    }
                toReturn
            }

        return detectPathRec(from)
    }

    /**
     * Collapses the given cycle (if nonempty).
     */
    private fun collapseCycle(cycle: List<Node>) {
        if (cycle.isNotEmpty()) {
            log.verb("Collapsing cycle $cycle")
            val first = cycle.first()
            val oldNode = cycle.last()
            first.succ += oldNode.succ
            first.conditionals.keys.forEach { k ->
                first.conditionals[k]?.plusAssign(oldNode.conditionals[k]!!)
            }
            first.tokenSol.or(oldNode.tokenSol)
            // Redirect all the variables that were pointing to this node to the new one
            oldNode.vars.forEach { v ->
                varToNode[v] = first
                first.vars += v
            }
        }
    }

    /**
     * Adds the set of tokens `s` to the variable `x` and propagates along the graph.
     */
    private fun addAndPropagateBits(s: BitSet, x: V) {
        val node = getOrPutNode(x)
        val old: BitSet = node.tokenSol.clone() as BitSet
        val newTokens = old
        newTokens.or(s)
        if (newTokens != old) {
            // Set the new bits
//            node.tokenSol |= s
//            val diff = newTokens &~ old
            node.tokenSol
            val diff = newTokens

            // Add edges from pending lists, then clear the lists
            diff.stream().forEach { t ->
                node.conditionals.getOrElse(t) { setOf() }.forEach {
                    addSubsetConstraint(it.first, it.second)
                }
            }
            diff.stream().forEach { t ->
                node.conditionals.remove(t)
            }

            // Propagate to successors
            node.succ.forEach { s ->
                addAndPropagateBits(newTokens, s)
            }
        }
    }

    /**
     * Adds a constraint of type <i>t</i> &#8712; <i>x</i>.
     */
    fun addConstantConstraints(t: T, x: V) {
        log.verb("Adding constraint $t \u2208 [[$x]]")
        val bs = BitSet()
        bs.set(getTokenInt(t))
        addAndPropagateBits(bs, x)
    }

    /**
     * Adds a constraint of type <i>x</i> &#8838; <i>y</i>.
     */
    fun addSubsetConstraint(x: V, y: V) {
        log.verb("Adding constraint [[$x]] \u2286 [[$y]]")
        val nx = getOrPutNode(x)
        val ny = getOrPutNode(y)

        if (nx != ny) {
            // Add the edge
            log.verb("Adding edge $x -> $y")
            nx.succ += y

            // Propagate the bits
            addAndPropagateBits(nx.tokenSol, y)

            // Collapse newly introduced cycle, if any
            if (cycleElimination)
                collapseCycle(detectPath(ny, nx))
        }
    }

    /**
     * Adds a constraint of type <i>t</i> &#8712; <i>x</i> &#8658; <i>y</i> &#8838; <i>z</i>.
     */
    fun addConditionalConstraint(t: T, x: V, y: V, z: V) {
        log.verb("Adding constraint $t \u2208 [[$x]] => [[$y]] \u2286 [[$z]]")
        val xn = getOrPutNode(x)
        val intT = getTokenInt(t)
        if (xn.tokenSol.get(intT)) {
            // Already enabled
            addSubsetConstraint(y, z)
        } else if (y != z) {
            // Not yet enabled, add to pending list
            log.verb("Condition $t \u2208 [[$x]] not yet enabled, adding ([[$y]],[[$z]]) to pending")
            if (xn.conditionals[intT] == null) {
                xn.conditionals[intT] = setOf<Pair<V, V>>() as MutableSet<Pair<V, V>>
                xn.conditionals.plus(y to z)
            }
        }
    }

    /**
     * Returns the current solution as a map from variables to token sets.
     */
    fun getSolution(): Map<V, Set<T>> {
        val intToToken: Map<Int, T> = tokenToInt.map { p -> p.value to p.key }.toMap()
        return varToNode.keys.associateWith { v -> getOrPutNode(v).tokenSol.stream().toList().map { i: Int -> intToToken[i]!! }.toSet()}
    }
}