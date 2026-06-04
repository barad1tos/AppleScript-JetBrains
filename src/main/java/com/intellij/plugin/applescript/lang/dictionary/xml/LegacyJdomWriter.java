package com.intellij.plugin.applescript.lang.dictionary.xml;

import org.jdom.Document;
import org.jdom.output.XMLOutputter;

import java.io.IOException;
import java.io.OutputStream;

public final class LegacyJdomWriter {
    private LegacyJdomWriter() {
    }

    @SuppressWarnings("deprecation")
    public static void write(Document document, OutputStream output) throws IOException {
        new XMLOutputter().output(document, output);
    }
}
