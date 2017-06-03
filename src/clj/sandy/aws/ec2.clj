(ns sandy.aws.ec2
  (:require [clojure.java.io :as io]
            [amazonica.aws.ec2 :as a]
            [sandy.utils :as utils]
            [camel-snake-kebab.core :refer :all]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [sandy.db.core :as sandy-db]
            [sandy.utils :as utils]
            [clojure.tools.logging :as log]))

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
  (map flatten-instance instances))

(defn instances->database-rows
  "Flatten instances, retain needed fields,
  make keys snake-cased, decorate with snapshot id"
  [instances snapshot-id]
  (let [flattened (-> instances
                      (flatten-instances)
                      (instances-with-canonical-keys)
                      (utils/rows->snake-cased))]
    (utils/decorate-with-snapshot-id snapshot-id flattened)))

(defn- create-snapshot-record
  "Create with instance metadata"
  []
  (let [snapshot {:table_name  "instance_snapshots"
                  :snapshot_id (utils/random-uuid)
                  :title       "instance snapshots"}]
    (first (sandy-db/create-snapshot snapshot))))

(defn- decorate-with-snapshot-id
  "Transform instances to database rows,
  create snapshot and decorate rows"
  [rows]
  (let [snapshot (create-snapshot-record)]
    (instances->database-rows rows (:id snapshot))))

(defn mk-instance-snapshot
  "Grab all the instances and generate
  rows (the essence of a snapshot)"
  []
  (let [transformed (-> (instances)
                        (decorate-with-snapshot-id))]
        (map #(sandy-db/create-instance-snapshot %) transformed)))

(defn mk-instance-snapshot->future
  "Wrap #mk-instance-snapshot in a future"
  []
  (future (mk-instance-snapshot)))
