(ns sandy.test.aws.costs
  (:require [sandy.aws.costs :as sut]
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
