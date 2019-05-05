(ns ^:figwheel-hooks hello-world.state
  (:require [reagent.core :as r]))

(defonce state (r/atom {:modals {:payment-modal false
                                 :shortcut-modal false} 
                        :payments []}))