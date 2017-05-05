(ns sandy.db.core
  (:require
    [cheshire.core :refer [generate-string parse-string]]
    [clojure.java.jdbc :as jdbc]
    [conman.core :as conman]
    [config.core :refer [env]]
    [mount.core :refer [defstate]]
    [clj-time.format :as format])
  (:import org.postgresql.util.PGobject
           org.postgresql.jdbc4.Jdbc4Array
           clojure.lang.IPersistentMap
           clojure.lang.IPersistentVector
           [java.sql
            BatchUpdateException
            Date
            Timestamp
            PreparedStatement]))

(def pool-spec
  {:adapter    :postgresql
   :init-size  1
   :min-idle   1
   :max-idle   4
   :max-active 32}) 

(defn connect! []
  (let [conn (atom nil)]
    (conman/connect!
      conn
      (assoc
        pool-spec
        :jdbc-url (env :database-url)))
    conn))

(defn disconnect! [conn]
  (conman/disconnect! conn))

(defstate ^:dynamic *db*
          :start (connect!)
          :stop (disconnect! *db*))

(conman/bind-connection *db* "sql/queries.sql")

(defn to-date [sql-date]
  (-> sql-date (.getTime) (java.util.Date.)))

(extend-protocol jdbc/IResultSetReadColumn
  Date
  (result-set-read-column [v _ _] (to-date v))

  Timestamp
  (result-set-read-column [v _ _] (to-date v))

  Jdbc4Array
  (result-set-read-column [v _ _] (vec (.getArray v)))

  PGobject
  (result-set-read-column [pgobj _metadata _index]
    (let [type  (.getType pgobj)
          value (.getValue pgobj)]
      (case type
        "json" (parse-string value true)
        "jsonb" (parse-string value true)
        "citext" (str value)
        value))))

(extend-type java.util.Date
  jdbc/ISQLParameter
  (set-parameter [v ^PreparedStatement stmt ^long idx]
    (.setTimestamp stmt idx (Timestamp. (.getTime v)))))

(extend-type clojure.lang.IPersistentVector
  jdbc/ISQLParameter
  (set-parameter [v ^java.sql.PreparedStatement stmt ^long idx]
    (let [conn      (.getConnection stmt)
          meta      (.getParameterMetaData stmt)
          type-name (.getParameterTypeName meta idx)]
      (if-let [elem-type (when (= (first type-name) \_) (apply str (rest type-name)))]
        (.setObject stmt idx (.createArrayOf conn elem-type (to-array v)))
        (.setObject stmt idx v)))))

(defn to-pg-json [value]
  (doto (PGobject.)
    (.setType "jsonb")
    (.setValue (generate-string value))))

(extend-protocol jdbc/ISQLValue
  IPersistentMap
  (sql-value [value] (to-pg-json value))
  IPersistentVector
  (sql-value [value] (to-pg-json value)))

;; ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
;; date formatters
;; TODO move these to utils namespace
;; ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
(def- aws-formatter
  (format/formatter "yyyy/MM/dd HH:mm:ss"))

(defn- aws-date-to-date
  "convert standard date string to timestamp"
  [date-string]
  (format/parse aws-formatter date-string))

(defn- date-to-timestamp
  [dt]
  (java.sql.Timestamp. (.getMillis dt)))

(defn date-converter
  "Syntactic sugar for string-to-to timestamp conversion"
  [datestring]
  (try
    (->> datestring
         aws-date-to-date
         date-to-timestamp)
    (catch java.lang.IllegalArgumentException _
      (date-to-timestamp (aws-date-to-date "1970/01/01 00:00:00")))
    (finally (str "Release some resource"))))

;; ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

;; (defn f
;;   []
;;   (create-instance-snapshot!
;;    {:snapshot_id "fake-snapshot-id"
;;     :instance_type "m4.xlarge"
;;     :instance_id "i-123456"
;;     :project "asap"
;;     :name "asap staging autoscale"
;;     :stages "staging"
;;     :availability_zone "us-west-2a"}))

;; (defn f []
;;   (create-cost-snapshot!
;;    {
;;     :snapshot_id 1
;;     :record_type "fake-record-type"
;;     :record_id 1
;;     :billing_period_start_date (date-converter "2017/04/01 00:00:00")
;;     :billing_period_end_date (date-converter "2017/04/01 00:00:00")
;;     :invoice_date (date-converter "2017/04/01 00:00:00")
;;     :product_code "fake-product-code"
;;     :product_name "fake-product-name"
;;     :usage_type "fake-usage-type"
;;     :operation "fake-operation"
;;     :rate_id "fake-rate"
;;     :item_description "fake-item-description"
;;     :usage_start_date (date-converter "2017/04/01 00:00:00")
;;     :usage_end_date (date-converter "2017/04/01 00:00:00")
;;     :usage_quantity 1.2
;;     :blended_rate 1.2
;;     :cost_before_tax 1.2
;;     :credits 1.2
;;     :total_cost 1.2
;;     }))

;; (try
;;   (f)
;;      (catch BatchUpdateException se (str (.getNextException se)))
;;      (finally (str "Release some resource")))
