package analysis

import ast.*
import ast.AstOps.UnlabelledNode
import ast.AstOps.appearingAllocs
import ast.AstOps.appearingExpressions
import ast.AstOps.appearingIds
import cfg.*
import cfg.CfgOps.appearingAssignments
import cfg.CfgOps.appearingIds
import cfg.CfgOps.declaredVars
import lattices.MapLattice
import lattices.PowersetLattice
import solvers.SimpleMapLatticeFixpointSolver
import solvers.SimpleWorklistFixpointSolver
import utils.MapWithDefault
import utils.withDefaultValue

/**
 * Base class for the reaching definitions analysis
 */
abstract class ReachingDefAnalysis(cfg: IntraproceduralProgramCfg, val declData: DeclarationData) : FlowSensitiveAnalysis<CfgNode, Set<AStmt>>(cfg) {

    val allAssignments: Set<AAssignStmt?> = cfg.nodes.map { it.appearingAssignments() }.toSet()

    private val chNodes: (CfgNode) -> Boolean = { cfg.nodes.contains(it) }
    private val chAllAssignments: (AStmt) -> Boolean = { allAssignments.contains(it) }
    private val powersetLattice = PowersetLattice(chAllAssignments)

    override val lattice = MapLattice(chNodes, powersetLattice)

    fun transfer(n: CfgNode, s: Set<AStmt>): Set<AStmt> =
        when (n) {
            is CfgFunEntryNode -> lattice.sublattice.bottom
            is CfgStmtNode ->
                when (val d = n.data) {
                    is AAssignStmt -> {
                        val l = d.left
                        if (l is AIdentifier) {
                            s.filterNot { e ->
                                if (e is AAssignStmt)
                                    e.left.appearingIds(declData).containsAll(l.appearingIds(declData))
                                else
                                    e.appearingIds(declData).containsAll(l.appearingIds(declData))
                            }.toSet() + d
                        }
                        else NoPointers.languageRestrictionViolation("$l not allowed")
                    }
                    is AVarStmt -> s + d
                    else -> s
                }
            else -> s
        }
}

/**
 * Live variables analysis that uses the simple fixpoint solver.
 */
class ReachingDefAnalysisSimpleSolver(cfg: IntraproceduralProgramCfg, declData: DeclarationData)
    : ReachingDefAnalysis(cfg, declData), SimpleMapLatticeFixpointSolver<CfgNode, Set<AStmt>>, BackwardDependencies {
    override fun analyze(): MapWithDefault<CfgNode, Set<AStmt>> {
        return super.analyze()
    }
}

/**
 * Live variables analysis that uses the worklist solver.
 */
class ReachingDefAnalysisWorklistSolver(cfg: IntraproceduralProgramCfg, declData: DeclarationData)
    : ReachingDefAnalysis(cfg, declData), SimpleWorklistFixpointSolver<CfgNode, Set<AStmt>>, BackwardDependencies {
    override fun analyze(): MapWithDefault<CfgNode, Set<AStmt>> {
        return super.analyze()
    }

    override var x = mapOf<CfgNode, Set<AAssignStmt>>().withDefaultValue(lattice.sublattice.bottom)
    override var worklist: Set<CfgNode> = setOf()
}