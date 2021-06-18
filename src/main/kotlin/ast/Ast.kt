package ast

import ast.AstPrinters.print
import utils.Product

object AstNodeObj {
    var lastUid: Int = 0
}

data class Loc(val line: Int, val col: Int) {
    override fun toString(): String = "${line}:${col}"
}

interface Operator
interface BinaryOperator
interface UnaryOperator

object Plus : Operator, BinaryOperator {
    override fun toString(): String = "+"
}

object Minus : Operator, BinaryOperator {
    override fun toString(): String = "-"
}

object Times : Operator, BinaryOperator {
    override fun toString(): String = "*"
}

object Divide : Operator, BinaryOperator {
    override fun toString(): String = "/"
}

object Eqq : Operator, BinaryOperator {
    override fun toString(): String = "=="
}

object GreaterThan : Operator, BinaryOperator {
    override fun toString(): String = ">"
}

object RefOp : Operator, UnaryOperator {
    override fun toString(): String = "&"
}

object DerefOp : Operator, UnaryOperator {
    override fun toString(): String = "*"
}

interface IAstNode : Product {

    val uid: Int

    val loc: Loc

    override fun toString(): String
}

/**
 * AST node.
 */
sealed class AstNode : IAstNode, Product {

    /**
     * Unique ID of the node.
     * Every new node object gets a fresh ID (but the ID is ignored in equals tests).
     */
    override val uid: Int = ++AstNodeObj.lastUid

    abstract override val loc: Loc

    override fun toString(): String = "${this.print()}:$loc"
}

//////////////// Expressions //////////////////////////

abstract class AExprOrIdentifierDeclaration : AstNode()
interface IAExpr : IAstNode
abstract class AExpr : AExprOrIdentifierDeclaration(), IAExpr
abstract class Assignable<out T> : AExpr()
interface AAtomicExpr : IAExpr
abstract class ADeclaration : AstNode()

data class ACallFuncExpr(val targetFun: AExpr, val args: List<AExpr>, val indirect: Boolean, override val loc: Loc) : AExpr() {
    override fun toString(): String = "${this.print()}:$loc"
}

data class AIdentifierDeclaration(val value: String, override val loc: Loc) : ADeclaration() {
    override fun toString(): String = "${this.print()}:$loc"
}

data class AIdentifier(val value: String, override val loc: Loc) : Assignable<Nothing>(), AAtomicExpr {
    override fun toString(): String = "${this.print()}:$loc"
}

data class ABinaryOp(val operator: BinaryOperator, val left: AExpr, val right: AExpr, override val loc: Loc) : AExpr() {
    override fun toString(): String = "${this.print()}:$loc"
}

data class AUnaryOp<out T : UnaryOperator>(val operator: T, val target: AExpr, override val loc: Loc) : Assignable<T>() {
    override fun toString(): String = "${this.print()}:$loc"
}

data class ANumber(val value: Int, override val loc: Loc) : AExpr(), AAtomicExpr {
    override fun toString(): String = "${this.print()}:$loc"
}

data class AInput(override val loc: Loc) : AExpr(), AAtomicExpr {
    override fun toString(): String = "${this.print()}:$loc"
}

data class AAlloc(override val loc: Loc) : AExpr(), AAtomicExpr {
    override fun toString(): String = "${this.print()}:$loc"
}

data class ANull(override val loc: Loc) : AExpr(), AAtomicExpr {
    override fun toString(): String = "${this.print()}:$loc"
}

//////////////// Statements //////////////////////////

interface IAStmt : IAstNode
abstract class AStmt : AstNode(), IAStmt

/**
 * A statement in the body of a nested block (cannot be a declaration or a return).
 */
abstract class AStmtInNestedBlock : AStmt()

data class AAssignStmt(val left: Assignable<DerefOp>, val right: AExpr, override val loc: Loc) : AStmtInNestedBlock() {
    override fun toString(): String = "${this.print()}:$loc"
}

interface IABlock : IAStmt {
    val body: List<AStmt>
}

abstract class ABlock : AStmt(), IABlock {

    /**
     * All the statements in the block, in order.
     */
    abstract override val body: List<AStmt>

}

data class ANestedBlockStmt(override val body: List<AStmtInNestedBlock>, override val loc: Loc) : AStmtInNestedBlock(), IABlock {
    override fun toString(): String = "${this.print()}:$loc"
}

data class AFunBlockStmt(val declarations: List<AVarStmt>, val others: List<AStmtInNestedBlock>, val ret: AReturnStmt, override val loc: Loc) : ABlock() {

    /**
     * The contents of the block, not partitioned into declarations, others and return
     */
    override val body: List<AStmt> = declarations + (others + ret)

    override fun toString(): String = "${this.print()}:$loc"
}

data class AIfStmt(val guard: AExpr, val ifBranch: AStmtInNestedBlock, val elseBranch: AStmtInNestedBlock?, override val loc: Loc) : AStmtInNestedBlock() {
    override fun toString(): String = "${this.print()}:$loc"
}

data class AOutputStmt(val value: AExpr, override val loc: Loc) : AStmtInNestedBlock() {
    override fun toString(): String = "${this.print()}:$loc"
}

data class AReturnStmt(val value: AExpr, override val loc: Loc) : AStmt() {
    override fun toString(): String = "${this.print()}:$loc"
}

data class AErrorStmt(val value: AExpr, override val loc: Loc) : AStmtInNestedBlock() {
    override fun toString(): String = "${this.print()}:$loc"
}

data class AVarStmt(val declIds: List<AIdentifierDeclaration>, override val loc: Loc) : AStmt() {
    override fun toString(): String = "${this.print()}:$loc"
}

data class AWhileStmt(val guard: AExpr, val innerBlock: AStmtInNestedBlock, override val loc: Loc) : AStmtInNestedBlock() {
    override fun toString(): String = "${this.print()}:$loc"
}

//////////////// Program and function ///////////////

data class AProgram(val funs: List<AFunDeclaration>, override val loc: Loc) : AstNode() {

    init {
        AstNodeObj.lastUid = 0
    }

    fun mainFunction(): AFunDeclaration = findMainFunction() ?: throw RuntimeException("Missing main function, declared functions are $funs")

    fun hasMainFunction(): Boolean = findMainFunction() != null

    private fun findMainFunction(): AFunDeclaration? {
        return funs.find { it.name == "main" }
    }

    override fun toString(): String = "${this.print()}:$loc"
}

data class AFunDeclaration(val name: String, val args: List<AIdentifierDeclaration>, val stmts: AFunBlockStmt, override val loc: Loc) : ADeclaration() {
    override fun toString() = "$name (${args.joinToString(",")}){...}"
}