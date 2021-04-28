package utils

const val CONSOLE_RESET = "\u001b[0m"
const val CONSOLE_RED = "\u001b[0;31m"
const val CONSOLE_YELLOW = "\u033b[0;33m"
const val CONSOLE_BLUE = "\u033b[0;34m"

/**
 * Basic logging functionality.
 */
object Log {

    /**
     * Log levels.
     */
    enum class Level {
        NONE, ERROR, WARN, INFO, DEBUG, VERBOSE
    }

    var defaultLevel = Level.NONE

    /**
     * Constructs a new logger.
     * @param forcedLevel log level
     * @param classTag class the logger belongs to
     */
    fun logger(classTag: Any, forcedLevel: Level = defaultLevel): Logger = Logger(classTag.javaClass.name, forcedLevel)

}

data class Logger(var tag: String, val level: Log.Level) {


    private fun log(message: String, t: Throwable?, msgLev: Log.Level) {
        if (msgLev.ordinal <= level.ordinal || msgLev.ordinal < Log.defaultLevel.ordinal) {
            var account = 0
            val color = when (msgLev) {
                Log.Level.ERROR -> {
                    account -= 9
                    //Console.BOLD + Console.RED
                    CONSOLE_RED
                }
                Log.Level.WARN -> {
                    account -= 9;
                    //Console.BOLD + Console.YELLOW
                    CONSOLE_YELLOW
                }
                Log.Level.INFO -> {
                    account -= 9;
                    //Console.BOLD + Console.BLUE
                    CONSOLE_BLUE
                }
                Log.Level.VERBOSE -> {
                    account -= 9;
                    //Console.BOLD + Console.BLUE
                    CONSOLE_BLUE
                }
                else -> {
                    account -= 1;
                    //Console.RESET
                    CONSOLE_RESET
                }
            }
            print("$color[${msgLev.toString().toLowerCase()}] ")
            print(CONSOLE_RESET)
            println(message)
        }
        t?.printStackTrace(System.out)
    }

    /**
     * Writes a message to the log at level "error" .
     */
    fun error(message: String) = log(message, null, Log.Level.ERROR)

    /**
     * Writes a message and a stack trace to the log at level "error".
     */
    fun error(message: String, t: Throwable) = log(message, t, Log.Level.ERROR)

    /**
     * Writes a message to the log at level "warn" .
     */
    fun warn(message: String) = log(message, null, Log.Level.WARN)

    /**
     * Writes a message and a stack trace to the log at level "warn".
     */
    fun warn(message: String, t: Throwable) = log(message, t, Log.Level.WARN)

    /**
     * Writes a message to the log at level "info" .
     */
    fun info(message: String) = log(message, null, Log.Level.INFO)

    /**
     * Writes a message and a stack trace to the log at level "info".
     */
    fun info(message: String, t: Throwable) = log(message, t, Log.Level.INFO)

    /**
     * Writes a message to the log at level "debug" .
     */
    fun debug(message: String) = log(message, null, Log.Level.DEBUG)

    /**
     * Writes a message and a stack trace to the log at level "debug".
     */
    fun debug(message: String, t: Throwable) = log(message, t, Log.Level.DEBUG)

    /**
     * Writes a message to the log at level "verbose" .
     */
    fun verb(message: String) = log(message, null, Log.Level.VERBOSE)

    /**
     * Writes a message and a stack trace to the log at level "verbose".
     */
    fun verb(message: String, t: Throwable) = log(message, t, Log.Level.VERBOSE)

}