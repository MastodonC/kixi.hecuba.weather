(ns kixi.hecuba.weather.metoffice-api 
  (:require [clojure.string :as str]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.periodic :as tp]
            [clojure.java.io :as io]
            [clj-http.client :as client]
            [clojure.data.csv :as csv]
            [clojure.set :as set]))

(defn format-key [str-key]
  (when (string? str-key)
    (-> str-key
        clojure.string/lower-case
        (clojure.string/replace #" " "-")
        keyword)))

(defn pull-data
  "Get Metoffice data as a string or a csv file"
  ([querydate querytime]
   (->> (client/post "http://datagovuk.cloudapp.net/query"
                     {:form-params {:Type "Observation"
                                    :PredictionSiteID "ALL"
                                    :ObservationSiteID "ALL"
                                    :Date querydate ;; dd/mm/yyyy
                                    :PredictionTime querytime ;; 0000
                                    }
                      :follow-redirects true})
        :body
        (re-find #"https://datagovuk.blob.core.windows.net/csv/[a-z0-9]+.csv")
        slurp))
  ([querydate querytime file-path]
   (spit (str file-path (str/replace querydate #"/" "-") "-" querytime ".csv")
         (pull-data querydate querytime))))

(defn process-data-str
  "Return the data as a sequence of maps."
  [data-str]
  (let [data (csv/read-csv data-str)
        headers (map format-key (first data))
        body (into [] (rest data))]
    (map #(zipmap headers %) body)))

(defn keep-temp-date-time [data-seq]
  (map #(set/rename-keys (select-keys % [:screen-temperature :site-code
                                         :observation-date :observation-time])
                         {:screen-temperature :temperature
                          :observation-date :date
                          :observation-time :time})
       data-seq))

(defn get-daily-temp
  [date-str]
  (let [fmt (f/formatter "dd/MM/YYYY")
        timefmt (f/formatter "HH00")
        date (f/parse fmt date-str)]
    (->> (take 24 (tp/periodic-seq date (t/hours 1)))
         (pmap #(-> (pull-data (f/unparse fmt %) (f/unparse timefmt %))
                    process-data-str
                    keep-temp-date-time))
         (reduce concat)
         (group-by :site-code))))

(comment (get-daily-temp "31/08/2015")
         (get-daily-temp ;; Yesterday's weather data
          (f/unparse (f/formatter "dd/MM/YYYY") (t/yesterday))))

(defn save-file [file-name file-content] 
  (spit file-name file-content))

(defn run-data-pull
  [startdate-str enddate-str file-path] 
  (let [fmt (f/formatter "dd/MM/YYYY")
        timefmt (f/formatter "HH00")
        start (f/parse fmt startdate-str)
        end (f/parse fmt enddate-str)]
    (->> (tp/periodic-seq start (t/hours 1))
         (take-while #(t/before? % (t/minus end (t/days 1))))
         (map #(pull-data (f/unparse fmt %) (f/unparse timefmt %) file-path)))))
