(ns sandy.aws.s3
  (:require [amazonica.aws.s3 :as s3]
            [sandy.utils :as utils])
  (:import com.amazonaws.services.s3.model.AmazonS3Exception))

(defn- fetch-lifecycle
  "Get the S3 lifecycle configuration for a bucket
  and return map of bucket name and lifecycle"
  [bucket]
  (let [lifecycle
        (try
          (s3/get-bucket-lifecycle-configuration bucket)
          (catch AmazonS3Exception se {:error (:cause se)})
          (finally (println (str "failed to get bucket lifecycle for " bucket))))]
    {:name      bucket
     :lifecycle lifecycle}))

(defn fetch-buckets
  "Fetch all buckets and lifecycles; side-effecting"
  []
  (let [names (map :name (s3/list-buckets))]
    (map fetch-lifecycle names)))

(defn has-lifecycle?
  "Is the lifecycle rule set populated?"
  [m]
  (not (nil? (:lifecycle m))))

(defn with-lifecycle-rules
  [buckets]
  (filter has-lifecycle? buckets))

(defn split-with-lifecycle
  "Given a seq of bucket maps,
  returns a tuple of buckets with lifecycles
  defined and buckets with undefined lifecycles"
  [buckets]
  ((juxt filter remove) has-lifecycle? buckets))

(defn flatten-rule
  [rule]
  (let [s (get-in rule [:transition :storage-class-as-string])
        d (get-in rule [:transition :days])]
    {:storage-class s :days d}))

(defn size
  "Given bucket metadata, return size
  of all assets inside the bucket"
  [bucket]
  (let [ms (:object-summaries bucket)
        sizefn (fn [m] (get m :size 0))
        sizes (map sizefn ms)]
    (reduce + 0 sizes)))

(defn age-of-newest-object
  "Given a bucket, get the age of the newest object"
  [bucket]
  (let [dates (map :last-modified (:object-summaries bucket))]
    (if (seq? dates)
      (last (sort dates)))))

(defn- decorate-with-size-and-age
  "Side-effecting"
  [m]
  (let [bucket-metadata (s3/list-objects :bucket-name (:name m))]
    (assoc m
           :size (size bucket-metadata)
           :most-recently-updated (age-of-newest-object bucket-metadata))))

(defn- save-buckets-without-policies
  "Pulls buckets without policies,
  saves size and age of most recent object to CSV;
  side-effecting"
  [filepath]
  (let [all-buckets (fetch-buckets)
        without-policies (second (split-with-lifecycle all-buckets))
        s (map decorate-with-size-and-age without-policies)]
    (utils/maps->csv filepath s [:name :size :most-recently-updated])))
