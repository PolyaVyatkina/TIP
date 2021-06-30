import analysis.*
import ast.AProgram
import ast.DeclarationData
import ast.TypeData
import ast.toTypedString
import cfg.*
import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.Parsed
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import parser.TIPGrammar
import utils.*
import java.io.File

@TestInstance(Lifecycle.PER_CLASS)
internal class TipKtTest {

    private val outputTypes = "./tests/tests_types"
    private val outputSimple = "./tests/tests_simple"
    private val outputWl = "./tests/tests_wl"
    private val outputWli = "./tests/tests_wli"
    private val outputWliw = "./tests/tests_wliw"
    private val outputWliwn = "./tests/tests_wliwn"
    private val outputWlip = "./tests/tests_wlip"
    private val outputIwli = "./tests/tests_iwli"
    private val outputIwlip = "./tests/tests_iwlip"
    private val outputCsiwlip = "./tests/tests_csiwlip"
    private val outputCfiwlip = "./tests/tests_cfiwlip"
    private val output = listOf(outputTypes, outputSimple, outputWl, outputWli, outputWliw, outputWliwn, outputWlip)

    lateinit var programs: Map<String, String>
    lateinit var parsedInfo: Map<String, AProgram>
    lateinit var declData: Map<String, DeclarationData>
    lateinit var cfgInfo: Map<String, IntraproceduralProgramCfg>
    lateinit var icfgInfo: Map<String, InterproceduralProgramCfg>

    @BeforeAll
    fun prepare() {
        val res = mutableMapOf<String, String>()
        File("examples/").walk().forEach { file ->
            if (file.isFile) {
                val text = file.readText()
                res[file.name] = text
            }
        }
        programs = res.toMap()
        makeDeclarationData()
        makeCfg()
        makeIcfg()
    }

    private fun parseFiles(): Map<String, AProgram> {
        val grammar = TIPGrammar()
        val result = mutableMapOf<String, AProgram>()
        for (file in programs) {
            grammar.lastBreaks = mutableListOf(0)
            val res = grammar.tryParseToEnd(file.value)
            if (res is Parsed)
                result[file.key] = res.value as AProgram
        }
        println("Parsed ${programs.size} files, success – ${result.size}")
        parsedInfo = result.toMap()
        return result
    }

    private fun makeDeclarationData() {
        val res = mutableMapOf<String, DeclarationData>()
        for (file in parseFiles()) {
            val programNode = file.value
            try {
                val data: DeclarationData = DeclarationAnalysis(programNode).analyze()
                res[file.key] = data
            } catch (e: Exception) {
                println("${file.key} : $e")
            }
        }
        println("Declarations were successfully made for ${res.size} programs")
        declData = res.toMap()
    }

    private fun makeCfg() {
        val cfgs = mutableMapOf<String, IntraproceduralProgramCfg>()
        for (entry in declData) {
            val file = entry.key
            val program = parsedInfo[file]!!
            cfgs[file] = IntraproceduralProgramCfgObj.generateFromProgram(program)

        }
        println("Cfgs were successfully made for ${cfgs.size} programs")
        cfgInfo = cfgs.toMap()
    }

    private fun makeIcfg() {
        val icfgs = mutableMapOf<String, InterproceduralProgramCfg>()
        val icfgFiles = File("examples/icfg/").list()!!
        for (entry in declData) {
            if (entry.key in icfgFiles) {
                val file = entry.key
                val data = entry.value
                val program = parsedInfo[file]!!
                icfgs[file] = InterproceduralProgramCfgObj.generateFromProgram(program, data)
            }
        }
        println("Icfgs were successfully made for ${icfgs.size} programs")
        icfgInfo = icfgs.toMap()
    }

    @Test
    fun typesUF() {
        val output = File("$outputTypes/types/")
        output.mkdirs()
        for (file in declData.keys) {
            val typeData: TypeData = TypeAnalysis(parsedInfo[file]!!, declData[file]!!).analyze()
            Output.output(
                File("examples/$file"), OtherOutput(OutputKindE.TYPES),
                parsedInfo[file]!!.toTypedString(typeData), output
            )
        }
    }

