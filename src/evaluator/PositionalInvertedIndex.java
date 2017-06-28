package evaluator;

import org.lemurproject.kstem.KrovetzStemmer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by mayara on 2/1/17.
 */
public class PositionalInvertedIndex {

    public PositionalInvertedIndex(String indexFileName) {
        initializeIndex(indexFileName);
    }

    protected Map<String, List<Integer>> invertedIndex = new TreeMap<>();
    protected Map<String, List<Document>> positionalIndex = new TreeMap<>();

    //map of <document id, token list>
    private Map<Integer, List<String>> documentTokens = new TreeMap<>();

    /**
     * Read a series of documents from a file
     *
     * @param fileName the name of the file
     * @return a Map with <id, docText> pair.
     * Where id is the Id of the document
     * and docText is the text body of the document
     */
    private Map<Integer, String> readFile(String fileName) {
        String line;
        StringBuilder doc = new StringBuilder();
        //maps document ID to document text
        Map<Integer, String> documents = new TreeMap<>();

        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            //reads the file line by line and save docId followed by doc text to a map
            Integer id = 0;
            StringBuilder word = new StringBuilder();
            while ((line = br.readLine()) != null) {
                if (line.startsWith("<DOC")) {
                    for (int i = 0; i < line.length(); i++) {
                        Character character = line.charAt(i);
                        if (Character.isDigit(character)) {
                            word.append(character);
                        }
                    }
                    id = Integer.valueOf(word.toString());
                    word.setLength(0);
                } else if (!line.startsWith("</DOC>")) {
                    doc.append(line);
                } else {
                    String docText = doc.toString();
                    documents.put(id, docText);
                    doc.setLength(0);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return documents;
    }

    /**
     * Splits a string into tokens
     *
     * @param st a string to be tokenized
     * @return a list of word tokens
     */
    protected List<String> stringTokenizer(String st) {
        List<String> tokens = new ArrayList<>();
        StringBuilder word = new StringBuilder();
        for (int i = 0; i < st.length(); i++) {
            Character character = st.charAt(i);
            if (Character.isLetterOrDigit(character)) {
                word.append(character);

                //If processing the last character of st string, add token to list.
                if (i + 1 == st.length()) {
                    tokens.add(word.toString());
                    word.setLength(0);
                }
            } else {
                if (word.length() > 0) {
                    tokens.add(word.toString());
                    word.setLength(0);
                }
            }
        }
        return tokens;
    }


    /**
     * This function normalizes strings into lower case tokens
     *
     * @param stringList a list of words
     * @return a list of lowercase word tokens
     */
    protected List<String> stringNormalizer(List<String> stringList) {
        return stringList.stream().map(String::toLowerCase).collect(Collectors.toList());
    }

    /**
     * This function reduces words to their stem form
     *
     * @param stringList a list of words
     * @return a list of stemmed word tokens
     */
    protected List<String> stringStemmer(List<String> stringList) {
        List<String> stemmedTokens = new ArrayList<>();
        KrovetzStemmer stemmer = new KrovetzStemmer();

        for (String token : stringList) {
            String tokenStemmed = stemmer.stem(token);
            stemmedTokens.add(tokenStemmed);
        }
        return stemmedTokens;
    }

    /**
     * This function remove specific words, stopwords,  from the words list
     *
     * @param stringList a list of words
     * @return list with a subset of stringList without the stopwords
     */
    protected List<String> stopwordsRemoval(List<String> stringList) {
        List<String> reducedTokenSet = new ArrayList<>();

        for (String token : stringList) {
            if (!token.equals("the") && !token.equals("is") && !token.equals("at") && !token.equals("of")
                    && !token.equals("on") && !token.equals("and") && !token.equals("a")) {
                reducedTokenSet.add(token);
            }
        }

        return reducedTokenSet;
    }

    /**
     * Creates an inverted Index
     *
     * @param documentTokens a map of <docId, wordList> pairs
     */
    private void createInvertedIndex(Map<Integer, List<String>> documentTokens) {
        for (Map.Entry<Integer, List<String>> entry : documentTokens.entrySet()) {
            Integer docId = entry.getKey();
            //iterates the list of word tokens from each document
            for (String token : entry.getValue()) {
                //checks if the word token is already in the map
                if (!invertedIndex.containsKey(token)) {
                    //creates a new list of docIds associated to that word token
                    List<Integer> docIdList = new ArrayList<>();
                    docIdList.add(docId);
                    invertedIndex.put(token, docIdList);
                } else {
                    //get the list associated to this word token
                    List<Integer> docIdList = invertedIndex.get(token);
                    //checks if the list associated to this word token already has this docId
                    if (!docIdList.contains(docId)) {
                        docIdList.add(docId);
                    }
                }
            }
        }
    }

    /**
     * Creates an positional Index
     *
     * @param documentTokens a map of <docId, wordList> pairs
     */
    private void createPositionalIndex(Map<Integer, List<String>> documentTokens) {
        for (Map.Entry<Integer, List<String>> entry : documentTokens.entrySet()) {
            Integer docId = entry.getKey();
            //iterates the list of word tokens from each document

            for (int i = 0; i < entry.getValue().size(); i++) {
                String token = entry.getValue().get(i);
                //remove stop words
                if (!token.equals("the") && !token.equals("is") && !token.equals("at") && !token.equals("of")
                        && !token.equals("on") && !token.equals("and") && !token.equals("a")) {
                    //checks if the word token is already in the map
                    if (!positionalIndex.containsKey(token)) {
                        //creates a new list of Documents associated to that word token
                        List<Document> docList = new ArrayList<>();
                        //creates a new document
                        Document doc = new Document(token, docId, 1, i + 1);
                        docList.add(doc);
                        positionalIndex.put(token, docList);
                    } else {
                        //get the list associated to this word token
                        List<Document> docList = positionalIndex.get(token);
                        //checks if the list associated to this word token already has this docId and updates its information
                        boolean updated = false;
                        for (Document doc : docList) {
                            if (doc.getDocId().equals(docId)) {
                                //increments the term frequency
                                doc.setTermFrequency(doc.getTermFrequency() + 1);
                                //adds term position to the Document position list
                                //token.indexOf(docId): index of the current token
                                doc.getTermPositionList().add(i + 1);
                                updated = true;
                            }
                        }

                        //adds a new doc to the list associated to this word token
                        //CHECK if it was not added before, then add here
                        if (!updated) {
                            Document newDoc = new Document(token, docId, 1, i + 1);
                            docList.add(newDoc);
                        }
                    }
                }
            }
        }
    }

    /**
     * Saves the positional index to a file
     */
    private void savePositionalIndexToFile() {
        try (PrintWriter writer = new PrintWriter("PositionalIndex.txt")) {
            writer.println("word,docFrequency:[docId termFrequency: termPosition ]");
            for (Map.Entry<String, List<Document>> entry : positionalIndex.entrySet()) {
                String key = entry.getKey();
                List<Document> postingList = entry.getValue();
                Integer docFrequency = postingList.size();

                //word and docFrequency
                writer.print(key + "," + docFrequency + ":");
                for (Document doc : postingList) {
                    //posting list
                    writer.print("[" + doc.getDocId() + " " + doc.getTermFrequency() + ":");
                    for (int position : doc.getTermPositionList()) {
                        writer.print(" " + position);
                    }
                    writer.print("]");
                }
                writer.println();
            }

        } catch (FileNotFoundException fnfe) {
            System.out.println(Arrays.toString(fnfe.getStackTrace()));
        }
    }

    private void initializeIndex(String fileName) {
        Map<Integer, String> documentSet = readFile(fileName);
        List<String> tokens;

        for (Map.Entry<Integer, String> entry : documentSet.entrySet()) {
            Integer key = entry.getKey();
            String value = entry.getValue();
            //tokenize text
            tokens = stringTokenizer(value);
            //normalize text to lower case
            tokens = stringNormalizer(tokens);
            //stem text
            tokens = stringStemmer(tokens);
            //add processed words to map
            documentTokens.put(key, tokens);
        }

        //create positionalIndex
        createPositionalIndex(documentTokens);

        //save to file
        savePositionalIndexToFile();
    }

    //return a list of terms in the document
    public List<String> getDocumentTokens(int docId) {
        return documentTokens.get(docId);
    }

    //return number of documents in the collection
    public int getCollectionSize() {
        return documentTokens.size();
    }

    public static void main(String[] args) {
        PositionalInvertedIndex index = new PositionalInvertedIndex("./documents.txt");
    }
}
