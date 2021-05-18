package analysis

import ast.*
import solvers.CubicSolver
import utils.Log
import javax.lang.model.type.NullType


class ControlFlowAnalysis(val program: AProgram, val declData: DeclarationData) : DepthFirstAstVisitor<NullType>, Analysis<Map<AstNode, Set<AFunDeclaration>>> {

    init {
        // Analysis does not accept pointers.
        NoPointers.assertContainsProgram(program)
    }

    val log = Log.logger(this.javaClass)

    data class Decl(val func: AFunDeclaration) {
        override fun toString(): String = "${func.name}:${func.loc}"
    }

    data class AstVariable(val n: AstNode) {
        override fun toString(): String =
            if (n is AFunDeclaration) "${n.name}:${n.loc}"
            else n.toString()
    }

    private val solver = CubicSolver<AstVariable, Decl>()

    val allFunctions = program.funs.toSet()

    /**
     * @inheritdoc
     */
    override fun analyze(): Map<AstNode, Set<AFunDeclaration>> {
        visit(program, null)
        val sol: Map<AstVariable, Set<Decl>> = solver.getSolution()
        log.info("Solution is:\n${sol.map {
            "  [[$it.key]] = {${it.value.joinToString(",")}}"
        }.joinToString( "," )
        }")
        return sol.map { vardecl ->
            vardecl.key.n to vardecl.value.map { it.func }.toSet()
        }.toMap()
    }

    /**
     * Generates the constraints for the given sub-AST.
     * @param node the node for which it generates the constraints
     * @param arg unused for this visitor
     */
    override fun visit(node: AstNode, arg: NullType?) {

        /**
         * Get the declaration if the supplied AstNode is an identifier,
         * which might be a variable declaration or a function declaration.
         * It returns the node itself, otherwise.
         */
        fun decl(n: AstNode): AstNode =
            if (n is AIdentifier) AstNodeWithDeclaration(n, declData).declaration!!
         else n

        fun AstNode.toVar(): AstVariable = AstVariable(this)

        when (node) {
            is AFunDeclaration -> TODO() //<--- Complete here
            is AAssignStmt -> {
                val id = node.left
                if (id is AIdentifier) TODO() //<--- Complete here
            }
            is ACallFuncExpr -> {
                // Simple call, resolving function name directly
                if (!node.indirect) {
                    val func = decl(node.targetFun)
                    val args = node.args
                    if (func is AFunDeclaration) {
                        // Add the constraints concerning parameters
                        func.args.zip(args).forEach {
                            solver.addSubsetConstraint(decl(it.second).toVar(), it.second.toVar())
                        }
                        // Add the constraints concerning the return
                        solver.addSubsetConstraint(decl(func.stmts.ret.value).toVar(), node.toVar())
                    }
                }
                else {
                    // Indirect call, using function pointer
                    TODO() //<--- Complete here}
                }
            }
            else -> { }
        }
        visitChildren(node, null)
    }
}