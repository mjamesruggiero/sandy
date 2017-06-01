(ns sandy.aws.costs
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [camel-snake-kebab.core :refer :all]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [sandy.utils :as utils]
            [sandy.db.core :as sandy-db]
            [clj-time.format :as format]
            [clojure.tools.logging :as log])
  (:import org.postgresql.util.PGobject
           org.postgresql.jdbc4.Jdbc4Array
           clojure.lang.IPersistentMap
           clojure.lang.IPersistentVector
           [java.sql
            BatchUpdateException
            Date
            Timestamp
            PreparedStatement]))

(def ^{:const true}
  numeric-fields-to-convert
  [:total-cost
   :usage-quantity
   :blended-rate
   :credits
   :cost-before-tax
   :record-id])

(def ^{:const true}
  date-fields-to-convert
  [:usage-start-date
   :usage-end-date
   :invoice-date
   :billing-period-start-date
   :billing-period-end-date])

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
  (let [config (utils/load-config config-file)
        csv-path (:costs-csv config)]
    (utils/csv-filtered-converted csv-path cost-columns numeric-fields-to-convert)))

(defn get-ec2-instance-name
  [line-item]
  (let [parts (clojure.string/split line-item #":")]
    (last parts)))

(defn decorate-row-with-cost-per-hour
  "Decorates cost row with a useful cost value"
  [row]
  (let [cost-per-hour (utils/safe-divide (get row :cost-before-tax 0)
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
  (let [filtered-rows (utils/filter-maps costs :product-code "AmazonEC2" :eq)
        desired-columns [:type
                         :usage-quantity
                         :cost-before-tax
                         :cost-per-hour]
        minimal-columns (utils/filter-columns filtered-rows desired-columns)]
    (maps->key-mean minimal-columns :type :cost-per-hour)))

(defn costs-by-product
  [costs]
  (utils/sum-by-key costs :product-name :total-cost))

(defn costs->database-rows
  [cost-rows snapshot-id]
  (let [filtered (utils/filter-maps cost-rows :record-type "LinkedLineItem" :not=)]
    (utils/decorate-with-snapshot-id snapshot-id (utils/rows->snake-cased filtered))))

(defn- mk-snapshot-rec
  []
  (let [snapshot {:table_name  "cost_snapshots"
                  :snapshot_id (utils/random-uuid)
                  :title       "cost snapshot"}]
    (first (sandy-db/create-snapshot snapshot))))

(def date-formatter
  (format/formatter "yyyy/MM/dd HH:mm:SS"))

(defn cost-date-to-date
  "convert standard date string to timestamp"
  [date-string]
  (cond
    (clojure.string/blank? date-string) (format/parse date-formatter "1970/01/01 00:00:00")
    :else (format/parse date-formatter date-string)))

(defn date-to-timestamp
  [dt]
  (try
    (java.sql.Timestamp. (.getMillis dt))
    (catch Exception e 0)))

(defn date-converter
  "Syntactic sugar for string-to-to timestamp conversion"
  [datestring]
  (->> datestring
       cost-date-to-date
       date-to-timestamp))

(defn convert-dates [m ks]
  (utils/map-values m ks date-converter))

(defn csv->database-rows
  [csv]
  (let [rows      (utils/csv-filtered-converted csv cost-columns numeric-fields-to-convert)
        filtered  (utils/filter-maps rows :record-type "LinkedLineItem" :not=)
        _ (log/debug (str "filtered CSV rows: " (count filtered)))

        formatted (map #(convert-dates % date-fields-to-convert) filtered)
        _ (log/debug (str "formatted CSV rows: " (count formatted)))

        snapshot  (mk-snapshot-rec)
        _ (log/debug (str "snapshot: " snapshot))

        decorated (utils/decorate-with-snapshot-id
                   (:id snapshot) (utils/rows->snake-cased formatted))
        _ (log/debug (str "decorated CSV rows: " (count decorated)))

        res (map #(sandy-db/create-cost-snapshot! %) decorated)
        _ (log/debug (str "cost snapshots created: " (count res)))]
    res))
