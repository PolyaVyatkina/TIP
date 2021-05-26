package analysis

import ast.*
import cfg.*
import lattices.IntervalLattice
import lattices.LiftLattice
import lattices.MapLattice
import lattices.PowersetLattice
import solvers.SimpleMapLatticeFixpointSolver
import solvers.SimpleWorklistFixpointSolver

/**
 * Base class for the live variables analysis
 */
abstract class LiveVarsAnalysis(cfg: IntraproceduralProgramCfg, val declData: DeclarationData) : FlowSensitiveAnalysis<CfgNode, Set<ADeclaration>>(cfg) {

    val allVars = cfg.nodes.flatMap { CfgOps.CfgNodeOps(it).appearingIds(declData) }

    private val chNodes: (CfgNode) -> Boolean = { cfg.nodes.contains(it) }
    private val chAllVars: (ADeclaration) -> Boolean = { allVars.contains(it) }
    private val powersetLattice = PowersetLattice(chAllVars)

    override val lattice = MapLattice(chNodes, powersetLattice)

    fun transfer(n: CfgNode, s: Set<ADeclaration>): Set<ADeclaration> =
        when (n) {
            is CfgFunExitNode -> lattice.sublattice.bottom
            is CfgStmtNode ->
                when (val d = n.data) {
                    is AExpr -> s union findAllIdentifiers(d).map { it.declaration(declData) }
                    is AAssignStmt -> {
                        val l = d.left
                        if (l is AIdentifier) {
                            s subtract setOf(l.declaration(declData))
                            s union  findAllIdentifiers(d.right).map { it.declaration(declData) }
                            s
                        }
                        else NoPointers.languageRestrictionViolation("$l not allowed")
                    }
                    is AVarStmt -> s subtract d.declIds.toSet()
                    is AReturnStmt -> s union findAllIdentifiers(d.value).map { it.declaration(declData) }
                    is AOutputStmt -> s union findAllIdentifiers(d.value).map { it.declaration(declData) }

                    else -> s
            }
            else -> s
        }

    private fun findAllIdentifiers(n: AExpr): Set<AIdentifier> =
        when (n) {
            is ACallFuncExpr -> {
                val s = setOf<AIdentifier>()
                n.args.forEach { s + findAllIdentifiers(it) }
                s
            }
            is ABinaryOp -> findAllIdentifiers(n.left) + findAllIdentifiers(n.right)
            is AIdentifier -> setOf(n)
            else -> setOf()
        }
}

/**
 * Live variables analysis that uses the simple fixpoint solver.
 */
class LiveVarsAnalysisSimpleSolver(cfg: IntraproceduralProgramCfg, declData: DeclarationData)
: LiveVarsAnalysis(cfg, declData), SimpleMapLatticeFixpointSolver<CfgNode, Set<ADeclaration>>, BackwardDependencies {
    override fun analyze(): Map<CfgNode, Set<ADeclaration>> {
        return super.analyze()
    }
}

/**
 * Live variables analysis that uses the worklist solver.
 */
class LiveVarsAnalysisWorklistSolver(cfg: IntraproceduralProgramCfg, declData: DeclarationData)
: LiveVarsAnalysis(cfg, declData), SimpleWorklistFixpointSolver<CfgNode, Set<ADeclaration>>, BackwardDependencies {
    override fun analyze(): Map<CfgNode, Set<ADeclaration>> {
        return super.analyze()
    }

    override var x: Map<CfgNode, Set<ADeclaration>> = mapOf()
    override var worklist: Set<CfgNode> = setOf()
}