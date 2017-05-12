(ns sandy.utils
  (:require [clojure.data.csv :as csv]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [amazonica.aws.ec2 :as a]
            [camel-snake-kebab.core :refer :all]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [cheshire.core :as json]
            [amazonica.aws.s3 :as s3]))

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

;;~~~~~~~~~~~~~~~~~~
;;  AWS cost data
;;~~~~~~~~~~~~~~~~~~

(def ^{:const true}
  keys-to-convert [:total-cost
                   :usage-quantity
                   :blended-rate
                   :credits
                   :cost-before-tax])

(def ^{:const true}
  cost-columns
  [:billing-period-end-date
   :billing-period-start-date
   :blended-rate
   :cost-before-tax
   :credits
   :invoice-date
   :item-description
   :operation
   :product-code
   :product-name
   :rate-id
   :record-id
   :record-type
   :total-cost
   :usage-end-date
   :usage-quantity
   :usage-start-date
   :usage-type])

(defn cost-csv
  "Take CSV of AWS costs; filter to columns
  we need and convert some strings to floats"
  [& {:keys [config-file]
      :or {config-file (io/resource "dev-config.edn")}}]
  (let [config (load-config config-file)
        csv-path (:costs-csv config)]
    (csv-filtered-converted csv-path cost-columns keys-to-convert)))

;;~~~~~~~~~~~~~~~~~~~
;; Instances
;;~~~~~~~~~~~~~~~~~~~

(defn get-ec2-instance-name
  [line-item]
  (let [parts (clojure.string/split line-item #":")]
    (last parts)))

(defn decorate-row-with-cost-per-hour
  "Decorates cost row with a useful cost value"
  [row]
  (let [cost-per-hour (safe-divide (get row :cost-before-tax 0)
                                   (get row :usage-quantity 1))]
    (assoc row :cost-per-hour cost-per-hour)))

(defn decorate-row-with-instance-name
  "Decorates cost row with a useful instance type"
  [row]
  (let [instance-name (get-ec2-instance-name (get row :usage-type ""))]
    (assoc row :type instance-name)))

(defn mean
  [coll]
  (let [sum (apply + coll)
        count (count coll)]
    (if (pos? count)
      (/ sum count)
      (0))))

(defn maps->key-mean
  "Take a seq of maps, the key to group on,
  and the values to average;
  return average for each key"
  [ms grouping-k sum-k]
  (let [grouped (group-by grouping-k ms)]
    (into {}
          (for [[k v] grouped]
            [k (mean
                (flatten
                 (map vals
                      (map #(select-keys % [sum-k]) v))))]))))

(defn ec2-instance-costs
  "Create the map of instance-type->cost;
  cost is a mean of costs across the instance type"
  [costs]
  (let [filtered-rows (filter-maps costs :product-code "AmazonEC2" :eq)
        desired-columns [:type
                         :usage-quantity
                         :cost-before-tax
                         :cost-per-hour]
        minimal-columns (filter-columns filtered-rows desired-columns)]
    (maps->key-mean minimal-columns :type :cost-per-hour)))

(defn costs-by-product
  [costs]
  (sum-by-key costs :product-name :total-cost))

(defn- random-uuid []
  (str (java.util.UUID/randomUUID)))

(defn decorate-with-snapshot-id
  [guid rows]
  (map #(assoc % :snapshot_id guid) rows))

(defn rows->snake-cased
  [rows]
  (map #(transform-keys ->snake_case %) rows))

(defn costs->database-rows
  [cost-rows snapshot-id]
  (let [filtered
        (filter-maps cost-rows :record-type "LinkedLineItem" :not=)]
    (decorate-with-snapshot-id snapshot-id (rows->snake-cased filtered))))
