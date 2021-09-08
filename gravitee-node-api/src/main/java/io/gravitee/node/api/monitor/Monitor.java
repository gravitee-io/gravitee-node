/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.api.monitor;

import io.gravitee.reporter.api.Reportable;
import java.io.Serializable;
import java.time.Instant;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
// TODO: This class is a duplicate of the one coming from gravitee-reporter-api and will soon completely replace it.
public class Monitor implements Serializable, Reportable {

  private String nodeId;
  private long timestamp;

  JvmInfo jvm;
  OsInfo os;
  ProcessInfo process;

  protected Monitor() {}

  public Monitor(String nodeId, final long timestamp) {
    this.nodeId = nodeId;
    this.timestamp = timestamp;
  }

  @Override
  public Instant timestamp() {
    return Instant.ofEpochMilli(timestamp);
  }

  public JvmInfo getJvm() {
    return jvm;
  }

  public OsInfo getOs() {
    return os;
  }

  public ProcessInfo getProcess() {
    return process;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public String getNodeId() {
    return nodeId;
  }

  public static Builder on(String nodeId) {
    return new Builder().on(nodeId);
  }

  public static class Builder {

    private String nodeId;
    private long timestamp;
    private OsInfo os;
    private JvmInfo jvm;
    private ProcessInfo process;

    public Builder on(String nodeId) {
      this.nodeId = nodeId;
      return this;
    }

    public Builder at(long timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Builder os(OsInfo os) {
      this.os = os;
      return this;
    }

    public Builder jvm(JvmInfo jvm) {
      this.jvm = jvm;
      return this;
    }

    public Builder process(ProcessInfo process) {
      this.process = process;
      return this;
    }

    public Monitor build() {
      Monitor metrics = new Monitor(nodeId, timestamp);
      metrics.os = os;
      metrics.jvm = jvm;
      metrics.process = process;
      return metrics;
    }
  }
}
