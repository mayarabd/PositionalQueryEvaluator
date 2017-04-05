package evaluator;

/**
 * Created by mayara on 2/19/17.
 */
public class ProximityQuery {
    private int termProximity;
    private String termOne;
    private String termTwo;

    ProximityQuery() {

    }

    ProximityQuery(int prox, String wordOne, String wordTwo) {
        this.termProximity = prox;
        this.termOne = wordOne;
        this.termTwo = wordTwo;
    }

    public void setTermProximity(int termProximity) {
        this.termProximity = termProximity;
    }

    public void setTermOne(String termOne) {
        this.termOne = termOne;
    }

    public void setTermTwo(String termTwo) {
        this.termTwo = termTwo;
    }

    public int getTermProximity() {
        return termProximity;
    }

    public String getTermOne() {
        return termOne;
    }

    public String getTermTwo() {
        return termTwo;
    }
}
