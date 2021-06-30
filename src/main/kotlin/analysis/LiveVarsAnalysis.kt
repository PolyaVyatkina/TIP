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

    fun transfer(n: CfgNode, s: Set<ADeclaration>): Set<ADeclaration> {
        println(s)
        return when (n) {
            is CfgFunExitNode -> TODO() // <--- Complete here
            is CfgStmtNode ->
                when (val d = n.data) {
                    is AExpr -> TODO() // <--- Complete here
                    is AAssignStmt -> {
                        val l = d.left
                        if (l is AIdentifier) TODO() // <--- Complete here
                        else NoPointers.languageRestrictionViolation("$l not allowed")
                    }
                    is AVarStmt -> TODO() // <--- Complete here
                    is AReturnStmt -> TODO() // <--- Complete here
                    is AOutputStmt -> TODO() // <--- Complete here
                    else -> s
                }
            else -> s
        }
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