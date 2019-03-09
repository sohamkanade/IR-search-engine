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
import java.util.Collections;
import java.util.Comparator;
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
 * Driver class for the "Soniya" Search Engine for INF 225 Includes code for
 * building the Inverted Index and Searching via Console
 *
 * @author soham
 */
public class Driver {

    /**
     * Location for the Parent Folder where all the HTML files are dumped
     */
    final static String PARENT_FOLDER = "/home/soham/Desktop/webpages/WEBPAGES_RAW/";

    /**
     * Stop Words List -- used for Tokenizing
     */
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

    /**
     * Query for getting Token Details from the DB with ORDER BY
     */
    private static final String TOKEN_ORDERED = "SELECT t FROM Token t WHERE t.tokenPK.tokenName = :tokenName ORDER BY t.weight DESC, t.tfIdf DESC";

    /**
     * Query for getting aggregated Token Details using GROUP BY
     */
    private static final String TOKEN_AGGREGATED = "SELECT COUNT(t.tokenPK.documentID), t.tokenPK.tokenName FROM Token t GROUP BY t.tokenPK.tokenName";

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

    /**
     * Entity Manager (JPA) used for Persistence
     */
    private static EntityManagerFactory emFactory;

    /**
     * Unit name for JPA Persistence
     */
    private static final String PERSISTENCE_UNIT_NAME = "wq2019.inf225_IR-search-engine_jar_1.0PU";

    public static void main(String[] args) throws IOException {
        System.out.println("SONIYA SEARCH ENGINE");
        System.out.println("------------------------------------------------------------");
        //Create the EntityManagerFactory
        emFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);

        /**
         * *
         * CREATE INVERTED INDEX in DB block Uncomment when required.
         */
        /*for (int i = 0; i < 75; i++) {
            //Iterate from Folders 0 to 74 to Parse the HTML Files and create the Inverted Index in DB
            createIndex(i);
        }*/
        /**
         * *
         * CREATE IDF DETAILS in DB block Uncomment when required.
         */
        /*Map<String, Integer> idfMap = getTokenDocCountMap();
        insertIntoMetaToken(idfMap);*/
        /**
         * *
         * SEARCH Block
         */
        //Search Query - change to appropriate literal when required
        String query = "artificial intelligence";
        //Perform the search on the Inverted Index
        List<DocSearchResult> result = searchQuery(query);

