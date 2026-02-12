package mtf.snifitapi.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XmlNode {
    private String tagName;
    private String id;
    private String refId;
    private Map<String, String> attributes = new HashMap<>();
    private List<XmlNode> children = new ArrayList<>();
    private StringBuilder textContent = new StringBuilder();

    public XmlNode(String tagName) {
        this.tagName = tagName;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRefId() {
        return refId;
    }

    public void setRefId(String refId) {
        this.refId = refId;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void addAttribute(String key, String value) {
        this.attributes.put(key, value);
    }

    public List<XmlNode> getChildren() {
        return children;
    }

    public void addChild(XmlNode child) {
        this.children.add(child);
    }

    public String getTextContent() {
        return textContent.toString();
    }

    public void setTextContent(String text) {
        this.textContent = new StringBuilder(text != null ? text : "");
    }

    public void appendTextContent(String text) {
        if (text != null) {
            this.textContent.append(text);
        }
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        toJson(sb);
        return sb.toString();
    }

    private void toJson(StringBuilder sb) {
        sb.append("{");
        sb.append("\"tagName\":\"").append(escapeJson(tagName)).append("\"");

        if (id != null) {
            sb.append(",\"id\":\"").append(escapeJson(id)).append("\"");
        }

        if (refId != null) {
            sb.append(",\"refId\":\"").append(escapeJson(refId)).append("\"");
        }

        if (!attributes.isEmpty()) {
            sb.append(",\"attributes\":{");
            boolean first = true;
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                if (!first)
                    sb.append(",");
                sb.append("\"").append(escapeJson(entry.getKey())).append("\":\"")
                        .append(escapeJson(entry.getValue())).append("\"");
                first = false;
            }
            sb.append("}");
        }

        String text = getTextContent();
        if (text != null && !text.isEmpty()) {
            sb.append(",\"textContent\":\"").append(escapeJson(text)).append("\"");
        }

        if (!children.isEmpty()) {
            sb.append(",\"children\":[");
            boolean first = true;
            for (XmlNode child : children) {
                if (!first)
                    sb.append(",");
                child.toJson(sb);
                first = false;
            }
            sb.append("]");
        }

        sb.append("}");
    }

    private String escapeJson(String s) {
        if (s == null)
            return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < ' ') {
                        String t = "000" + Integer.toHexString(c);
                        sb.append("\\u" + t.substring(t.length() - 4));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "XmlNode{" +
                "tagName='" + tagName + '\'' +
                ", id='" + id + '\'' +
                ", refId='" + refId + '\'' +
                ", attributes=" + attributes +
                ", textContent='" + textContent + '\'' +

                ", children=" + children +
                '}';
    }
}
