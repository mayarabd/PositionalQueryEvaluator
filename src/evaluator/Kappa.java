package evaluator;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by mayara on 4/2/17.
 * Calculates kappa between two relevance judgement lists.
 */
public class Kappa {

    //relevance ratings from file one and two
    private List<String> relevanceOne;
    private List<String> relevanceTwo;

    //list of files containing relevance judgement
    private File[] listOfFiles;

    private Kappa(String fileNameOne, String folderPath) {
        relevanceOne = new ArrayList<>();
        relevanceTwo = new ArrayList<>();

        readFile(fileNameOne);
        readFileList(folderPath);
    }


    /**
     * Reads a relevance judgement file in the format
     * [QryID] 0 [DocID] [Relevance] and extracts the relevance
     * values into a list
     *
     * @param fileName
     */
    private void readFile(String fileName) {
        relevanceTwo.clear();

        String line;
        List<String> relevanceList = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            while ((line = br.readLine()) != null) {
                String relevance = line.substring(6);
                relevanceList.add(relevance);
            }

            if (relevanceOne.isEmpty()) {
                relevanceOne.addAll(relevanceList);
            } else {
                relevanceTwo.addAll(relevanceList);
            }

        } catch (FileNotFoundException fnfe) {
            System.out.println(fnfe.getMessage());

        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }

    }

    /**
     * Find all equal relevance scores from two lists
     *
     * @param listOne relevance agreement from annotator one
     * @param listTwo relevance agreement from annotator two
     * @return a list of all the equal relevance from both lists
     */
    private List<String> getEqualRows(List<String> listOne, List<String> listTwo) {
        List<String> result = new ArrayList<>();

        int indexOne = 0;
        int indexTwo = 0;
        while (indexOne < listOne.size() && indexTwo < listTwo.size()) {

            String valueOne = listOne.get(indexOne);
            String valueTwo = listTwo.get(indexTwo);

            if (valueOne.equals(valueTwo)) {
                result.add(valueOne);
            }

            indexOne++;
            indexTwo++;
        }
        return result;

    }


    /**
     * Gets the total number of relevant and non relevant scores from a list
     *
     * @param relevanceList a list with relevant / non-relevant scores
     * @return an object with the counts of relevant and non relevant scores
     */
    private Relevance countRelevanceRating(List<String> relevanceList) {
        int relevant = 0;
        int nonRelevant = 0;
        for (String value : relevanceList) {
            //count number of relevant
            value = value.trim();
            if (value.equals("1")) {
                relevant++;

                //count number of non-relevant
            } else if (value.equals("0")) {
                nonRelevant++;
            }
        }

        return new Relevance(relevant, nonRelevant);
    }

    /**
     * Probability that two annotators agree on their relevance and on-relevance scores
     *
     * @param relevanceAgreement counter of all relevance and non-relevance agreements
     *                           between two annotators
     * @return the probability of agreement
     */
    private double probabilityOfAgreement(Relevance relevanceAgreement) {
        return (relevanceAgreement.getRelevantCount() + relevanceAgreement.getNonRelevantCount())
                / (double) relevanceOne.size();
    }

    /**
     * Probability that both annotators would agree in their relevance scores
     *
     * @param relevanceJudgementOne the relevance count from annotator one
     * @param relevanceJudgementTwo the relevance count from annotator two
     * @return the probability of relevance agreement
     */
    private double probRelevant(Relevance relevanceJudgementOne, Relevance relevanceJudgementTwo) {
        double relOne = relevanceJudgementOne.getRelevantCount() / (double) relevanceOne.size();
        double relTwo = relevanceJudgementTwo.getRelevantCount() / (double) relevanceTwo.size();

        return relOne * relTwo;
    }

    /**
     * Probability that both annotators would agree in their non-relevance scores
     *
     * @param relevanceJudgementOne the non-relevance count from annotator one
     * @param relevanceJudgementTwo the non-relevance count from annotator two
     * @return the probability of non-relevance agreement
     */
    private double probNonRelevant(Relevance relevanceJudgementOne, Relevance relevanceJudgementTwo) {
        double relOne = relevanceJudgementOne.getNonRelevantCount() / (double) relevanceOne.size();
        double relTwo = relevanceJudgementTwo.getNonRelevantCount() / (double) relevanceTwo.size();

        return relOne * relTwo;
    }

    /**
     * Probability two annotators agree by chance
     *
     * @param probNonRelevant the probability of non-relevance agreement
     * @param probRelevant    the probability of relevance agreement
     * @return the probability of chance agreement
     */
    private double probChanceAgreement(double probNonRelevant, double probRelevant) {
        double sqRel = probRelevant * probRelevant;
        double sqNonRel = probNonRelevant * probRelevant;

        return sqNonRel + sqRel;
    }

    /**
     * The Kappa statistic for two relevance lists
     *
     * @param probAgreement       probability of agreement between annotators
     * @param probChanceAgreement probability of chance agreement between annotators
     * @return the kappa result
     */
    private double getKappa(double probAgreement, double probChanceAgreement) {
        return (probAgreement - probChanceAgreement) / (1 - probChanceAgreement);
    }


    /**
     * Saves the kappa statistic values to file
     *
     * @param kappaResult result from calculated kappa and
     *                    fileName the name of the file
     */
    private void saveToFile(double kappaResult, String fileName) {
        try (FileWriter writer = new FileWriter("Kappa.txt", true)) {
            writer.write("File: " + String.format("%30s", fileName) + "\t:" + String.format("%10.4f", kappaResult) + "\n");

        } catch (IOException ioe) {
            System.out.println(Arrays.toString(ioe.getStackTrace()));
        }

    }


    void start() {
        List<String> result;

        for (int i = 0; i < listOfFiles.length; i++) {

            readFile(listOfFiles[i].getAbsolutePath());

            //get a list with all the agreements among two annotators
            result = getEqualRows(relevanceOne, relevanceTwo);

            //get a counter of all relevance and non-relevance agreements
            Relevance relevanceAgreement = countRelevanceRating(result);

            //get probability of agreement between two annotators
            double probAgreement = probabilityOfAgreement(relevanceAgreement);

            //relevance judgement count from annotator One
            Relevance relevanceJudgementOne = countRelevanceRating(relevanceOne);

            //relevance judgement count from annotator Two
            Relevance relevanceJudgementTwo = countRelevanceRating(relevanceTwo);

            //get probability of non-relevant
            double probNonRelevant = probNonRelevant(relevanceJudgementOne, relevanceJudgementTwo);

            //get probability of relevant
            double probRelevant = probRelevant(relevanceJudgementOne, relevanceJudgementTwo);

            //get probability of chance agreement
            double probChanceAgreement = probChanceAgreement(probNonRelevant, probRelevant);

            //calculate kappa
            double kappaResult = getKappa(probAgreement, probChanceAgreement);

            //save kappa to file
            String fileName = listOfFiles[i].getName();
            saveToFile(kappaResult, fileName);

        }

    }

    /**
     * Reads a list of .txt files
     *
     * @param folderPath a path to a folder containing .txt files
     */
    private void readFileList(String folderPath) {
        File folder = new File(folderPath);
        //saves .txt files to a list
        this.listOfFiles = folder.listFiles((dir, name) -> name.endsWith(".txt"));
    }

    public static void main(String[] args) {

        Kappa k = new Kappa("./BrandaoDusheyko-qrels.txt",
                "/Users/mayara/IdeaProjects/PositionalQueryEvaluator/relevance_docs/");
        k.start();
    }
}

