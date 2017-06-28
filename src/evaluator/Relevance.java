package evaluator;

/**
 * Created by mayara on 4/2/17.
 */
public class Relevance {
    private int relevantCount;
    private int nonRelevantCount;


    Relevance(int relevant, int nonRelevant) {
        this.relevantCount = relevant;
        this.nonRelevantCount = nonRelevant;
    }

    public int getRelevantCount() {
        return relevantCount;
    }

    public int getNonRelevantCount() {
        return nonRelevantCount;
    }
}
