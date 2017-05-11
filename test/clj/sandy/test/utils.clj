(ns sandy.test.utils
  (:require [sandy.utils :as sut]
            [clojure.test :refer :all]))

(deftest can-grep-ec2-instance-type-from-aws-line-item
  (testing "given an EC2 line item, can I grep the instance type?"
    (is (= "r3.4xlarge" (sut/get-ec2-instance-name "HeavyUsage:r3.4xlarge")))))

(def rows [{:instance-type     "c3.2xlarge"
            :instance-id       "i-0ad33c31bb4615311"
            :stages            "production-eu-central-1"
            :project           "adserver"
            :name              "adserver production blue autoscale"
            :availability-zone "eu-central-1a"
            :cost              0.0
            :daily-cost        0.0}
           {:instance-type     "c3.2xlarge"
            :instance-id       "i-0ad33c31bb4615311"
            :stages            "production-eu-central-1"
            :project           "adserver"
            :name              "adserver production blue autoscale"
            :availability-zone "eu-central-1a"
            :cost              0.0
            :daily-cost        0.0}])

(deftest can-decorate-ec2-instance
  (testing "Given a colon-concat usage-type label, can decorate with instance type"
    (is (= {:usage-type "BoxUsage:m3.medium" :type "m3.medium"}
        (sut/decorate-row-with-instance-name {:usage-type  "BoxUsage:m3.medium"})))))

(deftest can-decorate-cost-with-cost-per-hour
  (testing "given a row, can decorate map with new key value"
    (is (= {:cost-before-tax 8
            :usage-quantity  2
            :cost-per-hour   4}
           (sut/decorate-row-with-cost-per-hour {:cost-before-tax 8
                                                 :usage-quantity  2})))))

(def ^{:const true} ec2-cost-csv-output
  [{:type "c3.2xlarge" :usage-quantity 10 :cost-before-tax 100 :cost-per-hour 10 :product-code "AmazonEC2" :foo "bar"}
   {:type "c3.2xlarge" :usage-quantity 10 :cost-before-tax 200 :cost-per-hour 20 :product-code "AmazonEC2" :foo "bar"}
   {:type "t1.micro" :usage-quantity 10 :cost-before-tax 50 :cost-per-hour 5 :product-code "AmazonEC2" :foo "bar"}
   {:type "SomethingSomething" :usage-quantity 1 :cost-before-tax 1 :cost-per-hour 1 :product-code "AmazonDynamo" :foo "bar"}])

(deftest ec2-instance-costs-can-map-instance-to-averag-cost
  (testing "Given CSV cost data, we can extract a map of instance name to cost"
    (is (= {"c3.2xlarge" 15 "t1.micro" 5} (sut/ec2-instance-costs ec2-cost-csv-output)))))


(deftest is-rule-populated-returns-true-for-populated-rule
  (testing "Given a rule that is populated, returns true"
    (let [fake-lifecycle
          {:rules
           [{:id                                    "tf-s3-lifecycle-nss4gdxzinaota2imfbyl75tmy",
             :prefix                                "",
             :transition                            {:storage-class           {},
                                                     :days                    90,
                                                     :storage-class-as-string "STANDARD_IA"},
             :status                                "Enabled",
             :expiration-in-days                    -1,
             :transitions                           [{:storage-class           {},
                                                      :days                    90,
                                                      :storage-class-as-string "STANDARD_IA"}],
             :expired-object-delete-marker?         false,
             :noncurrent-version-expiration-in-days -1}]
           }]
      (is (true? (sut/has-lifecycle? fake-lifecycle))))))

(deftest is-rule-populated-returns-false-for-empty-rule
  (testing "Given unpopulated rule, returns false"
    (let [fake-lifecycle {:rules []}]
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

