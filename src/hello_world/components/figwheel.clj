(ns hello-world.components.figwheel
  (:require
   [com.stuartsierra.component :as component]
   [cljs.stacktrace]
   [figwheel.main.api :as fig]))

(defrecord Figwheel [opts]
  component/Lifecycle
  (start [this]
    (println "Starting figwheel build" (:build opts))
    (fig/start {:mode               :serve
                :cljs-devtools      false
                :helpful-classpaths false}
      (:build opts))
    this)
  (stop [this]
    (println "Stopping figwheel build" (:build opts))
    (fig/stop (:build opts))
    this))

(defn figwheel [opts]
  (map->Figwheel {:opts opts}))
