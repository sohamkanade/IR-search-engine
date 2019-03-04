/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package searchengine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 *
 * @author soham
 */
public class Driver {

    final static String FILE_PATH = "/home/soham/Desktop/blah/";

    final static List<String> STOP_WORDS = Arrays.asList("a", "able", "about", "across", "after", "all", "almost",
            "also", "am", "among", "an", "and", "any", "are", "as", "at", "be", "because", "been", "brought", "but", "by", "can",
            "cannot", "could", "dear", "did", "do", "does", "either", "else", "ever", "every", "for", "from", "get",
            "got", "had", "has", "have", "he", "her", "hers", "him", "his", "how", "however", "i", "if", "in", "into",
            "is", "it", "its", "just", "know", "known", "least", "let", "like", "likely", "may", "made", "make", "me", "might", "more",
            "most", "must", "my", "neither", "no", "nor", "not", "of", "off", "often", "on", "only", "or", "other", "our", "own", "rather",
            "said", "say", "says", "she", "should", "since", "so", "some", "than", "that", "the", "their", "them",
            "then", "there", "these", "they", "this", "tis", "to", "too", "twas", "us", "wants", "was", "we", "well", "were",
            "what", "when", "where", "which", "while", "who", "whom", "why", "will", "with", "would", "yet", "you",
            "your");

    private static final String CHECK_DOCUMENT = "SELECT t FROM Token t WHERE t.tokenPK.tokenName = :tokenName AND t.tokenPK.documentID = :documentID";

    /**
     * Total Documents Indexed
     */
    private static final int CORPUS_SIZE = 35474;

    /**
     * Weights for Title, Meta, Heading, Body
     */
    private static final int WEIGHT_TITLE = 50;
    private static final int WEIGHT_HEADING = 20;
    private static final int WEIGHT_BODY = 30;

    private static EntityManagerFactory emFactory;
    static int fileCounter = 0;

    public static void main(String[] args) throws IOException {
        emFactory = Persistence.createEntityManagerFactory("wq2019.inf225_IR-search-engine_jar_1.0PU");

        List<File> files = listAllFiles(new File(FILE_PATH), new ArrayList<>());

        for (File document : files) {
            if (!isValidHTML(FileUtils.readFileToString(document))) {
                continue;
            }

            Map<String, Integer> tokenWeights = new HashMap<>();
            Map<String, Integer> tokenCounter = new HashMap<>();

            String parsedString = parseHTML(document);
            String docId = document.getAbsolutePath().replace(FILE_PATH, "").replace("\\", "/");
            parsedString = parsedString.trim();
            String[] tokens = parsedString.split(" ");
            int totalWords = tokens.length;

            for (String token : tokens) {
                int weight = getTokenWeight(token);
                token = token.substring(3, token.length());
                
                int tokenCount = tokenCounter.getOrDefault(token, 0);
                tokenCount++;
                tokenCounter.put(token, tokenCount);
                
                int existingWeight = tokenWeights.getOrDefault(token, 0);
                
                if(existingWeight == 0 || (weight-existingWeight) == 0) {
                    tokenWeights.put(token, weight);
                } else {
                    
                }
                
                
                
            }

        }


        /*System.out.println("Indexing file: " + docId);

            Map<String, Integer> docMap = new HashMap<>();

            for (String word : parsedString.split("[\\W_]+")) {
                word = word.toLowerCase();

                if (STOP_WORDS.contains(word) || word.length() < 3 || word.length() > 25 || word.matches("-?\\d+(\\.\\d+)?")) {
                    continue;
                }

                Integer wordFreq = docMap.getOrDefault(word, 0);
                ++wordFreq;
                docMap.put(word, wordFreq);

            }
            EntityManager em = emFactory.createEntityManager();
            em.getTransaction().begin();
            for (String key : docMap.keySet()) {
                // createNewToken(key, docId, docMap.get(key));

                Token token = new Token();
                TokenPK tokenPK = new TokenPK(key, docId);
                token.setTokenPK(tokenPK);
                token.setFrequency(docMap.get(key));

                em.persist(token);

            }
            em.getTransaction().commit();
            em.close();

            fileCounter++;
            System.out.println("Done Indexing: " + docId);
            System.out.println("Files Processed - " + fileCounter);
            System.out.println("--------------------------");*/
    }

    private static int getTokenWeight(String token) {
        String identifier = token.substring(0, 3);
        switch (identifier) {
            case "|T|":
                return WEIGHT_TITLE;
            case "|H|":
                return WEIGHT_HEADING;
            case "|B|":
                return WEIGHT_BODY;
            default:
                return WEIGHT_BODY;
        }
    }

