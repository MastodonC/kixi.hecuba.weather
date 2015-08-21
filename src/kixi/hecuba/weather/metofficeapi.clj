(ns kixi.hecuba.weather.metofficeapi 
  (:require [clojure.string :as str]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.periodic :as tp]
            [clojure.java.io :as io]
            [clj-http.client :as client]))

(defn pull-data [querydate querytime]
  (->> (client/post "http://datagovuk.cloudapp.net/query"
                    {:form-params {
                                   :Type "Observation"
                                   :PredictionSiteID "ALL"
                                   :ObservationSiteID "ALL"
                                   :Date querydate ;; dd/mm/yyyy
                                   :PredictionTime querytime ;; 0000
                                   }
                     :follow-redirects true})
       (:body)
       (re-find #"https://datagovuk.blob.core.windows.net/csv/[a-z0-9]+.csv")
       (slurp)))

(defn save-file [file-name file-content] 
  (spit file-name file-content))

(defn run-data-pull [startdate] 
  (let [fmt (f/formatter "dd/mm/YYYY")
        timefmt (f/formatter "HH00")
        month (f/parse fmt startdate)]
    (->> (tp/periodic-seq month (t/hours 1))
         (take-while #(t/before? % (t/minus (t/now) (t/days 1))))
         (map #(pull-data (f/unparse fmt %) (f/unparse timefmt %)))
         doall)))
