package com.example.data.update

/**
 * Robust Semantic Versioning (SemVer) parser and comparator.
 *
 * Correctly compares multi-part numeric versions (e.g. 1.10.0 > 1.2.0, 2.0.0 > 1.9.9),
 * handles leading 'v' / 'V' prefixes, optional patch versions (1.0 vs 1.0.0),
 * and gracefully falls back on unexpected version formats without throwing exceptions.
 */
data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val prerelease: String? = null
) : Comparable<SemVer> {

    override fun compareTo(other: SemVer): Int {
        if (this.major != other.major) {
            return this.major.compareTo(other.major)
        }
        if (this.minor != other.minor) {
            return this.minor.compareTo(other.minor)
        }
        if (this.patch != other.patch) {
            return this.patch.compareTo(other.patch)
        }
        // If versions are otherwise identical, a release with no prerelease is newer than one with a prerelease.
        if (this.prerelease == null && other.prerelease != null) return 1
        if (this.prerelease != null && other.prerelease == null) return -1
        if (this.prerelease != null && other.prerelease != null) {
            return this.prerelease.compareTo(other.prerelease)
        }
        return 0
    }

    companion object {
        fun parse(versionStr: String?): SemVer {
            if (versionStr.isNullOrBlank()) {
                return SemVer(0, 0, 0)
            }
            try {
                // Strip whitespace and optional 'v' prefix
                var clean = versionStr.trim()
                if (clean.startsWith("v", ignoreCase = true)) {
                    clean = clean.substring(1).trim()
                }

                // Check for prerelease suffix (e.g. "-alpha.1", "+build123")
                var prerelease: String? = null
                val dashIdx = clean.indexOfAny(charArrayOf('-', '+'))
                if (dashIdx != -1) {
                    prerelease = clean.substring(dashIdx + 1)
                    clean = clean.substring(0, dashIdx)
                }

                val parts = clean.split(".")
                val major = parts.getOrNull(0)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
                val minor = parts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
                val patch = parts.getOrNull(2)?.filter { it.isDigit() }?.toIntOrNull() ?: 0

                return SemVer(major, minor, patch, prerelease)
            } catch (_: Exception) {
                return SemVer(0, 0, 0)
            }
        }

        fun isLessThan(installedVersion: String?, minimumSupportedVersion: String?): Boolean {
            val installed = parse(installedVersion)
            val minimum = parse(minimumSupportedVersion)
            return installed < minimum
        }

        fun isAtLeast(installedVersion: String?, targetVersion: String?): Boolean {
            val installed = parse(installedVersion)
            val target = parse(targetVersion)
            return installed >= target
        }
    }
}
