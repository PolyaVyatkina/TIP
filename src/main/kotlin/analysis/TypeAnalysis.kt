package analysis

import ast.*
import solvers.Term
import solvers.UnionFindSolver
import types.*
import types.TipTypeObj.ast2typevar
import utils.Log
import javax.lang.model.type.NullType

/**
 * Unification-based type analysis.
 * The analysis associates a [types.TipType] with each variable declaration and expression node in the AST.
 * It is implemented using [solvers.UnionFindSolver].
 */
class TypeAnalysis(val program: AProgram, val declData: DeclarationData) : DepthFirstAstVisitor<NullType>, Analysis<TypeData> {

    val log = Log.logger(this.javaClass)
    val solver = UnionFindSolver<TipType>()

    override fun analyze(): TypeData {
        // generate the constraints by traversing the AST and solve them on-the-fly
        visit(program, null)

        var ret: TypeData = mutableMapOf()
        // close the terms and create the TypeData
        object : DepthFirstAstVisitor<NullType> {
            val sol = solver.solution()
            init {
                visit(program, null)
            }

            // extract the type for each identifier declaration and each non-identifier expression
            override fun visit(node: AstNode, arg: NullType?) {
                if (node is AExpr || node is ADeclaration)
                    ret = ret.plus(node to (TipTypeOps.close(TipVar(node), sol) as TipType))
                visitChildren(node, null)
            }
        }

        log.info("Inferred types are:\n${ret.map { "  [[${it.key}]] = ${it.value}" }.joinToString("\n")}")
        return ret
    }

    /**
     * Generates the constraints for the given sub-AST.
     * @param node the node for which it generates the constraints
     * @param arg unused for this visitor
     */
    override fun visit(node: AstNode, arg: NullType?) {
        log.verb("Visiting ${node.javaClass.name} at ${node.loc}")
        when (node) {
            is AProgram -> TODO() // <--- Complete here
            is ANumber -> TODO() // <--- Complete here
            is AInput -> TODO() // <--- Complete here
            is AIfStmt -> TODO() // <--- Complete here
            is AOutputStmt -> TODO() // <--- Complete here
            is AWhileStmt -> TODO() // <--- Complete here
            is AAssignStmt -> TODO() // <--- Complete here
            is ABinaryOp -> {
                when (node.operator) {
                    is Eqq -> TODO() // <--- Complete here
                    else -> TODO() // <--- Complete here
                }
            }
            is AUnaryOp<*> -> {
                when (node.operator) {
                    is RefOp -> TODO() // <--- Complete here
                    is DerefOp -> TODO() // <--- Complete here
                }
            }
            is AAlloc -> TODO() // <--- Complete here
            is ANull -> TODO() // <--- Complete here
            is AFunDeclaration -> TODO() // <--- Complete here
            is ACallFuncExpr -> TODO() // <--- Complete here
            else -> {}
        }
        visitChildren(node, null)
    }
}