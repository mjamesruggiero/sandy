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
