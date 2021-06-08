package ast

import ast.AstPrinters.EMPTY_PRINTER
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
fun AIdentifier.declaration(declData: DeclarationData): ADeclaration = declData[this]!!

/**
 * Make type data available on AST nodes.
 */
fun AstNode.theType(data: TypeData): TipType? = data[this]

class AstNodeWithType(val n: AstNode, val data: TypeData) {

    private fun printer(): (AstNode) -> String = {
        when (it) {
            is AIdentifierDeclaration -> "${it.value}: ${n.theType(data) ?: "??"}"
            is AFunDeclaration ->
                "${it.name}(${it.args.joinToString(",") { it.value }}): " +
                        "${n.theType(data) ?: "??"}\n${AstNodeWithType(it.stmts, data).printer()}"
            else -> EMPTY_PRINTER
        }
    }

    fun toTypedString(): String = n.print(printer())

}