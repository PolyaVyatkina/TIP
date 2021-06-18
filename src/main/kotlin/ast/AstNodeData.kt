package ast

import types.TipType

/**
 * Map from identifier node to corresponding declaration node.
 * @see [[analysis.DeclarationAnalysis]]
 */
typealias DeclarationData = MutableMap<AIdentifier, ADeclaration>

/**
 * Map from AST node to type, if available.
 * @see [[analysis.TypeAnalysis]]
 */
typealias TypeData = Map<AstNode, TipType?>

/**
 * Make declaration data available on identifier AST nodes.
 */
fun AIdentifier.declaration(declData: DeclarationData): ADeclaration = declData[this]!!

/**
 * Make type data available on AST nodes.
 */
fun AstNode.theType(data: TypeData): TipType? = data[this]

fun AstNode.toTypedString(data: TypeData): String =
    when (val n = this) {
        is AIdentifierDeclaration -> "${n.value}: ${n.theType(data) ?: "??"}"
        is AFunDeclaration ->
            "${n.name}(${n.args.joinToString(",") { it.value }}): ${n.theType(data) ?: "??"}\n" +
                    n.stmts.toTypedString(data)
        is ACallFuncExpr -> {
            val targetStr =
                if (n.indirect)
                    "(${n.targetFun.toTypedString(data)})"
                else
                    n.targetFun.toTypedString(data)
            "$targetStr(${n.args.map { it.toTypedString(data) }.joinToString(",")})"
        }
        is AIdentifier ->
            n.value
        is ABinaryOp ->
            n.left.toTypedString(data) + " " + n.operator + " " + n.right.toTypedString(data)
        is AUnaryOp<*> ->
            "${n.operator}${n.target.toTypedString(data)}"
        is ANumber ->
            n.value.toString()
        is AInput ->
            "input"
        is AAlloc ->
            "alloc"
        is ANull ->
            "null"
        is AAssignStmt ->
            "${n.left.toTypedString(data)} = ${n.right.toTypedString(data)};"
        is AIfStmt -> {
            val elseb = n.elseBranch?.let { "else " + it.toTypedString(data) } ?: ""
            "if (${n.guard.toTypedString(data)}) ${n.ifBranch.toTypedString(data)}  $elseb"
        }
        is AOutputStmt ->
            "output ${n.value.toTypedString(data)};"
        is AErrorStmt ->
            "error ${n.value.toTypedString(data)};"
        is AWhileStmt ->
            "while (${n.guard.toTypedString(data)}) ${n.innerBlock.toTypedString(data)}"
        is IABlock ->
            "{\n${n.body.map { it.toTypedString(data) }.joinToString("\n")}\n}"
        is AReturnStmt ->
            "return ${n.value.toTypedString(data)};"
        is AVarStmt ->
            "var ${n.declIds.map { it.toTypedString(data) }.joinToString(",")};"
        is AProgram ->
            n.funs.map { it.toTypedString(data) }.joinToString("\n\n")
        else -> ""
    }