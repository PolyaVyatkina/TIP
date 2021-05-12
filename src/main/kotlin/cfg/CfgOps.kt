package cfg

import ast.*

object CfgOps {
    /**
     * A class with convenience methods for operations on CFG nodes.
     */
    class CfgNodeOps(val n: CfgNode) {

        /**
         * Returns the set of identifiers declared by the node, including only local variables.
         */
        fun declaredVars(declData: DeclarationData): Set<ADeclaration> =
            if (n is CfgStmtNode) AstOps.AstOp(n.data).declaredLocals()
            else setOf()

        /**
         * Returns the set of identifiers declared by the node, including local variables, function parameters, and function identifiers.
         */
        fun declaredVarsAndParams(declData: DeclarationData): Set<ADeclaration> =
            when (n) {
                is CfgStmtNode -> AstOps.AstOp(n.data).declaredLocals()
                is CfgFunEntryNode -> n.data.args.toSet() + n.data
                else -> setOf()
            }

        /**
         * Returns the set of declarations of the identifiers that appear in the node.
         */
        fun appearingIds(declData: DeclarationData): Set<ADeclaration> =
            if (n is CfgStmtNode) AstOps.AstOp(n.data).appearingIds(declData)
            else setOf()

        /**
         * Returns the set of expressions that appear in the node.
         */
        fun appearingExpressions(): Set<AExpr> =
            if (n is CfgStmtNode) AstOps.AstOp(n.data).appearingExpressions()
            else setOf()

        /**
         * Returns the assignment that appears in the node, if any.
         */
        fun appearingAssignments(): AAssignStmt? =
            if (n is CfgStmtNode) {
                val d = n.data
                if (d is AAssignStmt) d
                else null
            } else null


        /**
         * Returns the set of constants appearing in the node, if any.
         */
        fun appearingConstants(): Set<ANumber> =
            if (n is CfgStmtNode) AstOps.AstOp(n.data).appearingConstants()
            else setOf()
    }
}