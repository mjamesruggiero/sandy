(ns sandy.routes.upload
  (:require [compojure.core :refer [defroutes GET POST]]
            [ring.util.response :refer [file-response redirect]]
            [sandy.layout :as layout]
            [sandy.aws.ec2 :as ec2]
            [sandy.aws.costs :as costs]
            [clojure.tools.logging :as log])
  (:import [java.io File FileInputStream FileOutputStream]))

(def resource-path "/tmp/")

(defn file-path [path & [filename]]
  (java.net.URLDecoder/decode
   (str path File/separator filename)
   "utf-8"))

(defn upload-file
  "Uploads a file to the target folder;
  when :create-path? flag is set to true
  then the target path will be created"
  [path {:keys [tempfile size filename]}]
  (try
    (with-open [in (new FileInputStream tempfile)
                out (new FileOutputStream (file-path path filename))]
      (let [source (.getChannel in)
            dest (.getChannel out)]
        (.transferFrom dest source 0 (.size source))
        (.flush out)))))

(defn generate-instance-snapshot
  []
  (ec2/mk-instance-snapshot->future))

(defn handle-csv
  [filepath]
  (future (costs/csv->database-rows filepath)))

(defroutes upload-routes
  (GET "/upload" []
    (layout/render "upload.html"))
  (POST "/upload" [file]
    (upload-file resource-path file)
    (log/debug (str "the file is " (str resource-path (:filename file))))
    (handle-csv (str resource-path (:filename file)))
    (redirect "/"))
  (POST "/generate" []
    (generate-instance-snapshot)
    (redirect "/"))
  (GET "/files/:filename" [filename]
    (file-response (str resource-path filename))))
