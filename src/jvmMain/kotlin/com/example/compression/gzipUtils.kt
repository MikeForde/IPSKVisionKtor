package com.example.compression

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Compresses the given data into GZIP format.
 *
 * @param data The data to be compressed; may be a ByteArray or a String.
 * @return ByteArray containing the compressed GZIP data.
 * @throws IOException if compression fails.
 */
suspend fun gzipEncode(data: Any): ByteArray =
    withContext(Dispatchers.IO) {
      val inputBytes =
          when (data) {
            is ByteArray -> data
            is String -> data.toByteArray(Charsets.UTF_8)
            else -> throw IllegalArgumentException("Data must be ByteArray or String")
          }

      ByteArrayOutputStream().use { byteStream ->
        GZIPOutputStream(byteStream).use { gzipStream ->
          gzipStream.write(inputBytes)
          // finishing the stream ensures all data is written
          gzipStream.finish()
        }
        byteStream.toByteArray()
      }
    }

/**
 * Decompresses the given GZIP-compressed data.
 *
 * @param compressedData ByteArray containing GZIP-compressed data.
 * @return The decompressed data as a UTF-8 String.
 * @throws IOException if decompression fails.
 */
suspend fun gzipDecode(compressedData: ByteArray): String =
    withContext(Dispatchers.IO) {
      ByteArrayInputStream(compressedData).use { byteIn ->
        GZIPInputStream(byteIn).use { gzipIn ->
          // Read all decompressed bytes into a buffer
          val buffer = ByteArrayOutputStream()
          val tmp = ByteArray(4096)
          var read = gzipIn.read(tmp)
          while (read >= 0) {
            buffer.write(tmp, 0, read)
            read = gzipIn.read(tmp)
          }
          buffer.toString(Charsets.UTF_8.name())
        }
      }
    }
