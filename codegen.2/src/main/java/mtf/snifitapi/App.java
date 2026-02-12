package mtf.snifitapi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import mtf.snifitapi.model.SnifitModel;
import mtf.snifitapi.model.XmlNode;
import mtf.snifitapi.parser.SnifitParser;

import mtf.snifitapi.codegen.CSharpGenerator;

public class App {
    public static void main(String[] args) {
        String xmlsDir = "xmls";
        String fmtDefId = null;

        if (args.length > 0) {
            xmlsDir = args[0];
        }
        if (args.length > 1) {
            fmtDefId = args[1];
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
                System.out.println("Parsing " + xmlFile.getName() + "...");
                parser.parseInto(xmlFile, unifiedModel);

            } catch (Exception e) {
                System.err.println("Error parsing " + xmlFile.getName());
                e.printStackTrace();
            }
        }

        if (fmtDefId != null) {
            System.out.println("\n--- Generating C# Code for " + fmtDefId + " ---\n");
            CSharpGenerator generator = new CSharpGenerator(unifiedModel);
            Map<String, String> generatedFiles = generator.generate(fmtDefId);

            String outputDirArgs = "/tmp/generated_cs";
            File outDir = new File(outputDirArgs);
            if (!outDir.exists()) {
                outDir.mkdirs();
            }

            System.out.println("Writing " + generatedFiles.size() + " files to " + outputDirArgs);

            for (Map.Entry<String, String> entry : generatedFiles.entrySet()) {
                String className = entry.getKey();
                String code = entry.getValue();
                Path path = Paths.get(outputDirArgs, className + ".cs");
                try {
                    Files.write(path, code.getBytes());
                    System.out.println("Wrote " + path.getFileName());
                } catch (IOException e) {
                    System.err.println("Failed to write " + path);
                    e.printStackTrace();
                }
            }

        } else {
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
}
