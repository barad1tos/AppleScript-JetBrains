package com.intellij.plugin.applescript.lang.ide.sdef;

import org.jdom.Document;
import org.jdom.output.XMLOutputter;

import java.io.IOException;
import java.io.OutputStream;

final class LegacyJdomWriter {
    private LegacyJdomWriter() {
    }

    @SuppressWarnings("deprecation")
    static void write(Document document, OutputStream output) throws IOException {
        new XMLOutputter().output(document, output);
    }
}
