package analysis

import ast.*
import cfg.*
import cfg.CfgOps.declaredVarsAndParams
import lattices.*
import lattices.LiftLattice.Lift
import lattices.LiftLattice.Lifted
import solvers.*
import utils.MapWithDefault
import utils.withDefaultValue

private typealias Element = MapWithDefault<ADeclaration, FlatLattice.FlatElement>

/**
 * Transfer functions for sign analysis (intraprocedural only).
 */
interface IntraprocSignAnalysisFunctions {

    val declData: DeclarationData

    val domain: Set<CfgNode>

    val declaredVars: List<ADeclaration>
        get() = domain.flatMap { it.declaredVarsAndParams() }

    private val ch: (ADeclaration) -> Boolean
        get() = { declaredVars.contains(it) }

    val statelattice: MapLattice<ADeclaration, FlatLattice.FlatElement, SignLattice>
        get() = MapLattice(ch, SignLattice)

    /**
     * The transfer functions.
     */
    fun localTransfer(n: CfgNode, s: Element): Element {
        NoPointers.assertContainsNode(n.data)
        NoCalls.assertContainsNode(n.data)
        return when (n) {
            is CfgStmtNode -> {
                when (val d = n.data) {
                    // var declarations
                    is AVarStmt -> TODO() // <--- Complete here
                    // assignments
                    is AAssignStmt -> {
                        val l = d.left
                        when (l) {
                            is AUnaryOp<*> -> NoPointers.languageRestrictionViolation("${n.data} not allowed")
                            is AIdentifier -> TODO() // <--- Complete here
                            else -> throw IllegalArgumentException()
                        }
                    }
                    // all others: like no-ops
                    else -> s
                }
            }
            else -> s
        }
    }
}

/**
 * Common functionality for interprocedural analysis.
 */
interface InterprocSignAnalysisMisc<N> {

    val lattice: MapLattice<N, Lifted<Element>, LiftLattice<Element, MapLattice<ADeclaration, FlatLattice.FlatElement, SignLattice>>>

    val cfg: InterproceduralProgramCfg

    val declData: DeclarationData

    val s: MapLattice<ADeclaration, FlatLattice.FlatElement, SignLattice>
        get() = lattice.sublattice.sublattice

    fun evalArgs(
        formalParams: Collection<ADeclaration>,
        actualParams: Collection<AExpr>,
        state: Element
    ): Element =
        formalParams.zip(actualParams).fold(lattice.sublattice.sublattice.bottom) { acc, d ->
            acc + (d.first to lattice.sublattice.sublattice.sublattice.eval(d.second, state, declData))
        }
}


/**
 * Constraint functions for sign analysis (including interprocedural).
 * This version is for the basic worklist algorithm.
 */
interface InterprocSignAnalysisFunctions :
    MapLiftLatticeSolver<CfgNode, Element>,
    InterprocSignAnalysisMisc<CfgNode>,
    InterproceduralForwardDependencies {

    override fun funsub(n: CfgNode, x: Map<CfgNode, Lifted<Element>>): Lifted<Element> {
        NormalizedCalls(declData).assertContainsNode(n.data)

        return when (n) {
            // function entry nodes
            is CfgFunEntryNode -> TODO() // <--- Complete here

            // after-call nodes
            is CfgAfterCallNode -> TODO() // <--- Complete here

            // return node
            is CfgStmtNode -> {
                val d = n.data
                if (d is AReturnStmt) {
                    val j = lattice.sublattice.unlift(join(n, x))
                    val el = AstOps.returnId to lattice.sublattice.sublattice.sublattice.eval(d.value, j, declData)
                    lattice.sublattice.lift(j + el)
                }
                else super.funsub(n, x)
            }

            // call nodes (like no-ops here)
            is CfgCallNode -> join(n, x)

            // function exit nodes (like no-ops here)
            is CfgFunExitNode -> join(n, x)

            // all other nodes
            else -> super.funsub(n, x)
        }
    }
}

/**
 * Constraint functions for sign analysis (including interprocedural), propagation style.
 * This is a variant of [InterprocSignAnalysisFunctions] for use with [solvers.WorklistFixpointPropagationSolver].
 */
