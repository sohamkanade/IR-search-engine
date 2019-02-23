/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir_search_engine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 *
 * @author soham
 */
public class Driver {
    
    final static String FILE_PATH = "/home/soham/Desktop/blah/";

    final static List<String> STOP_WORDS = Arrays.asList("a", "able", "about",
            "across", "after", "all", "almost", "also", "am", "among", "an",
            "and", "any", "are", "as", "at", "be", "because", "been", "but",
            "by", "can", "cannot", "could", "dear", "did", "do", "does",
            "either", "else", "ever", "every", "for", "from", "get", "got",
            "had", "has", "have", "he", "her", "hers", "him", "his", "how",
            "however", "i", "if", "in", "into", "is", "it", "its", "just",
            "least", "let", "like", "likely", "may", "me", "might", "most",
            "must", "my", "neither", "no", "nor", "not", "of", "off", "often",
            "on", "only", "or", "other", "our", "own", "rather", "said", "say",
            "says", "she", "should", "since", "so", "some", "than", "that",
            "the", "their", "them", "then", "there", "these", "they", "this",
            "tis", "to", "too", "twas", "us", "wants", "was", "we", "were",
            "what", "when", "where", "which", "while", "who", "whom", "why",
            "will", "with", "would", "yet", "you", "your");

    public static void main(String[] args) throws IOException {
        List<File> files = listAllFiles(new File(FILE_PATH), new ArrayList<>());
        for (File file : files) {
            String parsedString = parseHTML(file);

            List<String> tokens = new ArrayList<>();

            System.out.println(parsedString);
            for (String word : parsedString.split("[\\W_]+")) {
                word = word.toLowerCase();
                if (STOP_WORDS.contains(word) || word.length() < 3) {
                    continue;
                }
                tokens.add(word);
                System.out.println(word);
            }

            for (String token : tokens) {
                System.out.println(token);
            }
        }
    }

    public static String parseHTML(File htmlFile) throws IOException {
        StringBuilder parsedStringBuilder = new StringBuilder();
        if (htmlFile != null) {
            Document doc = Jsoup.parse(htmlFile, "utf-8");
            doc.select("a, script, style, .hidden, option, span").remove();
            parsedStringBuilder.append(doc.text());
            //meta - description
            if (doc.select("meta[name=description]") != null && doc.select("meta[name=description]").size() > 0) {
                parsedStringBuilder.append(doc.select("meta[name=description]").get(0).attr("content"));
            }
            //meta - keywords
            if (doc.select("meta[name=keywords]") != null && doc.select("meta[name=keywords]").size() > 0) {
                parsedStringBuilder.append(doc.select("meta[name=keywords]").get(0).attr("content"));
            }

        }
        return parsedStringBuilder.toString();
    }

    public static List<File> listAllFiles(File folder, List<File> files) {
        File[] fileNames = folder.listFiles();
        if (fileNames != null) {
            for (File file : fileNames) {
                // if directory call the same method again
                if (file.isDirectory()) {
                    listAllFiles(file, files);
                } else {
                    files.add(file);
                }
            }
        }
        return files;
    }

}
