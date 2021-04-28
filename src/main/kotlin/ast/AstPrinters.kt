package ast

object AstPrinters {

    fun AstNode.withRelevantLocations() {
        if (this is AIdentifierDeclaration || this is AAlloc)
            "${this.print("")}:${this.loc}"
    }

    fun AstNode.withAllLocations() {
        "${this.print("")}:${this.loc}"
    }

    /**
     * Method that makes a 'print' method available on 'AstNode' objects.
     */
    fun AstNode.print(printer: String): String =
        when (val n = this) {
            is ACallFuncExpr -> {
                val targetStr =
                    if (n.indirect)
                        "(${n.targetFun.print(printer)})"
                    else
                        n.targetFun.print(printer)
                "$targetStr(${n.args.map { it.print(printer) }.joinToString("\n")})"
            }
            is AIdentifier ->
                n.value
            is ABinaryOp ->
                n.left.print(printer) +
                        " " + n.operator + " " +
                        n.right.print(printer)
            is AUnaryOp<*> ->
                "${n.operator}${n.target.print(printer)}"
            is ANumber ->
                n.value.toString()
            is AInput ->
                "input"
            is AAlloc ->
                "alloc"
            is ANull ->
                "null"
            is AIdentifierDeclaration ->
                n.value
            is AFunDeclaration ->
                "${n.name}(" +
                        n.args.map { it.print(printer) }.joinToString(",") +
                        ")\n" +
                        n.stmts.print(printer)
            is AAssignStmt ->
                "${n.left.print(printer)} = " +
                        "${n.right.print(printer)};"
            is AIfStmt -> {
                val elseb = n.elseBranch?.let { "else " + it.print(printer) } ?: ""
                "if (${n.guard.print(printer)}) " +
                        "${n.ifBranch.print(printer)}  " + elseb
            }
            is AOutputStmt ->
                "output ${n.value.print(printer)};"
            is AErrorStmt ->
                "error ${n.value.print(printer)};"
            is AWhileStmt ->
                "while (${n.guard.print(printer)}) " +
                        n.innerBlock.print(printer)
            is ABlock ->
                "{\n${n.body.joinToString("\n") { it.print(printer) }}\n}"
            is AReturnStmt ->
                "return ${n.value.print(printer)};"
            is AVarStmt ->
                "var ${n.declIds.joinToString(",") { it.print(printer) }};"
            is AProgram ->
                n.funs.joinToString("\n\n") { it.print(printer) }
            else -> ""
        }
}


