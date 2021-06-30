package types

import ast.AIdentifier
import ast.AstNode
import ast.DeclarationData
import solvers.*
import java.lang.IllegalArgumentException

object TipTypeObj {
    /**
     * Сonverts any AstNode to its type variable.
     * For identifiers the type variable is associated with the declaration;
     * for any other kind of AST node the type variable is associated with the node itself.
     */
    fun ast2typevar(node: AstNode, declData: DeclarationData): Var<TipType> = when (node) {
        is AIdentifier -> TipVar(declData[node]!!)
        else -> TipVar(node)
    }
}

/**
 * A type for a TIP variable or expression.
 */
interface TipType

object TipTypeOps : TermOps<TipType>() {

    override fun makeAlpha(x: Var<TipType>): Var<TipType> = when (x) {
        is TipVar -> TipAlpha(x.node.uid)
        is TipAlpha -> x
        else -> throw IllegalArgumentException()
    }

    override fun makeMu(v: Var<TipType>, t: Term<TipType>): Mu<TipType> = TipMu(v, t)
}

/**
 * Int type.
 */
class TipInt() : TipType, Cons<TipType>() {

    override fun subst(v: Var<TipType>, t: Term<TipType>): Term<TipType> = this

    override fun toString(): String = "int"
}

/**
 * Function type.
 */
data class TipFunction(val params: List<Term<TipType>>, val ret: Term<TipType>) : TipType, Cons<TipType>() {

    init {
        args.add(ret)
        args.addAll(params)
    }

    override fun subst(v: Var<TipType>, t: Term<TipType>): Term<TipType> =
        TipFunction(params.map { p -> p.subst(v, t) }, ret.subst(v, t))

    override fun toString(): String = "(${params.joinToString(",")}) -> $ret"
}

/**
 * Pointer reference type.
 */
data class TipRef(val of: Term<TipType>) : TipType, Cons<TipType>() {

    init {
        args.addAll(mutableListOf(of))
    }

    override fun subst(v: Var<TipType>, t: Term<TipType>): Term<TipType> = TipRef(of.subst(v, t))

    override fun toString(): String = "&$of"
}

/**
 * Type variable for a program variable or expression.
 */
data class TipVar(val node: AstNode) : TipType, Var<TipType>() {

    override fun toString(): String = "[[$node]]"
}

/**
 * Fresh type variable, whose identity is uniquely determined by `x`.
 */
data class TipAlpha(val x: Any) : TipType, Var<TipType>() {

    override fun toString(): String = "\u03B1<$x>"
}

/**
 * Recursive type (only created when closing terms).
 */
data class TipMu(override val v: Var<TipType>, override val t: Term<TipType>) : TipType, Mu<TipType>() {

    override fun subst(v: Var<TipType>, t: Term<TipType>): Term<TipType> =
        if (v == this.v) this
        else TipMu(this.v, this.t.subst(v, t))

    override fun toString(): String = super.toString()
}