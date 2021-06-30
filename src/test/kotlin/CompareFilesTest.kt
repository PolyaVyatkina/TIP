import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import java.io.File

@TestInstance(Lifecycle.PER_CLASS)
internal class CompareFilesTest {

    val loc = """:\d+:\d+""".toRegex()
    val allSymbols = """[\w\s\d.,+-=></*:;(){}]""".toRegex()
    val id = """\w+""".toRegex()

    val results = mutableMapOf<String, String>()
    val expected = mutableMapOf<String, String>()

    fun prepare(resDir: List<String>, expDir: List<String>) {
        for (dir in resDir) {
            File(dir).walk().forEach { file ->
                if (file.isFile && file.name.first() != '.') {
                    val text = file.readText()
                    results[file.name] = text
                }
            }
        }
        for (dir in expDir) {
            File(dir).walk().forEach { file ->
                if (file.isFile && file.name.first() != '.') {
                    val text = file.readText()
                    expected[file.name] = text
                }
            }
        }
    }

    fun parse(s: String): Map<String, List<String>> {
        val m = mutableMapOf<String, List<String>>()
        val lines: List<String> = s.split("\n").filterNot { it.contains("[label=\"\"]") }
        val regexEntry = """(Function $id entry) [\[{]($allSymbols*)[}\]]""".toRegex()
        val regexExit = """(Function $id exit) [\[{]($allSymbols*)[}\]]""".toRegex()
        val regexStmt = """\[Stmt] ($allSymbols+) [\[{]($allSymbols*)[}\]]""".toRegex()
        lines.forEach { line ->
            val matchResultStmt = regexStmt.find(line)
            val matchResultEntry = regexEntry.find(line)
            val matchResultExit = regexExit.find(line)
            if (matchResultEntry != null) {
                val (k, v) = matchResultEntry.destructured
                m[k.replace(loc, "")] = v.replace(loc, "").split(", ")
            }
            if (matchResultExit != null) {
                val (k, v) = matchResultExit.destructured
                m[k.replace(loc, "")] = v.replace(loc, "").split(", ")
            }
            if (matchResultStmt != null) {
                val (k, v) = matchResultStmt.destructured
                m[k.replace(loc, "")] = v.replace(loc, "").split(", ")
            }
        }
//        println("\n\n$s\n$m\n\n")
        return m
    }

    fun parseGraph(s: String): Set<Pair<String, String>> {
        val lines: Pair<List<String>, List<String>> = s.split("\n").partition { it.contains(" -> ") }
        val stmts = mutableMapOf<String, String>()
        for (line in lines.second.filter { it.contains("label") }) {
            val parts = line.split("[label=\"")
            stmts[parts[0]] = parts[1].replace(loc, "").replace("\"]", "")
        }
        val deps = mutableSetOf<Pair<String, String>>()
        for (line in lines.first) {
            val parts = line.split(" -> ")
            val pair = parts[0] to parts[1].replace("[label=\"\"]", "")
            deps.add(stmts[pair.first]!! to stmts[pair.second]!!)
        }
        return deps
    }

    fun checkEquals(res: Map<String, List<String>>, exp: Map<String, List<String>>): Boolean =
        if (res.size != exp.size) {
            println("\nDifferent size\n")
            println()
            println("---------------------------------")
            println(res.keys.sorted())
            println(exp.keys.sorted())
            println("---------------------------------")
            println()
            false
        }
        else if (!res.keys.containsAll(exp.keys)) {
            println()
            println("---------------------------------")
            println(res.keys)
            println(exp.keys)
            println("---------------------------------")
            println()
            false
        }
        else {
            exp.entries.forEach {
                if (!it.value.containsAll(res[it.key]!!)) {
                    println()
                    println("---------------------------------")
                    println(it.value)
                    println(res[it.key])
                    println("---------------------------------")
                    println()
                    return false
                }
            }
            true
        }


