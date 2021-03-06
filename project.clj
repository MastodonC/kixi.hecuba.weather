(defproject kixi.hecuba.weather "0.2.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure       "1.7.0"]
                 [org.clojure/data.csv      "0.1.2"]
                 [clj-http                  "2.0.0"]
                 [clj-time                  "0.10.0"]
                 [org.clojure/data.json     "0.2.6"]
                 [org.clojure/tools.cli     "0.3.3"]
                 [org.clojure/tools.logging "0.3.1"]]
  :plugins [[lein-cljfmt     "0.1.11"]
            [jonase/eastwood "0.2.1"]
            [lein-kibit      "0.1.2"]]
  :main kixi.hecuba.weather.core)
