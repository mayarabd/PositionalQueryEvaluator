package evaluator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by mayara on 2/15/17.
 */
public class QueryEvaluator extends PositionalInvertedIndex {

    //private List<WeightedDocument> rankedResults;

    public QueryEvaluator(String indexFileName) {
        super(indexFileName);
    }


    /**
     * Reads a query string and returns a collection of proximity and/or regular query tokens
     *
     * @param query a string with the query to be evaluated
     * @return a collection of proximity and/or regular query tokens
     * contained in the query string
     */
    private QueryCollection getQueryCollection(String query) {

        List<ProximityQuery> proximityQueries = new ArrayList<>();
        List<String> regularQueries;

        List<String> tokens;

        while (true) {
            if (query.contains("(")) {

                //get proximity query
                String proxQuery = query.substring(query.indexOf("(") - 1, query.indexOf(")") + 1);

                //Pre-process proximity query
                //tokenize proximity query
                tokens = stringTokenizer(proxQuery);

                //normalize tokens to lower case
                tokens = stringNormalizer(tokens);

                //stem tokens
                tokens = stringStemmer(tokens);

                //removes stopwords from tokens
                tokens = stopwordsRemoval(tokens);

                //creates proximity query object
                ProximityQuery proximityQuery = new ProximityQuery();
                try {
                    proximityQuery.setTermProximity(Integer.valueOf(tokens.get(0)));
                    proximityQuery.setTermOne(tokens.get(1));
                    proximityQuery.setTermTwo(tokens.get(2));
                    proximityQueries.add(proximityQuery);

                } catch (NumberFormatException nfe) {
                    System.out.println(nfe.getMessage());
                }

                //regular query
                query = query.replace(proxQuery, " ");

            } else {
                //Pre-process regular query
                //tokenize query
                regularQueries = stringTokenizer(query);

                //normalize tokens to lower case
                regularQueries = stringNormalizer(regularQueries);

                //stem tokens
                regularQueries = stringStemmer(regularQueries);

                //removes stopwords from tokens
                regularQueries = stopwordsRemoval(regularQueries);

                break;
            }

        }

        QueryCollection queryTokensCollection = new QueryCollection(proximityQueries, regularQueries);

        return queryTokensCollection;

    }


    /**
     * Intersect two lists of Documents
     *
     * @param postOne postOne posting list for termOne
     * @param postTwo posting list for termOne
     * @return a list with the intersection
     */
    private List<Document> intersect(List<Document> postOne, List<Document> postTwo) {
        List<Document> result = new ArrayList<>();

        int indexOne = 0;
        int indexTwo = 0;

        while (indexOne < postOne.size() && indexOne < postTwo.size() && indexTwo < postTwo.size()) {
            //checks if the docId in list one is also in list two
            int docIdOne = postOne.get(indexOne).getDocId();
            int docIdTwo = postTwo.get(indexTwo).getDocId();
            if (docIdOne == docIdTwo) {
                //adds doc one and doc two to the list
                result.add(postOne.get(indexOne));
                result.add(postTwo.get(indexTwo));
                indexOne++;
                indexTwo++;
            } else if (docIdOne < docIdTwo) {
                indexOne++;
            } else {
                indexTwo++;
            }
        }
        return result;
    }

