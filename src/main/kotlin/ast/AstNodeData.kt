package ast

import ast.AstPrinters.print
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
class AstNodeWithDeclaration(n: AIdentifier, data: DeclarationData) {
    val declaration: ADeclaration? = data[n]
}

/**
 * Make type data available on AST nodes.
 */
class AstNodeWithType(val n: AstNode, val data: TypeData) {
    val theType: TipType? = data[n]

    private fun printer(): String = when (n) {
        is AIdentifierDeclaration -> "${n.value}: ${this.theType ?: "??"}"
        is AFunDeclaration ->
            "${n.name}(${n.args.joinToString(",") { it.value }}): " +
                    "${this.theType ?: "??"}\n${AstNodeWithType(n.stmts, data).printer()}"
        else -> TODO()
    }

//    fun toTypedString(): String = n.print(printer())
    fun toTypedString(): String = ""

}