package svcs

import java.io.File
import java.security.MessageDigest


private val HELP_PAGE = """
    These are SVCS commands:
    config     Get and set a username.
    add        Add a file to the index.
    log        Show commit logs.
    commit     Save changes.
    checkout   Restore a file.
""".trimIndent()

private val VCS_PATH: String = ".${File.separator}vcs"
private val CONFIG_PATH: String = "${VCS_PATH}${File.separator}config.txt"
private val INDEX_PATH: String = "${VCS_PATH}${File.separator}index.txt"
private val COMMIT_PATH: String = "${VCS_PATH}${File.separator}commits"
private val LOG_PATH: String = "${VCS_PATH}${File.separator}log.txt"

private val NO_TRACKED_FILES_TEXT = "NoTrackedFiles"
private val NO_COMMIT_TEXT = "Nothing to commit."
private val NO_SUCH_COMMIT = "Commit does not exist."


fun main(args: Array<String>) {

    if(args.isEmpty()) {
        help()
    } else {
        when (args.first()) {
            "--help" -> help()
            "config" -> {
                if(args.size > 1) {
                    config(args[1])
                } else {
                    config()
                }
            }
            "add" -> {
                if(args.size > 1) {
                    add(args[1])
                } else {
                    add()
                }
            }
            "log" -> log()
            "commit" -> {
                if(args.size > 1) {
                    commit(args[1])
                } else {
                    println("Message was not passed.")
                }

            }
            "checkout" -> {
                if(args.size > 1) {
                    checkout(args[1])
                } else {
                    print("Commit id was not passed.")
                }
            }
            else -> wrongArg(args.first())
        }
    }
}


private fun wrongArg(arg: String) {
    println("\'$arg\' is not a SVCS command.")
}

private fun config(configArg: String = "") {
    if(!validateFileExists(VCS_PATH)) {
        File(VCS_PATH).mkdir()
    }

    if(!validateFileExists(COMMIT_PATH)) {
        File(COMMIT_PATH).mkdir()
    }

    if(!validateFileExists(LOG_PATH)) {
        File(LOG_PATH).writeText("")
    }

    val configFile = File(CONFIG_PATH)
    if(configArg == "" && !configFile.exists()) {
        println("Please, tell me who you are.")
    } else {
        if(configArg != "") {
            configFile.writeText("username:${configArg}")
        }
        println("The username is ${getUser()}.")
    }
}

private fun getUser(): String {
    if(validateFileExists(CONFIG_PATH)) {
        return File(CONFIG_PATH).readText().split(":")[1]
    } else {
        return "***ERROR: CONFIG.TXT DOES NOT EXIST***"
    }
}

private fun help() {
    println(HELP_PAGE)
}

private fun add(addArgs: String = "") {
    val trackedFiles = File(INDEX_PATH)
    if(addArgs == "") {
        if(!trackedFiles.exists()) {
            println("Add a file to the index.")
        } else {
            displayTrackedFiles()
        }
    } else {
        if(validateFileExists(addArgs)) {
            if(!File(VCS_PATH).exists()) {
                File(VCS_PATH).mkdir()
            }
            trackedFiles.appendText(addArgs)
            trackedFiles.appendText("\n")
            println("The file \'${addArgs}\' is tracked.")
        } else {
            println("Can't find \'${addArgs}\'.")
        }
    }
}

private fun validateFileExists(filePath: String): Boolean {
    return File(filePath).exists()
}

private fun displayTrackedFiles() {
    val trackedFiles = File(INDEX_PATH)
    if(trackedFiles.exists()) {
        println("Tracked Files:")
        trackedFiles.forEachLine { println(it) }
    }
}

