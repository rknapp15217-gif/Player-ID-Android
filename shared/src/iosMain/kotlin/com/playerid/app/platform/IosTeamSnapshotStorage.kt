package com.playerid.app.platform

import com.playerid.app.domain.team.TeamSnapshotStorage
import com.playerid.app.domain.team.TeamStorageException
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.BetaInteropApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosTeamSnapshotStorage : TeamSnapshotStorage {
    private fun path(): String {
        val manager = NSFileManager.defaultManager
        val support = manager.URLForDirectory(
            NSApplicationSupportDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null
        )?.path ?: throw TeamStorageException("Cannot locate PlayerID's application data directory.")
        val directory = "$support/PlayerID"
        if (!manager.createDirectoryAtPath(directory, withIntermediateDirectories = true, attributes = null, error = null)) {
            throw TeamStorageException("Cannot create PlayerID's application data directory.")
        }
        return "$directory/team-workspace-v1.json"
    }

    override suspend fun read(): String? = withContext(Dispatchers.Default) {
        val file = path()
        if (!NSFileManager.defaultManager.fileExistsAtPath(file)) return@withContext null
        NSString.stringWithContentsOfFile(file, encoding = NSUTF8StringEncoding, error = null)
            ?: throw TeamStorageException("Cannot read PlayerID's saved data. The file has not been changed.")
    }

    override suspend fun write(contents: String) = withContext(Dispatchers.Default) {
        val saved = NSString.create(string = contents).writeToFile(
            path(),
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null
        )
        if (!saved) {
            throw TeamStorageException("PlayerID could not save your changes. Check the iPhone's available storage and try again.")
        }
    }
}
