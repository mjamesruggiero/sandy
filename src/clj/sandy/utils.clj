(ns sandy.utils
  (:require [clojure.data.csv :as csv]
            [clojure.edn :as edn]
            [camel-snake-kebab.core :refer :all]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [clojure.java.io :as io]))

(defn get-csv
  [filepath]
  (with-open [in-file (io/reader filepath)]
    (doall
     (csv/read-csv in-file))))

(defn maps->csv
  "Take a filepath, seq of maps,
  and desired columns,and write them to a CSV;
  keys are the CSV headers"
  [path row-data columns]
  (let [headers (map name columns)
        rows (mapv #(mapv % columns) row-data)]
    (with-open [file (io/writer path)]
      (csv/write-csv file (cons headers rows)))))

(defn sum-by-key
  "Given a seq of maps, group by a key and sum
  the numeric value associated in each case"
  [ms grouping-key numeric-key]
  (into {}
        (for [[k v] (dissoc (group-by grouping-key ms) "")]
          [k (reduce + (map numeric-key v))])))

(defn map-values
  [m ks f]
  (reduce #(update-in %1 [%2] f) m ks))

(defn filter-columns
  "Shrink seq of maps to smaller set of keys"
  [maps columns]
  (map #(select-keys % columns) maps))

(defn filter-maps
  "Given seq of maps, return only maps
  having value for a given key"
  [maps k v predicate]
  (let [desired?
        (if (= predicate :eq)
          (fn [m] (= v (k m)))
          (fn [m] (not= v (k m))))]
    (filter desired? maps)))

(defn csv-to-maps
  "Processes CSV into seq of maps;
  takes optional keyword fn (in case you want
  to convert case or make it kebab-d)"
  ([csv]
   (csv-to-maps csv keyword))
  ([csv keyword-fn]
   (let [new-keywords (map keyword-fn (first csv))]
     (map #(zipmap new-keywords %) (rest csv)))))

(defn str->float [s]
  (try
    (Float/parseFloat s)
    (catch Exception e (float 0))))

(defn load-config
  "Given a filename, load & return a config file"
  [filename]
  (edn/read-string (slurp filename)))

(defn safe-divide
  "Given dividend x and divisor y
  perform some division but don't throw"
  [x y]
  (try (/ x y)
       (catch ArithmeticException _
         (println "Division by zero caught!")
         (cond (> x 0)   Double/POSITIVE_INFINITY
               (zero? x) 0.0
               :else     Double/NEGATIVE_INFINITY))))

(defn csv-filtered-converted
  "Parse CSV, filter to desired columns,
  and convert any numeric columns to floats
  TODO allow the converter to be passed"
  [filepath desired-cols cols-to-convert]
  (let [rows (-> filepath
                 (get-csv)
                 (csv-to-maps ->kebab-case-keyword))
        filtered (map #(select-keys % desired-cols) rows)]
    (map #(map-values % cols-to-convert str->float) filtered)))

(defn rows->snake-cased
  [rows]
  (map #(transform-keys ->snake_case %) rows))

(defn decorate-with-snapshot-id
  [guid rows]
  (map #(assoc % :snapshot_id guid) rows))

