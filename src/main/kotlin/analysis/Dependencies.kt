package analysis

import cfg.*

/**
 * Dependency methods for worklist-based analyses.
 */
interface Dependencies<N> {

    /**
     * Outgoing dependencies. Used when propagating dataflow to successors.
     * @param n an element from the worklist
     * @return the elements that depend on the given element
     */
    fun outdep(n: N): Set<N>

    /**
     * Incoming dependencies. Used when computing the join from predecessors.
     * @param n an element from the worklist
     * @return the elements that the given element depends on
     */
    fun indep(n: N): Set<N>
}

/**
 * Dependency methods for forward analyses.
 */
interface ForwardDependencies : Dependencies<CfgNode> {

    override fun outdep(n: CfgNode) = n.succ.toSet()

    override fun indep(n: CfgNode) = n.pred.toSet()
}

/**
 * Dependency methods for backward analyses.
 */
interface BackwardDependencies : Dependencies<CfgNode> {

    override fun outdep(n: CfgNode) = n.pred.toSet()

    override fun indep(n: CfgNode) = n.succ.toSet()
}

/**
 * Variant of [[ForwardDependencies]] for interprocedural analysis.
 */
interface InterproceduralForwardDependencies : Dependencies<CfgNode> {

    val cfg: InterproceduralProgramCfg


    /**
     * Like [[ForwardDependencies.outdep]] but with call and return edges.
     * A call node has an outdep to its after-call node.
     */
    override fun outdep(n: CfgNode): Set<CfgNode> {
        val interDep = when (n) {
            is CfgCallNode -> cfg.IpNodeInfoCall(n).callees
            is CfgFunExitNode -> cfg.IpNodeInfoExit(n).callersAfterCall
            else -> setOf()
        }
        return interDep + n.succ.toSet()
    }

    /**
     * Like [[ForwardDependencies.indep]] but returning an empty set for after-call nodes.
     */
    override fun indep(n: CfgNode) =
        if (n is CfgAfterCallNode) setOf()
        else n.pred.toSet()

}

/**
 * Variant of [[ForwardDependencies]] for context-sensitive interprocedural analysis.
 */
interface ContextSensitiveForwardDependencies<C : CallContext> : Dependencies<Pair<C, CfgNode>> {

    val cfg: InterproceduralProgramCfg

    /**
     * Like [[InterproceduralForwardDependencies.outdep]] but returning an empty set for call nodes and function exit nodes,
     * and using the same context as the given pair.
     */
    override fun outdep(n: Pair<C, CfgNode>) =
        (if (n.second is CfgCallNode) setOf()
        else n.second.succ.toSet())
            .map { n.first to it }
            .toSet()

    /**
     * (Not implemented as it is not used by any existing analysis.)
     */
    override fun indep(n: Pair<C, CfgNode>) = TODO()
}
