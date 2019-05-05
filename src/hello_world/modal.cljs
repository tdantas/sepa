(ns ^:figwheel-hooks hello-world.modal
  (:require [reagent.core :as r]
            [hello-world.state :refer [state]]
            [clojure.string :as string]
            [reagent-keybindings.keyboard :as kb])) 

(defn close-modals [modal-cursor]
  (reset! modal-cursor (reduce-kv #(assoc %1 %2 false) {}  @modal-cursor)))

(defn modal [opts state-cursor title body]
  (let [show-modal @state-cursor
        shortcut (:shortcut opts)]

      [:div.modal {:role "dialog", :tabIndex "-1" :style {"display" (if show-modal "block" "none")}}
          (when shortcut [kb/kb-action shortcut #(reset! state-cursor true)])

          [:div.modal-dialog {:role "document"}
            
            [:div.modal-content
              [:div.modal-header
                [:h5.modal-title title]

                [:button.close
                  {:aria-label "Close", :data-dismiss "modal", :type "button" :on-click #(reset! state-cursor false)}
                  [:span {:aria-hidden "true"} "×"]]]
              
              [:div.modal-body 
                body]]]]))

