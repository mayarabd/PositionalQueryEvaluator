package evaluator;

/**
 * Created by mayara on 2/24/17.
 */
public class WeightedDocument {
    private double weight;
    private int docId;

    WeightedDocument(double weight, int docId) {
        this.weight = weight;
        this.docId = docId;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public int getDocId() {
        return docId;
    }

    public void setDocId(int docId) {
        this.docId = docId;
    }
}
