package ast

object AstPrinters {

    fun AstNode.withRelevantLocations() {
        if (this is AIdentifierDeclaration || this is AAlloc)
            "${this.print()}:${this.loc}"
    }

    fun AstNode.withAllLocations() {
        "${this.print()}:${this.loc}"
    }

    /**
     * Method that makes a 'print' method available on 'AstNode' objects.
     */
    fun AstNode.print(): String =
        when (val n = this) {
            is ACallFuncExpr -> {
                val targetStr =
                    if (n.indirect)
                        "(${n.targetFun.print()})"
                    else
                        n.targetFun.print()
                "$targetStr(${n.args.map { it.print() }.joinToString(",")})"
            }
            is AIdentifier ->
                n.value
            is ABinaryOp ->
                n.left.print() + " " + n.operator + " " + n.right.print()
            is AUnaryOp<*> ->
                "${n.operator}${n.target.print()}"
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
                "${n.name}(${n.args.map { it.print() }.joinToString(",")})\n${n.stmts.print()}"
            is AAssignStmt ->
                "${n.left.print()} = ${n.right.print()};"
            is AIfStmt -> {
                val elseb = n.elseBranch?.let { "else " + it.print() } ?: ""
                "if (${n.guard.print()}) ${n.ifBranch.print()}  $elseb"
            }
            is AOutputStmt ->
                "output ${n.value.print()};"
            is AErrorStmt ->
                "error ${n.value.print()};"
            is AWhileStmt ->
                "while (${n.guard.print()}) ${n.innerBlock.print()}"
            is IABlock ->
                "{\n${n.body.map { it.print() }.joinToString("\n")}\n}"
            is AReturnStmt ->
                "return ${n.value.print()};"
            is AVarStmt ->
                "var ${n.declIds.map { it.print() }.joinToString(",")};"
            is AProgram ->
                n.funs.map { it.print() }.joinToString("\n\n")
            else -> ""
        }
}


