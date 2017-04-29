(ns sandy.config
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [sandy.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[sandy started successfully using the development profile]=-"))
   :middleware wrap-dev})
