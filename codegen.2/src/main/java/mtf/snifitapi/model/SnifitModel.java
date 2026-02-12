package mtf.snifitapi.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SnifitModel {
    private List<XmlNode> rootNodes = new ArrayList<>();
    private Map<String, XmlNode> idToNodeMap = new HashMap<>();

    public void setRootNodes(List<XmlNode> rootNodes) {
        this.rootNodes = rootNodes;
    }

    public void addRootNodes(List<XmlNode> newRootNodes) {
        this.rootNodes.addAll(newRootNodes);
    }

    public List<XmlNode> getRootNodes() {
        return rootNodes;
    }

    public void addIdToNodeMap(Map<String, XmlNode> newIdMap) {
        this.idToNodeMap.putAll(newIdMap);
    }

    public void setIdToNodeMap(Map<String, XmlNode> idToNodeMap) {
        this.idToNodeMap = idToNodeMap;
    }

    public Map<String, XmlNode> getIdToNodeMap() {
        return idToNodeMap;
    }

    public XmlNode getNodeById(String id) {
        return idToNodeMap.get(id);
    }

    // Inner static classes for specific node types to add semantic meaning
    // These currently inherit from XmlNode but could be expanded with specific
    // logic

    public static class Context extends XmlNode {
        public Context() {
            super("context");
        }
    }

    public static class KColl extends XmlNode {
        public KColl() {
            super("kColl");
        }
    }

    public static class IColl extends XmlNode {
        public IColl() {
            super("iColl");
        }
    }

    public static class MatafIColl extends XmlNode {
        public MatafIColl() {
            super("matafIColl");
        }
    }

    public static class RefKColl extends XmlNode {
        public RefKColl() {
            super("refKColl");
        }
    }

    public static class RefData extends XmlNode {
        public RefData() {
            super("refData");
        }
    }

    public static class Field extends XmlNode {
        public Field() {
            super("field");
        }
    }

    public static class FmtDef extends XmlNode {
        public FmtDef() {
            super("fmtDef");
        }
    }

    public static class RefFmt extends XmlNode {
        public RefFmt() {
            super("refFmt");
        }
    }

    public static class Record extends XmlNode {
        public Record() {
            super("record");
        }
    }

    public static class FCSRecord extends XmlNode {
        public FCSRecord() {
            super("fCSRecord");
        }
    }

    public static class FCSList extends XmlNode {
        public FCSList() {
            super("fCSList");
        }
    }

    public static class ICollF extends XmlNode {
        public ICollF() {
            super("iCollF");
        }
    }

    public static class FCSString extends XmlNode {
        public FCSString() {
            super("fCSString");
        }
    }

    public static class FHostString extends XmlNode {
        public FHostString() {
            super("fHostString");
        }
    }

    // Decorators could be nodes too
    public static class Decorator extends XmlNode {
        public Decorator(String tagName) {
            super(tagName);
        }
    }

    public static class Operation extends XmlNode {
        public Operation() {
            super("operation");
        }
    }

    public static class OpStep extends XmlNode {
        public OpStep() {
            super("opStep");
        }
    }

    public static class RefOpSteps extends XmlNode {
        public RefOpSteps() {
            super("refOpSteps");
        }
    }

    public static class RefFormat extends XmlNode {
        public RefFormat() {
            super("refFormat");
        }
    }

    public static class VisualField extends XmlNode {
        public VisualField() {
            super("visualField");
        }
    }

    public static class MsgField extends XmlNode {
        public MsgField() {
            super("msgField");
        }
    }

    public static class MapperConverter extends XmlNode {
        public MapperConverter() {
            super("mapperConverter");
        }
    }

    public static class Move extends XmlNode {
        public Move() {
            super("move");
        }
    }

    public static class FCSDynXml extends XmlNode {
        public FCSDynXml() {
            super("fCSDynXml");
        }
    }

    public static class FCSMessage extends XmlNode {
        public FCSMessage() {
            super("fCSMessage");
        }
    }

    public static class FString extends XmlNode {
        public FString() {
            super("fString");
        }
    }

    public static class Constant extends XmlNode {
        public Constant() {
            super("constant");
        }
    }

    public static class FixedLength extends XmlNode {
        public FixedLength() {
            super("fixedLength");
        }
    }

    public static class GenericNode extends XmlNode {
        public GenericNode(String tagName) {
            super(tagName);
        }
    }
}
