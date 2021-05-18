package cfg

import ast.*

object IntraproceduralProgramCfgObj {

    /**
     * Converts the given CFG node into a [[cfg.FragmentCfg]].
     * No call and after-call nodes are generated.
     */
    private fun simpleNodeBuilder(): (CfgNode) -> FragmentCfg = { n ->
        when (n) {
            is CfgFunEntryNode -> FragmentCfgObj.nodeToGraph(CfgFunEntryNode(data = n.data))
            is CfgFunExitNode -> FragmentCfgObj.nodeToGraph(CfgFunExitNode(data = n.data))
            is CfgStmtNode ->
                when (val d = n.data) {
                    is AWhileStmt -> FragmentCfgObj.nodeToGraph(CfgStmtNode(data = d.guard))
                    is AIfStmt -> FragmentCfgObj.nodeToGraph(CfgStmtNode(data = d.guard))
                    else -> FragmentCfgObj.nodeToGraph(CfgStmtNode(data = d))
                }
            else -> throw IllegalArgumentException()
        }
    }

    /**
     * Generates an [[IntraproceduralProgramCfg]] from a program.
     */
    fun generateFromProgram(prog: AProgram, declData: DeclarationData): IntraproceduralProgramCfg {
        val funGraphs = FragmentCfgObj.generateFromProgram(prog, simpleNodeBuilder())
        val allEntries = funGraphs.mapValues { cfg ->
            assert(cfg.value.graphEntries.size == 1)
            cfg.value.graphEntries.first() as CfgFunEntryNode
        }
        val allExits = funGraphs.mapValues { cfg ->
            assert(cfg.value.graphExits.size == 1)
            cfg.value.graphExits.first() as CfgFunExitNode
        }
        return IntraproceduralProgramCfg(prog, allEntries, allExits)
    }
}

/**
 * Control-flow graph for a program, where function calls are represented as expressions, without using call/after-call nodes.
 */
class IntraproceduralProgramCfg(prog: AProgram, funEntries: Map<AFunDeclaration, CfgFunEntryNode>, funExits: Map<AFunDeclaration, CfgFunExitNode>) :
    ProgramCfg(prog, funEntries, funExits)