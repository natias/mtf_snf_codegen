package mtf.snifitapi.parser;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import mtf.snifitapi.model.SnifitModel;
import mtf.snifitapi.model.XmlNode;

public class SnifitSaxHandler extends DefaultHandler {

    private List<XmlNode> rootNodes = new ArrayList<>();
    private Map<String, XmlNode> idMap = new HashMap<>();
    private Stack<XmlNode> stack = new Stack<>();
    private Locator locator;
    private String currentFileName;

    public SnifitSaxHandler(File file) {
        this.currentFileName = file.getName();
    }

    public SnifitSaxHandler() {
        this.currentFileName = "unknown";
    }

    public List<XmlNode> getResult() {
        return rootNodes;
    }

    public Map<String, XmlNode> getIdMap() {
        return idMap;
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        XmlNode node = createNode(qName);

        if (locator != null) {
            node.setSourceLocation(currentFileName, locator.getLineNumber());
        }

        // Populate attributes
        for (int i = 0; i < attributes.getLength(); i++) {
            String attrName = attributes.getQName(i);
            String attrValue = attributes.getValue(i);

            if ("id".equals(attrName)) {
                node.setId(attrValue);
                idMap.put(attrValue, node);
            } else if ("refId".equals(attrName)) {
                node.setRefId(attrValue);
            } else {
                node.addAttribute(attrName, attrValue);
            }
        }

        if (!stack.isEmpty()) {
            stack.peek().addChild(node);
        } else {
            rootNodes.add(node);
        }

        stack.push(node);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (!stack.isEmpty()) {
            // Check if poppeing correct tag
            XmlNode top = stack.peek();
            if (top.getTagName().equals(qName)) {
                stack.pop();
            } else {
                // This shouldn't happen in well-formed XML unless logic is flawed
            }
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (!stack.isEmpty()) {
            String text = new String(ch, start, length);
            // Only append if it's not purely whitespace or if whitespace is significant
            // Assuming whitespace-only text nodes are irrelevant for structure unless
            // significant
            if (!text.trim().isEmpty()) {
                stack.peek().appendTextContent(text);
            }
        }
    }

    private XmlNode createNode(String tagName) {
        switch (tagName) {
            case "context":
                return new SnifitModel.Context();
            case "kColl":
                return new SnifitModel.KColl();
            case "iColl":
                return new SnifitModel.IColl();
            case "matafIColl":
                return new SnifitModel.MatafIColl();
            case "refKColl":
                return new SnifitModel.RefKColl();
            case "refData":
                return new SnifitModel.RefData();
            case "field":
                return new SnifitModel.Field();
            case "fmtDef":
                return new SnifitModel.FmtDef();
            case "refFmt":
                return new SnifitModel.RefFmt();
            case "record":
                return new SnifitModel.Record();
            case "fCSRecord":
                return new SnifitModel.FCSRecord();
            case "fCSList":
                return new SnifitModel.FCSList();
            case "iCollF":
                return new SnifitModel.ICollF();
            case "fCSString":
                return new SnifitModel.FCSString();
            case "fHostString":
                return new SnifitModel.FHostString();
            case "operation":
                return new SnifitModel.Operation();
            case "opStep":
                return new SnifitModel.OpStep();
            case "refOpSteps":
                return new SnifitModel.RefOpSteps();
            case "refFormat":
                return new SnifitModel.RefFormat();
            case "visualField":
                return new SnifitModel.VisualField();
            case "msgField":
                return new SnifitModel.MsgField();
            case "mapperConverter":
                return new SnifitModel.MapperConverter();
            case "move":
                return new SnifitModel.Move();
            case "fCSDynXml":
                return new SnifitModel.FCSDynXml();
            case "fCSMessage":
                return new SnifitModel.FCSMessage();
            case "fString":
                return new SnifitModel.FString();
            case "constant":
                return new SnifitModel.Constant();
            case "fixedLength":
                return new SnifitModel.FixedLength();
            default:
                if (tagName.endsWith("Decor")) {
                    return new SnifitModel.Decorator(tagName);
                }
                return new SnifitModel.GenericNode(tagName);
        }
    }
}