interface InterprocSignAnalysisFunctionsWithPropagation
    : MapLiftLatticeSolver<CfgNode, Element>,
    WorklistFixpointPropagationSolver<CfgNode, Element>,
    InterprocSignAnalysisMisc<CfgNode>,
    IntraprocSignAnalysisFunctions {

    override fun transferUnlifted(n: CfgNode, s: Element): Element {
        // helper function that propagates dataflow from a function exit node to an after-call node
        fun returnFlow(funexit: CfgFunExitNode, aftercall: CfgAfterCallNode) {
            when (val exit = x[funexit]) {
                is Lift<Element> -> {
                    val newState = lattice.sublattice.unlift(x[aftercall.pred.first()]) +
                            (cfg.AfterCallNodeContainsAssigment(aftercall).targetIdentifier.declaration(declData) to exit.n[AstOps.returnId])
                    propagate(lattice.sublattice.lift(newState), aftercall)
                }
                else -> {
                    // not (yet) any dataflow at funexit
                }
            }
        }

        return when (n) {
            // call nodes
            is CfgCallNode -> {
                cfg.IpNodeInfoCall(n).callees.forEach { entry ->
                    // build entry state and new call context, then propagate to function entry
                    val newState = lattice.sublattice.lift(evalArgs(entry.data.args, cfg.CallNodeContainsAssigment(n).invocation.args, s))
                    propagate(newState, entry)
                    // make sure existing return flow gets propagated
                    returnFlow(cfg.IpNodeInfoEntry(entry).exit, cfg.IpNodeInfoCall(n).afterCallNode)
                }
                lattice.sublattice.sublattice.bottom // no flow directly to the after-call node
            }

            // function exit nodes
            is CfgFunExitNode -> {
                for (aftercall in cfg.IpNodeInfoExit(n).callersAfterCall)
                    returnFlow(n, aftercall)
                lattice.sublattice.sublattice.bottom // no successors for this kind of node, but we have to return something
            }

            // return statement
            is CfgStmtNode -> {
                val ret = n.data as AReturnStmt
                s + (AstOps.returnId to lattice.sublattice.sublattice.sublattice.eval(ret.value, s, declData))
            }

            // function entry nodes (like no-op here)
            is CfgFunEntryNode -> s

            // after-call nodes (like no-op here)
            is CfgAfterCallNode -> s

            // all other nodes
            else -> localTransfer(n, s)
        }
    }
}

/**
 * Context-sensitive variant of [InterprocSignAnalysisFunctionsWithPropagation].
 * @tparam C type of call contexts
 */
interface ContextSensitiveSignAnalysisFunctions<C : CallContext>
    : MapLiftLatticeSolver<Pair<C, CfgNode>, Element>,
    WorklistFixpointPropagationSolver<Pair<C, CfgNode>, Element>,
    InterprocSignAnalysisMisc<Pair<C, CfgNode>>,
    IntraprocSignAnalysisFunctions,
    CallContext.CallContextFunctions<C, Element, MapLattice<ADeclaration, FlatLattice.FlatElement, SignLattice>> {

    /**
     * Collect (reverse) call edges, such that we don't have to search through the global lattice element to find the relevant call contexts.
     */
    val returnEdges: HashMap<Pair<C, CfgFunExitNode>, MutableSet<Pair<C, CfgAfterCallNode>>>

    override fun transferUnlifted(n: Pair<C, CfgNode>, s: Element): Element {

        // helper function that propagates dataflow from a function exit node to an after-call node
        fun returnflow(exitContext: C, funexit: CfgFunExitNode, callerContext: C, aftercall: CfgAfterCallNode) {
            when (val exit = x[exitContext to funexit]) {
                is Lift -> {
                    val newEl = cfg.AfterCallNodeContainsAssigment(aftercall).targetIdentifier.declaration(declData) to exit.n[AstOps.returnId]
                    val newState = lattice.sublattice.unlift(x[callerContext to aftercall.pred.first()]) + newEl
                    propagate(lattice.sublattice.lift(newState), callerContext to aftercall)
                }
                is LiftLattice.Bottom -> {
                } // not (yet) any dataflow at funexit
            }
        }

        val currentContext = n.first
        return when (val node = n.second) {
            // call nodes
            is CfgCallNode -> {
                cfg.IpNodeInfoCall(node).callees.forEach { entry ->
                    // build entry state and new call context, then propagate to function entry
                    val newState = evalArgs(entry.data.args, cfg.CallNodeContainsAssigment(node).invocation.args, s)
                    val newContext = makeCallContext(currentContext, node, newState, entry)
                    propagate(lattice.sublattice.lift(newState), newContext to entry)
                    // record the (reverse) call edge, and make sure existing return flow gets propagated
                    val exit = cfg.IpNodeInfoEntry(entry).exit
                    val afterCall = cfg.IpNodeInfoCall(node).afterCallNode
                    returnEdges[newContext to exit]?.plusAssign((currentContext to afterCall))
                    returnflow(newContext, exit, currentContext, afterCall)
                }
                lattice.sublattice.sublattice.bottom // no successors for this kind of node, but we have to return something
            }

            // function exit nodes
            is CfgFunExitNode -> {
                if (returnEdges[currentContext to node] != null)
                    returnEdges[currentContext to node]!!.forEach {
                    returnflow(currentContext, node, it.first, it.second)
                }
                lattice.sublattice.sublattice.bottom // no successors for this kind of node, but we have to return something
            }

            // return statement
            is CfgStmtNode -> {
                val d = node.data
                if (d is AReturnStmt)
                    s + (AstOps.returnId to lattice.sublattice.sublattice.sublattice.eval(d.value, s, declData))
                else localTransfer(node, s)
            }

            // function entry nodes (like no-op here)
            is CfgFunEntryNode -> s

            // after-call nodes (like no-op here)
            is CfgAfterCallNode -> s

            // all other nodes
            else -> localTransfer(node, s)
        }
    }

    override val statelattice: MapLattice<ADeclaration, FlatLattice.FlatElement, SignLattice>
        get() = super.statelattice
}

