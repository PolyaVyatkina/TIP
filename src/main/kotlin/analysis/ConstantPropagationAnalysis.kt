package analysis

import ast.*
import cfg.CfgNode
import cfg.CfgOps.declaredVars
import cfg.CfgStmtNode
import cfg.IntraproceduralProgramCfg
import lattices.FlatLattice
import lattices.FlatLattice.*
import lattices.MapLattice
import solvers.SimpleMapLatticeFixpointSolver
import solvers.SimpleWorklistFixpointSolver
import utils.MapWithDefault
import utils.withDefaultValue

abstract class ConstantPropagationAnalysis(cfg: IntraproceduralProgramCfg, val declData: DeclarationData) :
    FlowSensitiveAnalysis<CfgNode, MapWithDefault<ADeclaration, FlatElement>>(cfg) {

    val declaredVars: Set<ADeclaration> = cfg.nodes.flatMap { it.declaredVars() }.toSet()

    private val chNodes: (CfgNode) -> Boolean = { cfg.nodes.contains(it) }
    private val chDeclaredVars: (ADeclaration) -> Boolean = { declaredVars.contains(it) }

    override val lattice = MapLattice(chNodes, MapLattice(chDeclaredVars, FlatLattice<Int>()))

    fun transfer(n: CfgNode, s: MapWithDefault<ADeclaration, FlatElement>): MapWithDefault<ADeclaration, FlatElement> =
        when (n) {
            is CfgStmtNode ->
                when (val d = n.data) {
                    is AVarStmt ->
                        d.declIds.fold(s) { acc, id ->
                            acc + (id to Top)
                        }
                    is AAssignStmt -> {
                        when (val l = d.left) {
                            is AIdentifier -> {
                                val vdef = l.declaration(declData)
                                s + (vdef to absEval(d.right, s))
                            }
                            else -> throw IllegalArgumentException()
                        }
                    }
                    else -> s
                }
            else -> s
        }

    private fun absEval(exp: AExpr, env: Map<ADeclaration, FlatElement>): FlatElement =
        when (exp) {
            is AIdentifier -> env[exp.declaration(declData)]!!
            is ANumber -> FlatEl(exp.value)
            is ABinaryOp -> {
                val left = absEval(exp.left, env)
                val right = absEval(exp.right, env)
                when {
                    left is FlatEl<*> && right is FlatEl<*> -> {
                        val x = left.el as Int
                        val y = right.el as Int
                        when (exp.operator) {
                            Eqq -> FlatEl(if (x == y) 1 else 0)
                            Divide -> if (y != 0) FlatEl(x / y) else Top
                            GreaterThan -> FlatEl(if (x > y) 1 else 0)
                            Minus -> FlatEl(x - y)
                            Plus -> FlatEl(x + y)
                            Times -> FlatEl(x * y)
                            else -> throw IllegalArgumentException()
                        }
                    }
                    left is Bot -> Bot
                    right is Bot -> Bot
                    right is Top -> Top
                    left is Top -> Top
                    else -> throw IllegalArgumentException()
                }
            }
            is AInput -> Top
            else -> throw IllegalArgumentException()
        }
}

/**
 * Constant propagation analysis that uses the simple fipoint solver.
 */
class ConstantPropagationAnalysisSimpleSolver(cfg: IntraproceduralProgramCfg, declData: DeclarationData) : ConstantPropagationAnalysis(cfg, declData),
    SimpleMapLatticeFixpointSolver<CfgNode, MapWithDefault<ADeclaration, FlatElement>>, ForwardDependencies {
    override fun analyze(): MapWithDefault<CfgNode, MapWithDefault<ADeclaration, FlatElement>> {
        return super.analyze()
    }
}

/**
 * Constant propagation analysis that uses the worklist solver.
 */
class ConstantPropagationAnalysisWorklistSolver(cfg: IntraproceduralProgramCfg, declData: DeclarationData) : ConstantPropagationAnalysis(cfg, declData),
    SimpleWorklistFixpointSolver<CfgNode, MapWithDefault<ADeclaration, FlatElement>>, ForwardDependencies {
    override fun analyze(): MapWithDefault<CfgNode, MapWithDefault<ADeclaration, FlatElement>> {
        return super.analyze()
    }

    override var x = mapOf<CfgNode, MapWithDefault<ADeclaration, FlatElement>>().withDefaultValue(lattice.sublattice.bottom)
    override var worklist: Set<CfgNode> = setOf()
}