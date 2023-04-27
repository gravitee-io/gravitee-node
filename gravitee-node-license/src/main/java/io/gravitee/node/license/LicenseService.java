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

import ch.qos.logback.classic.Level;
import io.gravitee.common.service.AbstractService;
import io.gravitee.node.api.Node;
import io.gravitee.node.license.license3j.License3JLicense;
import io.gravitee.node.license.management.NodeLicenseManagementEndpoint;
import io.gravitee.node.management.http.endpoint.ManagementEndpointManager;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax0.license3j.License;
import javax0.license3j.io.LicenseReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LicenseService extends AbstractService<LicenseService> {

    private final Logger logger = LoggerFactory.getLogger(LicenseService.class);

    private static final String GRAVITEE_HOME_PROPERTY = "gravitee.home";
    private static final String GRAVITEE_LICENSE_PROPERTY = "gravitee.license";
    private static final String GRAVITEE_LICENSE_KEY = "license.key";
    private static final String LICENSE_EXPIRE_AT = "expiryDate";

    private static final long DAY_AS_LONG = 24 * 60 * 60 * 1000L;

    @Autowired
    private ManagementEndpointManager managementEndpointManager;

    @Autowired
    private Node node;

    @Autowired
    private Environment environment;

    // prettier-ignore
    private final byte [] key = new byte[] {
            (byte)0x52,
            (byte)0x53, (byte)0x41, (byte)0x00, (byte)0x30, (byte)0x82, (byte)0x01, (byte)0x22, (byte)0x30,
            (byte)0x0D, (byte)0x06, (byte)0x09, (byte)0x2A, (byte)0x86, (byte)0x48, (byte)0x86, (byte)0xF7,
            (byte)0x0D, (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x05, (byte)0x00, (byte)0x03, (byte)0x82,
            (byte)0x01, (byte)0x0F, (byte)0x00, (byte)0x30, (byte)0x82, (byte)0x01, (byte)0x0A, (byte)0x02,
            (byte)0x82, (byte)0x01, (byte)0x01, (byte)0x00, (byte)0xD8, (byte)0x59, (byte)0xEC, (byte)0xB6,
            (byte)0x27, (byte)0xF2, (byte)0x20, (byte)0x2F, (byte)0x7A, (byte)0x39, (byte)0x86, (byte)0x2B,
            (byte)0x62, (byte)0xB6, (byte)0xEA, (byte)0x5B, (byte)0xE4, (byte)0x80, (byte)0xA0, (byte)0x32,
            (byte)0x35, (byte)0xB3, (byte)0xC8, (byte)0xD5, (byte)0x4E, (byte)0xB7, (byte)0xA1, (byte)0xFE,
            (byte)0x15, (byte)0x84, (byte)0xC7, (byte)0x75, (byte)0x66, (byte)0x8F, (byte)0x48, (byte)0xF1,
            (byte)0xD6, (byte)0x30, (byte)0x7B, (byte)0x39, (byte)0xB7, (byte)0xD7, (byte)0x48, (byte)0x4F,
            (byte)0xAF, (byte)0x38, (byte)0xC6, (byte)0xB9, (byte)0xBC, (byte)0x3C, (byte)0xEB, (byte)0xED,
            (byte)0xB3, (byte)0x03, (byte)0xEE, (byte)0x1B, (byte)0x9D, (byte)0x85, (byte)0x8B, (byte)0xFC,
            (byte)0x93, (byte)0x56, (byte)0x5C, (byte)0x09, (byte)0x91, (byte)0x41, (byte)0x22, (byte)0xE7,
            (byte)0x4C, (byte)0xF6, (byte)0x94, (byte)0x9D, (byte)0xC5, (byte)0x71, (byte)0x3C, (byte)0x2D,
            (byte)0xE4, (byte)0x4C, (byte)0x0E, (byte)0xD5, (byte)0x3D, (byte)0x6F, (byte)0xD7, (byte)0x20,
            (byte)0x6C, (byte)0xD8, (byte)0xDD, (byte)0x12, (byte)0x4D, (byte)0xEA, (byte)0x55, (byte)0xDD,
            (byte)0x26, (byte)0xA3, (byte)0x25, (byte)0xE3, (byte)0x83, (byte)0x9C, (byte)0x92, (byte)0x15,
            (byte)0xC6, (byte)0x45, (byte)0xFE, (byte)0x9A, (byte)0x0F, (byte)0x47, (byte)0x86, (byte)0x45,
            (byte)0x04, (byte)0xB6, (byte)0x44, (byte)0xFC, (byte)0x01, (byte)0x86, (byte)0x2A, (byte)0xF6,
            (byte)0xE2, (byte)0xFD, (byte)0x37, (byte)0xF1, (byte)0xBB, (byte)0x70, (byte)0xAD, (byte)0x15,
            (byte)0xE0, (byte)0x7C, (byte)0xB3, (byte)0x94, (byte)0xDE, (byte)0x2F, (byte)0xD2, (byte)0xC4,
            (byte)0x4F, (byte)0xCB, (byte)0xA7, (byte)0x31, (byte)0x87, (byte)0x66, (byte)0xD0, (byte)0xF7,
            (byte)0x5D, (byte)0x09, (byte)0x84, (byte)0x4B, (byte)0xB7, (byte)0x4B, (byte)0xE6, (byte)0x1C,
            (byte)0xA7, (byte)0xD9, (byte)0xBE, (byte)0x9E, (byte)0xAD, (byte)0xE7, (byte)0x03, (byte)0xCC,
            (byte)0xAB, (byte)0x04, (byte)0x4D, (byte)0xDF, (byte)0x92, (byte)0x8E, (byte)0xC5, (byte)0xA1,
            (byte)0x04, (byte)0xD0, (byte)0x7F, (byte)0x89, (byte)0x71, (byte)0x2D, (byte)0x6D, (byte)0x8F,
            (byte)0xCC, (byte)0x1E, (byte)0x25, (byte)0x5E, (byte)0x66, (byte)0xBF, (byte)0xA9, (byte)0xF8,
            (byte)0x8C, (byte)0xEF, (byte)0x8E, (byte)0x6A, (byte)0xFA, (byte)0xCE, (byte)0xFA, (byte)0x79,
            (byte)0xA0, (byte)0xDE, (byte)0x72, (byte)0xCB, (byte)0xBC, (byte)0x90, (byte)0x8E, (byte)0x29,
            (byte)0x4C, (byte)0x40, (byte)0x05, (byte)0x61, (byte)0x5A, (byte)0x44, (byte)0x0D, (byte)0xC7,
            (byte)0x46, (byte)0x65, (byte)0xA4, (byte)0x2A, (byte)0xD3, (byte)0xAA, (byte)0xEC, (byte)0x83,
            (byte)0x78, (byte)0x52, (byte)0x04, (byte)0x2B, (byte)0xB4, (byte)0x15, (byte)0xE3, (byte)0x66,
            (byte)0xD3, (byte)0xB5, (byte)0xE5, (byte)0xE7, (byte)0x04, (byte)0xB7, (byte)0xDB, (byte)0xFC,
            (byte)0x46, (byte)0x32, (byte)0xC5, (byte)0x71, (byte)0x93, (byte)0xDD, (byte)0xE3, (byte)0x07,
            (byte)0xD0, (byte)0xCD, (byte)0xD8, (byte)0x0E, (byte)0xDE, (byte)0xC1, (byte)0xA8, (byte)0xA6,
            (byte)0x0F, (byte)0x45, (byte)0x8F, (byte)0x06, (byte)0x51, (byte)0xFE, (byte)0x72, (byte)0xB7,
            (byte)0x61, (byte)0xCA, (byte)0x29, (byte)0x67, (byte)0x02, (byte)0x03, (byte)0x01, (byte)0x00,
            (byte)0x01,
    };

    private LicenseReader reader;
    private License license;

    private Timer checkerTimer;
    private LicenseChecker checkerTask;
    private LicenseWatcher licenseWatcher;

    public io.gravitee.node.api.license.License getLicense() {
        return this.license == null ? null : new License3JLicense(this.license);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        // Ensure log level for license module to INFO
        ((ch.qos.logback.classic.Logger) logger).setLevel(Level.INFO);

        this.loadLicense();
        this.startLicenseChecker();
        this.startLicenseWatcher();

        managementEndpointManager.register(new NodeLicenseManagementEndpoint(node));
    }

    private void startLicenseChecker() {
        checkerTimer = new Timer("gravitee-license-checker");

        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DATE, 1);
        c.set(Calendar.HOUR, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        checkerTask = new LicenseChecker();
        checkerTimer.schedule(checkerTask, c.getTime(), DAY_AS_LONG);
    }

    private void startLicenseWatcher() {
        String licenseKey = environment.getProperty(GRAVITEE_LICENSE_KEY);
        if (!StringUtils.hasLength(licenseKey)) {
            licenseWatcher = new LicenseWatcher(new File(getLicenseFile()));
            licenseWatcher.setName("gravitee-license-watcher");
            licenseWatcher.start();
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        if (checkerTimer != null) {
            checkerTimer.cancel();
        }

        if (licenseWatcher != null) {
            licenseWatcher.close();
        }

        if (reader != null) {
            reader.close();
        }
    }

    @Override
    protected String name() {
        return "License service";
    }

    public void loadLicense() throws Exception {
        byte[] licenseContent = getLicenseContent();

        if (licenseContent.length > 0) {
            try {
                reader = new LicenseReader(new ByteArrayInputStream(licenseContent));
                license = reader.read();

                this.printLicenseInfo();
                this.verify();
            } catch (IllegalArgumentException iae) {
                logger.error("License file is not valid", iae);
            } catch (IOException ioe) {
                logger.error("License file can not be read", ioe);
            }
        }
    }

    private byte[] getLicenseContent() {
        byte[] licenseContent;

        String licenseKey = environment.getProperty(GRAVITEE_LICENSE_KEY);
        if (StringUtils.hasLength(licenseKey)) {
            try {
                return Base64.getDecoder().decode(licenseKey);
            } catch (Exception ex) {
                logger.error("Can't decode the license key.", ex);
                return new byte[0];
            }
        }

        try {
            licenseContent = Files.readAllBytes(Paths.get(getLicenseFile()));
        } catch (IOException e) {
            logger.info("No license file found. Some plugins may be disabled");
            return new byte[0];
        }

        return licenseContent;
    }

    private String getLicenseFile() {
        String licenseFile = System.getProperty(GRAVITEE_LICENSE_PROPERTY);
        if (licenseFile == null || licenseFile.isEmpty()) {
            licenseFile = System.getProperty(GRAVITEE_HOME_PROPERTY) + File.separator + "license" + File.separator + GRAVITEE_LICENSE_KEY;
        }

        return licenseFile;
    }

    private void verify() throws Exception {
        if (license == null) {
            logger.debug("License will not be verified as it has not been loaded");
            return;
        }

        boolean valid = license.isOK(key);

        if (!valid) {
            logger.error("License is not valid. Please contact GraviteeSource to ask for a valid license.");
            stopNode();
        }

        if (license.isExpired()) {
            logger.error("License is expired. Please contact GraviteeSource to ask for a renewed license.");
            stopNode();
        }

        Date expiration = license.get(LICENSE_EXPIRE_AT).getDate();
        long remainingDays = Math.round((expiration.getTime() - System.currentTimeMillis()) / (double) 86400000);

        if (remainingDays <= 30) {
            logger.warn("License will be no longer valid in {} days. Please contact GraviteeSource to renew it.", remainingDays);
        }
    }

    private void printLicenseInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("License information: \n");
        license
            .getFeatures()
            .forEach((name, feature) -> sb.append("\t").append(name).append(": ").append(feature.valueString()).append("\n"));
        logger.info(sb.toString());
    }

    private class LicenseChecker extends TimerTask {

        @Override
        public void run() {
            try {
                LicenseService.this.verify();
            } catch (Exception e) {
                logger.error("An error occurred while checking license", e);
            }
        }
    }

    private class LicenseWatcher extends Thread {

        private final File file;
        private final AtomicBoolean stop = new AtomicBoolean(false);

        LicenseWatcher(File file) {
            this.file = file;
        }

        @Override
        public void run() {
            logger.debug("Watching license for next changes: {}", file.getAbsolutePath());

            try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
                Path path = file.toPath().getParent();
                path.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
                while (!stop.get()) {
                    WatchKey watchKey;
                    try {
                        watchKey = watcher.poll(25, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        return;
                    }
                    if (watchKey == null) {
                        Thread.yield();
                        continue;
                    }

                    for (WatchEvent<?> event : watchKey.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();

                        @SuppressWarnings("unchecked")
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path filename = ev.context();

                        if (kind == StandardWatchEventKinds.OVERFLOW) {
                            Thread.yield();
                            continue;
                        } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY && filename.toString().equals(file.getName())) {
                            LicenseService.this.loadLicense();
                        }
                        boolean valid = watchKey.reset();
                        if (!valid) {
                            break;
                        }
                    }
                    Thread.yield();
                }
            } catch (Exception e) {
                logger.debug("An error occurred while watching license file", e);
            }
        }

        void close() {
            stop.set(true);
        }
    }

    private void stopNode() throws Exception {
        node.stop();
        System.exit(0);
    }
}
