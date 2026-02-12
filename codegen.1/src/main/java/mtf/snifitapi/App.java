package mtf.snifitapi;

import java.io.File;
import java.io.IOException;

import mtf.snifitapi.model.SnifitModel;
import mtf.snifitapi.model.XmlNode;
import mtf.snifitapi.parser.SnifitParser;

public class App {
    public static void main(String[] args) {
        String xmlsDir = "xmls";
        if (args.length > 0) {
            xmlsDir = args[0];
        }

        File dir = new File(xmlsDir);
        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println("Directory not found or is not a directory: " + dir.getAbsolutePath());
            return;
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".xml"));
        if (files == null || files.length == 0) {
            System.out.println("No XML files found in " + xmlsDir);
            return;
        }

        SnifitParser parser = new SnifitParser();
        SnifitModel unifiedModel = new SnifitModel();

        for (File xmlFile : files) {
            try {
                System.out.println("--------------------------------------------------");
                System.out.println("Parsing " + xmlFile.getName() + "...");
                parser.parseInto(xmlFile, unifiedModel);
                System.out.println("Parsed successfully!");

            } catch (Exception e) {
                System.err.println("Error parsing " + xmlFile.getName());
                e.printStackTrace();
            }
        }

        System.out.println("--------------------------------------------------");
        System.out.println("Total Root Nodes: " + unifiedModel.getRootNodes().size());
        System.out.println("Total Indexed Nodes (by ID): " + unifiedModel.getIdToNodeMap().size());

        for (XmlNode root : unifiedModel.getRootNodes()) {
            System.out.println("Root Node: " + root.getTagName() +
                    (root.getId() != null ? " id=" + root.getId() : ""));

            int direct = 0;
            if (root.getChildren() != null)
                direct = root.getChildren().size();
            System.out.println("  Total direct children: " + direct);
        }

        XmlNode targetNode = unifiedModel.getIdToNodeMap().get("MTFC_Hitmakdut");
        if (targetNode != null) {
            System.out.println(targetNode.toJson());
        } else {
            System.out.println("Node MTFC_Hitmakdut not found.");
        }
    }
}
