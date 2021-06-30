package ast

/**
 * Defines restrictions of TIP for the different analyses.
 */
interface TipSublanguages : DepthFirstAstVisitor<Any> {

    /**
     * Throws an exception if `prog` is not in the sub-language.
     */
    fun assertContainsProgram(prog: AProgram) = visit(prog, null)


    /**
     * Throws an exception if the AST node `n` is not in the sub-language.
     */
    fun assertContainsNode(n: AstNode) = visit(n, null)

    fun languageRestrictionViolation(message: String): Nothing =
        throw IllegalArgumentException("The TIP program is required to be in the ${this.javaClass} sub-language.\n   $message")
}

/**
 * In this sub-language, function identifiers can only be used in direct calls, and indirect calls are prohibited.
 */
data class NoFunctionPointers(val declData: DeclarationData) : TipSublanguages {

    override fun visit(node: AstNode, arg: Any?) {
        when (node) {
            is ACallFuncExpr ->
                if (node.indirect) {
                    languageRestrictionViolation("Indirect call of thr form $node are not supported")
                    node.args.forEach { visit(it, arg) }
                }
            is AIdentifier ->
                if (node.declaration(declData) is AFunDeclaration)
                    languageRestrictionViolation("Identifier $node is a function identifier not appearing in a direct call expression")
            else -> visitChildren(node, arg)
        }
    }
}

/**
 * In this sub-language, the only allowed statements are the following:
 *
 * id = alloc
 * id1 = &id2
 * id1 = id2
 * id1 = *id2
 * *id1 = id2
 * id = null
 */
object NormalizedForPointsToAnalysis : TipSublanguages {

    override fun visit(node: AstNode, arg: Any?) {
        when {
            node is AAssignStmt && node.left is AIdentifier && node.right is AAlloc -> { }
            node is AAssignStmt && node.left is AIdentifier && node.right is AUnaryOp<*> && node.right.target is AIdentifier -> { }
            node is AAssignStmt && node.left is AIdentifier && node.right is AIdentifier -> { }
            node is AAssignStmt && node.left is AUnaryOp<*> && node.left.target is AIdentifier && node.right is AIdentifier -> { }
            node is AAssignStmt && node.left is AIdentifier && node.right is ANull -> { }
            node is ABlock -> { }
            node is AVarStmt -> { }
            node is AStmt -> languageRestrictionViolation("Statement $arg is not allowed")
            else -> visitChildren(node, arg)
        }
    }
}

/**
 * In this sub-language, no pointers are allowed.
 */
object NoPointers : TipSublanguages {

    override fun visit(node: AstNode, arg: Any?) {
        if (node is AUnaryOp<*>)
            languageRestrictionViolation("Pointer operation $node is not allowed")
        visitChildren(node, arg)
    }
}

/**
 * In this sub-language, no calls are allowed.
 */
object NoCalls : TipSublanguages {

    override fun visit(node: AstNode, arg: Any?) {
        if (node is ACallFuncExpr)
            languageRestrictionViolation("Call $node is not allowed")
        visitChildren(node, arg)
    }
}

/**
 * In this sub-language, all calls are normalized, i.e. they only appear in statements of the form
 *
 * `x = f(e1, ..., en)`
 *
 * where `f` is a function identifier, `x` is a variable identifier
 * and the parameters `e_i` are atomic expressions.
 */
data class NormalizedCalls(val declData: DeclarationData) : TipSublanguages {

    override fun visit(node: AstNode, arg: Any?) {
        when {
            node is AAssignStmt && node.left is AIdentifier && node.right is ACallFuncExpr -> {
                if (!node.right.indirect && node.right.args.any { it !is AAtomicExpr })
                    languageRestrictionViolation("One of the arguments $node.right.args is not atomic")
                if (node.right.indirect) languageRestrictionViolation("Indirect call to expression ${node.right.targetFun}")
            }
            node is ACallFuncExpr -> languageRestrictionViolation("Call $node outside an assignment is not allowed")
            else -> visitChildren(node, arg)
        }
    }
}