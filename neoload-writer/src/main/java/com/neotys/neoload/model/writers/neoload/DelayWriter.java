package com.neotys.neoload.model.writers.neoload;

import com.neotys.neoload.model.repository.Delay;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DelayWriter extends ElementWriter {

    public static final String XML_TAG_NAME = "delay-action";
    public static final String XML_DURATION_ATT = "duration";
    
    public DelayWriter(Delay delay) {
        super(delay);
    }

    public static DelayWriter of(final Delay delay) {
        return new DelayWriter(delay);
    }

    @Override
    public void writeXML(final Document document, final Element currentElement, final String outputFolder) {
        Element xmlDelay = document.createElement(XML_TAG_NAME);
        super.writeXML(document, xmlDelay, outputFolder);
        xmlDelay.setAttribute(XML_DURATION_ATT, ((Delay)element).getDelay());
        currentElement.appendChild(xmlDelay);
    }
}