    @Test
    fun signCfgSimple() {
        val type = FlowSensitiveAnalysisObj.AnalysisType.sign
        val output = File("$outputSimple/$type")
        output.mkdirs()
        var s = 0
        var f = 0
        var m = 0
        val signCfgSimpleFiles = File("examples/sign_simple/").list()!!
        for (file in cfgInfo.keys) {
            if (file !in signCfgSimpleFiles) m++
            else {
                //println("[$type][simple] : proceed on $file")
                val cfg = cfgInfo[file]!!
                val data = declData[file]!!
                try {
                    val simple = IntraprocSignAnalysisSimpleSolver(cfg, data).analyze()
                    Output.output(File("examples/$file"), DataFlowOutput(type), cfg.toDot({ node ->
                        Output.labeler(simple, node)
                    }, { node -> Output.dotIder(node) }), output)
                    s++
                } catch (e: Exception) {
                    //println("[$type][simple] : cannot perform on $file due to $e")
                    f++
                }
            }
        }
        println("[$type][simple] : on ${cfgInfo.size} files: success = $s, failed = $f, skipped = $m")
    }

    @Test
    fun signCfgWl() {
        val type = FlowSensitiveAnalysisObj.AnalysisType.sign
        val output = File("$outputWl/$type")
        output.mkdirs()
        var s = 0
        var f = 0
        var m = 0
        for (file in cfgInfo.keys) {
            //println("[$type][wl] : proceed on $file")
            val cfg = cfgInfo[file]!!
            val data = declData[file]!!
            try {
                val wl = IntraprocSignAnalysisWorklistSolver(cfg, data).analyze()
                Output.output(File("examples/$file"), DataFlowOutput(type), cfg.toDot({ node ->
                    Output.labeler(wl, node)
                }, { node -> Output.dotIder(node) }), output)
                s++
            } catch (e: Exception) {
                //println("[$type][wl] : cannot perform on $file due to $e")
                f++
            }
        }
        println("[$type][wl] : on ${cfgInfo.size} files: success = $s, failed = $f")
    }

    @Test
    fun signCfgWli() {
        val type = FlowSensitiveAnalysisObj.AnalysisType.sign
        val output = File("$outputWli/$type")
        output.mkdirs()
        var s = 0
        var f = 0
        var m = 0
        for (file in cfgInfo.keys) {
            //println("[$type][wli] : proceed on $file")
            val cfg = cfgInfo[file]!!
            val data = declData[file]!!
            try {
                val wli = IntraprocSignAnalysisWorklistSolverWithInit(cfg, data).analyze()
                Output.output(File("examples/$file"), DataFlowOutput(type), cfg.toDot({ node ->
                    Output.labeler(wli, node)
                }, { node -> Output.dotIder(node) }), output)
                s++
            } catch (e: Exception) {
                //println("[$type][wli] : cannot perform on $file due to $e")
                f++
            }

        }
        println("[$type][wli] : on ${cfgInfo.size} files: success = $s, failed = $f")
    }

    @Test
    fun signCfgWlip() {
        val type = FlowSensitiveAnalysisObj.AnalysisType.sign
        val output = File("$outputWlip/$type")
        output.mkdirs()
        var s = 0
        var f = 0
        var m = 0
        for (file in cfgInfo.keys) {
            //println("[$type][wlip] : proceed on $file")
            val cfg = cfgInfo[file]!!
            val data = declData[file]!!
            try {
                val wlip = IntraprocSignAnalysisWorklistSolverWithInitAndPropagation(cfg, data).analyze()
                Output.output(File("examples/$file"), DataFlowOutput(type), cfg.toDot({ node ->
                    Output.labeler(wlip, node)
                }, { node -> Output.dotIder(node) }), output)
                s++
            } catch (e: Exception) {
                //println("[$type][wlip] : cannot perform on $file due to $e")
                f++
            }
        }
        println("[$type][wlip] : on ${cfgInfo.size} files: success = $s, failed = $f")
    }