    public static boolean isValidWord(String word) {
        if (STOP_WORDS.contains(word)
                || word.length() < 3
                || word.length() > 45
                || word.matches(".*\\d+.*")) {
            return false;
        }
        return true;
    }

    public static boolean isValidHTML(String fileString) {
        Pattern htmlPattern = Pattern.compile(".*\\<[^>]+>.*", Pattern.DOTALL);
        return htmlPattern.matcher(fileString).matches();
    }

    private static String buildSpecificString(String inputText, String appendText) {
        StringBuilder sb = new StringBuilder();
        for (String word : inputText.split("[\\W_]+")) {
            if (isValidWord(word)) {
                sb.append(appendText + word);
            }
        }
        return sb.toString();
    }

    public static String parseHTML(File htmlFile) throws IOException {
        StringBuilder parsedStringBuilder = new StringBuilder();
        if (htmlFile != null) {
            Document doc = Jsoup.parse(htmlFile, "utf-8");
            doc.select("a, script, style, .hidden, option, span").remove();

            String titleText = doc.getElementsByTag("title").text().toLowerCase();
            parsedStringBuilder.append(buildSpecificString(titleText, " |T|"));
            doc.select("title").remove();

            String headingText = doc.select("h1, h2, h3, h4, h5, h6").text().toLowerCase();
            parsedStringBuilder.append(buildSpecificString(headingText, " |H|"));
            doc.select("h1, h2, h3, h4, h5, h6").remove();

            String bodyText = doc.select("body").text().toLowerCase();
            parsedStringBuilder.append(buildSpecificString(bodyText, " |B|"));

            // meta - description
            if (doc.select("meta[name=description]") != null && doc.select("meta[name=description]").size() > 0) {
                String descText = doc.select("meta[name=description]").get(0).attr("content").toLowerCase();
                parsedStringBuilder.append(buildSpecificString(descText, " |B|"));
            }
            // meta - keywords
            if (doc.select("meta[name=keywords]") != null && doc.select("meta[name=keywords]").size() > 0) {
                String keywordText = doc.select("meta[name=description]").get(0).attr("content").toLowerCase();
                parsedStringBuilder.append(buildSpecificString(keywordText, " |B|"));
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

    /**
     * Returns true if the token already exists in the DB
     *
     * @param tokenName
     * @return
     */
    public static boolean tokenExists(String tokenName) {
        EntityManager em = emFactory.createEntityManager();
        em.getTransaction().begin();
        TypedQuery<Token> query = em.createNamedQuery("Token.findByTokenName", Token.class);
        query.setParameter("tokenName", tokenName);
        em.getTransaction().commit();
        em.close();
        return !query.getResultList().isEmpty();
    }

    /**
     * Creates a new Token in the Database
     *
     * @param tokenName
     * @param documentID
     * @param frequency
     * @return
     */
    public static Token createNewToken(String tokenName, String documentID, Integer frequency) {
        EntityManager em = emFactory.createEntityManager();
        Token token = new Token();
        TokenPK tokenPK = new TokenPK(tokenName, documentID);
        token.setTokenPK(tokenPK);
        

        em.getTransaction().begin();
        em.persist(token);
        em.getTransaction().commit();

        em.close();

        return token;
    }

    /**
     * Gets the corresponding DocumentID for a given Token Name
     *
     * @param tokenName
     * @return
     */
    public static List<String> getCorrespondingDocumentIDs(String tokenName) {
        EntityManager em = emFactory.createEntityManager();

        TypedQuery<Token> query = em.createNamedQuery("Token.findByTokenName", Token.class);
        query.setParameter("tokenName", tokenName);

        em.getTransaction().begin();
        em.getTransaction().commit();
        List<Token> resultList = query.getResultList();
        em.close();

        List<String> documentIDs = new ArrayList<>();

        if (!resultList.isEmpty()) {
            for (Token t : resultList) {
                documentIDs.add(t.getTokenPK().getDocumentID());
            }
        }
        return documentIDs;

    }

    /**
     * Checks if a documentID exists for the given tokenName
     *
     * @param tokenName
     * @param documentID
     * @return
     */
    public static boolean documentExists(String tokenName, String documentID) {
        EntityManager em = emFactory.createEntityManager();

        TypedQuery<Token> query = em.createQuery(CHECK_DOCUMENT, Token.class);
        query.setParameter("tokenName", tokenName);
        query.setParameter("documentID", documentID);
        em.getTransaction().begin();
        em.getTransaction().commit();
        List resultList = query.getResultList();
        em.close();
        return !resultList.isEmpty();

    }

}
