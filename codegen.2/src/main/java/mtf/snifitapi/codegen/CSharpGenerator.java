package mtf.snifitapi.codegen;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import mtf.snifitapi.model.SnifitModel;
import mtf.snifitapi.model.XmlNode;

public class CSharpGenerator {

    private final SnifitModel model;
    private final Map<String, String> generatedFiles = new HashMap<>();
    private final Set<String> processedClasses = new HashSet<>();
    private boolean includeSourceComments = true;

    public CSharpGenerator(SnifitModel model) {
        this.model = model;
    }

    public void setIncludeSourceComments(boolean include) {
        this.includeSourceComments = include;
    }

    public Map<String, String> generate(String rootId) {
        XmlNode root = model.getNodeById(rootId);
        if (root == null) {
            System.err.println("// Root node '" + rootId + "' not found.");
            return generatedFiles;
        }

        generateClass(rootId, root);
        return generatedFiles;
    }

    private void generateClass(String className, XmlNode node) {
        String cleanClassName = sanitizeName(className);

        if (processedClasses.contains(cleanClassName)) {
            return;
        }
        processedClasses.add(cleanClassName);

        StringBuilder classContent = new StringBuilder();
        Set<String> existingFieldNames = new HashSet<>();

        if (includeSourceComments && node.getSourceFile() != null) {
            classContent.append("// Source: ").append(node.getSourceFile())
                    .append(" Line: ").append(node.getLineNumber()).append("\n");
        }
        classContent.append("public class ").append(cleanClassName).append(" {\n");

        processChildren(node, classContent, existingFieldNames);

        classContent.append("}\n");

        generatedFiles.put(cleanClassName, classContent.toString());
    }

    private void processChildren(XmlNode node, StringBuilder classContent, Set<String> existingFieldNames) {
        if (node == null || node.getChildren() == null)
            return;

        for (XmlNode child : node.getChildren()) {
            processNode(child, classContent, node, existingFieldNames);
        }
    }

    private void processNode(XmlNode node, StringBuilder classContent, XmlNode parentNode,
            Set<String> existingFieldNames) {
        String tagName = node.getTagName();
        String dataName = node.getAttributes().get("dataName");
        String refId = node.getRefId();

        String indent = "    ";

        if ("fCSRecord".equals(tagName) || "record".equals(tagName)) {
            if (dataName != null) {
                generateClass(dataName, node); // Recursive generation of separate class

                String typeName = sanitizeName(dataName);
                String propertyName = typeName;

                propertyName = getUniqueName(existingFieldNames, propertyName);

                if (includeSourceComments && node.getSourceFile() != null) {
                    classContent.append(indent).append("// Source: ").append(node.getSourceFile())
                            .append(" Line: ").append(node.getLineNumber()).append("\n");
                }

                boolean isList = false;
                if (parentNode != null && parentNode.getAttributes().get("times") != null) {
                    isList = true;
                }

                if (isList) {
                    classContent.append(indent).append("public List<").append(typeName).append("> ")
                            .append(propertyName).append(" { get; set; } = new List<").append(typeName)
                            .append(">();\n");
                } else {
                    classContent.append(indent).append("public ").append(typeName).append(" ")
                            .append(propertyName).append(" { get; set; }\n");
                }
            } else {
                processChildren(node, classContent, existingFieldNames);
            }
        } else if ("fCSList".equals(tagName)) {
            if (dataName != null) {
                XmlNode itemNode = findItemNode(node);
                String itemType = "string";

                if (itemNode != null) {
                    String itemDataName = itemNode.getAttributes().get("dataName");
                    if (itemDataName != null) {
                        itemType = sanitizeName(itemDataName);
                        generateClass(itemType, itemNode);
                    } else {
                        itemType = sanitizeName(dataName) + "Item";
                        generateClass(itemType, itemNode);
                    }
                }

                String propertyName = sanitizeName(dataName);
                propertyName = getUniqueName(existingFieldNames, propertyName);

                if (includeSourceComments && node.getSourceFile() != null) {
                    classContent.append(indent).append("// Source: ").append(node.getSourceFile())
                            .append(" Line: ").append(node.getLineNumber()).append("\n");
                }

                classContent.append(indent).append("public List<").append(itemType).append("> ")
                        .append(propertyName).append(" { get; set; } = new List<").append(itemType)
                        .append(">();\n");
            }
        } else if ("fCSString".equals(tagName) || "fHostString".equals(tagName) || "fString".equals(tagName)
                || "fCSMessage".equals(tagName) || "fCSDynXml".equals(tagName)) {
            if (dataName != null) {
                String propertyName = sanitizeName(dataName);
                propertyName = getUniqueName(existingFieldNames, propertyName);

                if (includeSourceComments && node.getSourceFile() != null) {
                    classContent.append(indent).append("// Source: ").append(node.getSourceFile())
                            .append(" Line: ").append(node.getLineNumber()).append("\n");
                }

                classContent.append(indent).append("public string ").append(propertyName)
                        .append(" { get; set; }\n");
            }
        } else if ("refFmt".equals(tagName)) {
            if (refId != null) {
                XmlNode referencedNode = model.getNodeById(refId);
                if (referencedNode != null) {
                    processChildren(referencedNode, classContent, existingFieldNames);
                } else {
                    classContent.append(indent).append("// Error: Referenced ID '").append(refId)
                            .append("' not found\n");
                }
            }
        } else if ("fmtDef".equals(tagName)) {
            processChildren(node, classContent, existingFieldNames);
        } else {
            if (!node.getChildren().isEmpty()) {
                processChildren(node, classContent, existingFieldNames);
            }
        }
    }

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
