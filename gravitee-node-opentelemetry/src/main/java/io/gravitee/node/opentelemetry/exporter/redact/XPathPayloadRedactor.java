/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.opentelemetry.exporter.redact;

import io.gravitee.node.api.opentelemetry.redaction.PayloadPhase;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import lombok.CustomLog;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Applies {@link CompiledPayloadMaskingRule} entries to an XML body string using javax.xml.xpath.
 *
 * <p>{@link javax.xml.xpath.XPathExpression} is NOT thread-safe; a fresh expression is compiled
 * per invocation from the thread-safe {@link XPathFactory#newInstance()} singleton.
 * Fail-open: any parse / evaluation error returns the original body unchanged.
 */
@CustomLog
final class XPathPayloadRedactor {

    // XPathFactory.newInstance() IS thread-safe per the JAXP spec.
    private static final XPathFactory XPATH_FACTORY = XPathFactory.newInstance();

    // DocumentBuilderFactory and TransformerFactory are also thread-safe for newInstance().
    private static final DocumentBuilderFactory DOC_BUILDER_FACTORY;
    private static final TransformerFactory TRANSFORMER_FACTORY;

    static {
        DOC_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
        DOC_BUILDER_FACTORY.setNamespaceAware(false);
        TRANSFORMER_FACTORY = TransformerFactory.newInstance();
    }

    /**
     * Applies all rules that match {@code phase} to {@code xml} and returns the masked result.
     * Returns {@code xml} unchanged if no rule matches or the body cannot be parsed.
     */
    String redact(String xml, List<CompiledPayloadMaskingRule> rules, PayloadPhase phase) {
        if (xml == null || xml.isBlank() || rules.isEmpty()) {
            return xml;
        }

        org.w3c.dom.Document doc;
        try {
            DocumentBuilder builder;
            synchronized (DOC_BUILDER_FACTORY) {
                builder = DOC_BUILDER_FACTORY.newDocumentBuilder();
            }
            doc = builder.parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            log.warn("PayloadMasking: failed to parse XML body — returning original body unchanged. Cause: {}", e.getMessage());
            return xml;
        }

        boolean anyApplied = false;
        for (CompiledPayloadMaskingRule rule : rules) {
            if (!rule.appliesToPhase(phase) || rule.rawXPath() == null) {
                continue;
            }
            try {
                // XPathExpression is NOT thread-safe — compile fresh per call.
                javax.xml.xpath.XPathExpression expr = XPATH_FACTORY.newXPath().compile(rule.rawXPath());
                NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
                for (int i = 0; i < nodes.getLength(); i++) {
                    org.w3c.dom.Node node = nodes.item(i);
                    node.setTextContent(rule.applyMask(node.getTextContent()));
                    anyApplied = true;
                }
            } catch (Exception e) {
                log.warn("PayloadMasking: failed to apply XPath '{}' — skipping rule. Cause: {}", rule.rawXPath(), e.getMessage());
            }
        }

        if (!anyApplied) {
            return xml;
        }

        try {
            Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            log.warn("PayloadMasking: failed to serialize masked XML — returning original body unchanged. Cause: {}", e.getMessage());
            return xml;
        }
    }
}
