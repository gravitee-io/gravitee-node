package io.gravitee.node.license;

import static io.gravitee.node.api.license.License.REFERENCE_ID_PLATFORM;
import static io.gravitee.node.api.license.License.REFERENCE_TYPE_PLATFORM;

import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.api.license.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

/**
 * Default {@link LicenseFetcher} implementation that load the license from the environment variable or a file location.
 * The load of the license will be tried in the following order:
 * <ul>
 *     <li>From the {@link #GRAVITEE_LICENSE_KEY} environment variable: if specified, must be base64 encoded</li>
 *     <li>From the {@link #GRAVITEE_LICENSE_PROPERTY} environment variable: if specified, must correspond to a path to the license key file</li>
 *     <li>From the gravitee home: search for {@link #GRAVITEE_HOME_PROPERTY}/license/license.key file</li>
 * </ul>
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@RequiredArgsConstructor
public class DefaultLicenseFetcher implements LicenseFetcher {

    static final String GRAVITEE_HOME_PROPERTY = "gravitee.home";
    static final String GRAVITEE_LICENSE_KEY = "license.key";
    static final String GRAVITEE_LICENSE_PROPERTY = "gravitee.license";

    private final Configuration configuration;
    private final LicenseFactory licenseFactory;
    private Consumer<License> onChange;
    private LicenseWatcher licenseWatcher;

    @Override
    public License fetch() throws InvalidLicenseException, MalformedLicenseException {
        return readLicense();
    }

    @Override
    public void startWatch(Consumer<License> onChange) {
        this.onChange = onChange;
        startLicenseWatcher();
    }

    @Override
    public void stopWatch() {
        if (licenseWatcher != null) {
            licenseWatcher.close();
        }
    }

    private License readLicense() throws InvalidLicenseException, MalformedLicenseException {
        final String licenseKey = configuration.getProperty(GRAVITEE_LICENSE_KEY);

        if (StringUtils.hasLength(licenseKey)) {
            return licenseFactory.create(REFERENCE_TYPE_PLATFORM, REFERENCE_ID_PLATFORM, Base64.getDecoder().decode(licenseKey));
        }

        try {
            final String licenseFile = getLicenseFile();
            return licenseFactory.create(REFERENCE_TYPE_PLATFORM, REFERENCE_ID_PLATFORM, Files.readAllBytes(Paths.get(licenseFile)));
        } catch (IOException e) {
            log.info("No license file found. Some plugins may be disabled");
        }

        return null;
    }

    private String getLicenseFile() {
        String licenseFile = System.getProperty(GRAVITEE_LICENSE_PROPERTY);

        if (licenseFile == null || licenseFile.isEmpty()) {
            licenseFile = System.getProperty(GRAVITEE_HOME_PROPERTY) + File.separator + "license" + File.separator + GRAVITEE_LICENSE_KEY;
        }

        return licenseFile;
    }

    private void startLicenseWatcher() {
        String licenseKey = configuration.getProperty(GRAVITEE_LICENSE_KEY);
        if (!StringUtils.hasLength(licenseKey)) {
            licenseWatcher = new LicenseWatcher(new File(getLicenseFile()));
            licenseWatcher.setName("gravitee-license-watcher");
            licenseWatcher.start();
        }
    }

    private class LicenseWatcher extends Thread {

        private final File file;
        private boolean stopped = false;

        LicenseWatcher(File file) {
            this.file = file;
        }

        @Override
        public void run() {
            log.debug("Watching license for next changes: {}", file.getAbsolutePath());

            try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
                Path path = file.toPath().getParent();
                path.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
                while (!stopped) {
                    WatchKey watchKey;
                    try {
                        watchKey = watcher.poll(1, TimeUnit.SECONDS);
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
                            onChange.accept(fetch());
                        }
                        boolean valid = watchKey.reset();
                        if (!valid) {
                            break;
                        }
                    }
                    Thread.yield();
                }
            } catch (Exception e) {
                log.debug("An error occurred while watching license file", e);
            }
        }

        void close() {
            stopped = true;
        }
    }
}
