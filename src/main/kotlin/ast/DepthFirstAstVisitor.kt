package ast

/**
 * A depth-first visitor for ASTs
 * @tparam A argument type
 */
interface DepthFirstAstVisitor<A> {

    fun visit(node: AstNode, arg: A?)

    /**
     * Recursively perform the visit to the sub-node of the passed node, passing the provided argument.
     *
     * @param node the node whose children need to be visited
     * @param arg the argument to be passed to all sub-nodes
     */
    fun visitChildren(node: AstNode, arg: A?) {
        when (node) {
            is ACallFuncExpr -> {
                visit(node.targetFun, arg)
                node.args.forEach { visit(it, arg) }
            }
            is ABinaryOp -> {
                visit(node.left, arg)
                visit(node.right, arg)
            }
            is AUnaryOp<*> -> visit(node.target, arg)
            is AAssignStmt -> {
                visit(node.right, arg)
                visit(node.left, arg)
            }
            is ABlock -> node.body.forEach { visit(it, arg) }
            is ANestedBlockStmt -> node.body.forEach { visit(it, arg) }
            is AIfStmt -> {
                visit(node.guard, arg)
                visit(node.ifBranch, arg)
                if (node.elseBranch != null) {
                    visit(node.elseBranch, arg)
                }
            }
            is AOutputStmt -> visit(node.value, arg)
            is AReturnStmt -> visit(node.value, arg)
            is AErrorStmt -> visit(node.value, arg)
            is AVarStmt -> node.declIds.forEach { visit(it, arg) }
            is AWhileStmt -> {
                visit(node.guard, arg)
                visit(node.innerBlock, arg)
            }
            is AFunDeclaration -> {
                node.args.forEach { visit(it, arg) }
                visit(node.stmts, arg)
            }
            is AProgram -> node.funs.forEach { visit(it, arg) }
            is AAtomicExpr -> {}
            is AIdentifierDeclaration -> {} 
            else -> throw IllegalArgumentException(node.toString())
        }
    }
}