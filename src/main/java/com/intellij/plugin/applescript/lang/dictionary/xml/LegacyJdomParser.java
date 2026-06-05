package com.intellij.plugin.applescript.lang.dictionary.xml;

import com.intellij.openapi.util.JDOMUtil;
import org.jdom.Document;
import org.jdom.JDOMException;

import java.io.File;
import java.io.IOException;

public final class LegacyJdomParser {
    private LegacyJdomParser() {
    }

    public static Document build(File file) throws JDOMException, IOException {
        return new Document(JDOMUtil.load(file.toPath()));
    }
}
