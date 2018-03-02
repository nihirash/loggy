(defproject loggy "0.1.0-SNAPSHOT"
  :description "Minimalist blog engine"
  :dependencies [
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.562"]
                 [org.clojure/data.xml       "0.0.8"]
                 [ring/ring "1.6.3"]
                 [org.immutant/web "2.1.10"]
                 [javax.servlet/servlet-api "2.5"]
                 [compojure "1.6.0"]
                 [rum "0.10.0"]
                 [endophile "0.2.1"]]
  :main loggy.server
  :profiles {:uberjar
             {:aot [loggy.server]
              :uberjar-name "blog.jar"}}) 
