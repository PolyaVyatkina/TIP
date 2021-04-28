package analysis

import ast.*
import org.jetbrains.annotations.Nullable
import solvers.Term
import solvers.UnionFindSolver
import types.*
import types.TipTypeObj.ast2typevar
import utils.Log
import javax.lang.model.type.NullType

/**
 * Unification-based type analysis.
 * The analysis associates a [[types.TipType]] with each variable declaration and expression node in the AST.
 * It is implemented using [[solvers.UnionFindSolver]].
 */
class TypeAnalysis(val program: AProgram, val declData: DeclarationData) : DepthFirstAstVisitor<NullType>, Analysis<TypeData> {

    val log = Log.logger(this.javaClass)
    val solver = UnionFindSolver<TipType>()

    override fun analyze(): TypeData {
        // generate the constraints by traversing the AST and solve them on-the-fly
        visit(program, null)
        val s = solver.solution()
        log.info("Solution is:\n${s.map { "  [[${it.key}]] = ${it.value}" }.joinToString("\n")}")

        var ret: TypeData = mutableMapOf()
        // close the terms and create the TypeData
        object : DepthFirstAstVisitor<NullType> {
            val sol = solver.solution()
            val v = visit(program, null)
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
            is AProgram -> {
                node.funs.forEach {
                    solver.unify(ast2typevar(node, declData), ast2typevar(it, declData))
                }
            }
            is ANumber -> {
                solver.unify(ast2typevar(node, declData), TipInt())
            }
            is AInput -> {
                solver.unify(ast2typevar(node, declData), TipInt())
            }
            is AIfStmt -> {
                solver.unify(ast2typevar(node.guard, declData), TipInt())
            }
            is AOutputStmt -> {
                solver.unify(ast2typevar(node, declData), TipInt())
            }
            is AWhileStmt -> {
                solver.unify(ast2typevar(node.guard, declData), TipInt())
            }
            is AAssignStmt -> {
                solver.unify(ast2typevar(node.left, declData), ast2typevar(node.right, declData))
            }
            is ABinaryOp -> {
                when (node.operator) {
                    is Eqq -> {
                        solver.unify(ast2typevar(node, declData), TipInt())
                        val l = ast2typevar(node.left, declData)
                        val r = ast2typevar(node.right, declData)
                        solver.unify(l, r)
                    }
                    else -> {
                        val p = solver.find(ast2typevar(node, declData))
                        val l = ast2typevar(node.left, declData)
                        val r = ast2typevar(node.right, declData)
                        solver.unify(l, TipInt())
                        solver.unify(r, TipInt())
                        solver.unify(p, TipInt())
                    }
                }
            }
            is AUnaryOp<*> -> {
                when (node.operator) {
                    is RefOp -> {
                        val tr = TipRef(ast2typevar(node.target, declData))
                        solver.unify(ast2typevar(node, declData), tr)
                    }
                    is DerefOp -> {
                        val tr = TipRef(ast2typevar(node, declData))
                        solver.unify(ast2typevar(node.target, declData), tr)
                    }
                }
            }
            is AAlloc -> {
                val al = ast2typevar(node, declData)
                solver.unify(al, TipRef(TipAlpha(al)))
            }
            is ANull -> {
                val nl = ast2typevar(node, declData)
                solver.unify(nl, TipRef(TipAlpha(nl)))
            }
            is AFunDeclaration -> {
                val params: List<Term<TipType>> = node.args.map { ast2typevar(it, declData) }
                val ret: Term<TipType> = ast2typevar(node.stmts.ret.value, declData)
                val tf = TipFunction(params, ret)
                solver.unify(ast2typevar(node, declData), tf)
            }
            is ACallFuncExpr -> {
                val params: List<Term<TipType>> = node.args.map { ast2typevar(it, declData) }
                val ret: Term<TipType> = ast2typevar(node.targetFun, declData)
                val tf = TipFunction(params, ret)
                solver.unify(ast2typevar(node, declData), tf)
            }
        }
        visitChildren(node, null)
    }
}