(ns sandy.aws.s3
  (:require [amazonica.aws.s3 :as s3]))

(defn- fetch-lifecycle
  "Get the S3 lifecycle configuration for a bucket
  and return map of bucket name and lifecycle"
  [bucket]
  {:name bucket
   :lifecycle (s3/get-bucket-lifecycle-configuration bucket)})

(defn fetch-buckets
  "Fetch all buckets and lifecycles"
  []
  (let [names (map :name (s3/list-buckets))]
    (map fetch-lifecycle names)))

(defn has-lifecycle?
  "Is the lifecycle rule set populated?"
  [lc]
  (not (empty? (:rules lc))))

(defn with-lifecycle-rules
  [buckets]
  (filter
   (fn [m]
     (has-lifecycle? (:lifecycle m)))
   buckets))

(defn flatten-rule
  [rule]
  (let [s (get-in rule [:transition :storage-class-as-string])
        d (get-in rule [:transition :days])]
    {:storage-class s :days d}))
