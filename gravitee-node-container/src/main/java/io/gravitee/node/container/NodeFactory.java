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

import io.gravitee.node.api.Node;
import io.gravitee.node.api.NodeDeployer;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NodeFactory extends AbstractFactoryBean<Node> implements ApplicationContextAware {

    @Autowired
    private NodeDeployerFactoriesLoader nodeDeployerFactoriesLoader;

    private ApplicationContext applicationContext;

    private final Class<? extends Node> nodeClass;

    public NodeFactory(Class<? extends Node> nodeClass) {
        this.nodeClass = nodeClass;
    }

    @Override
    public Class<?> getObjectType() {
        return Node.class;
    }

    @Override
    protected Node createInstance() throws Exception {
        Node node = nodeClass.newInstance();
        autowire(node);

        List<NodeDeployer> deployers = nodeDeployerFactoriesLoader.getNodeDeployers();

        for(NodeDeployer deployer : deployers) {
            node = deployer.deploy(node);
            autowire(node);
        }

        return node;
    }

    private void autowire(Node node) throws Exception {
        if (node != null) {
            this.applicationContext.getAutowireCapableBeanFactory().autowireBean(node);
            if (node instanceof ApplicationContextAware) {
                ((ApplicationContextAware) node).setApplicationContext(this.applicationContext);
            }

            if (node instanceof InitializingBean) {
                ((InitializingBean) node).afterPropertiesSet();
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}
