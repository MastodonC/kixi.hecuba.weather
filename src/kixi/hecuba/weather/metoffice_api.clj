(ns kixi.hecuba.weather.metoffice-api 
  (:require [clojure.string :as str]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.periodic :as tp]
            [clojure.java.io :as io]
            [clj-http.client :as client]
            [clojure.data.csv :as csv]
            [clojure.data.json :as json]
            [clojure.set :as set]))

(def devices-file (io/resource "live-device-sensors.csv"))

(def tformat (f/formatter "YYYY-MM-dd HH:mm"))

(def tbase 15.5)

(def url "http://www.getembed.com/4/")


(defn format-key [str-key]
  (when (string? str-key)
    (-> str-key
        clojure.string/lower-case
        (clojure.string/replace #" " "-")
        keyword)))

(defn load-csv-file [filename]
  (let [file-info (csv/read-csv (slurp filename) :quot-char \" :separator \,)
        headers (map format-key (first file-info))]
    (map #(zipmap headers %) (rest file-info))))

(def devices (load-csv-file devices-file))

(def devices-grp (group-by :property-code devices))

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

(defn get-max-and-min [daily-readings]
  (let [data (map (fn [t] (Float/parseFloat (:temperature t))) daily-readings)]
    {:max (apply max data) :min (apply min data)}))

;; The McKiver method (or British Gas method) as employed by the Met Office
;; For more info see http://www.vesma.com/ddd/ddcalcs.htm 
(defn calc-degreedays-mckiver [tbase tmin tmax] 
  (if (> tmin tbase)
    0
    (if (> (/ (+ tmax tmin) 2) tbase)
      (double (/ (- tbase tmin) 4)) 
      (if (>= tmax tbase)
        (double (- (/ (- tbase tmin) 2) (/ (- tmax tbase) 4))) 
        (if (< tmax tbase)
          (double (/ (- tbase (+ tbase tmin)) 2)) 
          -1)))))

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

(defn create-sensor-measurements [observed-data]
  (map (fn [observation]
         (let [maxmin (get-max-and-min (second observation))
               degreedays (calc-degreedays-mckiver tbase (:min maxmin) (:max maxmin))
               obs-date (:date (first (second observation)))
               measurements (mapv (fn [site-obs]
                                    {:value (:temperature site-obs)
                                     :type "Temperature"
                                     :timestamp (f/unparse (f/formatters :date-time) 
                                                           (f/parse tformat (str (:date site-obs) " " (:time site-obs))))
                                     }) (second observation))]
           [{:measurements measurements
             :entity-id (:entity-id (first (get devices-grp (first observation))))
             :device-id (:device-id (first (get devices-grp (first observation))))}
            {:measurements [{:value degreedays
                             :type "Temperature_degreeday"                               
                             :timestamp (f/unparse (f/formatters :date-time) 
                                                   (f/parse tformat (str obs-date " 00:00")))}] 
             :entity-id (:entity-id (first (get devices-grp (first observation))))
             :device-id (:device-id (first (get devices-grp (first observation))))}
            ])) 
       observed-data))

(defn push-payload-to-hecuba [json-payload entity-id device-id user pwd]
  (try (client/post 
        (str url "entities/" entity-id "/devices/" device-id "/measurements/")
        {:basic-auth [user pwd]
         :body (json/write-str {:measurements json-payload})
         :headers {"X-Api-Version" "2"}
         :content-type :json
         :socket-timeout 20000
         :conn-timeout 20000  
         :accept "application/json"})
       (catch Exception e (str "Caught Exception " (.getMessage e))))
  
  )

;; This is not how I want to do this, needs a refactor
;; doseq will have a vector (payload) with two maps in
;; send the first, then send the second. 
(defn upload-measurements [vectormap-payloads user pwd]
  (doseq [payload vectormap-payloads] 
    (push-payload-to-hecuba (:measurements (first payload)) 
                            (:entity-id (first payload))
                            (:device-id (first payload)) 
                            user 
                            pwd)
    (push-payload-to-hecuba (:measurements (second payload))
                            (:entity-id (second payload))
                            (:device-id (second payload))
                            user
                            pwd)))

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
