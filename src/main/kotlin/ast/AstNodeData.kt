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

fun AIdentifier.declaration(declData: DeclarationData) = AstNodeWithDeclaration(this, declData).declaration!!

/**
 * Make type data available on AST nodes.
 */
class AstNodeWithType(val n: AstNode, val data: TypeData) {
    val theType: TipType? = data[n]

    private fun printer(): (AstNode) -> String = {
        when (it) {
            is AIdentifierDeclaration -> "${it.value}: ${this.theType ?: "??"}"
            is AFunDeclaration ->
                "${it.name}(${it.args.joinToString(",") { it.value }}): " +
                        "${this.theType ?: "??"}\n${AstNodeWithType(it.stmts, data).printer()}"
            else -> ""
        }
    }

    fun toTypedString(): String = n.print(printer())

}