    @Test
    fun signIcfgIwli() {
        val type = FlowSensitiveAnalysisObj.AnalysisType.sign
        val output = File("$outputIwli/$type")
        output.mkdirs()
        var s = 0
        var f = 0
        for (file in icfgInfo.keys) {
            println("[$type][iwli] : proceed on $file")
            val icfg = icfgInfo[file]!!
            val data = declData[file]!!
            try {
                val iwli = InterprocSignAnalysisWorklistSolverWithInit(icfg, data).analyze()
                Output.output(File("examples/$file"), DataFlowOutput(type), icfg.toDot({ node ->
                    Output.labeler(iwli, node)
                }, { node -> Output.dotIder(node) }), output)
                s++
            } catch (e: Exception) {
                println("[$type][iwli] : cannot perform on $file due to $e")
                f++
            }
        }
        println("[$type][iwli] : on ${cfgInfo.size} files: success = $s, failed = $f")
    }

    @Test
    fun signIcfgIwlip() {
        val type = FlowSensitiveAnalysisObj.AnalysisType.sign
        val output = File("$outputIwlip/$type")
        output.mkdirs()
        var s = 0
        var f = 0
        for (file in icfgInfo.keys) {
            println("[$type][iwlip] : proceed on $file")
            val icfg = icfgInfo[file]!!
            val data = declData[file]!!
            try {
                val iwlip = InterprocSignAnalysisWorklistSolverWithInitAndPropagation(icfg, data).analyze()
                Output.output(File("examples/$file"), DataFlowOutput(type), icfg.toDot({ node ->
                    Output.labeler(iwlip, node)
                }, { node -> Output.dotIder(node) }), output)
                s++
            } catch (e: Exception) {
                println("[$type][iwlip] : cannot perform on $file due to $e")
                f++
            }
        }
        println("[$type][iwlip] : on ${cfgInfo.size} files: success = $s, failed = $f")
    }

    @Test
    fun signIcfgCsiwlip() {
        val type = FlowSensitiveAnalysisObj.AnalysisType.sign
        val output = File("$outputCsiwlip/$type")
        output.mkdirs()
        var s = 0
        var f = 0
        for (file in icfgInfo.keys) {
            println("[$type][csiwlip] : proceed on $file")
            val icfg = icfgInfo[file]!!
            val data = declData[file]!!
            try {
                val csiwlip = CallStringSignAnalysis(icfg, data).analyze() as MapWithDefault<Pair<CallContext, CfgNode>, *>
                Output.output(File("examples/$file"), DataFlowOutput(type), icfg.toDot({ node ->
                    Output.labeler(Output.transform(csiwlip), node)
                }, { node -> Output.dotIder(node) }), output)
                s++
            } catch (e: Exception) {
                println("[$type][csiwlip] : cannot perform on $file due to $e")
                f++
            }
        }
        println("[$type][csiwlip] : on ${cfgInfo.size} files: success = $s, failed = $f")
    }

    @Test
    fun signIcfgCfiwlip() {
        val type = FlowSensitiveAnalysisObj.AnalysisType.sign
        val output = File("$outputCfiwlip/$type")
        output.mkdirs()
        var s = 0
        var f = 0
        for (file in icfgInfo.keys) {
            println("[$type][cfiwlip] : proceed on $file")
            val icfg = icfgInfo[file]!!
            val data = declData[file]!!
            try {
                val cfiwlip = FunctionalSignAnalysis(icfg, data).analyze() as MapWithDefault<Pair<CallContext, CfgNode>, *>
                Output.output(File("examples/$file"), DataFlowOutput(type), icfg.toDot({ node ->
                    Output.labeler(Output.transform(cfiwlip), node)
                }, { node -> Output.dotIder(node) }), output)
                s++
            } catch (e: Exception) {
                println("[$type][cfiwlip] : cannot perform on $file due to $e")
                f++
            }
        }
        println("[$type][cfiwlip] : on ${cfgInfo.size} files: success = $s, failed = $f")
    }

    @Test
    fun livevarsSimple() {
        val type = FlowSensitiveAnalysisObj.AnalysisType.livevars
        val output = File("$outputSimple/$type")
        output.mkdirs()
        var s = 0
        var f = 0
        for (file in cfgInfo.keys) {
            val cfg = cfgInfo[file]!!
            val data = declData[file]!!
            //println("[$type][simple] : proceed on $file")
            try {
                val simple = LiveVarsAnalysisSimpleSolver(cfg, data).analyze()
                Output.output(File("examples/$file"), DataFlowOutput(type), cfg.toDot({ node ->
                    Output.labeler(simple, node)
                }, { node -> Output.dotIder(node) }), output)
                s++
            } catch (e: Exception) {
                //println("[$type][simple] : cannot perform on $file due to $e")
                f++
            }
        }
        println("[$type][simple] : on ${cfgInfo.size} files: success = $s, failed = $f")
    }

