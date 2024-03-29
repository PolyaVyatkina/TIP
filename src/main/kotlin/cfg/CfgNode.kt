package cfg

import ast.AAssignStmt
import ast.AFunDeclaration
import ast.AstNode

object CfgNodeObj {
    var lastUid = 0

    val uid: Int
        get() = ++lastUid
}

/**
 * Node in a control-flow graph.
 */
abstract class CfgNode {

    /**
     * Predecessors of the node.
     */
    abstract val pred: MutableSet<CfgNode>

    /**
     * Successors of the node.
     */
    abstract val succ: MutableSet<CfgNode>

    /**
     * Unique node ID.
     */
    abstract val id: Int

    /**
     * The AST node contained by this node.
     */
    abstract val data: AstNode

    override fun equals(other: Any?): Boolean =
        if (other is CfgNode) other.id == this.id
        else false

    override fun hashCode() = id
}

/**
 * Node in a CFG representing a program statement.
 * The `data` field holds the statement, or in case of if/while instructions, the branch condition.
 */
class CfgStmtNode(
    override val pred: MutableSet<CfgNode> = mutableSetOf(),
    override val succ: MutableSet<CfgNode> = mutableSetOf(),
    override val id: Int = CfgNodeObj.uid,
    override val data: AstNode
) : CfgNode() {
    override fun toString() = "[Stmt] $data"
}

/**
 * Node in a CFG representing a function call.
 * The `data` field holds the assignment statement where the right-hand-side is the function call.
 */
class CfgCallNode(
    override val pred: MutableSet<CfgNode> = mutableSetOf(),
    override val succ: MutableSet<CfgNode> = mutableSetOf(),
    override val id: Int = CfgNodeObj.uid,
    override val data: AAssignStmt
) : CfgNode() {
    override fun toString() = "[Call] $data"
}

/**
 * Node in a CFG representing having returned from a function call.
 * The `data` field holds the assignment statement where the right-hand-side is the function call.
 */
class CfgAfterCallNode(
    override val pred: MutableSet<CfgNode> = mutableSetOf(),
    override val succ: MutableSet<CfgNode> = mutableSetOf(),
    override val id: Int = CfgNodeObj.uid,
    override val data: AAssignStmt
) : CfgNode() {
    override fun toString() = "[AfterCall] $data"
}

/**
 * Node in a CFG representing the entry of a function.
 */
class CfgFunEntryNode(
    override val pred: MutableSet<CfgNode> = mutableSetOf(),
    override val succ: MutableSet<CfgNode> = mutableSetOf(),
    override val id: Int = CfgNodeObj.uid,
    override val data: AFunDeclaration
) : CfgNode() {
    override fun toString() = "[FunEntry] $data"
}

/**
 * Node in a CFG representing the exit of a function.
 */
class CfgFunExitNode(
    override val pred: MutableSet<CfgNode> = mutableSetOf(),
    override val succ: MutableSet<CfgNode> = mutableSetOf(),
    override val id: Int = CfgNodeObj.uid,
    override val data: AFunDeclaration
) : CfgNode() {
    override fun toString() = "[FunExit] $data"
}