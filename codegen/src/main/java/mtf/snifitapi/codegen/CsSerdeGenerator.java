package mtf.snifitapi.codegen;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mtf.snifitapi.model.SnifitModel;
import mtf.snifitapi.model.XmlNode;

public class CsSerdeGenerator {

    private final SnifitModel model;
    private final Map<String, String> generatedFiles = new HashMap<>();
    private final Set<String> processedClasses = new HashSet<>();

    public CsSerdeGenerator(SnifitModel model) {
        this.model = model;
    }

    public Map<String, String> generate(String rootId) {
        XmlNode root = model.getNodeById(rootId);
        if (root == null) {
            System.err.println("// Root node '" + rootId + "' not found.");
            return generatedFiles;
        }

        generateSerializerClass(rootId, root);
        return generatedFiles;
    }

    private void generateSerializerClass(String className, XmlNode node) {
        String cleanClassName = sanitizeName(className);
        String serializerClassName = cleanClassName + "Serializer";

        if (processedClasses.contains(cleanClassName)) {
            return;
        }
        processedClasses.add(cleanClassName);

        StringBuilder sb = new StringBuilder();
        sb.append("using System;\n");
        sb.append("using System.Text;\n");
        sb.append("using System.Collections.Generic;\n\n");

        sb.append("public class ").append(serializerClassName).append(" {\n");

        // Serialize Method
        sb.append("    public string Serialize(").append(cleanClassName).append(" obj) {\n");
        sb.append("        if (obj == null) return \"\";\n");
        sb.append("        StringBuilder sb = new StringBuilder();\n");

        Set<String> existingFieldNames = new HashSet<>();
        processChildrenSerialize(node, sb, existingFieldNames, "obj");

        sb.append("        return sb.ToString();\n");
        sb.append("    }\n\n");

        // Deserialize Entry Point
        sb.append("    public ").append(cleanClassName).append(" Deserialize(string buffer) {\n");
        sb.append("        int offset = 0;\n");
        sb.append("        return Deserialize(buffer, ref offset);\n");
        sb.append("    }\n\n");

        // Deserialize Internal
        sb.append("    public ").append(cleanClassName).append(" Deserialize(string buffer, ref int offset) {\n");
        sb.append("        var obj = new ").append(cleanClassName).append("();\n");

        existingFieldNames.clear(); // Reset for deserialize pass
        processChildrenDeserialize(node, sb, existingFieldNames, "obj");

        sb.append("        return obj;\n");
        sb.append("    }\n");

        sb.append("}\n");

        generatedFiles.put(serializerClassName, sb.toString());
    }

