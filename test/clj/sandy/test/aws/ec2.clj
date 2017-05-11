(ns sandy.test.aws.ec2
  (:require [sandy.aws.ec2 :as sut]
            [clojure.test :refer :all]) )

(deftest tag-desired-identifies-desired-tags
  (testing "given a map, is the tag useful/helpful?"
    (is (true? (sut/tag-desired? {:key "Stages" :value "many"})))))

(deftest tag-desired-identifies-undesired-tags
  (testing "given a map, is the tag not useful/helpful?"
    (is (false? (sut/tag-desired? {:key "Genus" :value "Hibiscus"})))))

(def i
  {:instance-type      "c3.2xlarge",
   :instance-id        "i-0dd37f9d69fcc8b3e",
   :placement {:tenancy           "default",
               :group-name        "",
               :availability-zone "us-east-1d"},
   :tags      [
               {:value "production-us-east-1",
                :key   "Stages"},
               {:value "adserver production blue vpc 3",
                :key   "Name"},
               {:value "adserver",
                :key   "Project"},
               {:value "blue",
                :key   "Group"},
               {:value "adserver",
                :key   "Roles"}
               ]})

(deftest flatten-instance-turns-nested-aws-json-into-basic-row
  (testing "given AWS cli JSON node, return flattened structure"
    (let [expected {:instance-type     "c3.2xlarge"
                    :instance-id       "i-0dd37f9d69fcc8b3e"
                    :stages            "production-us-east-1"
                    :name              "adserver production blue vpc 3"
                    :project           "adserver"
                    :availability-zone "us-east-1d"}]
      (is (= expected (sut/flatten-instance i))))))

(def test-instances
  [{:monitoring              {:state "enabled"},
    :tags                    [{:value "adserver",:key "Roles"}
                              {:value "production-eu-central-1", :key "Stages"}
                              {:value "data-processing",:key "Project"}
                              {:value "Malibu",:key "aws:autoscaling:groupName"}
                              {:value "ProjectMalibu",:key "Name"}],
    :root-device-type        "ebs",
    :private-dns-name        "ip-151-678-253-10.eu-central-1.compute.internal",
    :hypervisor              "xen",
    :subnet-id               "subnet-6e439406",
    :key-name                "engineering",
    :architecture            "x86_64",
    :security-groups         [{:group-id   "sg-77fb611f",
                               :group-name "malibu-production"} {:group-id   "sg-bd8d11d5",
                                                                 :group-name "vpc-app"}],
    :source-dest-check       true,
    :root-device-name        "/dev/sda1",
    :virtualization-type     "hvm",
    :product-codes           [],
    :instance-type           "c3.2xlarge",
    :ami-launch-index        0,
    :image-id                "ami-e5e1298a",
    :state                   {:name "running",
                              :code 16},
    :state-transition-reason "",
    :network-interfaces      [{:description          "",
                               :private-dns-name     "ip-151-678-253-10.eu-central-1.compute.internal",
                               :subnet-id            "subnet-6e439406",
                               :source-dest-check    true,
                               :private-ip-addresses [{:primary            true,
                                                       :private-ip-address "151.678.253.10",
                                                       :private-dns-name   "ip-151-678-253-10.eu-central-1.compute.internal"}],
                               :network-interface-id "eni-91c685fd",
                               :vpc-id               "vpc-1518047c",
                               :mac-address          "02:30:da:c7:70:8b",
                               :status               "in-use",
                               :private-ip-address   "151.678.253.10",
                               :owner-id             "119933218031",
                               :ipv6addresses        [],
                               :groups               [{:group-id   "sg-77fb611f",
                                                       :group-name "adserver-production"} {:group-id   "sg-bd8d11d5",
                                                                                           :group-name "vpc-app"}],
                               :attachment           {:status                "attached",
                                                      :attach-time           "2017-02-23T18:24:39.000-08:00",
                                                      :delete-on-termination true,
                                                      :device-index          0,
                                                      :attachment-id         "eni-attach-aadb08c4"}}],
    :vpc-id                  "vpc-1518047c",
    :ebs-optimized           false,
    :instance-id             "i-0ad33c31bb4615311",
    :iam-instance-profile    {:id  "AIPAIOM52JG3HV3HERTJK",
                              :arn "arn:aws:iam::119933218031:instance-profile/adserver-eu-central-1-production"},
    :public-dns-name         "",
    :private-ip-address      "151.678.253.10",
    :placement               {:tenancy           "default",
                              :availability-zone "eu-central-1a",
                              :group-name        ""},
    :client-token            "6a75306b-603f-4063-8dac-1e1055a48775_subnet-6e439406_1",
    :launch-time             "2017-02-23T18:24:39.000-08:00",
    :block-device-mappings   [{:ebs         {:status                "attached",
                                             :attach-time           "2017-02-23T18:24:40.000-08:00",
                                             :delete-on-termination true,
                                             :volume-id             "vol-056d9c61ebb55432c"},
                               :device-name "/dev/sda1"}]}])

(deftest instances-can-be-converted-to-database-rows
  (testing "Given a seq of raw AWS instances, can build database rows"
    (let [expected '({:instance_type "c3.2xlarge"
                      :instance_id "i-0ad33c31bb4615311"
                      :project "data-processing"
                      :name "ProjectMalibu"
                      :stages "production-eu-central-1"
                      :availability_zone "eu-central-1a"
                      :snapshot_id "foo"})]
      (is (= expected (sut/instances->database-rows test-instances "foo"))))))

(deftest tag->vec-can-convert-tag-map-to-k-v-vector
  (testing "given a tag map, get a key-value vector"
    (is (= [:foobar "baz"] (sut/tag->vec {:key "FooBar" :value "baz"})))))

(def test-tags
  [{:key "Stages" :value "foo"},
   {:key "Name" :value "bar"},
   {:key "Project" :value "baz"},
   {:key "Monkey" :value "capuchin"}])

(deftest transform-tags-creates-canonical-tag-columns
  (testing "given tags, create canonical tag columns"
    (is (= {:stages "foo" :name "bar" :project "baz"}
           (sut/transform-tags test-tags)))))

