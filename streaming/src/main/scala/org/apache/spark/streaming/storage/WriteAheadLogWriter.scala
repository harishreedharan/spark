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

import java.io.Closeable
import java.nio.ByteBuffer

import org.apache.hadoop.fs.FSDataOutputStream

private[streaming] class WriteAheadLogWriter(path: String) extends Closeable {
  private val stream = HdfsUtils.getOutputStream(path)
  private var nextOffset = stream.getPos
  private var closed = false
  private val hflushMethod = {
    try {
      Some(classOf[FSDataOutputStream].getMethod("hflush", new Array[Class[Object]](0): _*))
    } catch {
      case e: Exception => None
    }
  }

  // Data is always written as:
  // - Length - Long
  // - Data - of length = Length
  def write(data: ByteBuffer): FileSegment = synchronized {
    assertOpen()
    val lengthToWrite = data.remaining()
    val segment = new FileSegment(path, nextOffset, lengthToWrite)
    stream.writeInt(lengthToWrite)
    if (data.hasArray) {
      stream.write(data.array())
    } else {
      // If the buffer is not backed by an array we need to copy the data to an array
      data.rewind() // Rewind to ensure all data in the buffer is retrieved
      val dataArray = new Array[Byte](lengthToWrite)
      data.get(dataArray)
      stream.write(dataArray)
    }
    hflushMethod.foreach(_.invoke(stream))
    nextOffset = stream.getPos
    segment
  }

  override private[streaming] def close(): Unit = synchronized {
    closed = true
    stream.close()
  }

  private def assertOpen() {
    HdfsUtils.checkState(!closed, "Stream is closed. Create a new Writer to write to file.")
  }
}
