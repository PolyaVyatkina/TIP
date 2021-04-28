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