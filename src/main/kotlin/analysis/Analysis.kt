package analysis

import ast.ADeclaration
import ast.AstNode

/**
 * Trait for program analyses.
 *
 * @tparam R the type of the analysis result
 **/
interface Analysis<out R> {

    /**
     * Performs the analysis and returns the result.
     */
    fun analyze(): R
}

/**
 * Trait for (may-)points-to analyses.
 * Can answer may-points-to and may-alias queries.
 */
interface PointsToAnalysis : Analysis<Unit> {

    /**
     * Builds the points-to map.
     * For each identifier, the points-to map gives the set of locations the identifier may point to.
     */
    fun pointsTo(): Map<ADeclaration, Set<AstNode>>

    /**
     * Returns a function that tells whether two given identifiers may point to the same cell.
     * @return a function that returns true if the identifiers may point to the same cell; false if they definitely do not point to the same cell
     */
    fun mayAlias(): (ADeclaration, ADeclaration) -> Boolean
}