/**
 * Base class for sign analysis with simple (non-lifted) lattice.
 */
abstract class SimpleSignAnalysis(cfg: ProgramCfg) :
    FlowSensitiveAnalysis<CfgNode, Element>(cfg),
    IntraprocSignAnalysisFunctions,
    ForwardDependencies {

    private val ch: (CfgNode) -> Boolean = { cfg.nodes.contains(it) }

    override val lattice: MapLattice<CfgNode, Element, Lattice<Element>> = MapLattice(ch, statelattice)

    fun transfer(n: CfgNode, s: Element) = localTransfer(n, s)
}

/**
 * Base class for sign analysis with lifted lattice.
 */
abstract class LiftedSignAnalysis(cfg: ProgramCfg) :
    FlowSensitiveAnalysis<CfgNode, Lifted<Element>>(cfg),
    MapLatticeSolver<CfgNode, Lifted<Element>>,
    IntraprocSignAnalysisFunctions,
    ForwardDependencies {

    private val ch: (CfgNode) -> Boolean = { cfg.nodes.contains(it) }

    override val lattice = MapLattice(ch, LiftLattice(statelattice))

    open val first = cfg.funEntries.values.toSet() as Set<CfgNode>

    override fun funsub(n: CfgNode, x: Map<CfgNode, Lifted<Element>>): Lifted<Element> =
        when (n) {
            is CfgFunEntryNode -> lattice.sublattice.lift(lattice.sublattice.sublattice.bottom)
            else -> super.funsub(n, x)
        }
}

/**
 * Base class for sign analysis with context sensitivity and lifted lattice.
 */
abstract class ContextSensitiveSignAnalysis<C : CallContext>(cfg: InterproceduralProgramCfg) :
    FlowSensitiveAnalysis<Pair<C, CfgNode>, Lifted<Element>>(cfg),
    ContextSensitiveSignAnalysisFunctions<C>,
    ContextSensitiveForwardDependencies<C> {

    // in principle, we should check that the node is in the CFG, but this function is not called anyway...
    private val ch: (Pair<C, CfgNode>) -> Boolean = { true }

    override val lattice = MapLattice(ch, LiftLattice(statelattice))
}

/**
 * Intraprocedural sign analysis that uses [solvers.SimpleFixpointSolver].
 */
class IntraprocSignAnalysisSimpleSolver(cfg: ProgramCfg, override val declData: DeclarationData) :
    SimpleSignAnalysis(cfg),
    SimpleMapLatticeFixpointSolver<CfgNode, Element> {
    override fun analyze(): MapWithDefault<CfgNode, Element> = super.analyze()
}

/**
 * Intraprocedural sign analysis that uses [solvers.SimpleWorklistFixpointSolver].
 */
class IntraprocSignAnalysisWorklistSolver(cfg: ProgramCfg, override val declData: DeclarationData) :
    SimpleSignAnalysis(cfg),
    SimpleWorklistFixpointSolver<CfgNode, Element> {

    override fun analyze(): MapWithDefault<CfgNode, Element> = super.analyze()
    override var worklist: Set<CfgNode> = setOf()
    override var x = mapOf<CfgNode, Element>().withDefaultValue(lattice.sublattice.bottom)
}

/**
 * Intraprocedural sign analysis that uses [solvers.WorklistFixpointSolverWithInit],
 * with all function entries as start nodes.
 */
open class IntraprocSignAnalysisWorklistSolverWithInit(cfg: ProgramCfg, override val declData: DeclarationData) :
    LiftedSignAnalysis(cfg),
    WorklistFixpointSolverWithInit<CfgNode, Element> {

    override fun analyze(): MapWithDefault<CfgNode, Lifted<Element>> = super.analyze()

    override fun transferUnlifted(n: CfgNode, s: Element): Element = localTransfer(n, s)
    override var worklist: Set<CfgNode> =setOf()
    override var x = mapOf<CfgNode, Lifted<Element>>().withDefaultValue(lattice.sublattice.bottom)
}

