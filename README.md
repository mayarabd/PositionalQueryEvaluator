# PositionalQueryEvaluator

This program simulates a simple search engine. 
It evaluates free-text and proximity operators [eg.: priximiy(termOne, termTwo)] queries. It uses tf.idf scoring function to generate a ranked result list of documents for each query.


## Dependencies
* Stemmer library:[kstem-3.4.jar](https://sourceforge.net/projects/lemur/files/lemur/KrovetzStemmer-3.4/kstem-3.4.jar/download)

## Build

```
compile:  javac -cp  absolute_path_to_kstem-3.4.jar evaluator/*.java
run:     java -cp absolute_path_to_kstem-3.4.jar:. evaluator.QueryEvaluator documents.txt queries.xml - for formatted input and pseudo-feedback
run:    java -cp  /Users/mayara/Downloads/kstem-3.4.jar:. evaluator.Kappa - for kappa statistics 
```

## Output

##  Technologies
* Java
* IntelliJ IDEA 




