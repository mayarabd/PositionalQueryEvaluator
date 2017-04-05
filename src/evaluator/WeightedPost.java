package evaluator;

/**
 * Created by mayara on 2/24/17.
 */
public class WeightedPost extends Document {
    private double weight;

    WeightedPost(String token, int docId, double weight) {
        super(token, docId);
        this.weight = weight;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }
}
