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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

/**
 *
 * @author soham
 */
public class Driver {

    final static String PARENT_FOLDER = "/home/soham/Desktop/webpages/WEBPAGES_RAW/";

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
    private static final String DISTINCT_TOKENS = "SELECT distinct(t.tokenPK.tokenName) FROM Token t";

    /**
     * Total Documents Indexed
     */
    private static final int CORPUS_SIZE = 34317;

    /**
     * Weights for Title, Meta, Heading, Body
     */
    private static final int WEIGHT_TITLE = 50;
    private static final int WEIGHT_HEADING = 20;
    private static final int WEIGHT_BODY = 30;

    private static EntityManagerFactory emFactory;

    public static void main(String[] args) throws IOException {
        emFactory = Persistence.createEntityManagerFactory("wq2019.inf225_IR-search-engine_jar_1.0PU");

//        for (int i = 0; i < 75; i++) {
//
//        }
        Map<String, Integer> idfMap = getTokenDocCountMap();

        //insertIntoMetaToken(idfMap);
    }

    private static void insertIntoMetaToken(Map<String, Integer> idfMap) {
        EntityManager em = emFactory.createEntityManager();
        em.getTransaction().begin();
        for (String token : idfMap.keySet()) {
            int docCount = idfMap.get(token);
            double logVal = (double) CORPUS_SIZE / docCount;
            double idfVal = Math.log10(logVal);

            MetaToken mtoken = new MetaToken(token, idfVal);
            em.persist(mtoken);

        }
        System.out.println("Committing to DB......");
        em.getTransaction().commit();        
        em.close();
        System.out.println("Committed to DB.");
    }

    private static Map<String, Integer> getTokenDocCountMap() {
        EntityManager em = emFactory.createEntityManager();
        Map<String, Integer> docCountMap = new HashMap<>();

        List<Object[]> results = em
                .createQuery("SELECT COUNT(t.tokenPK.documentID), t.tokenPK.tokenName FROM Token t GROUP BY t.tokenPK.tokenName").getResultList();
        for (Object[] result : results) {
            String tokenName = (String) result[1];
            int count = ((Number) result[0]).intValue();
            docCountMap.put(tokenName, count);
        }

        /*TypedQuery<String> query = em.createQuery(DISTINCT_TOKENS, String.class);
        em.getTransaction().begin();
        em.getTransaction().commit();
        List<String> tokenNames = query.getResultList();
        em.close();
        System.out.println(tokenNames.size());
        return tokenNames;*/
        System.out.println(docCountMap.size());
        return docCountMap;
    }

    private static void createIndex(int folderNumber) throws IOException {
        int fileCount = 0;
        int countMapSize = 0;
        int weightMapSize = 0;
        int invalidFileCount = 0;

        Set<Token> tokenSet = new HashSet<>();

        String filePath = PARENT_FOLDER + folderNumber + "/";
        File[] files = new File(filePath).listFiles();
        for (File document : files) {
            fileCount++;
            String docId = document.getAbsolutePath().replace(PARENT_FOLDER, "").replace("\\", "/");
            System.out.println("PROGRESS : Current Doc ID - " + docId);

            Map<String, Integer> tokenCountMap = new HashMap<>();
            Map<String, List<Integer>> weightMap = new HashMap<>();

            try {
                if (isInvalidHTML(FileUtils.readFileToString(document))) {
                    System.out.println("INVALID : HTML file - " + docId);
                    invalidFileCount++;
                    continue;
                }
            } catch (Exception e) {
                invalidFileCount++;
                System.out.println("INVALID : Exception occured for DocID - " + docId);
                System.out.println("INVALID : Exception is - " + e.toString());
                continue;
            }

            String parsedString = parseHTML(document, weightMap).trim();
            if (!parsedString.isEmpty()) {
                String[] parsedStringArray = parsedString.split("\\s+");

                int docWordCount = parsedStringArray.length;

                for (String token : parsedStringArray) {
                    int tokenCount = tokenCountMap.getOrDefault(token, 0);
                    tokenCount++;
                    tokenCountMap.put(token, tokenCount);
                }

                for (String tokenName : tokenCountMap.keySet()) {
                    double tFreq = ((double) tokenCountMap.get(tokenName)) / docWordCount;
                    int weight = 0;

                    weight = sumElementsInList(weightMap.get(tokenName));

                    Token tkn = new Token(tokenName, docId);
                    tkn.setTfrequency(tFreq);
                    tkn.setWeight(weight);
                    tokenSet.add(tkn);
                }
                countMapSize += tokenCountMap.size();
                weightMapSize += weightMap.size();
            } else {
                invalidFileCount++;
                System.out.println("INVALID : No tokens in file - " + docId);
            }
        }

        System.out.println("FINAL STATS : " + fileCount + " files processed. | " + "Invalid Files - " + invalidFileCount + " | Valid Files - " + (fileCount - invalidFileCount));
        System.out.println("FINAL STATS : Total unique tokens in tokenSet - " + tokenSet.size());
        System.out.println("FINAL STATS : Total tokens in all count maps - " + countMapSize);
        System.out.println("FINAL STATS : Total tokens in all weight maps - " + weightMapSize);

        EntityManager em = emFactory.createEntityManager();
        em.getTransaction().begin();
        for (Token tkn : tokenSet) {
            em.persist(tkn);
        }
        System.out.println("Persisted " + tokenSet.size() + " tokens.");
        System.out.println("Committing to DB......");
        em.getTransaction().commit();
        System.out.println("Committed to DB.");
        em.close();
        System.out.println("Goodbye. Committed Folder " + folderNumber + " to DB.");
    }

