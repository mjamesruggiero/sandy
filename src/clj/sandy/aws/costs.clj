(ns sandy.aws.costs
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [camel-snake-kebab.core :refer :all]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [sandy.utils :as utils]
            [sandy.db.core :as sandy-db]
            [clojure.tools.logging :as log]))

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

(defn transform-rows
  "Filter out unwanted CSV rows,
  convert date values,
  and convert keys to snake-case"
  [rows]
  (let [filtered       (utils/filter-maps rows :record-type "LinkedLineItem" :not=)
        date-converted (map #(utils/convert-dates % date-fields-to-convert) filtered)]
    (utils/rows->snake-cased date-converted)))

(defn- fetch-csv
  "Open CSV, remove unwanted columns
  and convert numeric values to doubles"
  [csv]
  (utils/csv-filtered-converted csv cost-columns numeric-fields-to-convert))

(defn- create-cost-snapshot []
  "Create database record for snapshot"
  (first (sandy-db/create-snapshot {:table_name  "cost_snapshots"
                                    :snapshot_id (utils/random-uuid)
                                    :title       "cost snapshot"})))

(defn- decorate-with-snapshot-id
  "Creates snapshot record (side-effecting)
  and decorates the rows with the id.
  TODO might be better handled in transaction"
  [rows]
  (let [snapshot (create-cost-snapshot)]
    (utils/decorate-with-snapshot-id (:id snapshot) rows)))

(defn csv->database-rows
  "Given a CSV, parse, reformat, add a
  snapshot id and insert into the database"
  [filepath]
  (let [rows (-> filepath
                 (fetch-csv)
                 (transform-rows)
                 (decorate-with-snapshot-id))]
    (map #(sandy-db/create-cost-snapshot! %) rows)))
