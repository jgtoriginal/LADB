package com.draco.ladb.utils

/**
 * Utility class for validating input data
 */
object ValidationUtils {

    /**
     * Validates if a string is a valid Android package name.
     * Package names must match the pattern: [a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)*
     *
     * @param packageName The package name to validate
     * @return true if the package name is valid, false otherwise
     */
    fun isValidPackageName(packageName: String): Boolean {
        if (packageName.isEmpty()) return false

        // Package name regex pattern
        val packageNamePattern = Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$")
        return packageNamePattern.matches(packageName)
    }

    /**
     * Validates if a string is a valid ADB pairing port number.
     * Port numbers must be between 1 and 65535.
     *
     * @param port The port number string to validate
     * @return true if the port is valid, false otherwise
     */
    fun isValidPort(port: String): Boolean {
        return try {
            val portNum = port.toInt()
            portNum in 1..65535
        } catch (e: NumberFormatException) {
            false
        }
    }

    /**
     * Validates if a string is a non-empty ADB pairing code.
     *
     * @param code The pairing code to validate
     * @return true if the code is valid (non-empty), false otherwise
     */
    fun isValidPairingCode(code: String): Boolean {
        return code.trim().isNotEmpty()
    }

    /**
     * Sanitizes a string to remove potentially dangerous shell metacharacters.
     * Note: This is a basic sanitization and should not be relied upon as the sole security measure.
     *
     * @param input The input string to sanitize
     * @return A sanitized string with shell metacharacters escaped
     */
    fun sanitizeForShell(input: String): String {
        return input.replace(Regex("([;&|`$\\(\\)<>])"), "\\$1")
    }
}
