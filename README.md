# PositionalQueryEvaluator

This program simulates a simple search engine. 
It evaluates free-text and proximity operator queries eg.: priximiy(termOne, termTwo).
It uses tf.idf scoring function to generate a ranked result list of documents for each query.

Further, it can accept formatted input text with queries to be expanded using pseudo-relevance feedback. 

Finally, it computes the kappa staistic between several pseudo-relevance feedback files.

## Dependencies
* Stemmer library:[kstem-3.4.jar](https://sourceforge.net/projects/lemur/files/lemur/KrovetzStemmer-3.4/kstem-3.4.jar/download)

## Build

```
compile:  javac -cp  absolute_path_to_kstem-3.4.jar evaluator/*.java
run:     java -cp absolute_path_to_kstem-3.4.jar:. evaluator.QueryEvaluator documents.txt queries.xml - for formatted input and pseudo-feedback
run:    java -cp  /Users/mayara/Downloads/kstem-3.4.jar:. evaluator.Kappa - for kappa statistics 
```

##  Technologies
* Java
* IntelliJ IDEA 




