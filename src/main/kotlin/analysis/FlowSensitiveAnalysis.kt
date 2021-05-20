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

    fun select(kind: AnalysisType, options: AnalysisOption, cfg: FragmentCfg, declData: DeclarationData): FlowSensitiveAnalysis<CfgNode, *>? {
        val typedCfg =
            if (options == iwli ||
                options == iwlip ||
                options == csiwlip ||
                options == cfiwlip ||
                options == ide
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
//                livevars -> LiveVarsAnalysisSimpleSolver(typedCfg.left.get, declData)
//                available -> AvailableExpAnalysisSimpleSolver(typedCfg.left.get, declData)
//                vbusy -> VeryBusyExpAnalysisSimpleSolver(typedCfg.left.get, declData) //TODO() <--- Complete here
//                reaching -> ReachingDefAnalysisSimpleSolver(typedCfg.left.get, declData) //TODO() <--- Complete here
//                constprop -> ConstantPropagationAnalysisSimpleSolver(typedCfg.left.get, declData)
                else -> throw RuntimeException("Unsupported solver option `$options` for the analysis $kind")
            }
            wl -> when (kind) {
//                sign -> IntraprocSignAnalysisSimpleSolver(typedCfg.left.get, declData)
//                livevars -> LiveVarsAnalysisSimpleSolver(typedCfg.left.get, declData)
//                available -> AvailableExpAnalysisSimpleSolver(typedCfg.left.get, declData)
//                vbusy -> VeryBusyExpAnalysisSimpleSolver(typedCfg.left.get, declData) //TODO() <--- Complete here
//                reaching -> ReachingDefAnalysisSimpleSolver(typedCfg.left.get, declData) //TODO() <--- Complete here
//                constprop -> ConstantPropagationAnalysisSimpleSolver(typedCfg.left.get, declData)
                else -> throw RuntimeException("Unsupported solver option `$options` for the analysis $kind")
            }
            wli -> when (kind) {
//                sign -> IntraprocSignAnalysisWorklistSolverWithInit(typedCfg.left.get, declData)
//                interval -> IntervalAnalysisWorklistSolverWithInit(typedCfg.left.get, declData)
                else -> throw RuntimeException("Unsupported solver option `$options` for the analysis $kind")
            }
            wliw -> when (kind) {
//                interval -> IntervalAnalysisWorklistSolverWithWidening(typedCfg.left.get, declData)
                else -> throw RuntimeException("Unsupported solver option `$options` for the analysis $kind")
            }
            wliwn -> when (kind) {
//                interval -> IntervalAnalysisWorklistSolverWithWideningAndNarrowing(typedCfg.left.get, declData)
                else -> throw RuntimeException("Unsupported solver option `$options` for the analysis $kind")
            }
            wlip -> when (kind) {
//                sign -> IntraprocSignAnalysisWorklistSolverWithInitAndPropagation(typedCfg.left.get, declData)
                else -> throw RuntimeException("Unsupported solver option `$options` for the analysis $kind")
            }
            iwli -> when (kind) {
//                sign -> InterprocSignAnalysisWorklistSolverWithInit(typedCfg.right.get, declData)
                else -> throw RuntimeException("Unsupported solver option `$options` for the analysis $kind")
            }
            iwlip -> when (kind) {
//                sign -> InterprocSignAnalysisWorklistSolverWithInitAndPropagation(typedCfg.right.get, declData)
                else -> throw RuntimeException("Unsupported solver option `$options` for the analysis $kind")
            }
            csiwlip -> when (kind) {
//                sign -> CallStringSignAnalysis(typedCfg.right.get, declData)
                else -> throw RuntimeException("Unsupported solver option `$options` for the analysis $kind")
            }
            cfiwlip -> when (kind) {
//                sign -> FunctionalSignAnalysis(typedCfg.right.get, declData)
                else -> throw RuntimeException("Unsupported solver option `$options` for the analysis $kind")
            }
            ide -> when (kind) {
//                copyconstprop -> CopyConstantPropagationIDEAnalysis(typedCfg.right.get, declData)
                else -> throw RuntimeException("Unsupported solver option `$options` for the analysis $kind")
            }
        }
    }


    /**
     * A flow sensitive analysis kind
     */
    enum class AnalysisType {
        sign, livevars, available, vbusy, reaching, constprop, interval, copyconstprop
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
        simple, Disabled, wl, wli, wliw, wliwn, wlip, iwli, iwlip, csiwlip, cfiwlip, ide;

        fun interprocedural(): Boolean =
            when (this) {
                iwli -> true
                iwlip -> true
                csiwlip -> true
                cfiwlip -> true
                ide -> true
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