    @Test
    fun compare(resDir: List<String>, expDir: List<String>) {
        prepare(resDir, expDir)
        var s = 0
        var f = 0
        var m = 0
        for (file in results.keys) {
            var passed = false
            print("Processing $file")
            if (expected[file] != null) {
                val text = results[file]!!
                val expectedText = expected[file]!!
                    .replace("digraph CFG{", "digraph CFG{\n")
                    .replace("Map(", "{")
                    .replace("Set(", "{")
                    .replace("\\{", "{")
                    .replace("\\}", "}")
                    .replace(")\"]", "}\"]")
                    .replace(" -\\> ", "=")
                    .replace("\\>", ">")
                    .replace("\\n-\"", "\\n{}\"")
                    .replace("\\n-\"", "\\n{}\"")
                    .replace("\\n", " ")
                val res = parse(text)
                val exp = parse(expectedText)
                if (checkEquals(res, exp)) passed = true
            } else m++
            if (passed) {
                println(" – passed"); s++
            }
            else {
                println(" – failed"); f++
            }
        }
        println("Success = $s, failed = $f (missed = $m)")
        println("Results ${results.size}, expected ${expected.size}")
        println("Skipped ${expected.keys - results.keys}")
    }

    fun compareGraph(resDir: List<String>, expDir: List<String>) {
        prepare(resDir, expDir)
        var s = 0
        var f = 0
        var m = 0
        for (file in results.keys) {
            var passed = false
            print("Processing $file")
            if (expected[file] != null) {
                val text = results[file]!!
                val expectedText = expected[file]!!
                    .replace("digraph CFG{", "digraph CFG{\n")
                    .replace("\\{", "{")
                    .replace("\\}", "}")
                    .replace("\\>", ">")
                val res = parseGraph(text)
                val exp = parseGraph(expectedText)
                println("\n${res}\n")
                println("\n${exp}\n")
                if (res == exp) passed = true
            } else m++
            if (passed) {
                println(" – passed"); s++
            }
            else {
                println(" – failed"); f++;
            }
        }
        println("Success = $s, failed = $f (missed = $m)")
        println("Results ${results.size}, expected ${expected.size}")
        println("Skipped ${expected.keys - results.keys}")
    }

    fun compareType(resDir: List<String>, expDir: List<String>) {
        prepare(resDir, expDir)
        var s = 0
        var f = 0
        var m = 0
        for (file in results.keys) {
            var passed = false
            print("Processing $file")
            if (expected[file] != null) {
                val text = results[file]!!
                val expectedText = expected[file]!!
                if (text == expectedText) passed = true
            } else m++
            if (passed) {
                println(" – passed"); s++
            }
            else {
                println(" – failed"); f++
            }
        }
        println("Success = $s, failed = $f (missed = $m)")
        println("Results ${results.size}, expected ${expected.size}")
        println("Skipped ${expected.keys - results.keys}")
        println("Missed ${results.keys - expected.keys}")

    }


    @Test
    fun compareTypes() {
        val kt = listOf("tests/tests_types/")
        val sc = listOf("tests_scala/tests_types/")
        compareType(kt, sc)
    }

    @Test
    fun compareCfg() {
        val kt = listOf("tests/tests_cfg/")
        val sc = listOf("tests_scala/tests_cfg/")
        compareGraph(kt, sc)
    }

    @Test
    fun compareIcfg() {
        val kt = File("tests/tests_icfg/").list()!!.map { "tests/tests_icfg/$it" }
        val sc = File("tests_scala/tests_icfg/").list()!!.map { "tests_scala/tests_icfg/$it" }
        compareGraph(kt, sc)
    }

    @Test
    fun compareSimple() {
        val kt = File("tests/tests_simple/").list()!!.map { "tests/tests_simple/$it" }
        val sc = File("tests_scala/tests_simple/").list()!!.map { "tests_scala/tests_simple/$it" }
        compare(kt, sc)
    }

    @Test
    fun compareWl() {
        val kt = File("tests/tests_wl/").list()!!.map { "tests/tests_wl/$it" }
        val sc = File("tests_scala/tests_wl/").list()!!.map { "tests_scala/tests_wl/$it" }
        compare(kt, sc)
    }

    @Test
    fun compareWli() {
        val kt = File("tests/tests_wli/").list()!!.map { "tests/tests_wli/$it" }
        val sc = File("tests_scala/tests_wli/").list()!!.map { "tests_scala/tests_wli/$it" }
        compare(kt, sc)
    }

    @Test
    fun compareWlip() {
        val kt = File("tests/tests_wlip/").list()!!.map { "tests/tests_wlip/$it" }
        val sc = File("tests_scala/tests_wlip/").list()!!.map { "tests_scala/tests_wlip/$it" }
        compare(kt, sc)
    }

    @Test
    fun compareCustom() {
        val kt = listOf("tests/tests_simple/constprop/")
        val sc = listOf("tests_scala/tests_simple/constprop/")
        compare(kt, sc)
    }


}