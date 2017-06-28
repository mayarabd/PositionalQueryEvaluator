package evaluator;

import java.util.List;

/**
 * Created by mayara on 2/19/17.
 */
public class QueryCollection {
    private List<ProximityQuery> proximityQueryList;
    private List<String> regularQueryList;

    QueryCollection(List<ProximityQuery> proxQueryList, List<String> regQueryList) {
        this.proximityQueryList = proxQueryList;
        this.regularQueryList = regQueryList;
    }

    public List<ProximityQuery> getProximityQueryList() {
        return proximityQueryList;
    }

    public List<String> getRegularQueryList() {
        return regularQueryList;
    }

    public int getTotalTerms() {
        return proximityQueryList.size() + regularQueryList.size();
    }
}
