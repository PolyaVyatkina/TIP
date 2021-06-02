package utils

import analysis.CallContext
import analysis.FlowSensitiveAnalysis
import analysis.FlowSensitiveAnalysisObj
import cfg.*
import java.io.File
import java.io.PrintWriter

/**
 * Basic outputting functionality.
 */
object Output {

    val log = Log.logger(this.javaClass)

    /**
     * Generate an output to a file.
     * @param file the output file
     * @param kind output kind (determines the file name suffix)
     * @param outFolder the output directory
     */
    fun output(file: File, kind: OutputKind, content: String, outFolder: File) {
        println(content)
        val extension = when (kind) {
            OtherOutput(OutputKindE.CFG) -> "_cfg.dot"
            OtherOutput(OutputKindE.ICFG) -> "_icfg.dot"
            OtherOutput(OutputKindE.TYPES) -> "_types.ttip"
            is DataFlowOutput -> "_$kind.dot"
            else -> throw IllegalArgumentException()
        }
        val outFile = File(outFolder, "${file.nameWithoutExtension}_$extension")
        val pw = PrintWriter(outFile, "UTF-8")
        pw.write(content)
        pw.close()
        log.info("Results of $kind analysis of $file written to $outFile")
    }

    /**
     * Escapes special characters in the given string.
     * Special characters are all Unicode chars except 0x20-0x7e but including \, ", {, and }.
     */
    fun escape(s: String?): String? {
        if (s == null)
            return null
        val b = StringBuilder()
        for (i in s.indices) {
            when (val c = s[i]) {
                '"' -> b.append("\\\"")
                '\\' -> b.append("\\\\")
                '\b' -> b.append("\\b")
                '\t' -> b.append("\\t")
                '\n' -> b.append("\\n")
                '\r' -> b.append("\\r")
                //'\f' -> b.append("\\f")
                '<' -> b.append("\\<")
                '>' -> b.append("\\>")
                '{' -> b.append("\\{")
                '}' -> b.append("\\}")
                else ->
                    if (c >= 0x20.toChar() && c <= 0x7e.toChar())
                        b.append(c)
                    else {
                        b.append("\\%04X".format(c.toInt()))
                    }
            }
        }
        return b.toString()
    }

    /**
     * Helper function for producing string output for a control-flow graph node after an analysis.
     * @param res map from control-flow graph nodes to strings, as produced by the analysis
     */
    fun labeler(res: Map<CfgNode, *>, n: CfgNode) =
        when (n) {
            is CfgFunEntryNode -> "Function ${n.data.name} entry\n${res.getOrDefault(n, "-")}"
            is CfgFunExitNode -> "Function ${n.data.name} exit\n${res.getOrDefault(n, "-")}"
            else -> "$n\n${res.getOrDefault(n, "-")}"
        }


    /**
     * Transforms a map from pairs of call contexts and CFG nodes to values into a map from CFG nodes to strings.
     */
    fun transform(res: Map<Pair<CallContext, CfgNode>, *>): Map<CfgNode, String> {
        val m = mutableMapOf<CfgNode, List<String>>().withDefaultValue(null)
        res.forEach {
            m[it.key.second] = listOf("${it.key.first}: ${it.value}", m[it.key.second].toString())
        }
        return m.toMap().mapValues { it.value!!.joinToString("\n") }.withDefaultValue("")
    }

    /**
     * Generate an unique ID string for the given AST node.
     */
    fun dotIder(n: CfgNode): String =
        when (n) {
            is CfgStmtNode -> "real${n.data.loc.col}_${n.data.loc.line}"
            is CfgFunEntryNode -> "entry${n.data.loc.col}_${n.data.loc.line}"
            is CfgFunExitNode -> "exit${n.data.loc.col}_${n.data.loc.line}"
            is CfgCallNode -> "cally${n.data.loc.col}_${n.data.loc.line}"
            is CfgAfterCallNode -> "acall${n.data.loc.col}_${n.data.loc.line}"
            else -> throw IllegalArgumentException()
        }
}

/**
 * Different kinds of output (determine output file names).
 */
enum class OutputKindE {
    CFG, ICFG, TYPES
}

sealed class OutputKind

/**
 * Output kind for a dataflow analysis (named according to the analysis).
 */
data class DataFlowOutput(val kind: FlowSensitiveAnalysisObj.AnalysisType) : OutputKind() {
    override fun toString(): String = kind.toString()
}

/**
 * Other output kinds (for other processing phases than the actual analysis).
 */
data class OtherOutput(val kind: OutputKindE) : OutputKind() {
    override fun toString(): String = kind.toString()
}