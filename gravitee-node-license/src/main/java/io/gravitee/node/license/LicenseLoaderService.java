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
package io.gravitee.node.license;

import io.gravitee.common.service.AbstractService;
import io.gravitee.node.api.license.*;
import io.gravitee.node.license.management.NodeLicenseManagementEndpoint;
import io.gravitee.node.management.http.endpoint.ManagementEndpointManager;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * @author David Brassely (david.brassely at graviteesource.com)
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@RequiredArgsConstructor
public class LicenseLoaderService extends AbstractService<LicenseLoaderService> {

    private final LicenseManager licenseManager;
    private final LicenseFetcher licenseFetcher;
    private final ManagementEndpointManager managementEndpointManager;

    private License license;

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        final License license = fetchLicense();
        this.setupLicense(license);
        this.licenseFetcher.startWatch(this::setupLicense);
        this.licenseManager.onLicenseExpires(this::onLicenseExpires);
        this.managementEndpointManager.register(new NodeLicenseManagementEndpoint(licenseManager));
    }

    private void onLicenseExpires(License license) {
        if (license == this.license) {
            stopNode();
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        licenseFetcher.stopWatch();
    }

    private License fetchLicense() {
        License license = null;
        try {
            license = licenseFetcher.fetch();
        } catch (MalformedLicenseException mle) {
            log.warn("Provided license is malformed, skipping.", mle);
        } catch (InvalidLicenseException lie) {
            log.error("Provided license is invalid, stopping.", lie);
            stopNode();
        }

        return license;
    }

    private void setupLicense(License license) {
        if (license != null) {
            printLicenseInfo(license);
            licenseManager.registerPlatformLicense(license);
            this.license = license;
        }
    }

    private void printLicenseInfo(License license) {
        final StringBuilder sb = new StringBuilder();
        sb.append("License information: \n");
        license.getRawAttributes().forEach((name, feature) -> sb.append("\t").append(name).append(": ").append(feature).append("\n"));
        log.info(sb.toString());
    }

    private void stopNode() {
        System.exit(0);
    }
}
