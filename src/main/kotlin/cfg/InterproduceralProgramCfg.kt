package cfg

import analysis.ControlFlowAnalysis
import ast.*
import ast.AstOps.containsInvocation
import utils.withDefaultValue
import java.lang.IllegalArgumentException
import javax.lang.model.type.NullType

object InterproceduralProgramCfgObj {

    /**
     * Converts the given CFG node into a [[cfg.FragmentCfg]].
     * Builds call and after-call nodes, and checks that the program is properly normalized.
     */
    private fun callreturnNodeBuilder(): (CfgNode) -> FragmentCfg = { n ->
        when (n) {
            is CfgFunEntryNode ->
                FragmentCfgObj.nodeToGraph(CfgFunEntryNode(data = n.data))
            is CfgFunExitNode ->
                FragmentCfgObj.nodeToGraph(CfgFunExitNode(data = n.data))
            is CfgStmtNode ->
                when (val d = n.data) {
                    is AWhileStmt -> {
                        assert(!d.guard.containsInvocation())
                        FragmentCfgObj.nodeToGraph(CfgStmtNode(data = d.guard))
                    }
                    is AIfStmt -> {
                        assert(!d.guard.containsInvocation())
                        FragmentCfgObj.nodeToGraph(CfgStmtNode(data = d.guard))
                    }
                    is AAssignStmt ->
                        when (val r = d.right) {
                            is ACallFuncExpr -> {
                                assert(
                                    !r.targetFun.containsInvocation()
                                            && r.args.all { !it.containsInvocation() }
                                )
                                val cnode = CfgCallNode(data = d)
                                val retnode = CfgAfterCallNode(data = d)
                                FragmentCfgObj.nodeToGraph(cnode).concat(FragmentCfgObj.nodeToGraph(retnode))
                            }
                            else -> {
                                assert(!d.containsInvocation())
                                FragmentCfgObj.nodeToGraph(CfgStmtNode(data = d))
                            }
                        }
                    else -> {
                        assert(!n.data.containsInvocation())
                        FragmentCfgObj.nodeToGraph(CfgStmtNode(data = n.data))
                    }
                }
            else -> throw IllegalArgumentException()
        }
    }

    private fun usingFunctionDeclarationCallInfo(declData: DeclarationData): (AAssignStmt) -> (Set<AFunDeclaration>) = { s ->
        if (s.right is ACallFuncExpr) {
            if (!s.right.indirect) {
                val d = declData[s.right.targetFun]
                if (d is AFunDeclaration) setOf(d)
                else NoFunctionPointers(declData).languageRestrictionViolation("${s.right.targetFun} is not a function identifier at ${s.right.loc}")
            } else
                NoFunctionPointers(declData).languageRestrictionViolation("Indirect call to ${s.right.targetFun} not supported at ${s.right.loc}")
        } else setOf()
    }

    /**
     * Generates an interprocedural CFG from a program with [[ast.NormalizedCalls]] and [[ast.NoFunctionPointers]].
     */
    fun generateFromProgram(prog: AProgram, declData: DeclarationData): InterproceduralProgramCfg {
        val funGraphs: Map<AFunDeclaration, FragmentCfg> = FragmentCfgObj.generateFromProgram(prog, callreturnNodeBuilder())
        val allEntries = funGraphs.mapValues { cfg ->
            assert(cfg.value.graphEntries.size == 1)
            cfg.value.graphEntries.first() as CfgFunEntryNode
        }
        val allExits = funGraphs.mapValues { cfg ->
            assert(cfg.value.graphExits.size == 1)
            cfg.value.graphExits.first() as CfgFunExitNode
        }

        // ensure that there are no function pointers or indirect calls
        NormalizedCalls(declData).assertContainsProgram(prog)
        NoFunctionPointers(declData).assertContainsProgram(prog)

        val callInfo = usingFunctionDeclarationCallInfo(declData)

        return InterproceduralProgramCfg(allEntries, allExits, prog, callInfo, declData)
    }

