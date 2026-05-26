// IUserService.aidl
package com.lamamics.spacecalculator;

/**
 * Runs inside a process spawned by Shizuku with shell (UID 2000) privileges.
 * The file walk happens here so it can read Android/data and Android/obb,
 * which a normal app process cannot on Android 11+.
 */
interface IUserService {
    // Shizuku calls this when the user service is destroyed.
    // The transaction id 16777114 is reserved by Shizuku for destroy().
    void destroy() = 16777114;

    void exit() = 1;

    /**
     * Walks [rootPath], pruning entries below [minSizeBytes], and writes the
     * resulting tree as JSON to [outputPath]. Returns null on success, or an
     * error message on failure.
     */
    String scanToFile(String rootPath, long minSizeBytes, String outputPath) = 2;
}
