(ns sandy.core
  (:require [sandy.handler :refer [app init destroy]]
            [luminus.repl-server :as repl]
            [luminus.http-server :as http]
            [sandy.db.migrations :as migrations]
            [config.core :refer [env]]
            [sandy.aws.ec2 :as ec2]
            [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

(defn parse-port [port]
  (when port
    (cond
      (string? port) (Integer/parseInt port)
      (number? port) port
      :else          (throw (Exception. (str "invalid port value: " port))))))

(defn http-port [port]
  ;;default production port is set in
  ;;env/prod/resources/config.edn
  (parse-port (or port (env :port))))

(defn stop-app []
  (repl/stop)
  (http/stop destroy)
  (shutdown-agents))

(defn start-app
  "e.g. lein run 3000"
  [[port]]
  (let [port (http-port port)]
    (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app))
    (when-let [repl-port (env :nrepl-port)]
      (repl/start {:port (parse-port repl-port)}))
    (http/start {:handler app
                 :init    init
                 :port    port})))

(defn placeholder []
  (println "this is your placeholder"))

(def cli-options
  [["-c" "--config CONFIG-FILE" "EDN file with config key-value settings"]
   ["-f" "--function FUNCTION" "Choices are: instances, costs"]
   ["-p" "--port PORT" "server port"]
   ["-h" "--help"]])

(defn runner
  "takes args and decides which fn to run"
  [args]
  (let [opts (:options args)
        config (:config opts)
        operation (:function opts)
        port (:port opts)]
    (case operation
      "instances" (placeholder)
      (start-app [port]))))

(defn -main [& args]
  (let [opts (parse-opts args cli-options)]
    (runner opts)))
