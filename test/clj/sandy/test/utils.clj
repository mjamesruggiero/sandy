(ns sandy.test.utils
  (:require [sandy.utils :refer :all]
            [clojure.test :refer :all]))

(def sample-row
  {:usage-quantity     "13997.0"
   :payer-account-name "Content Quality Services"
   :total-cost         "349.930000"
   :item-description   "$0.025 per LoadBalancer-hour (or partial hour)"
   :invoice-date       "2017/03/03 04:43:38"})

(deftest map-values-converts-selected-values
  (testing "Can map values for specific keys to f"
    (let [f (fn [v] (Float/parseFloat v))
          result (map-values sample-row [:usage-quantity :total-cost] f)]
      (is (= 13997.0 (:usage-quantity result))))))

(def ^{:const true}
  filterable-rows
  [
   {
    :usage-quantity            100
    :payer-account-name        "fake account"
    :total-cost                100
    :item-description          "fake description"
    :operation                 "fake operation"
    :product-name              "EC2"
    :credits                   100
    :payer-account-id          100
    :billing-period-start-date "fake date"
    :usage-type                "fake usage type"
    :product-code              "AmazonDynamoDB"
    :rate-id                   12345
    :billing-period-end-date   "fake date"
    :cost-before-tax           100
    :record-type               "fake record type"
    }
   {
    :usage-quantity            100
    :payer-account-name        "fake account"
    :total-cost                100
    :item-description          "fake description"
    :operation                 "fake operation"
    :product-name              "EC2"
    :credits                   100
    :payer-account-id          100
    :billing-period-start-date "fake date"
    :usage-type                "fake usage type"
    :product-code              "AmazonEC2"
    :rate-id                   56789
    :billing-period-end-date   "fake date"
    :cost-before-tax           100
    :record-type               "TypeIHate"
    }
   ]
  )

(deftest can-filter-desired-records
  (testing "given a list of AWS cost records, just return the EC2s"
    (is (= 56789
           (:rate-id
            (first
             (filter-maps filterable-rows :product-code "AmazonEC2" :eq)))))))

(deftest can-filter-undesired-records
  (testing "given list of AWS cost records, return ones that aren't dynamo"
    (is (= 12345
           (:rate-id
            (first
             (filter-maps filterable-rows :product-code "AmazonEC2" :not=)))))))

(deftest can-convert-strings-to-floats
  (testing "given a string number, can convert to float representation"
    (is (= (float 9.73) (str->float "9.73")))))

(deftest can-return-sensible-default-for-failed-conversion
  (testing "given a string number, can convert to float representation"
    (is (= (float 0) (str->float "")))))

(def ^{:const true} monkey-rows
  [{:monkey "Capuchin" :cost 100}
   {:monkey "Howler"   :cost 100}
   {:monkey "Howler"   :cost 200}
   {:monkey "Mandrill" :cost 100}
   {:monkey "Capuchin" :cost 100}])

(deftest sum-by-key-sums-rows-with-matching-keyvals
  (testing "given a seq of maps, reduce for a given key (i.e. group and sum)"
    (is (= {"Capuchin" 200 "Howler" 300 "Mandrill" 100}
           (sum-by-key monkey-rows :monkey :cost)))))

(deftest filtered-columns-shrinks-seq-of-maps
  (testing "foo"
    (let [ms           [{:foo 1, :bar 2, :baz 3}
                        {:foo 4, :bar 5, :baz 6}
                        {:foo 7, :bar 8, :baz 9}]
          expected [{:baz 3}
                    {:baz 6}
                    {:baz 9}]]
          (is (= expected (filter-columns ms [:baz]))))))

(deftest safe-divide-handles-zero-divisor
  (testing "Given a divide by zero, returns zero"
    (is (= 0.0 (safe-divide 0 0)))
    (is (= Double/NEGATIVE_INFINITY (safe-divide -100 0)))
    (is (= Double/POSITIVE_INFINITY (safe-divide 100 0)))))
