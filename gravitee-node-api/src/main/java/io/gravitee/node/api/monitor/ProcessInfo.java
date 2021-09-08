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

import java.io.Serializable;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ProcessInfo implements Serializable {

  public long timestamp = -1;

  public long openFileDescriptors = -1;
  public long maxFileDescriptors = -1;

  public Cpu cpu = null;
  public Mem mem = null;

  public static class Cpu implements Serializable {

    public short percent = -1;
    public long total = -1;

    public short getPercent() {
      return percent;
    }

    public long getTotal() {
      return total;
    }
  }

  public static class Mem implements Serializable {

    public long totalVirtual = -1;

    public long getTotalVirtual() {
      return totalVirtual;
    }
  }
}
