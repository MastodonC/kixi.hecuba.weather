(ns kixi.hecuba.weather.metoffice-api-test
  (:require [clojure.test :refer :all]
            [kixi.hecuba.weather.metoffice-api :refer :all]))

(deftest calc-degreedays-mckiver-test
  (testing "Returns the right value for degreedays"
    ;; Degree day must be > 0
    (is (> (calc-degreedays-mckiver 15.5 7.5 7.5) 0.0))
    ;; Degree day is a Double
    (is (= java.lang.Double (type (calc-degreedays-mckiver 15.5 7.5 7.5))))
    ;; Examples (http://www.degreedays.net/introduction
    (is (= 1.5 (calc-degreedays-mckiver 17 15 16)))
    (is (= 8.0 (calc-degreedays-mckiver 15.5 7.5 7.5)))))

(deftest max-min-test
  (testing "Returns max and min in a map")
  (let [testdata [{:temperature "5"}
                  {:temperature "7"}
                  {:temperature "4.4"}
                  {:temperature "4"}
                  {:temperature "11"}
                  {:temperature "12"}
                  {:temperature "9"}
                  {:temperature "8"}]]
    (is (= {:max 12.0 :min 4.0} (get-max-and-min testdata)))))

(deftest format-key-test
  (testing "Format key function")
  (is (= ":temperature" "Temperature"))
  (is (= ":device-id" "Device ID")))

