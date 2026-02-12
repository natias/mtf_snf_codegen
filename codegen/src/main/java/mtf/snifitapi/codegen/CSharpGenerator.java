package mtf.snifitapi.codegen;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mtf.snifitapi.model.SnifitModel;
import mtf.snifitapi.model.XmlNode;

public class CSharpGenerator {

    private final SnifitModel model;
    private final StringBuilder output = new StringBuilder();
    private final Set<String> processedClasses = new HashSet<>();

    public CSharpGenerator(SnifitModel model) {
        this.model = model;
    }

    public String generate(String rootId) {
        XmlNode root = model.getNodeById(rootId);
        if (root == null) {
            return "// Root node '" + rootId + "' not found.";
        }

        // Start generating from the root format definition
        // The root itself is usually a fmtDef which contains the structure
        generateClass(rootId, root, 0);
        return output.toString();
    }

    private void generateClass(String className, XmlNode node, int indentLevel) {
        String indent = getIndent(indentLevel);
        String cleanClassName = sanitizeName(className);

        // Avoid cyclic redundancy if needed, but for nested classes we might want
        // duplicates (nested context)
        // For now assuming we just generate a class structure

        output.append(indent).append("public class ").append(cleanClassName).append(" {\n");

        processChildren(node, indentLevel + 1);

        output.append(indent).append("}\n");
    }

    private void processChildren(XmlNode node, int indentLevel) {
        if (node == null || node.getChildren() == null)
            return;

        for (XmlNode child : node.getChildren()) {
            processNode(child, indentLevel);
        }
    }

    private void processNode(XmlNode node, int indentLevel) {
        String tagName = node.getTagName();
        String dataName = node.getAttributes().get("dataName");
        String refId = node.getRefId();
        String indent = getIndent(indentLevel);

        if ("fCSRecord".equals(tagName) || "record".equals(tagName)) {
            if (dataName != null) {
                // Nested class / property
                // Generate nested class definition inline
                generateClass(dataName, node, indentLevel);
                // Generate property of that type
                output.append(indent).append("public ").append(sanitizeName(dataName)).append(" ")
                        .append(sanitizeName(dataName)).append(" { get; set; }\n");
            } else {
                // Inline contents (grouping)
                processChildren(node, indentLevel);
            }
        } else if ("fCSList".equals(tagName)) { // List
            if (dataName != null) {
                // Usually contains a single fCSRecord child defining the type
                // But we handle children generically first to find the type
                // Actually, often fCSList has a child fCSRecord with dataName="ItemType"
                // Let's check children to see if there's a named record
                XmlNode itemNode = findItemNode(node);
                String itemType = "string"; // Default fallback

                if (itemNode != null) {
                    String itemDataName = itemNode.getAttributes().get("dataName");
                    if (itemDataName != null) {
                        itemType = sanitizeName(itemDataName);
                        // Generate the item class
                        generateClass(itemType, itemNode, indentLevel);
                    } else {
                        // Unnamed record in list? Treat as dynamic or just inline properties won't work
                        // for list
                        // Maybe generate an inner class based on list name + "Item"?
                        itemType = sanitizeName(dataName) + "Item";
                        generateClass(itemType, itemNode, indentLevel);
                    }
                }

                output.append(indent).append("public List<").append(itemType).append("> ")
                        .append(sanitizeName(dataName)).append(" { get; set; } = new List<").append(itemType)
                        .append(">();\n");
            }
        } else if ("fCSString".equals(tagName) || "fHostString".equals(tagName) || "fString".equals(tagName)
                || "fCSMessage".equals(tagName) || "fCSDynXml".equals(tagName)) {
            if (dataName != null) {
                output.append(indent).append("public string ").append(sanitizeName(dataName))
                        .append(" { get; set; }\n");
            }
        } else if ("refFmt".equals(tagName)) {
            if (refId != null) {
                XmlNode referencedNode = model.getNodeById(refId);
                if (referencedNode != null) {
                    processChildren(referencedNode, indentLevel);
                } else {
                    output.append(indent).append("// Error: Referenced ID '").append(refId).append("' not found\n");
                }
            }
        } else if ("fmtDef".equals(tagName)) {
            // Should only happen for root mostly, or nested definitions
            processChildren(node, indentLevel);
        } else {
            // Recurse for other tags (like decorators, etc - though decorators usually have
            // no children of interest)
            // But if it's a structural tag we missed:
            if (!node.getChildren().isEmpty()) {
                processChildren(node, indentLevel);
            }
        }
    }

    private XmlNode findItemNode(XmlNode listNode) {
        if (listNode.getChildren() == null)
            return null;
        for (XmlNode child : listNode.getChildren()) {
            if ("fCSRecord".equals(child.getTagName()) || "record".equals(child.getTagName())) {
                return child;
            }
            if ("refFmt".equals(child.getTagName())) {
                // If list contains refFmt, resolve it
                String refId = child.getRefId();
                if (refId != null) {
                    return model.getNodeById(refId);
                }
            }
        }
        return null;
    }

    private String getIndent(int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) {
            sb.append("    ");
        }
        return sb.toString();
    }

    private String sanitizeName(String name) {
        if (name == null)
            return "Unknown";
        // Replace invalid chars
        String clean = name.replace(".", "_")
                .replace("-", "_")
                .replace(":", "_");

        // Ensure valid C# identifier start
        if (Character.isDigit(clean.charAt(0))) {
            clean = "_" + clean;
        }
        return clean;
    }
}
