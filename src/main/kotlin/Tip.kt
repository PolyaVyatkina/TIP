import analysis.DeclarationAnalysis
import analysis.TypeAnalysis
import ast.AProgram
import ast.AstNodeWithType
import ast.DeclarationData
import ast.TypeData
import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.Parsed
import parser.TIPGrammar
import utils.Log
import utils.OtherOutput
import utils.Output
import utils.OutputKindE
import java.io.File
import java.lang.Exception
import kotlin.system.exitProcess

/**
 * Options for running the TIP system.
 */
class RunOption {

    val log = Log.logger(this)

    /**
     * If set, construct the (intraprocedural) control-flow graph after parsing.
     */
    var cfg = false

    /**
     * If set, construct the interprocedural control-flow graph after parsing.
     */
    var icfg = false

    /**
     * If set, perform type analysis.
     */
    var types = false

    /**
     * If set, perform control-flow analysis.
     */
    var cfa = false

    /**
     * If set, perform Andersen-style pointer analysis.
     */
    var andersen = false

    /**
     * If set, perform Steensgaard-style pointer analysis.
     */
    var steensgaard = false

//    var dfAnalysis = Map[dfa.Value, dfo.Value]().withDefaultValue(dfo.Disabled)

    /**
     * Source file, or directory containing .tip files.
     */
    var source: File? = null

    /**
     * Output directory. (Default: "./out")
     */
    var out: File = File("./out")

    /**
     * If set, execute the program.
     */
    var run = false

    /**
     * If set, perform concolic execution of the program.
     */
    var concolic = false

    /**
     * Checks that a source file or directory has been provided.
     * @return true if success
     */
    fun check(): Boolean =
        if (source == null) {
            log.error("Source file/directory missing")
            false
        } else
            true
}

/**
 * Command-line entry for the TIP system.
 */
object Tip {

    val log = Log.logger(this.javaClass)

    fun printUsage() {
        print(
            """
        | Usage:
        | tip <options> <source> [out]
        |
        | <source> can be a file or a directory,
        |
        | [out] is an output directory (default: ./out)
        |
        | Options for analyzing programs:
        |
        | -cfg               construct the (intraprocedural) control-flow graph, but do not perform any analysis
        | -icfg              construct the interprocedural control-flow graph, but do not perform any analysis
        | -types             enable type analysis
        | -cfa               enable control-flow analysis (interprocedural analyses use the call-graph obtained by this analysis)
        | -andersen          enable Andersen pointer analysis
        | -steensgaard       enable Steensgaard pointer analysis
        | -sign              enable sign analysis
        | -livevars          enable live variables analysis
        | -available         enable available expressions analysis
        | -vbusy             enable very busy expressions analysis
        | -reaching          enable reaching definitions analysis
        | -constprop         enable constant propagation analysis
        | -interval          enable interval analysis
        | -copyconstprop     enable copy constant propagation analysis
        |
        | For the dataflow analyses, the choice of fixpoint solver can be chosen by these modifiers
        | immediately after the analysis name (default: use the simple fixpoint solver):
        |
        | wl       use the worklist solver
        | wli      use the worklist solver with init
        | wliw     use the worklist solver with init and widening
        | wliwn    use the worklist solver with init, widening, and narrowing
        | wlip     use the worklist solver with init and propagation
        | iwli     use the worklist solver with init, interprocedural version
        | iwlip    use the worklist solver with init and propagation, interprocedural version
        | csiwlip  use the worklist solver with init and propagation, context-sensitive (with call string) interprocedural version
        | cfiwlip  use the worklist solver with init and propagation, context-sensitive (with functional approach) interprocedural version
        | ide      use the IDE solver
        |
        | e.g. -sign wl  will run the sign analysis using the basic worklist solver
        |
        | Options for running programs:
        |
        | -run               run the program as the last step
        | -concolic          perform concolic testing (search for failing inputs using dynamic symbolic execution)
        |
        | Other options:
        |
        | -verbose           verbose output
      """
        )
    }

    /**
     * Process the given file according to the specified options.
     */
    fun processFile(file: File, options: RunOption) {
        println(file.name)
        val program = file.readText()
        val tipParser = TIPGrammar()
        tipParser.lastBreaks = mutableListOf(0)
        val res = tipParser.tryParseToEnd(program)
        if (res !is Parsed) {
            log.error("Failure parsing the program: $file\n${res}")
        } else {
            val programNode = res.value as AProgram
            val declData: DeclarationData = DeclarationAnalysis(programNode).analyze()
            // run type analysis, if selected
            if (options.types) {
                // (for information about the use of 'implicit', see [[tip.analysis.TypeAnalysis]])
                val typeData: TypeData = TypeAnalysis(programNode, declData).analyze()
                Output.output(file, OtherOutput(OutputKindE.TYPES), AstNodeWithType(programNode, typeData).toTypedString(), options.out)
            }
        }
        log.info("Success")
    }
}

fun main(args: Array<String>) {
    val tip = Tip
    // parse options
    Log.defaultLevel = Log.Level.INFO
    val options = RunOption()
    var i = 0
    while (i < args.size) {
        val s = args[i]
        if (s.first() == '-')
            when (s) {
                "-cfg" -> options.cfg = true
                "-icfg" -> options.icfg = true
                "-types" -> options.types = true
                "-cfa" -> options.cfa = true
                "-andersen" -> options.andersen = true
                "-steensgaard" -> options.steensgaard = true
                "-run" -> options.run = true
                "-concolic" -> options.concolic = true
                "-verbose" -> Log.defaultLevel = Log.Level.VERBOSE
                else -> {
                    tip.log.error("Unrecognized option $s")
                    tip.printUsage()
                    exitProcess(1)
                }
            } else if (i == args.size - 1 && options.source != null)
            options.out = File(s)
        else if ((i == args.size - 1 && options.source == null) || i == args.size - 2)
            options.source = File(s)
        else {
            tip.log.error("Unexpected argument $s")
            tip.printUsage()
            exitProcess(1)
        }
        i += 1
    }
    if (!options.check()) {
        tip.printUsage()
        exitProcess(1)
    }
    val sources = if (options.source?.isDirectory == true) {
        // directory provided, get the .tip files
        options.source!!.listFiles { fl -> fl.name.endsWith(".tip") }
    } else {
        // single file provided
        arrayOf(options.source)
    }
    options.out.mkdirs()

    // process each source file
    sources.forEach { file ->
        tip.log.info("Processing ${file.name}")
        tip.processFile(file, options)
    }
}