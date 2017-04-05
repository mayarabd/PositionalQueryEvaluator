package evaluator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mayara on 2/16/17.
 */
public class Document {
    private String term;
    private Integer docId;
    private Integer termFrequency;
    private List<Integer> termPositionList;

    Document (String token, int docId) {
        this.term = token;
        this.docId = docId;
    }

    Document(String token, int docId, int termFrequency, int termPosition) {
        this(token, docId);
        this.termPositionList = new ArrayList<>();
        this.termFrequency = termFrequency;
        this.termPositionList.add(termPosition);
    }

    public Integer getDocId() {
        return docId;
    }

    public Integer getTermFrequency() {
        return termFrequency;
    }

    public void setTermFrequency(Integer termFrequency) {
        this.termFrequency = termFrequency;
    }

    public List<Integer> getTermPositionList() {
        return termPositionList;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }
}