        //Print Results
        if (result.isEmpty()) {
            System.out.println("Couldn't find a match in the corpus, try again.");
        } else {
            System.out.println("Search Complete. Found " + result.size() + " documents.");
            int rankCounter = 1;
            for (DocSearchResult dsr : result) {
                System.out.println("Result Number " + rankCounter);
                System.out.println("DocID - " + dsr.getDocID() + " | TF-IDF Value - " + dsr.getTfIdf() + " | Weight - " + dsr.getWeight());
                System.out.println("------------------");
                rankCounter++;
            }
        }
    }

    /**
     * *****
     * *******************************************************
     * ----------------SEARCH BLOCK---------------------------
     * *******************************************************
     */
    /**
     * Search Query function - performs the search for the given query on the
     * Inverted Index in DB
     *
     * @param query
     * @return
     */
    private static List<DocSearchResult> searchQuery(String query) {
        //tokenize given query
        List<String> queryWords = tokenizeQuery(query);
        if (queryWords.isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        //separate logic if the query consists of a single word or multiple
        if (queryWords.size() == 1) {
            return searchForSingleWord(queryWords.get(0));
        }
        return searchForMultipleWords(queryWords);
    }

    /**
     * Performs search if the query consists of/boils down to a single word
     *
     * @param word
     * @return
     */
    private static List<DocSearchResult> searchForSingleWord(String word) {
        //Query DB
        List<Token> tknList = getTokensOrdered(word);
        if (tknList.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        //Change the Token JPA Entity results to the Output DTO List
        List<DocSearchResult> resultList = new ArrayList<>();
        for (Token t : tknList) {
            resultList.add(new DocSearchResult(t.getTokenPK().getDocumentID(), t.getTfIdf(), t.getWeight()));
        }
        return resultList;
    }

    /**
     * Performs search if the query consists of/boils down to multiple words
     *
     * @param words
     * @return
     */
    private static List<DocSearchResult> searchForMultipleWords(List<String> words) {
        //Final Set that will ultimately store the intersection of all the Doc IDs for all the words in the query
        Set<String> docIDSetForAllWords = new HashSet<>();
        //Map that stores the Token JPA Entity instances that correspond to one Doc ID
        Map<String, List<Token>> docIDTokenMap = new HashMap<>();

        //iterate over the words list
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            //Query the DB to retrieve the corresponding List of "Token" JPA Entity for the current word in the query
            List<Token> tknList = getTokens(word);
            //List to store all the Document IDs returned for the current word from the DB
            List<String> allDocIDsForCurrentWord = new ArrayList<>();

            for (Token t : tknList) {
                String currentDocID = t.getTokenPK().getDocumentID();
                List<Token> tkns = docIDTokenMap.getOrDefault(currentDocID, new ArrayList<>());
                tkns.add(t);
                docIDTokenMap.put(currentDocID, tkns);
                allDocIDsForCurrentWord.add(currentDocID);
            }
            if (i == 0) {
                //For the first word in the query, append the entire list of Doc IDs retrieved to the set
                docIDSetForAllWords.addAll(allDocIDsForCurrentWord);
            } else {
                //For words apart from the first word in the query, keep intersecting with the remainder set
                docIDSetForAllWords.retainAll(allDocIDsForCurrentWord);
            }
        }

        //if no intersecting results
        if (docIDSetForAllWords.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        List<String> docIDs = new ArrayList<>(docIDTokenMap.keySet());

        //remove all the non-intersecting instances of DocID->Token from the Map
        for (String docID : docIDs) {
            if (!docIDSetForAllWords.contains(docID)) {
                docIDTokenMap.remove(docID);
            }
        }

        return buildReturnList(docIDTokenMap);
    }

    /**
     * Builds the Return List for Search performed on a query with multiple
     * words. Return List is comprised of the specific Output DTO
     * "DocSearchResult"
     *
     * @param docIDTokenMap
     * @return
     */
    private static List<DocSearchResult> buildReturnList(Map<String, List<Token>> docIDTokenMap) {
        //Map to store the DocID -> Output DTO relations
        Map<String, DocSearchResult> docIDSearchResultMap = new HashMap<>();

        for (String docID : docIDTokenMap.keySet()) {
            //Get the List of Token Entities for the current DocID from the input Map
            List<Token> correspondingTokens = docIDTokenMap.get(docID);

            for (Token t : correspondingTokens) {
                double currentTfIdfVal = t.getTfIdf();
                //Final weight for the doc is calculated as an average of the total weights
                double currentWeight = (double) t.getWeight() / correspondingTokens.size();

                //if the current DocID already exists in the DocSearchResultMap, update the values
                if (docIDSearchResultMap.containsKey(docID)) {
                    DocSearchResult existingDSR = docIDSearchResultMap.get(docID);
                    //add the tfIDF Values
                    double resultTfIDfVal = existingDSR.getTfIdf() + currentTfIdfVal;
                    //add the weight value
                    double resultWeight = existingDSR.getWeight() + currentWeight;
                    //set values
                    existingDSR.setTfIdf(resultTfIDfVal);
                    existingDSR.setWeight(resultWeight);
                    //update in the map
                    docIDSearchResultMap.put(docID, existingDSR);
                } else {
                    //create a new entry in the map
                    docIDSearchResultMap.put(docID, new DocSearchResult(docID, currentTfIdfVal, currentWeight));
                }
            }
        }

        //create the result list of Output DTOs based on the values in the Map
        List<DocSearchResult> resultList = new ArrayList<>(docIDSearchResultMap.values());

        //Sort the result list -- first on Weight, descending; then on TF-IDF value, descending.
        resultList.sort(Comparator.comparingDouble(DocSearchResult::getWeight)
                .reversed()
                .thenComparing(Comparator.comparingDouble(DocSearchResult::getTfIdf)
                        .reversed()));

        return resultList;
    }

    /**
     * Utility function - Tokenizes the Input query to eliminate stop words,
     * whitespaces, numbers, any alphanumeric characters
     *
     * @param query
     * @return
     */
    private static List<String> tokenizeQuery(String query) {
        List<String> wordsInQuery = new ArrayList<>();
        //Split query words on the non-alphanumeric characters regex
        for (String word : query.split("[\\W_]+")) {
            if (isValidWord(word)) {
                wordsInQuery.add(word.toLowerCase());
            }
        }
        return wordsInQuery;
    }

    /**
     * *****
     * *******************************************************
     * ----------------------INDEXING BLOCK-------------------
     * *******************************************************
     */
    /**
     * Creates the Inverted Index by Parsing HTML files from the Folder Given
     *
     * @param folderNumber
     * @throws IOException
     */
    private static void createIndex(int folderNumber) throws IOException {

        int fileCount = 0;
        int countMapSize = 0;
        int weightMapSize = 0;
        int invalidFileCount = 0;

        Set<Token> tokenSet = new HashSet<>();
        //build the file path from the Parent folder and folder number
        String filePath = PARENT_FOLDER + folderNumber + "/";
        File[] files = new File(filePath).listFiles();
        for (File document : files) {
            fileCount++;
            //DocID as specified by requirements
            String docId = document.getAbsolutePath().replace(PARENT_FOLDER, "").replace("\\", "/");
            System.out.println("PROGRESS : Current Doc ID - " + docId);
            //Map for keeping track of the count of a Token
            Map<String, Integer> tokenCountMap = new HashMap<>();
            //Map for keeping track of the weight of a Token
            Map<String, List<Integer>> weightMap = new HashMap<>();

            try {
                //Check for invalid HTML files
                if (isInvalidHTML(FileUtils.readFileToString(document))) {
                    System.out.println("INVALID : HTML file - " + docId);
                    invalidFileCount++;
                    continue;
                }
            } catch (Exception e) {
                //File couldn't be parsed, invalid HTML
                invalidFileCount++;
                System.out.println("INVALID : Exception occured for DocID - " + docId);
                System.out.println("INVALID : Exception is - " + e.toString());
                continue;
            }

            //Parse the valid HTML file
            String parsedString = parseHTML(document, weightMap).trim();

            if (!parsedString.isEmpty()) {
                //Split the parsed String on Whitespace
                String[] parsedStringArray = parsedString.split("\\s+");
                //total number of terms in the document
                int docWordCount = parsedStringArray.length;

                //update token count in the map
                for (String token : parsedStringArray) {
                    int tokenCount = tokenCountMap.getOrDefault(token, 0);
                    tokenCount++;
                    tokenCountMap.put(token, tokenCount);
                }

                //calculate termFrequency and weight
                for (String tokenName : tokenCountMap.keySet()) {
                    double tFreq = ((double) tokenCountMap.get(tokenName)) / docWordCount;

                    int weight = 0;
                    weight = sumElementsInList(weightMap.get(tokenName));

                    //create a new Token Instance
                    Token tkn = new Token(tokenName, docId);
                    tkn.setTfrequency(tFreq);
                    tkn.setWeight(weight);
                    //Add in the Token Set
                    tokenSet.add(tkn);
                }
                //set the counters
                countMapSize += tokenCountMap.size();
                weightMapSize += weightMap.size();
            } else {
                //file not valid, increment appropriate counters
                invalidFileCount++;
                System.out.println("INVALID : No tokens in file - " + docId);
            }
        }
        //print final stats after building the index.
        System.out.println("FINAL STATS : " + fileCount + " files processed. | " + "Invalid Files - " + invalidFileCount + " | Valid Files - " + (fileCount - invalidFileCount));
        System.out.println("FINAL STATS : Total unique tokens in tokenSet - " + tokenSet.size());
        System.out.println("FINAL STATS : Total tokens in all count maps - " + countMapSize);
        System.out.println("FINAL STATS : Total tokens in all weight maps - " + weightMapSize);

        //Dump the index into DB
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

    /**
     * Utility Function - Parses the HTML file and retains relevant info while
     * discarding the rest. Uses JSoup to parse
     *
     * @param htmlFile
     * @param weightMap
     * @return
     * @throws IOException
     */
    private static String parseHTML(File htmlFile, Map<String, List<Integer>> weightMap) throws IOException {
        StringBuilder parsedStringBuilder = new StringBuilder();

        if (htmlFile != null) {
            //Parse the file into a JSoup Document
            Document doc = Jsoup.parse(htmlFile, "utf-8");
            //Remove listed tags from the document
            doc.select("a, script, style, .hidden, option").remove();

            String titleText = doc.getElementsByTag("title").text().toLowerCase();
            doc.select("title").remove();
            if (!titleText.isEmpty()) {
                //add the title text by setting its appropriate weight to the parsed string builder
                parsedStringBuilder.append(buildParsedString(titleText, WEIGHT_TITLE, weightMap));
            }

            String headingText = doc.select("h1, h2, h3, h4, h5, h6").text().toLowerCase();
            doc.select("h1, h2, h3, h4, h5, h6").remove();
            if (!headingText.isEmpty()) {
                //add the heading text by setting its appropriate weight to the parsed string builder
                parsedStringBuilder.append(buildParsedString(headingText, WEIGHT_HEADING, weightMap));
            }

            String bodyText = doc.select("body").text().toLowerCase();
            if (!bodyText.isEmpty()) {
                //add the body text by setting its appropriate weight to the parsed string builder
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

    /**
     * Builds the parsed String for HTML files by setting weights for tokens and
     * checking for validity of the words being parsed
     *
     * @param inputText
     * @param weight
     * @param weightMap
     * @return
     */
    private static String buildParsedString(String inputText, int weight, Map<String, List<Integer>> weightMap) {
        StringBuilder sb = new StringBuilder();
        //Split on non-alphanumeric characters
        for (String word : inputText.split("[\\W_]+")) {
            if (isValidWord(word)) {
                List<Integer> weightStrings = weightMap.getOrDefault(word, new ArrayList<>());
                if (!weightStrings.contains(weight)) {
                    //check for weights and update accordingly
                    weightStrings.add(weight);
                    weightMap.put(word, weightStrings);
                    sb.append(word).append(" ");
                }
            }
        }
        return sb.toString();
    }

    /**
     * Checks if a word is valid. Any word that contains numbers, or is less
     * than 3 characters or greater than 45 is discarded. Also removes any word
     * that is in the STOP_WORDS list
     *
     * @param word
     * @return
     */
    private static boolean isValidWord(String word) {
        return !(STOP_WORDS.contains(word)
                || word.length() < 3
                || word.length() > 45
                || word.matches(".*\\d+.*"));
    }

    /**
     * Checks the HTML validity of a File after it has been converted to String.
     * A valid HTML file is assumed to have at least one of the three tags -
     * HTML, HEAD, BODY
     *
     * @param fileString
     * @return
     */
    private static boolean isInvalidHTML(String fileString) {
        Document doc = Jsoup.parse(fileString, "", Parser.xmlParser());
        return doc.select("html").isEmpty()
                && doc.select("head").isEmpty()
                && doc.select("body").isEmpty();

    }

    /**
     * DB Utility Function Used to populate IDF related data into the helper
     * table Meta Token
     *
     * @param idfMap
     */
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

    /**
     * Gets an aggregated Token Count Map from the DB
     *
     * @return
     */
    private static Map<String, Integer> getTokenDocCountMap() {
        EntityManager em = emFactory.createEntityManager();
        Map<String, Integer> docCountMap = new HashMap<>();

        List<Object[]> results = em.createQuery(TOKEN_AGGREGATED).getResultList();
        for (Object[] result : results) {
            String tokenName = (String) result[1];
            int count = ((Number) result[0]).intValue();
            docCountMap.put(tokenName, count);
        }

        System.out.println(docCountMap.size());
        return docCountMap;
    }

    /**
     * Sums the integers in a given list
     *
     * @param inputList
     * @return
     */
    private static int sumElementsInList(List<Integer> inputList) {
        int sum = 0;
        for (int i : inputList) {
            sum += i;
        }
        return sum;
    }

    /**
     * Gets the corresponding Token records for a given Token Name
     *
     * @param tokenName
     * @return
     */
    private static List<Token> getTokens(String tokenName) {
        EntityManager em = emFactory.createEntityManager();

        TypedQuery<Token> query = em.createNamedQuery("Token.findByTokenName", Token.class);
        query.setParameter("tokenName", tokenName);

        em.getTransaction().begin();
        em.getTransaction().commit();
        List<Token> resultList = query.getResultList();
        em.close();

        return resultList;
    }

    /**
     * Gets the corresponding Token records for a given Token Name Ordered based
     * on specified criteria
     *
     * @param tokenName
     * @return
     */
    private static List<Token> getTokensOrdered(String tokenName) {
        EntityManager em = emFactory.createEntityManager();

        TypedQuery<Token> query = em.createQuery(TOKEN_ORDERED, Token.class);
        query.setParameter("tokenName", tokenName);

        em.getTransaction().begin();
        em.getTransaction().commit();
        List<Token> resultList = query.getResultList();
        em.close();

        return resultList;
    }
}
