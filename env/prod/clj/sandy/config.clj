(ns sandy.config
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[sandy started successfully]=-"))
   :middleware identity})
