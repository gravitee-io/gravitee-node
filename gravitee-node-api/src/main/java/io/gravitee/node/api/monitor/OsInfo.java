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
public class OsInfo implements Serializable {

  public long timestamp;

  public Cpu cpu = null;

  public Mem mem = null;

  public Swap swap = null;

  public static class Cpu implements Serializable {

    public short percent = -1;
    public double[] loadAverage = null;

    public short getPercent() {
      return percent;
    }

    public double[] getLoadAverage() {
      return loadAverage;
    }
  }

  public static class Mem implements Serializable {

    public long total = -1;
    public long free = -1;

    public long getTotal() {
      return total;
    }

    public long getUsed() {
      return total - free;
    }

    public short getUsedPercent() {
      return calculatePercentage(getUsed(), getTotal());
    }

    public long getFree() {
      return free;
    }

    public short getFreePercent() {
      return calculatePercentage(getFree(), getTotal());
    }
  }

  public static class Swap implements Serializable {

    public long total = -1;
    public long free = -1;
  }

  private static short calculatePercentage(long used, long max) {
    return max <= 0 ? 0 : (short) (Math.round((100d * used) / max));
  }
}
