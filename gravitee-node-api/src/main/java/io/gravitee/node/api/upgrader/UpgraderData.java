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
package io.gravitee.node.api.upgrader;

import java.util.Date;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UpgraderData {

  /**
   * The unique identifier of the Upgrader object.
   */
  private String id;

  /**
   * date that the Upgrader is applied
   */
  private Date appliedAt;

  public UpgraderData() {}

  public UpgraderData(String id, Date appliedAt) {
    this.id = id;
    this.appliedAt = appliedAt;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Date getAppliedAt() {
    return appliedAt;
  }

  public void setAppliedAt(Date appliedAt) {
    this.appliedAt = appliedAt;
  }
}