private fun log() {
    val logfile = File(LOG_PATH)

    if(validateFileExists(logfile.absolutePath) && logfile.readText().isNotEmpty()) {
        val commitsStrings: MutableList<String> = mutableListOf()
        val authorsStrings: MutableList<String> = mutableListOf()
        val msgStrings: MutableList<String> = mutableListOf()

        logfile.forEachLine { commitEntry ->
            commitsStrings.add("commit ${commitEntry.split(" ")[0]}")
            authorsStrings.add("Author: ${commitEntry.split(" author:")[1].split(" mesag:")[0]}")
            msgStrings.add(commitEntry.split(" author:")[1].split(" mesag:")[1])
        }

        var i = commitsStrings.size - 1
        while(i > 0) {
            println(commitsStrings[i])
            println(authorsStrings[i])
            println(msgStrings[i])
            println()
            i--
        }

        println(commitsStrings[i])
        println(authorsStrings[i])
        print(msgStrings[i])
    } else {
        println("No commits yet.")
    }

}

private fun commit(msg: String) {
    if(msg.contentEquals("Files were not changed", true) || getTrackedFilesHash() == getLastCommitHash()) {
        println(NO_COMMIT_TEXT)
    } else {
        addNewCommit(msg)
        println("Changes are committed.")
    }
}

private fun getLastCommitHash(): String {
    if(!validateFileExists(LOG_PATH) || File(LOG_PATH).readText().isEmpty()) {
        return "No Commits"
    } else {
        val logFile = File(LOG_PATH)
        val commits = logFile.readLines()
        return commits.last().split(" ")[0]
    }
}

/**
 * Returns the requested commit path, or NO_SUCH_COMMIT string.
 */
private fun getCommit(commitHash: String): String {
    val commitPath = "$COMMIT_PATH${File.separator}$commitHash"
    if(validateFileExists(commitPath)) {
        return commitPath
    } else {
        return NO_SUCH_COMMIT
    }
}

private fun addNewCommit(msg: String): String {
    // The new commit hash will be a hash of the concatenation of each tracked file content hash
    val newCommitHash = getTrackedFilesHash()

    /**
     * "If getTrackedFilesHash didn't return NO_TRACKED_FILES_TEXT AND getTrackedFilesHash does not equal the name of the last commit"
     */
    if(newCommitHash != NO_TRACKED_FILES_TEXT && newCommitHash != getLastCommitHash()) {
        val commitLine = "$newCommitHash author:${getUser()} mesag:$msg"

        val logfile = File(LOG_PATH)
        logfile.appendText(commitLine + "\n")

        val newCommitPath = "${COMMIT_PATH}${File.separator}$newCommitHash"
        File(newCommitPath).mkdir()
        for (file in getTrackedFiles()) {
            file.copyTo(File("$newCommitPath${File.separator}${file.name}"))
        }

        return newCommitHash

    } else {
        return NO_COMMIT_TEXT
    }
}

private fun getTrackedFilesHash(): String {
    // Get all of the currently tracked files
    val trackedFiles = getTrackedFiles()
    var filesHash = ""

    if(!trackedFiles.isNullOrEmpty()) {
        // Concatenate a hash of each file contents to the filesHash var
        for (file in trackedFiles) {
            filesHash += getNewHashString(file.readText())
        }

        return getNewHashString(filesHash)
    } else {
        return NO_TRACKED_FILES_TEXT
    }
}

private fun getTrackedFiles(): List<File> {
    // Get all of the currently tracked files
    val trackedFilesPaths = File(INDEX_PATH).readLines()

    return trackedFilesPaths.map {filePath -> File(filePath)}
}

/**
 * https://www.javacodemonk.com/md5-and-sha256-in-java-kotlin-and-android-96ed9628
 * https://www.tutorialspoint.com/java_cryptography/java_cryptography_message_digest.htm#:~:text=You%20can%20generate%20the%20message%20digest%20using%20the,digest%20method.%20byte%20%5B%5D%20digest%20%3D%20md.digest%20%28%29%3B
 */
private fun  getNewHashString(input: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    return bytes.toHex()
}

private fun ByteArray.toHex():String {
    return joinToString("") { "%02x".format(it)}
}

private fun checkout(commitId: String) {
    if(getCommit(commitId) == NO_SUCH_COMMIT) {
        println(NO_SUCH_COMMIT)
    } else {

        val commitFiles = File(getCommit(commitId)).listFiles()

        for (commitFile in commitFiles!!) {
            commitFile.copyTo(File(commitFile.name), true)
        }

        println("Switched to commit $commitId.")
    }
}