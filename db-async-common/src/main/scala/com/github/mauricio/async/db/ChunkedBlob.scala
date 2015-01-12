package com.github.mauricio.async.db

import java.nio.ByteBuffer

import io.netty.buffer.ByteBuf

import concurrent.Future

/**
 * Used to write a BLOB to the database in chunks, to avoid buffering the whole BLOB in memory.
 *
 * Invoke any of the `write` methods multiple times, followed by the `close` method.
 *
 * Make sure to send at least 1 KiB data in each chunk (except possibly the last one), otherwise it will be inefficient.
 *
 * Each chunk can be up to `2^24-9` bytes (almost 16 MiB), the total size of the BLOB can be up to 1 GiB.
 */
trait ChunkedBlob {
  def write(src: Array[Byte]): Future[ChunkedBlob]
  def write(src: ByteBuffer): Future[ChunkedBlob]
  def write(src: ByteBuf): Future[ChunkedBlob]

  def close(): Future[QueryResult]
}
