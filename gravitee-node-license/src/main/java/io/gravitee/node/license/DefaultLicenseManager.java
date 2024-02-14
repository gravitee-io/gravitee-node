package io.gravitee.node.license;

import io.gravitee.common.service.AbstractService;
import io.gravitee.node.api.license.*;
import io.gravitee.plugin.core.api.PluginRegistry;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * This default {@link LicenseManager} is responsible for keeping a reference on the platform and the organizations licenses.
 * It allows to easily validate a feature is allowed by the license.
 * Internally, the expiration date of the license is checked regularly.
 * Anyone can register an action to execute when a license is expired
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class DefaultLicenseManager extends AbstractService<LicenseManager> implements LicenseManager {

    public static final License OSS_LICENSE = new OSSLicense(License.REFERENCE_ID_PLATFORM, License.REFERENCE_TYPE_PLATFORM);

    private static final long DAY_AS_LONG = 24 * 60 * 60 * 1000L;

    private final List<Consumer<License>> expirationListeners = new CopyOnWriteArrayList<>();
    private final Map<String, Optional<License>> organizationLicenses = new ConcurrentHashMap<>();
    private final PluginRegistry pluginRegistry;
    private final Timer checkerTimer;
    private License platformLicense;

    public DefaultLicenseManager(PluginRegistry pluginRegistry) {
        this.pluginRegistry = pluginRegistry;
        this.checkerTimer = new Timer("gravitee-license-checker");
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        // Fist check will occur the day after.
        Calendar firstTime = Calendar.getInstance();
        firstTime.add(Calendar.DATE, 1);
        firstTime.set(Calendar.HOUR, 0);
        firstTime.set(Calendar.MINUTE, 0);
        firstTime.set(Calendar.SECOND, 0);
        firstTime.set(Calendar.MILLISECOND, 0);

        checkerTimer.schedule(new LicenseChecker(), firstTime.getTime(), DAY_AS_LONG);
    }

    @Override
    protected void doStop() throws Exception {
        checkerTimer.cancel();
        super.doStop();
    }

    @Override
    public void registerOrganizationLicense(@NonNull String organizationId, License license) {
        this.organizationLicenses.put(organizationId, Optional.ofNullable(license));
    }

    @Override
    public void registerPlatformLicense(License license) {
        this.platformLicense = license;
    }

    @Override
    public License getOrganizationLicense(String organizationId) {
        return organizationLicenses.getOrDefault(organizationId, Optional.empty()).orElse(null);
    }

    @Override
    public @NonNull License getOrganizationLicenseOrPlatform(String organizationId) {
        return Optional.ofNullable(getOrganizationLicense(organizationId)).orElse(getPlatformLicense());
    }

    @Override
    public @NonNull License getPlatformLicense() {
        if (platformLicense == null) {
            return OSS_LICENSE;
        }
        return platformLicense;
    }

    @Override
    public void validatePluginFeatures(String organizationId, Collection<Plugin> plugins)
        throws InvalidLicenseException, ForbiddenFeatureException {
        if (plugins == null || plugins.isEmpty()) {
            // There is no plugin feature to validate.
            return;
        }

        final Set<ForbiddenFeature> errors = new HashSet<>();
        final License license = this.getOrganizationLicenseOrPlatform(organizationId);

        if (license.isExpired()) {
            throw new InvalidLicenseException("The license has expired.");
        }

        plugins.forEach(plugin -> validatePluginFeature(license, errors, plugin));

        if (!errors.isEmpty()) {
            throw new ForbiddenFeatureException(errors);
        }
    }

    @Override
    public void onLicenseExpires(Consumer<License> expirationListener) {
        expirationListeners.add(expirationListener);
    }

    private void validatePluginFeature(License license, Set<ForbiddenFeature> errors, Plugin plugin) {
        final io.gravitee.plugin.core.api.Plugin registryPlugin = pluginRegistry.get(plugin.type(), plugin.id());

        if (Objects.nonNull(registryPlugin) && !license.isFeatureEnabled(registryPlugin.manifest().feature())) {
            errors.add(new ForbiddenFeature(registryPlugin.manifest().feature(), plugin.id()));
        }
    }

    private void checkLicenses() {
        if (platformLicense != null) {
            checkLicense(platformLicense);
        }

        organizationLicenses.values().forEach(optLicense -> optLicense.ifPresent(this::checkLicense));
    }

    private void checkLicense(License license) {
        final Date expirationDate = license.getExpirationDate();

        if (expirationDate != null) {
            final long remainingDays = Math.round((expirationDate.getTime() - System.currentTimeMillis()) / (double) 86400000);
            if (remainingDays <= 30) {
                log.warn("License will be no longer valid in {} days. Please contact GraviteeSource to renew it.", remainingDays);
            }

            if (remainingDays <= 0) {
                notifyLicenseExpired(license);
            }
        }
    }

    private void notifyLicenseExpired(License license) {
        expirationListeners.forEach(listener -> listener.accept(license));
    }

    private class LicenseChecker extends TimerTask {

        @Override
        public void run() {
            checkLicenses();
        }
    }
}
