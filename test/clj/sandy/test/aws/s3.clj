(ns sandy.test.aws.s3
  (:require [sandy.aws.s3 :as sut]
            [clojure.test :refer :all])  )

(def fake-buckets
  [{:name      "fake-data-demo-1",
    :lifecycle {:rules [{:id "Delete: 7 Days",
                         :status "Enabled",
                         :prefix "",
                         :expired-object-delete-marker? false,
                         :noncurrent-version-expiration-in-days -1,
                         :expiration-in-days 7}]}}
   {:name "another-bucket" :lifecycle nil}
   {:name "fake-bucket-demo-2",
    :lifecycle {:rules [{:id "Delete: 7 Days",
                         :status "Enabled",
                         :prefix "",
                         :expired-object-delete-marker? false,
                         :noncurrent-version-expiration-in-days -1,
                         :expiration-in-days 7}]}}])

(deftest with-lifecycle-rules-filters-buckets-with-rules
  (testing "given a seq of buckets, returns only those with lifecycle rules"
    (is (= 2 (count (sut/with-lifecycle-rules fake-buckets))))))

(def fake-bucket-map
  {:name    "fake-bucket-name"
   :lifecycle {:rules [{:id                                    "tf-s3-lifecycle-p45rdatavvguzatnctp6i3djju"
                        :status                                "Enabled"
                        :transition                            {:storage-class-as-string "STANDARD_IA"
                                                                :storage-class           {}
                                                                :days                    30}
                        :prefix                                ""
                        :expiration-in-days                    -1
                        :transitions                           [{:storage-class-as-string "STANDARD_IA"
                                                                 :storage-class           {}
                                                                 :days                    30}]
                        :expired-object-delete-marker?         false
                        :noncurrent-version-expiration-in-days -1}]}})

(deftest has-lifecycle-populated-returns-true-for-populated-rule
  (testing "Given a rule that is populated, returns true"
      (is (true? (sut/has-lifecycle? fake-bucket-map)))))

(deftest is-rule-populated-returns-false-for-empty-rule
  (testing "Given unpopulated rule, returns false"
    (let [fake-lifecycle {:name "fake-bucket-name" :lifecycle nil}]
      (is (false? (sut/has-lifecycle? fake-lifecycle))))))

(deftest can-flatten-lifecycle-rule
  (testing "given a lifecycle rule, can remove unwanted stuff"
    (let [fake-rule
          {:id "tf-s3-lifecycle-nss4gdxzinaota2imfbyl75tmy"
           :prefix ""
           :transition {:storage-class {}
                        :days 90
                        :storage-class-as-string "STANDARD_IA"}
           :status "Enabled"
           :expiration-in-days -1
           :transitions [{:storage-class {}
                          :days 90
                          :storage-class-as-string "STANDARD_IA"}]
           :expired-object-delete-marker? false
           :noncurrent-version-expiration-in-days -1}
          expected {:storage-class "STANDARD_IA" :days 90}]
      (is (= expected (sut/flatten-rule fake-rule))))))

(def test-ia-row
  {:operation "StandardIAStorage"
   :usage-type "USW2-TimedStorage-SIA-ByteHrs"
   :resource "fake-bucket"
   :usage-value "3968822424"})

(deftest is-ia-returns-true-for-infrequent-access-resource
  (testing "given a map representing an S3 IA resource, return true"
    (is (true? (sut/is-ia? test-ia-row)))))

(def test-standard-row
  {:operation "StandardStorage"
   :usage-type "USW2-TimedStorage-SIA-ByteHrs"
   :resource "fake-bucket"
   :usage-value "3968822424"})

(deftest is-ia-returns-false-for-infrequent-access-resource
  (testing "given a map representing a non-IA S3 resource, return false"
    (is (false? (sut/is-ia? test-standard-row)))))

(deftest is-standard-returns-true-for-standard-resource
  (testing "given a map representing a non-IA S3 resource, return true"
    (is (true? (sut/is-standard? test-standard-row)))))

(deftest is-standard-returns-false-for-ia-resource
  (testing "given a map representing IA S3 resource, return false"
    (is (false? (sut/is-standard? test-ia-row)))))

(def test-rows
  '({:operation "StandardIAStorage", :usage-type "USW2-TimedStorage-SIA-ByteHrs", :resource "fantastic-staging", :usage-value "3968822424"}
    {:operation "StandardIAStorage", :usage-type "USW2-TimedStorage-SIA-ByteHrs", :resource "fox-staging", :usage-value "91726"}
    {:operation "StandardStorage", :usage-type "USW2-TimedStorage-ByteHrs", :resource "ooze-staging", :usage-value "1000"}
    {:operation "StandardIAStorage", :usage-type "USW2-TimedStorage-ByteHrs", :resource "ooze-staging", :usage-value "2000"}
    {:operation "StandardStorage", :usage-type "USW2-TimedStorage-ByteHrs", :resource "fantastic-staging", :usage-value "45667755864"}
    {:operation "StandardStorage", :usage-type "USW2-TimedStorage-ByteHrs", :resource "fantastic-staging", :usage-value "45667755864"}))

(deftest test-costs->maps-returns-useful-values
  (testing "given maps of S3 costs, get maps of bucket->[standard IA] values"
    (is (=
         '({"fantastic-staging" [0 3968822424]}
           {"fox-staging" [0 91726]}
           {"ooze-staging" [1000 0]}
           {"ooze-staging" [0 2000]}
           {"fantastic-staging" [45667755864 0]}
           {"fantastic-staging" [45667755864 0]})
         (sut/costs->maps test-rows)))))

(deftest test-summarize-costs-returns-useful-values
  (testing "given maps of S3 costs, get summed values for buckets"
    (is (=
         '{"fantastic-staging" [91335511728 3968822424]
            "fox-staging"       [0 91726]
            "ooze-staging"      [1000 2000]}
         (sut/summarize-costs test-rows)))))