    private void processChildrenSerialize(XmlNode node, StringBuilder sb, Set<String> existingFieldNames,
            String varName) {
        if (node == null || node.getChildren() == null)
            return;

        List<XmlNode> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            XmlNode child = children.get(i);
            processNodeSerialize(child, sb, existingFieldNames, varName, children, i);
        }
    }

    private void processChildrenDeserialize(XmlNode node, StringBuilder sb, Set<String> existingFieldNames,
            String varName) {
        if (node == null || node.getChildren() == null)
            return;

        List<XmlNode> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            XmlNode child = children.get(i);
            processNodeDeserialize(child, sb, existingFieldNames, varName, children, i);
        }
    }

    private void processNodeSerialize(XmlNode node, StringBuilder sb, Set<String> existingFieldNames, String varName,
            List<XmlNode> siblings, int index) {
        String tagName = node.getTagName();
        String dataName = node.getAttributes().get("dataName");
        String refId = node.getRefId();
        String indent = "        ";

        if ("fCSRecord".equals(tagName) || "record".equals(tagName)) {
            if (dataName != null) {
                String typeName = sanitizeName(dataName);
                String propertyName = getUniqueName(existingFieldNames, typeName);

                generateSerializerClass(dataName, node);

                // Nested serialization
                sb.append(indent).append("sb.Append(new ").append(typeName).append("Serializer().Serialize(")
                        .append(varName).append(".").append(propertyName).append("));\n");

            } else {
                processChildrenSerialize(node, sb, existingFieldNames, varName);
            }
        } else if ("fCSList".equals(tagName) || "iCollF".equals(tagName)) {
            String timesStr = node.getAttributes().get("times");
            int times = 1;
            boolean isList = false;
            if (timesStr != null) {
                if ("*".equals(timesStr)) {
                    isList = true;
                    // Assume dynamic list
                } else {
                    try {
                        times = Integer.parseInt(timesStr);
                        isList = true;
                    } catch (NumberFormatException e) {
                    }
                }
            }

            if (dataName != null) {
                String propertyName = sanitizeName(dataName);
                propertyName = getUniqueName(existingFieldNames, propertyName);
                String itemType = "string"; // Default

                XmlNode itemNode = findItemNode(node);
                if (itemNode != null) {
                    String itemDataName = itemNode.getAttributes().get("dataName");
                    if (itemDataName != null) {
                        itemType = sanitizeName(itemDataName);
                    } else {
                        itemType = sanitizeName(dataName) + "Item";
                    }
                    generateSerializerClass(itemType, itemNode);
                }

                sb.append(indent).append("// List ").append(propertyName).append("\n");
                if ("*".equals(timesStr)) {
                    sb.append(indent).append("if (").append(varName).append(".").append(propertyName)
                            .append(" != null) {\n");
                    sb.append(indent).append("    foreach (var item in ").append(varName).append(".")
                            .append(propertyName).append(") {\n");
                    sb.append(indent).append("        sb.Append(new ").append(itemType)
                            .append("Serializer().Serialize(item));\n");
                    sb.append(indent).append("    }\n");
                    sb.append(indent).append("}\n");
                } else {
                    sb.append(indent).append("for (int i = 0; i < ").append(times).append("; i++) {\n");
                    sb.append(indent).append("    if (").append(varName).append(".").append(propertyName)
                            .append(" != null && i < ").append(varName).append(".").append(propertyName)
                            .append(".Count) {\n");
                    sb.append(indent).append("        sb.Append(new ").append(itemType)
                            .append("Serializer().Serialize(").append(varName).append(".").append(propertyName)
                            .append("[i]));\n");
                    sb.append(indent).append("    } else {\n");
                    sb.append(indent).append("        sb.Append(new ").append(itemType)
                            .append("Serializer().Serialize(new ").append(itemType).append("()));\n");
                    sb.append(indent).append("    }\n");
                    sb.append(indent).append("}\n");
                }
            } else {
                // Inline list logic not supported well without Property, ignoring or recursing?
                // If no dataName, and times is set, we can't iterate a property.
                // We might just repeat the structure 'times' times relative to current obj?
                // But CsModelGenerator ignores these.
            }
        } else if (isDataField(tagName)) {
            if (dataName != null) {
                String propertyName = sanitizeName(dataName);
                propertyName = getUniqueName(existingFieldNames, propertyName);

                int length = 0;
                String padChar = " ";
                boolean rightJustify = false;

                for (int k = index + 1; k < siblings.size(); k++) {
                    XmlNode sibling = siblings.get(k);
                    if (sibling.getTagName().endsWith("Decor")) {
                        Map<String, String> attrs = sibling.getAttributes();
                        if (attrs.containsKey("length")) {
                            try {
                                length = Integer.parseInt(attrs.get("length"));
                            } catch (Exception e) {
                            }
                        }
                        if (sibling.getTagName().startsWith("num") || sibling.getTagName().contains("decimal")) {
                            padChar = "0";
                            rightJustify = true;
                        }
                        break;
                    }
                    if (isDataField(sibling.getTagName()) || "record".equals(sibling.getTagName())
                            || "fCSRecord".equals(sibling.getTagName())) {
                        break;
                    }
                }

                if (length > 0) {
                    String valProp = varName + "." + propertyName;
                    sb.append(indent).append("{\n");
                    sb.append(indent).append("    string val = ").append(valProp).append(" ?? \"\";\n");
                    if (rightJustify) {
                        sb.append(indent).append("    if (val.Length > ").append(length)
                                .append(") val = val.Substring(0, ").append(length).append(");\n");
                        sb.append(indent).append("    else val = val.PadLeft(").append(length).append(", '")
                                .append(padChar).append("');\n");
                    } else {
                        sb.append(indent).append("    if (val.Length > ").append(length)
                                .append(") val = val.Substring(0, ").append(length).append(");\n");
                        sb.append(indent).append("    else val = val.PadRight(").append(length).append(", '")
                                .append(padChar).append("');\n");
                    }
                    sb.append(indent).append("    sb.Append(val);\n");
                    sb.append(indent).append("}\n");
                }
            }
        } else if ("constant".equals(tagName)) {
            String constVal = node.getAttributes().get("value");
            if (constVal == null)
                constVal = "";
            int length = 0;
            for (int k = index + 1; k < siblings.size(); k++) {
                XmlNode sibling = siblings.get(k);
                if (sibling.getTagName().endsWith("Decor") && sibling.getAttributes().containsKey("length")) {
                    length = Integer.parseInt(sibling.getAttributes().get("length"));
                    break;
                }
                if (isDataField(sibling.getTagName()))
                    break;
            }
            if (length > 0) {
                sb.append(indent).append("sb.Append(\"").append(constVal).append("\".PadRight(").append(length)
                        .append("));\n");
            }
        } else if ("fixedLength".equals(tagName)) {
            String lenStr = node.getAttributes().get("length");
            if (lenStr != null) {
                sb.append(indent).append("sb.Append(new string(' ', ").append(lenStr).append("));\n");
            }
        } else if ("refFmt".equals(tagName)) {
            if (refId != null) {
                XmlNode referencedNode = model.getNodeById(refId);
                if (referencedNode != null) {
                    processChildrenSerialize(referencedNode, sb, existingFieldNames, varName);
                }
            }
        } else if ("fmtDef".equals(tagName)) {
            processChildrenSerialize(node, sb, existingFieldNames, varName);
        } else {
            if (node.getChildren().size() > 0 && !"fCSList".equals(tagName) && !"iCollF".equals(tagName)
                    && !"fCSRecord".equals(tagName)) {
                processChildrenSerialize(node, sb, existingFieldNames, varName);
            }
        }
    }

    private void processNodeDeserialize(XmlNode node, StringBuilder sb, Set<String> existingFieldNames, String varName,
            List<XmlNode> siblings, int index) {
        String tagName = node.getTagName();
        String dataName = node.getAttributes().get("dataName");
        String refId = node.getRefId();
        String indent = "        ";

        if ("fCSRecord".equals(tagName) || "record".equals(tagName)) {
            if (dataName != null) {
                String typeName = sanitizeName(dataName);
                String propertyName = getUniqueName(existingFieldNames, typeName);
                generateSerializerClass(dataName, node);

                sb.append(indent).append("{\n");
                sb.append(indent).append("    var serializer = new ").append(typeName).append("Serializer();\n");
                sb.append(indent).append("    ").append(varName).append(".").append(propertyName)
                        .append(" = serializer.Deserialize(buffer, ref offset);\n");
                sb.append(indent).append("}\n");

            } else {
                processChildrenDeserialize(node, sb, existingFieldNames, varName);
            }
        } else if ("fCSList".equals(tagName) || "iCollF".equals(tagName)) {
            String timesStr = node.getAttributes().get("times");
            int times = 1;
            boolean isDynamic = false;

            if (timesStr != null) {
                if ("*".equals(timesStr)) {
                    isDynamic = true;
                } else {
                    try {
                        times = Integer.parseInt(timesStr);
                    } catch (Exception e) {
                    }
                }
            }

            if (dataName != null) {
                String propertyName = sanitizeName(dataName);
                propertyName = getUniqueName(existingFieldNames, propertyName);
                String itemType = "string";
                XmlNode itemNode = findItemNode(node);
                if (itemNode != null) {
                    String itemDataName = itemNode.getAttributes().get("dataName");
                    if (itemDataName != null) {
                        itemType = sanitizeName(itemDataName);
                    } else {
                        itemType = sanitizeName(dataName) + "Item";
                    }
                    generateSerializerClass(itemType, itemNode);
                }

                if (isDynamic) {
                    sb.append(indent).append("while (offset < buffer.Length) {\n");
                    sb.append(indent).append("    ").append(varName).append(".").append(propertyName)
                            .append(".Add(new ").append(itemType)
                            .append("Serializer().Deserialize(buffer, ref offset));\n");
                    sb.append(indent).append("}\n");
                } else {
                    sb.append(indent).append("for (int i = 0; i < ").append(times).append("; i++) {\n");
                    sb.append(indent).append("    ").append(varName).append(".").append(propertyName)
                            .append(".Add(new ").append(itemType)
                            .append("Serializer().Deserialize(buffer, ref offset));\n");
                    sb.append(indent).append("}\n");
                }
            }
        } else if (isDataField(tagName)) {
            if (dataName != null) {
                String propertyName = sanitizeName(dataName);
                propertyName = getUniqueName(existingFieldNames, propertyName);

                int length = 0;
                for (int k = index + 1; k < siblings.size(); k++) {
                    XmlNode sibling = siblings.get(k);
                    if (sibling.getTagName().endsWith("Decor") && sibling.getAttributes().containsKey("length")) {
                        length = Integer.parseInt(sibling.getAttributes().get("length"));
                        break;
                    }
                    if (isDataField(sibling.getTagName()) || "record".equals(sibling.getTagName()))
                        break;
                }

                if (length > 0) {
                    sb.append(indent).append("if (offset + ").append(length).append(" <= buffer.Length) {\n");
                    sb.append(indent).append("    ").append(varName).append(".").append(propertyName)
                            .append(" = buffer.Substring(offset, ").append(length).append(").Trim();\n");
                    sb.append(indent).append("    offset += ").append(length).append(";\n");
                    sb.append(indent).append("}\n");
                }
            }
        } else if ("constant".equals(tagName)) {
            int length = 0;
            for (int k = index + 1; k < siblings.size(); k++) {
                XmlNode sibling = siblings.get(k);
                if (sibling.getTagName().endsWith("Decor") && sibling.getAttributes().containsKey("length")) {
                    length = Integer.parseInt(sibling.getAttributes().get("length"));
                    break;
                }
                if (isDataField(sibling.getTagName()) || "record".equals(sibling.getTagName()))
                    break;
            }
            if (length > 0) {
                sb.append(indent).append("offset += ").append(length).append(";\n");
            }
        } else if ("fixedLength".equals(tagName)) {
            String lenStr = node.getAttributes().get("length");
            if (lenStr != null) {
                sb.append(indent).append("offset += ").append(lenStr).append(";\n");
            }
        } else if ("refFmt".equals(tagName)) {
            if (refId != null) {
                XmlNode referencedNode = model.getNodeById(refId);
                if (referencedNode != null) {
                    processChildrenDeserialize(referencedNode, sb, existingFieldNames, varName);
                }
            }
        } else if ("fmtDef".equals(tagName)) {
            processChildrenDeserialize(node, sb, existingFieldNames, varName);
        } else {
            if (node.getChildren().size() > 0 && !"fCSList".equals(tagName) && !"iCollF".equals(tagName)) {
                processChildrenDeserialize(node, sb, existingFieldNames, varName);
            }
        }
    }

    private boolean isDataField(String tagName) {
        return "fCSString".equals(tagName) || "fHostString".equals(tagName) || "fString".equals(tagName)
                || "fCSMessage".equals(tagName) || "fCSDynXml".equals(tagName);
    }

    // Duplicate helper methods
    private String getUniqueName(Set<String> existing, String baseName) {
        if (!existing.contains(baseName)) {
            existing.add(baseName);
            return baseName;
        }

        int counter = 2;
        while (true) {
            String newName = baseName + "_" + counter;
            if (!existing.contains(newName)) {
                existing.add(newName);
                return newName;
            }
            counter++;
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
                String refId = child.getRefId();
                if (refId != null) {
                    return model.getNodeById(refId);
                }
            }
        }
        return null;
    }

    private String sanitizeName(String name) {
        if (name == null)
            return "Unknown";
        String clean = name.replace(".", "_")
                .replace("-", "_")
                .replace(":", "_");

        if (Character.isDigit(clean.charAt(0))) {
            clean = "_" + clean;
        }
        return clean;
    }
}
