(ns sandy.routes.home
  (:require [sandy.layout :as layout]
            [sandy.db.core :refer [find-instance-snapshot
                                   find-cost-snapshot
                                   most-recent-instance-snapshot]]
            [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]))

(defn home-page []
  (layout/render
    "home.html" {:results (most-recent-instance-snapshot)}))

(defn instance-snapshots-page [id rows]
  (layout/render
   "instance-snapshots.html" {:id id :snapshot-rows rows}))

(defn cost-snapshots-page [id rows]
  (layout/render
   "cost-snapshots.html" {:id id :snapshot-rows rows}))

(defn about-page []
  (layout/render "about.html"))

(defn- fetch-instance-snapshot
  [id]
  (let [parsed-id (Integer/parseInt id)]
      (if-let [snapshot-rows (find-instance-snapshot {:id parsed-id})]
        (instance-snapshots-page id snapshot-rows))))

(defn- fetch-cost-snapshot
  [id]
  (let [parsed-id (Integer/parseInt id)]
    (if-let [snapshot-rows (find-cost-snapshot {:id parsed-id})]
      (cost-snapshots-page id snapshot-rows))))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/instances/:id" [id] (fetch-instance-snapshot id))
  (GET "/costs/:id" [id] (fetch-cost-snapshot id))
  (GET "/about" [] (about-page)))

