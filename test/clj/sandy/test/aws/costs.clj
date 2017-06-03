(ns sandy.test.aws.costs
  (:require [sandy.aws.costs :as sut]
            [clojure.test :refer :all]
            [clj-time.core :as t]
            [sandy.utils :as utils]))

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

(def test-rows-to-filter
  [
   {:usage-quantity            8441.643
    :total-cost                168.83286
    :item-description          "$0.02 per GB - US East (Northern Virginia) data transfer to EU (Ireland)"
    :operation                 ""
    :invoice-date              "2017/05/29 17:02:32"
    :usage-end-date            "2017/05/31 23:59:59"
    :product-name              "AWS Data Transfer"
    :credits                   0.0
    :blended-rate              0.02
    :billing-period-start-date "2017/05/01 00:00:00"
    :usage-type                "USE1-EU-AWS-Out-Bytes"
    :product-code              "AWSDataTransfer"
    :rate-id                   "16012705"
    :usage-start-date          "2017/05/01 00:00:00"
    :billing-period-end-date   "2017/05/31 23:59:59"
    :record-id                 2.20000003E18
    :cost-before-tax           168.83286
    :record-type               "LinkedLineItem"
    }
   {
    :usage-quantity            8.69E-6
    :total-cost                0.0
    :item-description          "$0.00 per GB - EU (Germany) data transfer from Canada (Central)"
    :operation                 ""
    :invoice-date              "2017/05/29 17:02:32"
    :usage-end-date            "2017/05/31 23:59:59"
    :product-name              "AWS Data Transfer"
    :credits                   0.0
    :blended-rate              0.0
    :billing-period-start-date "2017/05/01 00:00:00"
    :usage-type                "EUC1-CAN1-AWS-In-Bytes"
    :product-code              "AWSDataTransfer"
    :rate-id                   "16012374"
    :usage-start-date          "2017/05/01 00:00:00"
    :billing-period-end-date   "2017/05/31 23:59:59"
    :record-id                 2.20000003E18
    :cost-before-tax           0.0
    :record-type               "LinkedLineItem"
    }
   {:usage-quantity 7981.4487
    :total-cost 638.51587
    :item-description "$0.08 per GB data transfer out (Australia)"
    :operation ""
    :invoice-date "2017/05/29 17:02:32"
    :usage-end-date "2017/05/31 23:59:59"
    :product-name "Amazon CloudFront"
    :credits 0.0
    :blended-rate 0.08
    :billing-period-start-date "2017/05/01 00:00:00"
    :usage-type "AU-DataTransfer-Out-Bytes"
    :product-code "AmazonCloudFront"
    :rate-id "14524896"
    :usage-start-date "2017/05/01 00:00:00"
    :billing-period-end-date "2017/05/31 23:59:59"
    :record-id 2.20000003E18
    :cost-before-tax 638.51587
    :record-type "LinkedLineItem"
    }
   {:usage-quantity            8441.643
    :total-cost                168.83286
    :item-description          "$0.02 per GB - US East (Northern Virginia) data transfer to EU (Ireland)"
    :operation                 ""
    :invoice-date              "2017/05/29 17:02:32"
    :usage-end-date            "2017/05/31 23:59:59"
    :product-name              "AWS Data Transfer"
    :credits                   0.0
    :blended-rate              0.02
    :billing-period-start-date "2017/05/01 00:00:00"
    :usage-type                "USE1-EU-AWS-Out-Bytes"
    :product-code              "AWSDataTransfer"
    :rate-id                   "16012705"
    :usage-start-date          "2017/05/01 00:00:00"
    :billing-period-end-date   "2017/05/31 23:59:59"
    :record-id                 2.20000003E18
    :cost-before-tax           168.83286
    :record-type               "OtherItem"
    }])

(deftest transform-rows-filters-linked-line-items
  (testing "Given rows of LinkedLineItem record type, filters them out"
    (is (= 1 (count (sut/transform-rows test-rows-to-filter))))))

;; TODO test all expected keys
(deftest transform-rows-converts-dates-to-date-objects
  (testing "Given rows of cost csv maps, converts the right keys to dates"
    (let [converted (sut/transform-rows test-rows-to-filter)
          expected (utils/datestring->timestamp "2017/05/1 00:00:00")
          returned (:usage_start_date (first converted))]
      (is (= expected returned)))))

(set (keys (first (sut/transform-rows test-rows-to-filter))))

(deftest transform-rows-makes-keys-snake-cased
  (testing "Given rows of cost csv maps, converts their keys to snake case"
    (let [keys-from-first-element
          (set (keys  (first (sut/transform-rows test-rows-to-filter))))
          expected-keys #{:usage_quantity
                          :total_cost
                          :item_description
                          :operation
                          :invoice_date
                          :usage_end_date
                          :product_name
                          :credits
                          :blended_rate
                          :billing_period_start_date
                          :usage_type
                          :product_code
                          :rate_id
                          :usage_start_date
                          :billing_period_end_date
                          :record_id
                          :cost_before_tax
                          :record_type }]
      (is (= expected-keys keys-from-first-element)))))
