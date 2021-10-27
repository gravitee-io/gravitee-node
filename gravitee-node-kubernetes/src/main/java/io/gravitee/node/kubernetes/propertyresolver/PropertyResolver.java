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
package io.gravitee.node.kubernetes.propertyresolver;

import io.reactivex.Flowable;
import io.reactivex.Maybe;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 * @since 3.9.11
 */
public interface PropertyResolver {
  /**
   * Check if this property can be resolved
   * @param currentValue
   * @return
   */
  boolean supports(String currentValue);

  /**
   * @param location
   * @return The values of the given property if exist
   */
  Maybe<Object> resolve(String location);

  /**
   * Watch for any changes in the property and emmit the new values
   * @param location
   * @return last value
   */
  Flowable<Object> watch(String location);
}
