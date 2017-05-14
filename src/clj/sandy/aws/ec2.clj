(ns sandy.aws.ec2
  (:require [clojure.java.io :as io]
            [amazonica.aws.ec2 :as a]
            [sandy.utils :as utils]
            [camel-snake-kebab.core :refer :all]
            [camel-snake-kebab.extras :refer [transform-keys]]))

(defn tag-desired?
  "Does tag map contain the :key value we want?"
  [m]
  (contains? #{"Stages" "Name" "Project"} (:key m)))

(defn tag->vec
  "Convert {:key <foo> :value <bar>} map to k->v vector"
  [t]
  (let [new-k (keyword (clojure.string/lower-case (:key t)))]
    [new-k (:value t)]))

(defn transform-tags
  "Turn tag structure into columns"
  [tags]
  (let [defaults {:stages "n/a" :name "n/a" :project "n/a"}
        filtered (filter tag-desired? tags)
        raw (into {} (map tag->vec filtered))]
    ;; merge defaults with potential tags found
    ;; (some will be missing)
    (merge-with #(or %1 %2) raw defaults)))

(defn flatten-instance
  "Turn slightly nested EC2 data into flattened map"
  [i]
  (let [initial    (into {} (select-keys i [:instance-type :instance-id]))
        zone       (get-in i [:placement :availability-zone])
        tags       (transform-tags (:tags i))]
    (assoc (into initial tags) :availability-zone zone)))

(defn instances
  "Grab all instances"
  []
  (let [endpoints (for [region (:regions (a/describe-regions))]
                    (:endpoint region))
        reservations (flatten
                      (for [endpoint endpoints]
                        (:reservations
                         (a/describe-instances {:endpoint endpoint}))))]
    (flatten (for [reservation reservations]
               (:instances reservation)))))

(defn- instances-with-canonical-keys
  [flattened]
  (let [instance-columns [:instance-type
                          :instance-id
                          :project
                          :name
                          :stages
                          :availability-zone]]
    (map #(select-keys % instance-columns) flattened)))

(defn- flatten-instances
  [instances]
  (map #(flatten-instance %) instances))

(defn instances->database-rows
  [instances snapshot-id]
  (let [flattened (-> instances
                      (flatten-instances)
                      (instances-with-canonical-keys)
                      (utils/rows->snake-cased))]
    (utils/decorate-with-snapshot-id snapshot-id flattened)))
