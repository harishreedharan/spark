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

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileStatus, FSDataInputStream, FSDataOutputStream, Path}

private[streaming] object HdfsUtils {

  def getOutputStream(path: String): FSDataOutputStream = {
    // HDFS is not thread-safe when getFileSystem is called, so synchronize on that

    val dfsPath = new Path(path)
    val conf = new Configuration()
    val dfs =
      this.synchronized {
        dfsPath.getFileSystem(new Configuration())
      }
    // If the file exists and we have append support, append instead of creating a new file
    val stream: FSDataOutputStream = {
      if (conf.getBoolean("hdfs.append.support", false) && dfs.isFile(dfsPath)) {
        dfs.append(dfsPath)
      } else {
        dfs.create(dfsPath)
      }
    }
    stream
  }

  def getInputStream(path: String): FSDataInputStream = {
    val dfsPath = new Path(path)
    val conf = new Configuration()
    val dfs = this.synchronized {
      dfsPath.getFileSystem(new Configuration())
    }
    val instream = dfs.open(dfsPath)
    instream
  }

  def checkState(state: Boolean, errorMsg: => String) {
    if(!state) {
      throw new IllegalStateException(errorMsg)
    }
  }

}