    /**
     * Generates an interprocedural CFG from a program with [[ast.NormalizedCalls]], using [[analysis.ControlFlowAnalysis]] for resolving function calls.
     */
    fun generateFromProgramWithCfa(prog: AProgram, declData: DeclarationData): InterproceduralProgramCfg {
        val funGraphs = FragmentCfgObj.generateFromProgram(prog, callreturnNodeBuilder())
        val allEntries = funGraphs.mapValues { cfg ->
            assert(cfg.value.graphEntries.size == 1)
            cfg.value.graphEntries.first() as CfgFunEntryNode
        }
        val allExits = funGraphs.mapValues { cfg ->
            assert(cfg.value.graphExits.size == 1)
            cfg.value.graphExits.first() as CfgFunExitNode
        }

        // ensure that there are no function pointers or indirect calls
        NormalizedCalls(declData).assertContainsProgram(prog)

        val cfaSolution: Map<AstNode, Set<AFunDeclaration>> = ControlFlowAnalysis(prog, declData).analyze()
        val callInfoMap: Map<AAssignStmt, Set<AFunDeclaration>> = mutableMapOf()

        // Using result of CFA to build callInfo
        object : DepthFirstAstVisitor<NullType> {
            override fun visit(node: AstNode, arg: NullType?) {
                if (node is AAssignStmt && node.right is ACallFuncExpr && node.right.targetFun is AIdentifier) {
                    val decl = node.right.targetFun.declaration(declData) as AFunDeclaration
                    callInfoMap.plus(node to cfaSolution[decl])
                }
                else visitChildren(node, arg)
            }
        }.visit(prog, null)

        val callInfo: (AAssignStmt) -> (Set<AFunDeclaration>) = { s: AAssignStmt -> callInfoMap[s]!! }

        return InterproceduralProgramCfg(allEntries, allExits, prog, callInfo, declData)
    }
}

/**
 * Interprocedural control-flow graph for a program, where function calls are represented using call/after-call nodes.
 * Requires the program to be normalized using [[ast.NormalizedCalls]], i.e. all calls are of the form x = f(..).
 *
 * @param funEntries map from AST function declarations to corresponding CFG function entry nodes
 * @param funExits map from AST function declarations to corresponding CFG function exit nodes
 * @param program the AST of the program
 * @param declData the declaration data
 * @param callInfo call graph
 */
