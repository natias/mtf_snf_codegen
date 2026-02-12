package mtf.snifitapi.parser;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import mtf.snifitapi.model.SnifitModel;
import mtf.snifitapi.model.XmlNode;

public class SnifitParser {

    public SnifitModel parse(File file) throws ParserConfigurationException, SAXException, IOException {
        SnifitModel model = new SnifitModel();
        parseInto(file, model);
        return model;
    }

    public void parseInto(File file, SnifitModel model) throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = factory.newSAXParser();
        SnifitSaxHandler handler = new SnifitSaxHandler(file);

        saxParser.parse(file, handler);

        model.addRootNodes(handler.getResult());
        model.addIdToNodeMap(handler.getIdMap());
    }
}
