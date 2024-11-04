package io.gravitee.node.secrets.plugin.mock.conf;

import java.util.List;
import java.util.Map;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public record Renewal(boolean loop, List<Map<String, Object>> revisions) {}
