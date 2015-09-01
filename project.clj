(defproject kixi.hecuba.weather "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/data.csv "0.1.2"]
                 [clj-http "2.0.0"]
                 [clj-time "0.10.0"]]
  :plugins [[lein-cljfmt "0.1.11"]
            [jonase/eastwood "0.2.1"]
            [lein-kibit "0.1.2"]])
