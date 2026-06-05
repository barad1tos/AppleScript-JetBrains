package com.intellij.plugin.applescript.lang.dictionary.xml;

import com.intellij.openapi.util.JDOMUtil;
import org.jdom.Document;

import java.io.IOException;
import java.io.OutputStream;

public final class LegacyJdomWriter {
    private LegacyJdomWriter() {
    }

    public static void write(Document document, OutputStream output) throws IOException {
        JDOMUtil.write(document, output);
    }
}
