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
package io.gravitee.node.api.certificate;

import java.security.cert.CRL;
import java.util.List;

/**
 * Interface for components that can be refreshed with updated Certificate Revocation Lists (CRLs).
 * Implementations should update their internal state with the provided CRLs.
 *
 * @author Guillaume SALA (guillaume.sala at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface CRLRefreshable {
    void refresh(List<CRL> crls);
}