/**
 * Intraprocedural sign analysis that uses [solvers.WorklistFixpointPropagationSolver].
 */
open class IntraprocSignAnalysisWorklistSolverWithInitAndPropagation(cfg: ProgramCfg, override val declData: DeclarationData) :
    IntraprocSignAnalysisWorklistSolverWithInit(cfg, declData),
    WorklistFixpointPropagationSolver<CfgNode, Element> {

    override fun analyze(): MapWithDefault<CfgNode, Lifted<Element>> = super<WorklistFixpointPropagationSolver>.analyze()
}

/**
 * Interprocedural sign analysis that uses [solvers.WorklistFixpointSolverWithInit].
 */
class InterprocSignAnalysisWorklistSolverWithInit(override val cfg: InterproceduralProgramCfg, override val declData: DeclarationData) :
    IntraprocSignAnalysisWorklistSolverWithInit(cfg, declData),
    InterprocSignAnalysisFunctions,
    InterproceduralForwardDependencies {

    override val first = setOf<CfgNode>(cfg.funEntries[cfg.program.mainFunction()]!!)
    override fun funsub(n: CfgNode, x: Map<CfgNode, Lifted<Element>>): Lifted<Element> =
        super<InterprocSignAnalysisFunctions>.funsub(n, x)

    override fun outdep(n: CfgNode): Set<CfgNode> = super<InterprocSignAnalysisFunctions>.outdep(n)

    override fun indep(n: CfgNode): Set<CfgNode> = super<InterprocSignAnalysisFunctions>.indep(n)
}

/**
 * Interprocedural sign analysis that uses [solvers.WorklistFixpointPropagationSolver].
 * Note that this class uses [analysis.ForwardDependencies] which has no interprocedural outdeps,
 * and it does not use indeps.
 */
class InterprocSignAnalysisWorklistSolverWithInitAndPropagation(override val cfg: InterproceduralProgramCfg, override val declData: DeclarationData) :
    IntraprocSignAnalysisWorklistSolverWithInitAndPropagation(cfg, declData),
    InterprocSignAnalysisFunctionsWithPropagation,
    ForwardDependencies {

    override fun transferUnlifted(n: CfgNode, s: Element): Element = super<InterprocSignAnalysisFunctionsWithPropagation>.transferUnlifted(n, s)
    override val first = setOf<CfgNode>(cfg.funEntries[cfg.program.mainFunction()]!!)
}

/**
 * Context-sensitive sign analysis with call-string approach.
 */
class CallStringSignAnalysis(override val cfg: InterproceduralProgramCfg, override val declData: DeclarationData) :
    ContextSensitiveSignAnalysis<CallStringContext>(cfg),
    CallStringFunctions<Element, MapLattice<ADeclaration, FlatLattice.FlatElement, SignLattice>> {

    override val init = Lift(lattice.sublattice.sublattice.bottom)

    override val first = setOf<Pair<CallStringContext, CfgNode>>(initialContext to cfg.funEntries[cfg.program.mainFunction()]!!)

    override val maxCallStringLength = 2 // overriding default from CallStringFunctions

    override val returnEdges =
        hashMapOf<Pair<CallStringContext, CfgFunExitNode>, MutableSet<Pair<CallStringContext, CfgAfterCallNode>>>()

    override var worklist: Set<Pair<CallStringContext, CfgNode>> = setOf()
    override var x = mapOf<Pair<CallStringContext, CfgNode>, Lifted<Element>>().withDefaultValue(lattice.sublattice.bottom)
}

/**
 * Context-sensitive sign analysis with functional approach.
 */
class FunctionalSignAnalysis(override val cfg: InterproceduralProgramCfg, override val declData: DeclarationData) :
    ContextSensitiveSignAnalysis<FunctionalContext>(cfg),
    FunctionalFunctions<Element, MapLattice<ADeclaration, FlatLattice.FlatElement, SignLattice>> {

    override val init = Lift(lattice.sublattice.sublattice.bottom)

    override val first = setOf<Pair<FunctionalContext, CfgNode>>(initialContext to cfg.funEntries[cfg.program.mainFunction()]!!)

    override val returnEdges =
        hashMapOf<Pair<FunctionalContext, CfgFunExitNode>, MutableSet<Pair<FunctionalContext, CfgAfterCallNode>>>()

    override var worklist: Set<Pair<FunctionalContext, CfgNode>> = setOf()
    override var x = mapOf<Pair<FunctionalContext, CfgNode>, Lifted<Element>>().withDefaultValue(lattice.sublattice.bottom)
}
