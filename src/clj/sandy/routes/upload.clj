(ns sandy.routes.upload
  (:require [compojure.core :refer [defroutes GET POST]]
            [ring.util.response :refer [file-response redirect]]
            [sandy.layout :as layout])
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

(defroutes upload-routes
  (GET "/upload" []
    (layout/render "upload.html"))
  (POST "/upload" [file]
    (upload-file resource-path file)
    (redirect (str "/files/" (:filename file))))
  (GET "/files/:filename" [filename]
    (file-response (str resource-path filename))))
