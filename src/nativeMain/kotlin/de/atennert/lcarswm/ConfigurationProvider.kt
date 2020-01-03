package de.atennert.lcarswm

import de.atennert.lcarswm.system.api.PosixApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned

class ConfigurationProvider(posixApi: PosixApi, configurationFilePath: String) : Properties {

    private val readBufferSize = 60

    private val properties: Map<String, String>

    init {
        val filePointer = posixApi.fopen(configurationFilePath, "r")

        properties = if (filePointer == null) {
            emptyMap()
        } else {
            val entryMap = mutableMapOf<String, String>()
            var bufferString = ""

            ByteArray(readBufferSize).usePinned { entry ->
                while (posixApi.fgets(entry.addressOf(0), readBufferSize, filePointer) != null) {
                    bufferString += entry.get()
                        .takeWhile { it > 0 }
                        .fold("") {acc, b -> acc + b.toChar()}

                    if (bufferString.endsWith('\n') || posixApi.feof(filePointer) != 0) {
                        val entryPair = bufferString.trim()
                            .split('=')

                        entryMap[entryPair[0]] = entryPair[1]

                        bufferString = ""
                    }
                }
            }

            posixApi.fclose(filePointer)
            entryMap
        }
    }

    override operator fun get(propertyKey: String): String? = properties[propertyKey]

    override fun getProperyNames(): Set<String> = properties.keys
}