    @Test
    fun livevarsWl() {
        val type = FlowSensitiveAnalysisObj.AnalysisType.livevars
        val output = File("$outputWl/$type")
        output.mkdirs()
        var s = 0
        var f = 0
        for (file in cfgInfo.keys) {
            val cfg = cfgInfo[file]!!
            val data = declData[file]!!
            //println("[$type][wl] : proceed on $file")
            try {
                val wl = LiveVarsAnalysisWorklistSolver(cfg, data).analyze()
                Output.output(File("examples/$file"), DataFlowOutput(type), cfg.toDot({ node ->
                    Output.labeler(wl, node)
                }, { node -> Output.dotIder(node) }), output)
                s++
            } catch (e: Exception) {
                //println("[$type][wl] : cannot perform on $file due to $e")
                f++
            }
        }
        println("[$type][wl] : on ${cfgInfo.size} files: success = $s, failed = $f")
    }

    @Test
    fun availableSimple() {
        val type = FlowSensitiveAnalysisObj.AnalysisType.available
        val output = File("$outputSimple/$type")
        output.mkdirs()
        var s = 0
        var f = 0
        for (file in cfgInfo.keys) {
            //println("[$type][simple] : proceed on $file")
            val cfg = cfgInfo[file]!!
            val data = declData[file]!!
            try {
                val simple = AvailableExpAnalysisSimpleSolver(cfg, data).analyze()
                Output.output(File("examples/$file"), DataFlowOutput(type), cfg.toDot({ node ->
                    Output.labeler(simple, node)
                }, { node -> Output.dotIder(node) }), output)
                s++
            } catch (e: Exception) {
                //println("[$type][simple] : cannot perform on $file due to $e")
                f++
            }
        }
        println("[$type][simple] : on ${cfgInfo.size} files: success = $s, failed = $f")
    }

    @Test
    fun availableWl() {
        val type = FlowSensitiveAnalysisObj.AnalysisType.available
        val output = File("$outputWl/$type")
        output.mkdirs()
        var s = 0
        var f = 0
        for (file in cfgInfo.keys) {
            //println("[$type][wl] : proceed on $file")
            val cfg = cfgInfo[file]!!
            val data = declData[file]!!
            try {
                val wl = AvailableExpAnalysisWorklistSolver(cfg, data).analyze()
                Output.output(File("examples/$file"), DataFlowOutput(type), cfg.toDot({ node ->
                    Output.labeler(wl, node)
                }, { node -> Output.dotIder(node) }), output)
                s++
            } catch (e: Exception) {
                //println("[$type][wl] : cannot perform on $file due to $e")
                f++
            }
        }
        println("[$type][wl] : on ${cfgInfo.size} files: success = $s, failed = $f")
    }

    @Test
    fun constpropSimple() {
        val type = FlowSensitiveAnalysisObj.AnalysisType.constprop
        val output = File("$outputSimple/$type")
        output.mkdirs()
        var s = 0
        var f = 0
        var m = 0
        for (file in cfgInfo.keys) {
            val cfg = cfgInfo[file]!!
            val data = declData[file]!!
            println("[$type][simple] : proceed on $file")
            try {
                val simple = ConstantPropagationAnalysisSimpleSolver(cfg, data).analyze()
                Output.output(File("examples/$file"), DataFlowOutput(type), cfg.toDot({ node ->
                    Output.labeler(simple, node)
                }, { node -> Output.dotIder(node) }), output)
                s++
            } catch (e: Exception) {
                println("[$type][simple] : cannot perform on $file due to $e")
                f++
            }
        }
        println("[$type][simple] : on ${cfgInfo.size} files: success = $s, failed = $f, skipped = $m")
    }

