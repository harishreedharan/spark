/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.spark.streaming.storage

import java.io.{EOFException, Closeable}
import java.nio.ByteBuffer

private[streaming] class WriteAheadLogReader(path: String)
  extends Iterator[ByteBuffer] with Closeable {

  private val instream = HdfsUtils.getInputStream(path)
  private var closed = false
  private var nextItem: Option[ByteBuffer] = None

  override def hasNext: Boolean = synchronized {
    assertOpen()
    if (nextItem.isDefined) { // handle the case where hasNext is called without calling next
      true
    } else {
      try {
        val length = instream.readInt()
        val buffer = new Array[Byte](length)
        instream.readFully(buffer)
        nextItem = Some(ByteBuffer.wrap(buffer))
        true
      } catch {
        case e: EOFException => false
        case e: Exception => throw e
      }
    }
  }

  override def next(): ByteBuffer = synchronized {
    // TODO: Possible error case where there are not enough bytes in the stream
    // TODO: How to handle that?
    val data = nextItem.getOrElse {
      throw new IllegalStateException("next called without calling hasNext or after hasNext " +
        "returned false")
    }
    nextItem = None // Ensure the next hasNext call loads new data.
    data
  }

  override def close(): Unit = synchronized {
    closed = true
    instream.close()
  }

  private def assertOpen() {
    HdfsUtils.checkState(!closed, "Stream is closed. Create a new Reader to read from the " +
      "file.")
  }

}
