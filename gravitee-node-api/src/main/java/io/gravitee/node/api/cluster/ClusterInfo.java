package io.gravitee.node.api.cluster;

import java.util.Set;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
public record ClusterInfo(String clusterId, boolean running, Member self, Set<Member> members) {}
