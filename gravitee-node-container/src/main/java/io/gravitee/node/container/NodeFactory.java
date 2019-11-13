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
package io.gravitee.node.container;

import io.gravitee.common.spring.factory.AbstractAutowiringFactoryBean;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.NodeDeployer;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NodeFactory extends AbstractAutowiringFactoryBean<Node> {

    @Autowired
    private NodeDeployerFactoriesLoader nodeDeployerFactoriesLoader;

    private final Class<? extends Node> nodeClass;

    public NodeFactory(Class<? extends Node> nodeClass) {
        this.nodeClass = nodeClass;
    }

    @Override
    public Class<?> getObjectType() {
        return Node.class;
    }

    @Override
    protected Node doCreateInstance() throws Exception {
        Node node = nodeClass.newInstance();
        List<NodeDeployer> nodeDeployers = nodeDeployerFactoriesLoader.getNodeDeployers();

        for(NodeDeployer deployer : nodeDeployers) {
            node = deployer.deploy(node);
        }
        return node;
    }
}
