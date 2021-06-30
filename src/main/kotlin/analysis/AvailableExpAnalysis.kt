package analysis

import ast.*
import ast.AstOps.UnlabelledNode
import ast.AstOps.appearingExpressions
import ast.AstOps.appearingIds
import cfg.CfgFunEntryNode
import cfg.CfgNode
import cfg.CfgOps.appearingExpressions
import cfg.CfgStmtNode
import cfg.IntraproceduralProgramCfg
import lattices.MapLattice
import lattices.ReversePowersetLattice
import solvers.SimpleMapLatticeFixpointSolver
import solvers.SimpleWorklistFixpointSolver
import utils.MapWithDefault
import utils.withDefaultValue

/**
 * Base class for available expressions analysis
 */
abstract class AvailableExpAnalysis(cfg: IntraproceduralProgramCfg, val declData: DeclarationData) : FlowSensitiveAnalysis<CfgNode, Set<UnlabelledNode<AExpr>>>(cfg) {

    val allExps: Set<UnlabelledNode<AExpr>> =
        cfg.nodes.flatMap { node -> node.appearingExpressions().map { UnlabelledNode(it, declData) } }.toSet()

    init {
        // Analysis does not accept pointers.
        NoPointers.assertContainsProgram(cfg.prog)
    }

    private val chNodes: (CfgNode) -> Boolean = { cfg.nodes.contains(it) }
    private val reversePowersetLattice = ReversePowersetLattice(allExps)

    override val lattice = MapLattice(chNodes, reversePowersetLattice)

    fun transfer(n: CfgNode, s: Set<UnlabelledNode<AExpr>>): Set<UnlabelledNode<AExpr>> =
        when (n) {
            is CfgFunEntryNode -> setOf()
            is CfgStmtNode ->
                when (val d = n.data) {
                    is AAssignStmt -> {
                        val l = d.left
                        if (l is AIdentifier) {
                            (s + d.right.appearingExpressions().map { UnlabelledNode(it, declData) }).filterNot { e ->
                                e.n.appearingIds(declData).containsAll(l.appearingIds(declData))
                            }.toSet()
                        } else throw IllegalArgumentException()
                    }
                    is AExpr -> s + d.appearingExpressions().map { UnlabelledNode(it, declData) }
                    is AOutputStmt -> s + d.value.appearingExpressions().map { UnlabelledNode(it, declData) }
                    is AReturnStmt -> s + d.value.appearingExpressions().map { UnlabelledNode(it, declData) }
                    else -> s
                }
            else -> s
        }
}

/**
 * Available expressions analysis that uses the simple fipoint solver.
 */
class AvailableExpAnalysisSimpleSolver(cfg: IntraproceduralProgramCfg, declData: DeclarationData) : AvailableExpAnalysis(cfg, declData),
    SimpleMapLatticeFixpointSolver<CfgNode, Set<UnlabelledNode<AExpr>>>, ForwardDependencies {
    override fun analyze(): MapWithDefault<CfgNode, Set<UnlabelledNode<AExpr>>> {
        return super.analyze()
    }
}

/**
 * Available expressions analysis that uses the worklist solver.
 */
class AvailableExpAnalysisWorklistSolver(cfg: IntraproceduralProgramCfg, declData: DeclarationData) : AvailableExpAnalysis(cfg, declData),
    SimpleWorklistFixpointSolver<CfgNode, Set<UnlabelledNode<AExpr>>>, ForwardDependencies {
    override fun analyze(): MapWithDefault<CfgNode, Set<UnlabelledNode<AExpr>>> {
        return super.analyze()
    }

    override var x = mapOf<CfgNode, Set<UnlabelledNode<AExpr>>>().withDefaultValue(lattice.sublattice.bottom)
    override var worklist: Set<CfgNode> = setOf()
}