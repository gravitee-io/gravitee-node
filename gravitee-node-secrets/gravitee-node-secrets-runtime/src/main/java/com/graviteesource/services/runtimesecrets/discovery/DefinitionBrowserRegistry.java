package com.graviteesource.services.runtimesecrets.discovery;

import io.gravitee.node.api.secrets.runtime.discovery.DefinitionBrowser;
import java.util.Collection;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefinitionBrowserRegistry {

    private final Collection<DefinitionBrowser> browsers;

    @Autowired
    public DefinitionBrowserRegistry(Collection<DefinitionBrowser> browsers) {
        this.browsers = browsers;
    }

    public <T> Optional<DefinitionBrowser<T>> findBrowser(T definition) {
        for (DefinitionBrowser<T> browser : this.browsers) {
            if (browser.canHandle(definition)) {
                return Optional.of(browser);
            }
        }
        return Optional.empty();
    }
}
