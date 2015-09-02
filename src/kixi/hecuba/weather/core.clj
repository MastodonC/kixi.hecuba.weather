(ns kixi.hecuba.weather.core
  (:require [clj-time.core           :as t]
            [clj-time.format         :as f]
            [clj-time.periodic       :as tp]
            [clojure.tools.cli       :refer [parse-opts]]
            [kixi.hecuba.weather.metoffice-api :as met])
  (:gen-class))

(def cli-options
  [["-d" "--date DATE" "Date for which you get measurements"
    :default (f/unparse (f/formatter "dd/MM/YYYY") (t/yesterday))]
   ["-u" "--user EMAIL" "Email to log into Embed"]
   ["-p" "--password PASSWORD" "Password to log into Embed"]])

(defn -main [& args]
  (let [{:keys [date user password] :as opts}
        (:options (parse-opts  args cli-options))]
    (println (str date " - " user " - " password))
    (comment
      (-> (met/get-daily-temp date) 
       met/create-sensor-measurements
       (met/upload-measurements )))))
