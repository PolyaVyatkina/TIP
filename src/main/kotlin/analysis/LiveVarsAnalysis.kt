package analysis

import ast.*
import ast.AstOps.appearingIds
import cfg.*
import cfg.CfgOps.appearingIds
import lattices.IntervalLattice
import lattices.LiftLattice
import lattices.MapLattice
import lattices.PowersetLattice
import solvers.SimpleMapLatticeFixpointSolver
import solvers.SimpleWorklistFixpointSolver
import utils.MapWithDefault
import utils.withDefaultValue

/**
 * Base class for the live variables analysis
 */
abstract class LiveVarsAnalysis(cfg: IntraproceduralProgramCfg, val declData: DeclarationData) : FlowSensitiveAnalysis<CfgNode, Set<ADeclaration>>(cfg) {

    val allVars = cfg.nodes.flatMap { it.appearingIds(declData) }

    private val chNodes: (CfgNode) -> Boolean = { cfg.nodes.contains(it) }
    private val chAllVars: (ADeclaration) -> Boolean = { allVars.contains(it) }
    private val powersetLattice = PowersetLattice(chAllVars)

    override val lattice = MapLattice(chNodes, powersetLattice)

    fun transfer(n: CfgNode, s: Set<ADeclaration>): Set<ADeclaration> =
        when (n) {
            is CfgFunExitNode -> lattice.sublattice.bottom
            is CfgStmtNode ->
                when (val d = n.data) {
                    is AExpr -> s union d.appearingIds(declData)
                    is AAssignStmt -> {
                        val l = d.left
                        if (l is AIdentifier) {
                            s - l.declaration(declData) + d.right.appearingIds(declData)
                        }
                        else NoPointers.languageRestrictionViolation("$l not allowed")
                    }
                    is AVarStmt -> s subtract d.declIds.toSet()
                    is AReturnStmt -> s union d.value.appearingIds(declData)
                    is AOutputStmt -> s union d.value.appearingIds(declData)
                    else -> s
            }
            else -> s
        }
}

/**
 * Live variables analysis that uses the simple fixpoint solver.
 */
class LiveVarsAnalysisSimpleSolver(cfg: IntraproceduralProgramCfg, declData: DeclarationData)
: LiveVarsAnalysis(cfg, declData), SimpleMapLatticeFixpointSolver<CfgNode, Set<ADeclaration>>, BackwardDependencies {
    override fun analyze(): MapWithDefault<CfgNode, Set<ADeclaration>> {
        return super.analyze()
    }
}

/**
 * Live variables analysis that uses the worklist solver.
 */
class LiveVarsAnalysisWorklistSolver(cfg: IntraproceduralProgramCfg, declData: DeclarationData)
: LiveVarsAnalysis(cfg, declData), SimpleWorklistFixpointSolver<CfgNode, Set<ADeclaration>>, BackwardDependencies {
    override fun analyze(): MapWithDefault<CfgNode, Set<ADeclaration>> {
        return super.analyze()
    }

    override var x = mapOf<CfgNode, Set<ADeclaration>>().withDefaultValue(lattice.sublattice.bottom)
    override var worklist: Set<CfgNode> = setOf()
}