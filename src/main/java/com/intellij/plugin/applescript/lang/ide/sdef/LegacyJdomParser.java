package com.intellij.plugin.applescript.lang.ide.sdef;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.File;
import java.io.IOException;

final class LegacyJdomParser {
    private static final String DISALLOW_DOCTYPE_DECL =
            "http://apache.org/xml/features/disallow-doctype-decl";
    private static final String LOAD_EXTERNAL_DTD =
            "http://apache.org/xml/features/nonvalidating/load-external-dtd";
    private static final String EXTERNAL_GENERAL_ENTITIES =
            "http://xml.org/sax/features/external-general-entities";
    private static final String EXTERNAL_PARAMETER_ENTITIES =
            "http://xml.org/sax/features/external-parameter-entities";

    private LegacyJdomParser() {
    }

    @SuppressWarnings("deprecation")
    static Document build(File file) throws JDOMException, IOException {
        return newSecureBuilder().build(file);
    }

    @SuppressWarnings("deprecation")
    private static SAXBuilder newSecureBuilder() {
        SAXBuilder builder = new SAXBuilder();
        builder.setFeature(DISALLOW_DOCTYPE_DECL, false);
        builder.setFeature(LOAD_EXTERNAL_DTD, false);
        builder.setFeature(EXTERNAL_GENERAL_ENTITIES, false);
        builder.setFeature(EXTERNAL_PARAMETER_ENTITIES, false);
        builder.setExpandEntities(false);
        return builder;
    }
}
