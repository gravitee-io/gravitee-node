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
package io.gravitee.node.cluster.member;

import io.gravitee.node.api.cluster.Member;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NodeMember implements Member {

  private final com.hazelcast.cluster.Member member;
  private final boolean master;

  public NodeMember(com.hazelcast.cluster.Member member, boolean master) {
    this.member = member;
    this.master = master;
  }

  @Override
  public String uuid() {
    return member.getUuid().toString();
  }

  @Override
  public boolean master() {
    return master;
  }

  @Override
  public String host() {
    return member.getAddress().getHost();
  }

  @Override
  public Map<String, String> attributes() {
    return member.getAttributes();
  }

  @Override
  public Member attribute(String key, String value) {
    member.getAttributes().put(key, value);
    return this;
  }
}
