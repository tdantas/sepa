(ns user
  (:require
   [clojure.tools.namespace.repl :as namespace]
   [com.stuartsierra.component :as component]
   [hello-world.components.figwheel :as fig]
   [hello-world.system :as s]
   [figwheel.main.api]))

(namespace/disable-reload!)
(namespace/set-refresh-dirs "src")

(defonce *system (atom nil))

(defn stop []
  (some-> @*system (component/stop))
  (reset! *system nil))

(defn refresh []
  (let [res (namespace/refresh)]
    (when (not= res :ok)
      (throw res))
    :ok))

(defn start 
  ([] (start {}))
  ([opts]
   (let [opts' (merge-with merge {:figwheel {:build "dev"}} opts)]
       (when-some [system (-> (s/system opts')
                              (assoc :figwheel (fig/figwheel (:figwheel opts'))))]
         (when-some [system' (component/start system)]
           (reset! *system system'))))))

(defn reset []
  (stop)
  (refresh)
  (start))

(defn cljs-repl []
  (figwheel.main.api/cljs-repl "dev"))