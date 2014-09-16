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
package org.apache.spark.storage

trait BlockRecoveryManager {

  /**
   * Get a list of executors this application is using. This list contains the port and host of
   * the BlockManagers of those executors which can be used to get a list of blocks they hold.
   * @return
   */
  def getExecutors(): ExecutorInfo

  /**
   * Get the data in blocks that were still being written to. These blocks were not completed
   * when the driver died.
   * @return
   */
  def getIncompleteBlocks(): List[Any]
}
