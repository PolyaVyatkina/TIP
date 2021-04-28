package analysis

import ast.*
import java.lang.Exception

class DeclarationAnalysis(val prog: AProgram) : DepthFirstAstVisitor<Map<String, ADeclaration>>, Analysis<DeclarationData> {

    private val declResult: DeclarationData = mutableMapOf()

    override fun analyze(): DeclarationData {
        visit(prog, mutableMapOf())
        return declResult
    }

    /**
     * Recursively visits the nodes of the AST.
     * An environment `env` is provided as argument, mapping each identifier name to the node that declares it.
     * Whenever an identifier is visited, `declResult` is extended accordingly.
     *
     * @param node the node to visit
     * @param arg the environment associating with each name its declaration in the current scope
     */
    override fun visit(node: AstNode, arg: Map<String, ADeclaration>?) {
        when (node) {
            is ABlock -> {
                // Extend the environment with the initial declarations in the block, if present
                val ext = if (node is AFunBlockStmt) peekDecl(node.declarations) else mapOf()
                // Extend the env
                val extendedEnv = extendEnv(arg!!, ext)
                // Visit each statement in the extended environment
                node.body.forEach { stmt -> visit(stmt, extendedEnv) }
            }
            is AFunDeclaration -> {
                // Associate to each parameter itself as definition
                val argsMap = node.args.fold(mapOf<String, ADeclaration>()) { acc, cur: AIdentifierDeclaration ->
                    extendEnv(acc, cur.value to cur)
                }
                // Visit the function body in the extended environment
                val extendedEnv = extendEnv(arg!!, argsMap)
                visit(node.stmts, extendedEnv)
            }
            is AProgram -> {
                // There can be mutually recursive functions, so pre-bind all the functions to their definitions before visiting each of them
                val extended = node.funs.fold(mapOf<String, ADeclaration>()) { accEnv, fd: AFunDeclaration ->
                    extendEnv(accEnv, fd.name to fd)
                }
                node.funs.forEach { fd ->
                    visit(fd, extended)
                }
            }
            is AIdentifier -> {
                // Associate with each identifier the definition in the environment
                try {
                    declResult[node] = arg!![node.value]!!
                } catch (e: Exception) {
                    throw RuntimeException("Error retrieving definition of $node in ${arg?.keys}", e)
                }
            }
            is AAssignStmt -> {
                val left = node.left
                if (
                    left is AIdentifier
                    && arg!!.contains(left.value)
                    && arg[left.value] is AFunDeclaration
                )
                    throw RuntimeException("Function identifier for function ${arg[left.value]} can not appears on the left-hand side of an assignment at ${node.loc}")
                visitChildren(node, arg)
            }
            is AUnaryOp<*> -> {
                val id = node.target
                if (id is AIdentifier && arg!!.contains(id.value) && arg[id.value] is AFunDeclaration)
                    throw RuntimeException("Cannot take address of function ${arg[id.value]} at ${node.loc}")
                visitChildren(node, arg)
            }
            is ACallFuncExpr -> {
                if (!node.indirect) {
                    when (node.targetFun) {
                        is AIdentifier ->
                            if (
                                arg!![node.targetFun.value] !is AFunDeclaration &&
                                arg[node.targetFun.value] !is ADeclaration
                            )
                                throw RuntimeException("Direct call with a non-function identifier at ${node.loc}")
                        else -> throw RuntimeException("Direct call is not possible without a function identifier at ${node.loc}")
                    }
                }
                visitChildren(node, arg)
            }
            else -> {
                // There is no alteration of the environment, just visit the children in the current environment
                visitChildren(node, arg)
            }
        }
    }

    /**
     * Extend the environment `env` with the bindings in `ext`, checking that no re-definitions occur.
     * @param env the environment to extend
     * @param ext the bindings to add
     * @return the extended environment if no conflict occurs, throws a RuntimeException otherwise
     */
    fun extendEnv(env: Map<String, ADeclaration>, ext: Map<String, ADeclaration>): Map<String, ADeclaration> {
        // Check for conflicts
        val conflicts: Set<String> = env.keys.toSet().intersect(ext.keys.toSet())
        if (conflicts.isNotEmpty()) redefinition(conflicts.map { env[it]!! }.toSet())
        return env + ext
    }

    /**
     * Extend the environment `env` with the binding `pair`, checking that no re-definition occurs.
     * @param env the environment to extend
     * @param pair the binding to add
     * @return the extended environment if no conflict occurs, throws a RuntimeException otherwise
     */
    fun extendEnv(env: Map<String, ADeclaration>, pair: Pair<String, ADeclaration>): Map<String, ADeclaration> {
        if (env.contains(pair.first)) redefinition(setOf(env[pair.first]!!))
        return env + pair
    }

    /**
     * Returns a map containing the new declarations contained in the given sequence of variable declaration statements.
     * If a variable is re-defined, a RuntimeException is thrown.
     * @param decls the sequence of variable declaration statements
     * @return a map associating with each name the node that declares it
     */
    private fun peekDecl(decls: Collection<AVarStmt>): Map<String, ADeclaration> {
        val allDecls = decls.flatMap { v: AVarStmt -> v.declIds.map { id -> id.value to id } }
        return allDecls.fold(mapOf()) { map, pair ->
            extendEnv(map, pair)
        }
    }

    fun redefinition(conflicting: Set<ADeclaration>): Nothing =
        throw RuntimeException("Redefinition of identifiers $conflicting")
}