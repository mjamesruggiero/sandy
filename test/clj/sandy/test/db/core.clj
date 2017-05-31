(ns sandy.test.db.core
  (:require [sandy.db.core :as db]
            [clojure.test :refer :all]))

(deftest test-date-converter
  (testing "can convert a string to a timestamp"
    (is (= (java.sql.Timestamp. 1447459200000) (db/date-converter "2015/11/14 00:00:00")))))

(deftest test-date-converter-handles-bad-dates
  (testing "can take a bad date string and return a zero-value timestamp"
    (is (= (java.sql.Timestamp. 0) (db/date-converter "")))))
