package ast

import utils.productIterator
import javax.lang.model.type.NullType

/**
 * Convenience operations related to ASTs.
 */
object AstOps {

    /**
     * Special 'result' variable, for function return values.
     */
    val returnId = AIdentifierDeclaration("#result", Loc(0, 0))

    /**
     * Checks whether the subtree of the node contains function calls.
     */
    fun AstNode.containsInvocation(): Boolean {
        var found = false
        val invocationFinder = object : DepthFirstAstVisitor<NullType> {
            override fun visit(node: AstNode, arg: NullType?) {
                if (node is ACallFuncExpr) found = true
                else visitChildren(node, null)
            }

        }
        invocationFinder.visit(this, null)
        return found
    }

    /**
     * Returns the set of local variable identifiers declared by the node (excluding function parameters and function identifiers).
     */
    fun AstNode.declaredLocals(): Set<ADeclaration> =
        if (this is AVarStmt) this.declIds.toSet()
        else setOf()

    /**
     * Returns the set of identifier declarations appearing in the subtree of the node.
     */
    fun AstNode.appearingIds(declData: DeclarationData): Set<ADeclaration> {
        val ids = mutableSetOf<ADeclaration>()
        val idFinder = object : DepthFirstAstVisitor<NullType> {
            override fun visit(node: AstNode, arg: NullType?) {
                when (node) {
                    is AIdentifier -> {
                        val d = node.declaration(declData)
                        if (d is AIdentifierDeclaration || d is AFunDeclaration) ids += d
                    }
                    is AIdentifierDeclaration -> ids += node
                    is AFunDeclaration -> ids += node
                    else -> {
                    }
                }
                visitChildren(node, null)
            }
        }
        idFinder.visit(this, null)
        return ids
    }

    /**
     * Returns the set of allocs appearing in the subtree of the node.
     */
    fun AstNode.appearingAllocs(): Set<AAlloc> {
        val allocs = mutableSetOf<AAlloc>()
        val allocsFinder = object : DepthFirstAstVisitor<NullType> {
            override fun visit(node: AstNode, arg: NullType?) {
                if (node is AAlloc) allocs += node
                else visitChildren(node, null)
            }
        }
        allocsFinder.visit(this, null)
        return allocs
    }

    /**
     * Returns the set of constants appearing in the subtree of the node.
     */
    fun AstNode.appearingConstants(): Set<ANumber> {
        val numbers = mutableSetOf<ANumber>()
        val numFinder = object : DepthFirstAstVisitor<NullType> {
            override fun visit(node: AstNode, arg: NullType?) {
                if (node is ANumber) numbers += node
                else visitChildren(node, null)
            }
        }
        numFinder.visit(this, null)
        return numbers
    }

    /**
     * Returns the set of expressions appearing in the subtree of the node.
     */
    fun AstNode.appearingExpressions(): Set<AExpr> {
        val exps = mutableSetOf<AExpr>()
        val expFinder = object : DepthFirstAstVisitor<NullType> {
            override fun visit(node: AstNode, arg: NullType?) {
                if (node is ABinaryOp) exps += node
                visitChildren(node, null)
            }
        }
        expFinder.visit(this, null)
        return exps
    }

    /**
     * A class for lifting the given [[AstNode]] `n` into an unlabelled one.
     * The unlabelled node represents `n` but without the source code location.
     * Thereby two nodes become "equal" if they are structurally the same even though they appear in different parts of the code.
     * Identifiers are compared using their declarations.
     */
    class UnlabelledNode<X : AstNode>(val n: X, val declData: DeclarationData) {

        /**
         * The members of the node, excluding `loc`.
         */
        val nonLocMembers by lazy { n.productIterator().asSequence().filter { x -> x !is Loc }.toList() }

        /**
         * Compares objects structurally, but ignores `loc`.
         * Identifiers are compared using their declarations.
         */
        override fun equals(other: Any?): Boolean =
            when (other) {
                is UnlabelledNode<*> -> {
                    if (this.n is AIdentifier && other.n is AIdentifier)
                        this.n.declaration(declData) == other.n.declaration(declData)
                    else {
                        if (this.javaClass != n.javaClass)
                            false
                        else {
                            this.nonLocMembers.zip(UnlabelledNode(n, declData).nonLocMembers).fold(true) { a, p ->
                                if (p.first is AstNode && p.second is AstNode) {
                                    val p1 = UnlabelledNode(p.first as AstNode, declData)
                                    val p2 = UnlabelledNode(p.second as AstNode, declData)
                                    a && p1 == p2
                                } else a && p.first == p.second
                            }
                        }
                    }
                }
                else -> false
            }

        override fun hashCode(): Int = nonLocMembers.map {
            if (it is AIdentifier) it.declaration(declData).hashCode()
            else it.hashCode()
        }.fold(n.javaClass.hashCode()) { m, el -> m * el }

        override fun toString(): String = n.toString()
    }
}
