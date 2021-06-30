package analysis

import analysis.FlowSensitiveAnalysisObj.AnalysisOption.*
import analysis.FlowSensitiveAnalysisObj.AnalysisType.*
import ast.DeclarationData
import cfg.CfgNode
import cfg.FragmentCfg
import cfg.InterproceduralProgramCfg
import cfg.IntraproceduralProgramCfg
import lattices.Lattice
import lattices.MapLattice

abstract class FlowSensitiveAnalysis<N, T>(cfg: FragmentCfg) : Analysis<Map<N, T>> {
    /**
     * The lattice used by the analysis.
     */
    abstract val lattice: MapLattice<N, T, Lattice<T>>

    /**
     * The domain of the map lattice.
     */
    val domain = cfg.nodes

    /**
     * @inheritdoc
     */
    abstract override fun analyze(): Map<N, T>
}

/**
 * A factory to create a specific flow-sensitive analysis that matches the options.
 */
object FlowSensitiveAnalysisObj {

    fun select(kind: AnalysisType, options: AnalysisOption, cfg: FragmentCfg, declData: DeclarationData): FlowSensitiveAnalysis<*, *>? {
        val typedCfg =
            if (options == iwli ||
                options == iwlip ||
                options == csiwlip ||
                options == cfiwlip
            )
                if (cfg is InterproceduralProgramCfg)
                    cfg
                else throw RuntimeException("Whole CFG needed")
            else if (cfg is IntraproceduralProgramCfg)
                cfg
            else throw RuntimeException("Intraprocedural CFG needed")

        return when (options) {
            Disabled -> null
            simple -> when (kind) {
                sign -> IntraprocSignAnalysisSimpleSolver(typedCfg, declData)
                livevars -> LiveVarsAnalysisSimpleSolver(typedCfg as IntraproceduralProgramCfg, declData)
                available -> AvailableExpAnalysisSimpleSolver(typedCfg as IntraproceduralProgramCfg, declData)
                //vbusy -> VeryBusyExpAnalysisSimpleSolver(typedCfg as IntraproceduralProgramCfg, declData) TODO() // <--- Complete here
                //reaching -> ReachingDefAnalysisSimpleSolver(typedCfg as IntraproceduralProgramCfg, declData)) TODO() // <--- Complete here
                constprop -> ConstantPropagationAnalysisSimpleSolver(typedCfg as IntraproceduralProgramCfg, declData)
                else -> throw RuntimeException("Unsupported solver option `$options` for the analysis $kind")
            }
            wl -> when (kind) {
                sign -> IntraprocSignAnalysisWorklistSolver(typedCfg, declData)
                livevars -> LiveVarsAnalysisWorklistSolver(typedCfg as IntraproceduralProgramCfg, declData)
                available -> AvailableExpAnalysisWorklistSolver(typedCfg as IntraproceduralProgramCfg, declData)
                //vbusy -> VeryBusyExpAnalysisWorklistSolver(typedCfg as IntraproceduralProgramCfg, declData) TODO() // <--- Complete here
                //reaching -> ReachingDefAnalysisWorklistSolver(typedCfg as IntraproceduralProgramCfg, declData)) TODO() // <--- Complete here
                constprop -> ConstantPropagationAnalysisWorklistSolver(typedCfg as IntraproceduralProgramCfg, declData)
                else -> throw RuntimeException("Unsupported solver option `$options` for the analysis $kind")
            }
            wli -> when (kind) {
                sign -> IntraprocSignAnalysisWorklistSolverWithInit(typedCfg, declData)
                interval -> IntervalAnalysisWorklistSolverWithInit(typedCfg, declData)
                else -> throw RuntimeException("Unsupported solver option `$options` for the analysis $kind")
            }
            wliw -> when (kind) {
                interval -> IntervalAnalysisWorklistSolverWithWidening(typedCfg, declData)
                else -> throw RuntimeException("Unsupported solver option `$options` for the analysis $kind")
            }
            wliwn -> when (kind) {
                interval -> IntervalAnalysisWorklistSolverWithWideningAndNarrowing(typedCfg, declData)
                else -> throw RuntimeException("Unsupported solver option `$options` for the analysis $kind")
            }
            wlip -> when (kind) {
                sign -> IntraprocSignAnalysisWorklistSolverWithInitAndPropagation(typedCfg, declData)
                else -> throw RuntimeException("Unsupported solver option `$options` for the analysis $kind")
            }
            iwli -> when (kind) {
                sign -> InterprocSignAnalysisWorklistSolverWithInit(typedCfg as InterproceduralProgramCfg, declData)
                else -> throw RuntimeException("Unsupported solver option `$options` for the analysis $kind")
            }
            iwlip -> when (kind) {
                sign -> InterprocSignAnalysisWorklistSolverWithInitAndPropagation(typedCfg as InterproceduralProgramCfg, declData)
                else -> throw RuntimeException("Unsupported solver option `$options` for the analysis $kind")
            }
            csiwlip -> when (kind) {
                sign -> CallStringSignAnalysis(typedCfg as InterproceduralProgramCfg, declData)
                else -> throw RuntimeException("Unsupported solver option `$options` for the analysis $kind")
            }
            cfiwlip -> when (kind) {
                sign -> FunctionalSignAnalysis(typedCfg as InterproceduralProgramCfg, declData)
                else -> throw RuntimeException("Unsupported solver option `$options` for the analysis $kind")
            }
        }
    }


    /**
     * A flow sensitive analysis kind
     */
    enum class AnalysisType {
        sign, livevars, available, vbusy, reaching, constprop, interval
    }

    /**
     * The options of the analysis:
     *
     * - Enabled: use the simple fixpoint solver
     * - wl: use the worklist solver
     * - wli: use the worklist solver with init
     * - wliw: use the worklist solver with init and widening
     * - wliwn: use the worklist solver with init, widening, and narrowing
     * - wlip: use the worklist solver with init and propagation
     * - iwli: use the worklist solver with init, interprocedural version
     * - iwlip: use the worklist solver with init and propagation, interprocedural version
     * - csiwlip: use the worklist solver with init and propagation, context-sensitive (with call string) interprocedural version
     * - cfiwlip: use the worklist solver with init and propagation, context-sensitive (with functional approach) interprocedural version
     * - ide: use the IDE solver
     */
    enum class AnalysisOption {
        simple, Disabled, wl, wli, wliw, wliwn, wlip, iwli, iwlip, csiwlip, cfiwlip;

        fun interprocedural(): Boolean =
            when (this) {
                iwli -> true
                iwlip -> true
                csiwlip -> true
                cfiwlip -> true
                else -> false
            }

        fun contextsensitive(): Boolean =
            when (this) {
                csiwlip -> true
                cfiwlip -> true
                else -> false
            }


        fun withWidening(): Boolean =
            when (this) {
                wliw -> true
                wliwn -> true
                else -> false
            }

    }
}