    public static String parseHTML(File htmlFile, Map<String, List<Integer>> weightMap) throws IOException {
        StringBuilder parsedStringBuilder = new StringBuilder();

        if (htmlFile != null) {
            Document doc = Jsoup.parse(htmlFile, "utf-8");
            doc.select("a, script, style, .hidden, option").remove();

            String titleText = doc.getElementsByTag("title").text().toLowerCase();
            doc.select("title").remove();
            if (!titleText.isEmpty()) {
                parsedStringBuilder.append(buildParsedString(titleText, WEIGHT_TITLE, weightMap));
            }

            String headingText = doc.select("h1, h2, h3, h4, h5, h6").text().toLowerCase();
            doc.select("h1, h2, h3, h4, h5, h6").remove();
            if (!headingText.isEmpty()) {
                parsedStringBuilder.append(buildParsedString(headingText, WEIGHT_HEADING, weightMap));
            }

            String bodyText = doc.select("body").text().toLowerCase();
            if (!bodyText.isEmpty()) {
                parsedStringBuilder.append(buildParsedString(bodyText, WEIGHT_BODY, weightMap));
            }

            // meta - description
            if (!doc.select("meta[name=description]").isEmpty()) {
                String descText = doc.select("meta[name=description]").get(0).attr("content").toLowerCase();
                parsedStringBuilder.append(buildParsedString(descText, WEIGHT_BODY, weightMap));
            }
            // meta - keywords
            if (!doc.select("meta[name=keywords]").isEmpty()) {
                String keywordText = doc.select("meta[name=keywords]").get(0).attr("content").toLowerCase();
                parsedStringBuilder.append(buildParsedString(keywordText, WEIGHT_BODY, weightMap));
            }

        }
        return parsedStringBuilder.toString();
    }

    private static String buildParsedString(String inputText, int weight, Map<String, List<Integer>> weightMap) {
        StringBuilder sb = new StringBuilder();
        for (String word : inputText.split("[\\W_]+")) {
            if (isValidWord(word)) {
                List<Integer> weightStrings = weightMap.getOrDefault(word, new ArrayList<>());
                if (!weightStrings.contains(weight)) {
                    weightStrings.add(weight);
                    weightMap.put(word, weightStrings);
                    sb.append(word).append(" ");
                }
            }
        }
        return sb.toString();
    }

    private static boolean isValidWord(String word) {
        return !(STOP_WORDS.contains(word)
                || word.length() < 3
                || word.length() > 45
                || word.matches(".*\\d+.*"));
    }

    private static boolean isInvalidHTML(String fileString) {
        Document doc = Jsoup.parse(fileString, "", Parser.xmlParser());
        return doc.select("html").isEmpty()
                && doc.select("head").isEmpty()
                && doc.select("body").isEmpty();

    }

    private static int sumElementsInList(List<Integer> inputList) {
        int sum = 0;
        for (int i : inputList) {
            sum += i;
        }
        return sum;
    }

    private static List<File> listAllFiles(File folder, List<File> files) {
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
            resultList.forEach((t) -> {
                documentIDs.add(t.getTokenPK().getDocumentID());
            });
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
