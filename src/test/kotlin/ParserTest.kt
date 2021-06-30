import ast.*
import com.github.h0tk3y.betterParse.grammar.parseToEnd
import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.Parsed
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import parser.TIPGrammar
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ParserTest {

    private lateinit var grammar: TIPGrammar

    @BeforeAll
    fun init() {
        grammar = TIPGrammar()
    }

    @BeforeEach
    fun resetLastBreaks() {
        grammar.lastBreaks = mutableListOf(0)
    }

    @Test
    fun examples() {
        File("examples/all/").walk().forEach { file ->
            if (file.isFile) {
                val text = file.readText()
                val res = grammar.tryParseToEnd(text)
                assert(res is Parsed<*> && res.value is AProgram)
            }
        }
    }

    @Test
    fun test1() {
        val text = "test(f,a){\n" +
                "    return (*f)(a);\n" +
                "}\n"
        val exp = AProgram(
            listOf(
                AFunDeclaration(
                    "test",
                    listOf(AIdentifierDeclaration("f", Loc(1,1)), AIdentifierDeclaration("a", Loc(1,3))),
                    AFunBlockStmt(
                        listOf(),
                        listOf(),
                        AReturnStmt(
                            ACallFuncExpr(
                                AUnaryOp(DerefOp, AIdentifier("f", Loc(2,9)), Loc(2,8)),
                                listOf(AIdentifier("a", Loc(2,12))),
                                indirect = true,
                                Loc(2,7)
                            ),
                            Loc(2,5)),
                        Loc(2,-1)),
                    Loc(1,-1))),
            Loc(1,-1))
        val res = grammar.parseToEnd(text)
        assert(res == exp)
    }

    @Test
    fun test2() {
        val text = "test(){\n" +
                "    var a;" +
                "    return 0;\n" +
                "}\n"
        val exp = AProgram(
            listOf(
                AFunDeclaration(
                    "test",
                    listOf(),
                    AFunBlockStmt(
                        listOf(AVarStmt(
                            listOf(AIdentifierDeclaration("a", Loc(2, 7))),
                            Loc(2,5)
                        )),
                        listOf(),
                        AReturnStmt(
                            ANumber(0, Loc(2,15)),
                            Loc(2,13)
                        ),
                        Loc(2,-1)
                    ),
                    Loc(1,-1)
                )),
            Loc(1,-1))
        val res = grammar.parseToEnd(text)
        assert(res == exp)
    }

    @Test
    fun test3() {
        val text = "test(){\n" +
                "    var a, b;" +
                "    return a+b;\n" +
                "}\n"
        val exp = AProgram(
            listOf(
                AFunDeclaration(
                    "test",
                    listOf(),
                    AFunBlockStmt(
                        listOf(AVarStmt(
                            listOf(AIdentifierDeclaration("a", Loc(2, 7)),
                                AIdentifierDeclaration("b", Loc(2, 9))
                            ),
                            Loc(2,5)
                        )),
                        listOf(),
                        AReturnStmt(
                            ABinaryOp(
                                Plus,
                                AIdentifier("a", Loc(2,18)),
                                AIdentifier("b", Loc(2,20)),
                                Loc(2,19)),
                            Loc(2,16)
                        ),
                        Loc(2,-1)
                    ),
                    Loc(1,-1)
                )),
            Loc(1,-1))
        val res = grammar.parseToEnd(text)
        assert(res == exp)
    }

    @Test
    fun test4() {
        val text = "test(){\n" +
                "    var a, b;" +
                "    return a-b;\n" +
                "}\n"
        val exp = AProgram(
            listOf(
                AFunDeclaration(
                    "test",
                    listOf(),
                    AFunBlockStmt(
                        listOf(AVarStmt(
                            listOf(AIdentifierDeclaration("a", Loc(2, 7)),
                                AIdentifierDeclaration("b", Loc(2, 9))
                            ),
                            Loc(2,5)
                        )),
                        listOf(),
                        AReturnStmt(
                            ABinaryOp(
                                Minus,
                                AIdentifier("a", Loc(2,18)),
                                AIdentifier("b", Loc(2,20)),
                                Loc(2,19)),
                            Loc(2,16)
                        ),
                        Loc(2,-1)
                    ),
                    Loc(1,-1)
                )),
            Loc(1,-1))
        val res = grammar.parseToEnd(text)
        assert(res == exp)
    }

    @Test
    fun test5() {
        val text = "test(){\n" +
                "    var a, b;" +
                "    return a*b;\n" +
                "}\n"
        val exp = AProgram(
            listOf(
                AFunDeclaration(
                    "test",
                    listOf(),
                    AFunBlockStmt(
                        listOf(AVarStmt(
                            listOf(AIdentifierDeclaration("a", Loc(2, 7)),
                                AIdentifierDeclaration("b", Loc(2, 9))
                            ),
                            Loc(2,5)
                        )),
                        listOf(),
                        AReturnStmt(
                            ABinaryOp(
                                Times,
                                AIdentifier("a", Loc(2,18)),
                                AIdentifier("b", Loc(2,20)),
                                Loc(2,19)),
                            Loc(2,16)
                        ),
                        Loc(2,-1)
                    ),
                    Loc(1,-1)
                )),
            Loc(1,-1))
        val res = grammar.parseToEnd(text)
        assert(res == exp)
    }

    @Test
    fun test6() {
        val text = "test(){\n" +
                "    var a, b;" +
                "    return a/b;\n" +
                "}\n"
        val exp = AProgram(
            listOf(
                AFunDeclaration(
                    "test",
                    listOf(),
                    AFunBlockStmt(
                        listOf(AVarStmt(
                            listOf(AIdentifierDeclaration("a", Loc(2, 7)),
                                AIdentifierDeclaration("b", Loc(2, 9))
                            ),
                            Loc(2,5)
                        )),
                        listOf(),
                        AReturnStmt(
                            ABinaryOp(
                                Divide,
                                AIdentifier("a", Loc(2,18)),
                                AIdentifier("b", Loc(2,20)),
                                Loc(2,19)),
                            Loc(2,16)
                        ),
                        Loc(2,-1)
                    ),
                    Loc(1,-1)
                )),
            Loc(1,-1))
        val res = grammar.parseToEnd(text)
        assert(res == exp)
    }

    @Test
    fun test8() {
        val text = "test(){\n" +
                "    var a,b;" +
                "}\n"
        val res = grammar.tryParseToEnd(text)
        assert(res !is Parsed<*>)
    }

    @Test
    fun test9() {
        val text = "test{\n" +
                "    return a/b;\n" +
                "}\n"
        val res = grammar.tryParseToEnd(text)
        assert(res !is Parsed<*>)
    }

    @Test
    fun test10() {
        val text = "test(f,a){\n" +
                "    return _l;\n" +
                "}\n"
        val res = grammar.tryParseToEnd(text)
        assert(res !is Parsed<*>)
    }
}