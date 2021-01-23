# Deployment

1. generate `pom.xml`: `clj -Spom`
2. update version number in `pom.xml`
3. build jar: `clojure -X:depstar jar :jar jadak.jar`
4. push to clojars: `clojure -M:deploy`

