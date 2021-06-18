package analysis

import ast.*
import cfg.*
import cfg.CfgOps.appearingConstants
import cfg.CfgOps.declaredVars
import lattices.IntervalLattice
import lattices.IntervalLattice.*
import lattices.LiftLattice
import lattices.LiftLattice.Lifted
import lattices.MapLattice
import solvers.MapLatticeSolver
import solvers.WorklistFixpointSolverWithInit
import solvers.WorklistFixpointSolverWithInitAndSimpleWidening
import solvers.WorklistFixpointSolverWithInitAndSimpleWideningAndNarrowing
import utils.MapWithDefault
import utils.withDefaultValue

/**
 * The base class for interval analysis.
 */
abstract class IntervalAnalysis(cfg: FragmentCfg, val declData: DeclarationData) : FlowSensitiveAnalysis<CfgNode, Lifted<MapWithDefault<ADeclaration, Pair<Num, Num>>>>(cfg),
    MapLatticeSolver<CfgNode, Lifted<MapWithDefault<ADeclaration, Pair<Num, Num>>>> {

    val declaredVars = cfg.nodes.flatMap { it.declaredVars() }

    private val chNodes: (CfgNode) -> Boolean = { cfg.nodes.contains(it) }
    private val chDeclaredVars: (ADeclaration) -> Boolean = { declaredVars.contains(it) }

    override val lattice = MapLattice(chNodes, LiftLattice(MapLattice(chDeclaredVars, IntervalLattice())))

    fun transferUnlifted(n: CfgNode, s: MapWithDefault<ADeclaration, Pair<Num, Num>>): MapWithDefault<ADeclaration, Pair<Num, Num>> =
        when (n) {
            is CfgStmtNode ->
                when (val d = n.data) {
                    is AVarStmt ->
                        d.declIds.fold(s) { acc, id ->
                            acc + (id to lattice.sublattice.sublattice.sublattice.FullInterval)
                        }
                    is AAssignStmt -> {
                        val l = d.left
                        if (l is AIdentifier) {
                            val vdef = l.declaration(declData)
                            s + (vdef to absEval(d.right, s))
                        } else throw IllegalArgumentException()
                    }
                    else -> s
                }
            else -> s
        }

    override fun funsub(
        n: CfgNode,
        x: Map<CfgNode, Lifted<MapWithDefault<ADeclaration, Pair<Num, Num>>>>
    ): Lifted<MapWithDefault<ADeclaration, Pair<Num, Num>>> =
        // function entry nodes are always reachable
        if (n is CfgFunEntryNode) lattice.sublattice.lift(lattice.sublattice.sublattice.bottom)
        // all other nodes are processed with join+transfer
        else super.funsub(n, x)

    /**
     * The abstract evaluation function for an expression.
     * @param exp the expression
     * @param env the current abstract environment
     * @return the result of the evaluation
     */
    private fun absEval(exp: AExpr, env: Map<ADeclaration, Pair<Num, Num>>): Pair<Num, Num> =
        when (exp) {
            is AIdentifier -> env[exp.declaration(declData)]!!
            is ANumber -> IntNum(exp.value) to IntNum(exp.value)
            is ABinaryOp -> {
                val left = absEval(exp.left, env)
                val right = absEval(exp.right, env)
                when (exp.operator) {
                    Eqq -> lattice.sublattice.sublattice.sublattice.eqq(left, right)
                    GreaterThan -> lattice.sublattice.sublattice.sublattice.gt(left, right)
                    Divide -> lattice.sublattice.sublattice.sublattice.div(left, right)
                    Minus -> lattice.sublattice.sublattice.sublattice.minus(left, right)
                    Plus -> lattice.sublattice.sublattice.sublattice.plus(left, right)
                    Times -> lattice.sublattice.sublattice.sublattice.times(left, right)
                    else -> throw IllegalArgumentException()
                }
            }
            is AInput -> lattice.sublattice.sublattice.sublattice.FullInterval
            else -> throw IllegalArgumentException()
        }
}

/**
 * Interval analysis, using the worklist solver with init and widening.
 */
class IntervalAnalysisWorklistSolverWithInit(cfg: ProgramCfg, declData: DeclarationData) : IntervalAnalysis(cfg, declData),
    WorklistFixpointSolverWithInit<CfgNode, MapWithDefault<ADeclaration, Pair<Num, Num>>>, ForwardDependencies {

    override val first = cfg.funEntries.values.toSet() as Set<CfgNode>

    override fun analyze(): MapWithDefault<CfgNode, Lifted<MapWithDefault<ADeclaration, Pair<Num, Num>>>> =
        super.analyze()

    override var x = mapOf<CfgNode, Lifted<MapWithDefault<ADeclaration, Pair<Num, Num>>>>().withDefaultValue(lattice.sublattice.bottom)
    override var worklist: Set<CfgNode> = setOf()
}

/**
 * Interval analysis, using the worklist solver with init and widening.
 */
open class IntervalAnalysisWorklistSolverWithWidening(val cfg: ProgramCfg, declData: DeclarationData) : IntervalAnalysis(cfg, declData),
    WorklistFixpointSolverWithInitAndSimpleWidening<CfgNode, MapWithDefault<ADeclaration, Pair<Num, Num>>>, ForwardDependencies {

    override val first = cfg.funEntries.values.toSet() as Set<CfgNode>

    override fun analyze(): MapWithDefault<CfgNode, Lifted<MapWithDefault<ADeclaration, Pair<Num, Num>>>> =
        super.analyze()

    override var x = mapOf<CfgNode, Lifted<MapWithDefault<ADeclaration, Pair<Num, Num>>>>().withDefaultValue(lattice.sublattice.bottom)

    /**
     * Int values occurring in the program, plus -infinity and +infinity.
     */
    private val B = cfg.nodes.flatMap { n ->
        n.appearingConstants().map { x ->
            IntNum(x.value)
        } + MInf + PInf
    }.toSet()

    override fun backedge(src: CfgNode, dst: CfgNode): Boolean = cfg.rank()[src]!! > cfg.rank()[dst]!!

    private fun minB(b: Num) = B.filter { b <= it }.minOrNull()

    private fun maxB(a: Num) = B.filter { it <= a }.maxOrNull()

    override fun widen(s: Lifted<MapWithDefault<ADeclaration, Pair<Num, Num>>>): Lifted<MapWithDefault<ADeclaration, Pair<Num, Num>>> =
        when (s) {
            is LiftLattice.Bottom<*> -> s
            is LiftLattice.Lift<MapWithDefault<ADeclaration, Pair<Num, Num>>> -> {
                val m = s.n
                for (i in m) {
                    m[i.key] = maxB(i.value.first)!! to minB(i.value.second)!!
                }
                lattice.sublattice.lift(m)
            }
            else -> throw IllegalArgumentException()
        }

    override var worklist: Set<CfgNode> = setOf()
}

/**
 * Interval analysis, using the worklist solver with init, widening, and narrowing.
 */
class IntervalAnalysisWorklistSolverWithWideningAndNarrowing(cfg: ProgramCfg, declData: DeclarationData): IntervalAnalysisWorklistSolverWithWidening(cfg, declData),
    WorklistFixpointSolverWithInitAndSimpleWideningAndNarrowing<CfgNode, MapWithDefault<ADeclaration, Pair<Num, Num>>> {

    override val narrowingSteps = 5

    override fun analyze(): MapWithDefault<CfgNode, Lifted<MapWithDefault<ADeclaration, Pair<Num, Num>>>> =
        super<IntervalAnalysisWorklistSolverWithWidening>.analyze()
}