package ru.riverx;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class JackAnalyzer {
    public static void main(String[] args) {
        if (args.length > 0) {
            String name = args[0];
            if (name.contains(".jack"))
                compileFile(name);
            else
                compileFolder(name);
        } else {
            System.out.println("Please provide the .jack file or directory to compile");
            System.out.println("Example: java GrammarAnalyzer Main.jack");
            System.out.println("Example: java GrammarAnalyzer PongGame-folder");
        }
    }

    private static void compileFile(String filename) {
        String file = readFile(filename);
        JackTokenizer tokenizer = new JackTokenizer(file);
        CompilationEngine engine = new CompilationEngine(tokenizer);
        writeTokensToFile(filename, engine.getTokensXml());
        //writeTokensToFile(filename, tokenizer.getTokenListAsStringList());
    }

    private static void compileFolder(String folderName) {
        List<String> filenames = getAllFilenames(folderName);
        if (filenames != null) {
            for (String filename : filenames) {
                compileFile(filename);
            }
        }
    }

    private static List<String> getAllFilenames(String folderName) {
        try (Stream<Path> paths = Files.walk(Paths.get(folderName))) {
            List<String> files = new ArrayList<>();
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".jack"))
                    .forEach(p -> {
                        String base = p.getFileName().toString();
                        files.add(folderName + "/" + base);
                    });
            return files;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String readFile(String filename) {
        StringBuilder buffer = new StringBuilder();
        try {
            List<String> tmp = Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8);
            for (String line : tmp) {
                buffer.append(line).append("\n");
            }
        } catch (IOException e) {
            System.err.println("Error: " + e);
            e.printStackTrace();
        }
        return buffer.toString();
    }

    private static void writeTokensToFile(String filename, List<String> tokenList) {
        try {
            String name = filename.substring(0, filename.indexOf("."));
            Files.write(Paths.get(name + ".xml"), tokenList, StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