    @Test
    fun constpropWl() {
        val type = FlowSensitiveAnalysisObj.AnalysisType.constprop
        val output = File("$outputWl/$type")
        output.mkdirs()
        var s = 0
        var f = 0
        var m = 0
        for (file in cfgInfo.keys) {
            val cfg = cfgInfo[file]!!
            val data = declData[file]!!
            //println("[$type][wl] : proceed on $file")
            try {
                val wl = ConstantPropagationAnalysisWorklistSolver(cfg, data).analyze()
                Output.output(File("examples/$file"), DataFlowOutput(type), cfg.toDot({ node ->
                    Output.labeler(wl, node)
                }, { node -> Output.dotIder(node) }), output)
                s++
            } catch (e: Exception) {
                //println("[$type][wl] : cannot perform on $file due to $e")
                f++
            }
        }
        println("[$type][wl] : on ${cfgInfo.size} files: success = $s, failed = $f")
    }

    @Test
    fun intervalWli() {
        val type = FlowSensitiveAnalysisObj.AnalysisType.interval
        val output = File("$outputWli/$type")
        output.mkdirs()
        var s = 0
        var f = 0
        var m = 0
        val intervalWliFiles = File("examples/interval_wli/").list()!!
        for (file in cfgInfo.keys) {
            if (file !in intervalWliFiles) m++
            else {
                val cfg = cfgInfo[file]!!
                val data = declData[file]!!
                try {
                    println("[$type][wli] : proceed on $file")
                    val wli = IntervalAnalysisWorklistSolverWithInit(cfg, data).analyze()
                    Output.output(File("examples/$file"), DataFlowOutput(type), cfg.toDot({ node ->
                        Output.labeler(wli, node)
                    }, { node -> Output.dotIder(node) }), output)
                    s++
                } catch (e: Exception) {
                    println("[$type][wli] : cannot perform on $file due to $e")
                    f++
                }
            }
        }
        println("[$type][wli] : on ${cfgInfo.size} files: success = $s, failed = $f, skipped = $m")
    }

    @Test
    fun intervalWliw() {
        val type = FlowSensitiveAnalysisObj.AnalysisType.interval
        val output = File("$outputWliw/$type")
        output.mkdirs()
        var s = 0
        var f = 0
        var m = 0
        for (file in cfgInfo.keys) {
            val cfg = cfgInfo[file]!!
            val data = declData[file]!!
            try {
                println("[$type][wliw] : proceed on $file")
                val wliw = IntervalAnalysisWorklistSolverWithWidening(cfg, data).analyze()
                Output.output(File("examples/$file"), DataFlowOutput(type), cfg.toDot({ node ->
                    Output.labeler(wliw, node)
                }, { node -> Output.dotIder(node) }), output)
                s++
            } catch (e: Exception) {
                println("[$type][wliw] : cannot perform on $file due to $e")
                f++
            }
        }
        println("[$type][wliw] : on ${cfgInfo.size} files: success = $s, failed = $f")
    }

    @Test
    fun intervalWliwn() {
        val type = FlowSensitiveAnalysisObj.AnalysisType.interval
        val output = File("$outputWliwn/$type")
        output.mkdirs()
        var s = 0
        var f = 0
        var m = 0
        for (file in cfgInfo.keys) {
            val cfg = cfgInfo[file]!!
            val data = declData[file]!!
            try {
                println("[$type][wliwm] : proceed on $file")
                val wliwn = IntervalAnalysisWorklistSolverWithWideningAndNarrowing(cfg, data).analyze()
                Output.output(File("examples/$file"), DataFlowOutput(type), cfg.toDot({ node ->
                    Output.labeler(wliwn, node)
                }, { node -> Output.dotIder(node) }), output)
                s++
            } catch (e: Exception) {
                println("[$type][wliwn] : cannot perform on $file due to $e")
                f++
            }
        }
        println("[$type][wliwn] : on ${cfgInfo.size} files: success = $s, failed = $f")
    }

    @Test
    fun types() {
        typesUF()
    }

    @Test
    fun signCfg() {
        signCfgSimple()
        signCfgWl()
        signCfgWli()
        signCfgWlip()
    }

    @Test
    fun signIcfg() {
        signIcfgIwli()
        signIcfgIwlip()
        signIcfgCsiwlip()
        signIcfgCfiwlip()
    }

    @Test
    fun livevars() {
        livevarsSimple()
        livevarsWl()
    }

    @Test
    fun available() {
        availableSimple()
        availableWl()
    }

    @Test
    fun constprop() {
        constpropSimple()
        constpropWl()
    }

    @Test
    fun interval() {
        intervalWli()
        intervalWliw()
        intervalWliwn()
    }
}