(ns sandy.routes.home
  (:require [sandy.layout :as layout]
            [sandy.db.core :refer [find-instance-snapshot]]
            [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]))

(defn home-page []
  (layout/render
    "home.html" {:docs (-> "docs/docs.md" io/resource slurp)}))

(defn instance-snapshots-page [id rows]
  (layout/render
   "instance-snapshots.html" {:id id :snapshot-rows rows}))

(defn about-page []
  (layout/render "about.html"))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/instances/:id" [id]
    (let [parsed-id (Integer/parseInt id)]
      (if-let [snapshot-rows (find-instance-snapshot {:id parsed-id})]
        (instance-snapshots-page id snapshot-rows))))
  (GET "/about" [] (about-page)))

