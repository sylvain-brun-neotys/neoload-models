package com.neotys.neoload.model.writers.neoload;

import com.neotys.neoload.model.repository.UserPath;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class UserPathWriter extends ElementWriter{

    public static final String XML_TAG_NAME = "virtual-user";
    public static final String XML_TAG_INIT = "init-container";
    public static final String XML_TAG_ACTIONS = "actions-container";
    public static final String XML_TAG_END = "end-container";
    public static final String XML_USERPATH_NAME_ATTR = "uid";

    private UserPath userPath;
    
    public UserPathWriter(UserPath userPath) {
        super(userPath);
        this.userPath = userPath;
    }

    public static UserPathWriter of(UserPath userPath) {
        return new UserPathWriter(userPath);
    }

    @Override
    public void writeXML(final Document document, final Element repositoryElement, final String outputFolder) {
        Element element = document.createElement(XML_TAG_NAME);
        element.setAttribute(XML_USERPATH_NAME_ATTR, this.userPath.getName());
        super.writeDescription(document, element);
        repositoryElement.appendChild(element);
        // write init-container
        ContainerWriter.of(this.userPath.getInitContainer(), XML_TAG_INIT).writeXML(document, element, outputFolder);

        // write actions-container
        ContainerWriter.of(this.userPath.getActionsContainer(), XML_TAG_ACTIONS).writeXML(document, element, outputFolder);

        // write end-container
        ContainerWriter.of(this.userPath.getEndContainer(), XML_TAG_END).writeXML(document, element, outputFolder);
    }
    
}
