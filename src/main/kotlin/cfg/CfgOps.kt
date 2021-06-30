package cfg

import ast.*
import ast.AstOps.appearingConstants
import ast.AstOps.appearingExpressions
import ast.AstOps.appearingIds
import ast.AstOps.declaredLocals

object CfgOps {

    /**
     * Returns the set of identifiers declared by the node, including only local variables.
     */
    fun CfgNode.declaredVars(): Set<ADeclaration> =
        if (this is CfgStmtNode) this.data.declaredLocals()
        else setOf()

    /**
     * Returns the set of identifiers declared by the node, including local variables, function parameters, and function identifiers.
     */
    fun CfgNode.declaredVarsAndParams(): Set<ADeclaration> =
        when (this) {
            is CfgStmtNode -> this.data.declaredLocals()
            is CfgFunEntryNode -> this.data.args.toSet() + this.data
            else -> setOf()
        }

    /**
     * Returns the set of declarations of the identifiers that appear in the node.
     */
    fun CfgNode.appearingIds(declData: DeclarationData): Set<ADeclaration> =
        if (this is CfgStmtNode) this.data.appearingIds(declData)
        else setOf()

    /**
     * Returns the set of expressions that appear in the node.
     */
    fun CfgNode.appearingExpressions(): Set<AExpr> =
        if (this is CfgStmtNode) this.data.appearingExpressions()
        else setOf()

    /**
     * Returns the assignment that appears in the node, if any.
     */
    fun CfgNode.appearingAssignments(): AAssignStmt? =
        if (this is CfgStmtNode) {
            val d = this.data
            if (d is AAssignStmt) d
            else null
        } else null


    /**
     * Returns the set of constants appearing in the node, if any.
     */
    fun CfgNode.appearingConstants(): Set<ANumber> =
        if (this is CfgStmtNode) this.data.appearingConstants()
        else setOf()

}