class InterproceduralProgramCfg(
    override val funEntries: Map<AFunDeclaration, CfgFunEntryNode>,
    override val funExits: Map<AFunDeclaration, CfgFunExitNode>,
    val program: AProgram,
    val callInfo: (AAssignStmt) -> (Set<AFunDeclaration>),
    val declData: DeclarationData
                                ) : ProgramCfg(program, funEntries, funExits) {

    init {
        // Check the calls are normalized
        NormalizedCalls(declData).assertContainsProgram(program)
        initdeps()
    }
    val graph = this

    /**
     * The node corresponding to entry of the main function.
     */
    val programEntry = funEntries[program.mainFunction]

    /**
     * Map from [[cfg.CfgFunEntryNode]] to the set of [[cfg.CfgCallNode]]s calling the function.
     */
    val callers = mutableMapOf<CfgFunEntryNode, Set<CfgCallNode>>().withDefaultValue(setOf())

    /**
     * Map from [[cfg.CfgCallNode]] to the set of [[cfg.CfgFunEntryNode]] of the called functions.
     */
    val callees = mutableMapOf<CfgCallNode, Set<CfgFunEntryNode>>().withDefaultValue(setOf())

    /**
     * Map from [[cfg.CfgAfterCallNode]] to the set of [[cfg.CfgFunExitNode]] of the called functions.
     */
    val calleeExits = mutableMapOf<CfgAfterCallNode, Set<CfgFunExitNode>>().withDefaultValue(setOf())

    /**
     * Map from [[cfg.CfgFunExitNode]] to the set of [[cfg.CfgAfterCallNode]]s of the calling functions.
     */
    val callerAfterCalls = mutableMapOf<CfgFunExitNode, Set<CfgAfterCallNode>>().withDefaultValue(setOf())

    /**
     * Map from [[cfg.CfgNode]] to the enclosing function entry node.
     */
    var enclosingFunctionEntry = mapOf<CfgNode, CfgFunEntryNode>()

    private fun initdeps() {
        nodes.forEach {
            when (it) {
                is CfgCallNode -> {
                    val invoked = callInfo(it.data)
                    val entries = invoked.map { d -> funEntries[d]!! }
                    for (entry in entries) {
                        callers += entry to (callers[entry] + it)
                        callees += it to (callees[it] + entry)
                    }
                }
                is CfgAfterCallNode -> {
                    val invoked = callInfo(it.data)
                    val exits = invoked.map { d -> funExits[d]!! }
                    for (exit in exits) {
                        callerAfterCalls += exit to (callerAfterCalls[exit] + it)
                        calleeExits += it to (calleeExits[it] + exit)
                    }
                }
                else -> {
                }
            }
        }
        enclosingFunctionEntry = functionNodes.map { it.value.first() to it.key }.toMap()
    }

    /**
     * Maps a function to the set of its nodes.
     */
    private val functionNodes: Map<CfgFunEntryNode, Set<CfgNode>> =
        funEntries.values.associateWith { entry -> nodesRec(entry).toSet() }


    /**
     * A class with convenience methods for CFG entry node operations that involve the whole-program CFG.
     */
    inner class IpNodeInfoEntry(nd: CfgFunEntryNode) {

        /**
         * Returns the set of [[cfg.CfgCallNode]]s of the called functions.
         */
        val callers: Set<CfgCallNode> = graph.callers[nd]

        /**
         * Returns the exit node of the function associated with this entry node
         */
        val exit: CfgFunExitNode = funExits[nd.data]!!
    }

    /**
     * A class with convenience methods for CFG call node operations that involve the whole-program CFG.
     */
    inner class IpNodeInfoCall(nd: CfgCallNode) {

        /**
         * Returns the set of [[cfg.CfgFunEntryNode]] of the called functions.
         */
        val callees: Set<CfgFunEntryNode> = graph.callees[nd]

        /**
         * Returns the after-call node of this call node.
         */
        val afterCallNode: CfgAfterCallNode = nd.succ.first() as CfgAfterCallNode
    }

    /**
     * A class with convenience methods for CFG after call node operations that involve the whole-program CFG.
     */
    inner class IpNodeInfoAfterCall(nd: CfgAfterCallNode) {

        /**
         * Returns the [[cfg.CfgFunExitNode]] of the called function.
         */
        val calledExit: Set<CfgFunExitNode> = graph.calleeExits[nd]

        /**
         * Returns the call node of this after call node
         */
        val callNode: CfgCallNode = nd.pred.first() as CfgCallNode

    }

    /**
     * A class with convenience methods for CFG exit node operations that involve the whole-program CFG.
     */
    inner class IpNodeInfoExit(nd: CfgFunExitNode) {

        /**
         * Map from [[cfg.CfgFunExitNode]] to the set of [[cfg.CfgAfterCallNode]]s of the calling function.
         */
        val callersAfterCall: Set<CfgAfterCallNode> = graph.callerAfterCalls[nd]


        /**
         * Returns the entry node of the function associated with this exit node
         */
        val entry: CfgFunEntryNode = funEntries[nd.data]!!
    }

    inner class CallNodeContainsAssigment(nd: CfgCallNode) {

        val targetIdentifier: AIdentifier =
            if (nd.data.left is AIdentifier) nd.data.left
            else throw IllegalArgumentException("Expected left-hand-side of call assignment to be an identifier")

        val assignment: AAssignStmt = nd.data

        val invocation: ACallFuncExpr =
            if (this.assignment.right is ACallFuncExpr) this.assignment.right
            else throw IllegalArgumentException("Expected right-hand-side of call assignment to be a call")

        val invokedFunctionIdentifier: AIdentifier =
            if (this.assignment.right is ACallFuncExpr && this.assignment.right.targetFun is AIdentifier && !this.assignment.right.indirect)
                this.assignment.right.targetFun
            else throw IllegalArgumentException("Expected direct call at call assignment")

    }

    inner class AfterCallNodeContainsAssigment(nd: CfgAfterCallNode) {

        val targetIdentifier: AIdentifier =
            if (nd.data.left is AIdentifier) nd.data.left
            else throw IllegalArgumentException("Expected left-hand-side of call assignment to be an identifier")


        val assignment: AAssignStmt = nd.data


        val invocation: ACallFuncExpr =
            if (this.assignment.right is ACallFuncExpr) this.assignment.right
            else throw IllegalArgumentException("Expected right-hand-side of call assignment to be a call")


        val invokedFunctionIdentifier: AIdentifier =
            if (this.assignment.right is ACallFuncExpr && this.assignment.right.targetFun is AIdentifier && !this.assignment.right.indirect)
                this.assignment.right.targetFun
            else throw IllegalArgumentException("Expected direct call at call assignment")

    }
}