    /**
     * Check proximity between two terms
     *
     * @param docList   a list of documents
     * @param proximity the distance
     * @return a list of documents that satisfy the condition
     */
    private List<Document> checkProximity(List<Document> docList, int proximity) {
        List<Document> result = new ArrayList<>();

        for (int i = 0; i < docList.size() - 1; i += 2) {
            //gets term position list for each term
            List<Integer> docPositionOne = docList.get(i).getTermPositionList();
            Set<Integer> docPositionTwo = new HashSet<>(docList.get(i + 1).getTermPositionList());

            match:
            for (int position : docPositionOne) {
                int value = 0;
                int j = 1;

                //checks if terms are in the correct proximity
                while (value <= position + proximity) {
                    value = position + j;
                    j++;

                    //found a match
                    if (docPositionTwo.contains(value)) {
                        result.add(docList.get(i));
                        result.add(docList.get(i + 1));
                        break match;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Weighs terms for each document. TF.IDF
     *
     * @param term a post for a query term
     * @return weighted post
     */
    private WeightedPost weighTerm(Document term) {
        //gets document frequency of the term
        double docFreqTerm = (double) positionalIndex.get(term.getTerm()).size();

        //gets term frequency
        double termFreq = (double) term.getTermFrequency();

        //TF: (1 + Math.log10(termFreq))
        //IDF: Math.log10(getCollectionSize() / docFreqTerm)
        double termWeigh = (1 + Math.log10(termFreq)) * Math.log10(getCollectionSize() / docFreqTerm);

        //returns the weighted term
        return new WeightedPost(term.getTerm(), term.getDocId(), termWeigh);

    }

    /**
     * ranks a list of documents based on the weight of its query terms
     *
     * @param weightedTermList a list of weighted terms
     */
    private List<WeightedDocument> rankDocuments(Map<Integer, List<WeightedPost>> weightedTermList) {
        List<WeightedDocument> rankedDocuments = new ArrayList<>();
        double docWeight = 0;
        for (Map.Entry<Integer, List<WeightedPost>> entry : weightedTermList.entrySet()) {
            Integer docId = entry.getKey();
            for (WeightedPost term : entry.getValue()) {
                docWeight += term.getWeight();
            }
            WeightedDocument doc = new WeightedDocument(docWeight, docId);
            rankedDocuments.add(doc);
            docWeight = 0;
        }

        //sort results
        Collections.sort(rankedDocuments, (docOne, docTwo) -> Double
                .compare(docTwo.getWeight(), docOne.getWeight()));

        return rankedDocuments;
    }

    /**
     * Evaluates proximity queries of type n(term1 term2)
     * where n is the proximity between terms
     *
     * @param proxQueryList a list of proximity query objects
     * @return a Map of <DocId, Weighted terms> that has met the search query
     */
    private Map<Integer, List<WeightedPost>> evaluateProximityQuery(List<ProximityQuery> proxQueryList) {
        List<Document> result = null;
        List<List<Document>> resultSet = new ArrayList<>();
        Map<Integer, List<WeightedPost>> resultMap = new HashMap<>();

        //get posting lists
        List<Document> docOneList = null;
        List<Document> docTwoList = null;

        for (int i = 0; i < proxQueryList.size(); i++) {
            if (positionalIndex.containsKey(proxQueryList.get(i).getTermOne())) {
                docOneList = positionalIndex.get(proxQueryList.get(i).getTermOne());
            }

            if (positionalIndex.containsKey(proxQueryList.get(i).getTermTwo())) {
                docTwoList = positionalIndex.get(proxQueryList.get(i).getTermTwo());
            }

            //intersect
            if (docOneList != null && docTwoList != null) {
                result = intersect(docOneList, docTwoList);
            }

            //check proximity
            result = checkProximity(result, proxQueryList.get(i).getTermProximity());

            resultSet.add(result);

            if (resultSet.size() == 2) {
                //intersect
                if (resultSet.get(0) != null && resultSet.get(1) != null) {
                    result = intersect(resultSet.get(0), resultSet.get(1));
                    resultSet.clear();
                    if (result.size() > 0) {
                        resultSet.add(result);
                    }
                }
            }
        }

        // weigh terms
        for (int i = 0; i < resultSet.size(); i++) {
            List<Document> docList = resultSet.get(i);
            for (int j = 0; j < docList.size(); j++) {
                Document document = docList.get(j);
                //weigh term and adds to map
                //checks if the term is already in the map
                if (!resultMap.containsKey(document.getDocId())) {
                    //creates a new list of weighted posts associated to that docId
                    List<WeightedPost> weightedPostList = new ArrayList<>();
                    //weighs term and adds to list
                    weightedPostList.add(weighTerm(document));
                    resultMap.put(document.getDocId(), weightedPostList);
                } else {
                    //adds weighted term the list associated to this docId
                    List<WeightedPost> weightedPostList = resultMap.get(document.getDocId());
                    weightedPostList.add(weighTerm(document));
                }
            }
        }

        return resultMap;

    }

    /**
     * Evaluates query of type term1 term2 ...
     *
     * @return a map of <DocId, List weighted terms>
     */
    private Map<Integer, List<WeightedPost>> evaluateRegularQuery(List<String> regQueryList) {
        Map<Integer, List<WeightedPost>> result = new HashMap<>();

        //get terms posting list
        for (String term : regQueryList) {
            if (positionalIndex.get(term) == null) {
                continue;
            }

            for (Document document : positionalIndex.get(term)) {
                //weigh term and adds to map
                //checks if the term is already in the map
                if (!result.containsKey(document.getDocId())) {
                    //creates a new list of weighted posts associated to that docId
                    List<WeightedPost> weightedPostList = new ArrayList<>();
                    //weighs term and adds to list
                    weightedPostList.add(weighTerm(document));
                    result.put(document.getDocId(), weightedPostList);
                } else {
                    //adds weighted term the list associated to this docId
                    List<WeightedPost> weightedPostList = result.get(document.getDocId());
                    weightedPostList.add(weighTerm(document));
                }
            }
        }

        return result;
    }

    /**
     * Saves the query results to a file
     *
     * @param result a list with documents that contain the query terms
     */
    private void saveQueryResultToFile(List<WeightedDocument> result, String query, String fileName) {
        try (FileWriter writer = new FileWriter(fileName, true)) {
            writer.write("Query: " + query + "\n");
            //the query returned a document set
            if (result != null && result.size() > 0) {
                writer.write("rank, document, and relevance score:\n");
                int counter = 1;
                for (WeightedDocument document : result) {
                    writer.write(counter + ". \tDoc id:" + document.getDocId() + "\tscore: " + document.getWeight() + "\n");
                    counter++;
                }
                writer.write("\n");
            } else {
                writer.write("No documents match your search query.\n");
            }

        } catch (IOException ioe) {
            System.out.println(Arrays.toString(ioe.getStackTrace()));
        }

    }

    /**
     * Saves the results from evaluating the expanded query.
     * Format: [QryID] 0 [DocID] [Rank] [Score] tfidf
     *
     * @param rankedResults the ranked documents
     * @param queryId       the id of query evaluated
     * @param fileName      the name of the output file
     */
    private void saveExpandedQueryResultToFile(List<WeightedDocument> rankedResults, String queryId, String fileName) {
        try (FileWriter writer = new FileWriter(fileName, true)) {
            if (rankedResults != null && rankedResults.size() > 0) {
                int rank = 1;
                for (WeightedDocument document : rankedResults) {
                    writer.write(queryId + " " + "0" + " " + document.getDocId() + " " + rank + " " + document.getWeight()
                            + " " + "tfidf" + "\n");
                    rank++;

                }
            }
        } catch (IOException ioe) {
            System.out.println(Arrays.toString(ioe.getStackTrace()));
        }
    }

    /**
     * Takes a list of weighted terms and returns the subset
     * requested
     *
     * @param numOfTerms the number of terms to be returned from a list
     * @return result a subset of a list
     */
    private List<WeightedPost> getTerms(int numOfTerms, List<WeightedPost> weightedPostList) {
        List<WeightedPost> result = new ArrayList<>();
        for (int i = 0; i < numOfTerms; i++) {
            result.add(weightedPostList.get(i));

        }
        return result;

    }

    /**
     * Expands a given query with additional numOfTerms terms
     *
     * @param query       a string query to be expanded
     * @param listOfTerms a list of terms to be used for query expansion
     * @param numOfTerms  the number of terms to be added to query
     * @return a new expanded query
     */
    private String expandQuery(String query, List<WeightedPost> listOfTerms, int numOfTerms) {

        List<WeightedPost> resultOne = getTerms(numOfTerms, listOfTerms);
        String newQuery = query;

        for (int i = 0; i < numOfTerms; i++) {
            newQuery += " " + resultOne.get(i).getTerm();
        }


        return newQuery;
    }

    /**
     * Pseudo-Relevance feedback, query expansion
     *
     * @param topResult  a weighted document
     * @param query      a query to be expanded
     * @param numOfTerms the number of terms to be added to original query
     * @return a query with additional numOfTerms terms
     */
    private String pseudoRelevanceFeedback(WeightedDocument topResult, String query, int numOfTerms) {
        //id of top result
        int docId = topResult.getDocId();

        //get unique tokens from docId
        Set<String> uniqueTokens = new HashSet<>();
        uniqueTokens.addAll(getDocumentTokens(docId));

        //add unique tokens to a list
        List<String> tokenList = new ArrayList<>();
        tokenList.addAll(uniqueTokens);

        //pre-process tokens
        //normalize tokens to lower case
        tokenList = stringNormalizer(tokenList);

        //stem tokens
        tokenList = stringStemmer(tokenList);

        //removes stopwords from tokens
        tokenList = stopwordsRemoval(tokenList);

        // calculates tf.idf for each term
        List<WeightedPost> weightedTermList = new ArrayList<>();
        Map<Integer, List<WeightedPost>> docPostList = new TreeMap<>();
        for (String token : tokenList) {
            //gets list of terms
            List<Document> termList = positionalIndex.get(token);
            for (Document term : termList) {
                if (term.getDocId() == docId) {
                    //weights term and saves to a map
                    WeightedPost weightedTerm = weighTerm(term);
                    weightedTermList.add(weightedTerm);
                    docPostList.put(docId, weightedTermList);
                }
            }
        }

        //rank terms
        Collections.sort(docPostList.get(docId), (docOne, docTwo) -> Double
                .compare(docTwo.getWeight(), docOne.getWeight()));

        //save result
        //saveRankedTermsToFile(docPostList.get(docId), query, "RankedDoc.txt");

        //expand query
        String newQuery = expandQuery(query, docPostList.get(docId), numOfTerms);

        return newQuery;

    }

    /**
     * Saves the result from evaluating a query
     *
     * @param result a list with documents that contain both query terms
     */
    private void saveRankedTermsToFile(List<WeightedPost> result, String query, String fileName) {
        try (FileWriter writer = new FileWriter(fileName, true)) {
            writer.write("Query: " + query + "\n");
            //the query returned a document set
            if (result != null && result.size() > 0) {
                writer.write("Document: " + result.get(0).getDocId() + "\n");
                writer.write("rank, term, and relevance score:\n");
                int counter = 1;
                for (WeightedPost term : result) {
                    writer.write(counter + ". \tTerm: " + term.getTerm() + "\tscore: " + term.getWeight() + "\n");
                    counter++;
                }
                writer.write("\n");
            } else {
                writer.write("No documents match your search query.\n");
            }

        } catch (IOException ioe) {
            System.out.println(Arrays.toString(ioe.getStackTrace()));
        }

    }

    /*
     * Evaluate a free text query, ranks the results
     * and saves to file
     *
     * @param query a string containing bag of words query
     *              and/or proximity query
     * @return a list of ranked documents
     */
    private List<WeightedDocument> evaluateQuery(String query) {
        Map<Integer, List<WeightedPost>> proxQueryResult = null;
        Map<Integer, List<WeightedPost>> regQueryResult = null;

        //pre-process query
        //split query into proximity query tokens or regular tokens
        QueryCollection queryCollection = getQueryCollection(query);

        //evaluate proximity query
        proxQueryResult = evaluateProximityQuery(queryCollection.getProximityQueryList());

        //evaluate regular query
        regQueryResult = evaluateRegularQuery(queryCollection.getRegularQueryList());

        //merge regular and proximity weighted terms
        for (Map.Entry<Integer, List<WeightedPost>> entry : proxQueryResult.entrySet()) {
            Integer docId = entry.getKey();
            //add docId - weighted posts to map
            if (!regQueryResult.containsKey(docId)) {
                regQueryResult.put(docId, proxQueryResult.get(docId));

            } else {
                //adds weighted term the list associated to this docId
                List<WeightedPost> weightedPostList = regQueryResult.get(docId);
                weightedPostList.addAll(proxQueryResult.get(docId));
            }


        }

        //rank document
        List<WeightedDocument> rankedResults = rankDocuments(regQueryResult);

        //save result
        //saveQueryResultToFile(rankedResults, query, "QueryResult.txt");

        return rankedResults;

    }

//    /**
//     * run query evaluator with user input
//     */
//    public void run() {
//        boolean exit = false;
//        while (!exit) {
//            System.out.println("Enter query: ");
//            Scanner input = new Scanner(System.in);
//
//            String query = input.nextLine();
//
//            //ranked query results
//            List<WeightedDocument> rankedResults = evaluateQuery(query);
//
//            System.out.println("Exit? : (yes or no)");
//
//            String answer = input.nextLine();
//            if (answer.equalsIgnoreCase("yes")) {
//                exit = true;
//            }
//        }
//    }

    /**
     * Parse a query file with format <query><number>1</number><text>battery</text>
     *
     * @param fileName the name of xml file
     * @return a map of <query id , query>
     */
    private Map<String, String> parse(String fileName) {

        Pattern tagRegexNumber = Pattern.compile("<number>(.+?)</number>");
        Pattern tagRegexQuery = Pattern.compile("<text>(.+?)</text>");
        String line;
        List<String> lines = new ArrayList<>();
        //saves file lines into a list
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //saves query id and query to a map
        Map<String, String> queries = new HashMap<>();
        Matcher matcher;
        Matcher matcher2;
        for (String line1 : lines) {
            matcher = tagRegexNumber.matcher(line1);
            matcher2 = tagRegexQuery.matcher(line1);
            while (matcher.find() && matcher2.find()) {
                String id = matcher.group(1);
                String query = matcher2.group(1);
                queries.put(id, query);
            }
        }

        return queries;
    }

    /**
     * run query evaluator from query xml fle
     */
    public void run(String fileName) {

        //parse file and save query and query id
        Map<String, String> queries = parse(fileName);

        queries.forEach((id, query) -> {
            //ranked query results
            List<WeightedDocument> rankedResults = evaluateQuery(query);
            String queryResult;
            //calculate pseudo-relevance feedback

            if (rankedResults != null && rankedResults.size() > 0) {
                //expanded query x = 1
                queryResult = pseudoRelevanceFeedback(rankedResults.get(0), query, 1);

                //ranked query results for expanded query
                rankedResults = evaluateQuery(queryResult);

                //save result
                saveExpandedQueryResultToFile(rankedResults, id, "ExpandedQueryResult" + 1 + ".txt");

                //expanded query x = 3
                queryResult = pseudoRelevanceFeedback(rankedResults.get(0), query, 3);

                //ranked query results for expanded query
                rankedResults = evaluateQuery(queryResult);

                //save result
                saveExpandedQueryResultToFile(rankedResults, id, "ExpandedQueryResult" + 3 + ".txt");

                //expanded query x = 5
                queryResult = pseudoRelevanceFeedback(rankedResults.get(0), query, 5);

                //ranked query results for expanded query
                rankedResults = evaluateQuery(queryResult);

                //save result
                saveExpandedQueryResultToFile(rankedResults, id, "ExpandedQueryResult" + 5 + ".txt");
            }

        });

    }


    public static void main(String[] args) {
        QueryEvaluator queryEvaluator = new QueryEvaluator(args[0]); //pass documents.txt and queries.xml
        queryEvaluator.run(args[1]);
        //queryEvaluator.run();

    }
}
