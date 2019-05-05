(ns ^:figwheel-hooks hello-world.core
  (:require [reagent.core :as r]
            ["sepa" :as sepa]
            [hello-world.modal :refer [close-modals modal]]
            [hello-world.payments :refer [payments-container payment-form]]
            [hello-world.drag-and-drop :refer [drag-and-drop]]
            [hello-world.state :refer [state]]
            [clojure.string :as string]
            [goog.labs.format.csv :as csv]
            [cljs-drag-n-drop.core :as dnd]
            [goog.string.format :as format]  
            [clojure.string :as string]
            [reagent-keybindings.keyboard :as kb])) 

(enable-console-print!)

(defn url [content type]
  (let [blob (js/Blob. content {:type type})]
    (js/webkitURL.createObjectURL blob)))
  
(defn download-link [filename]
  (let [payments (:payments @state)
        href (url payments "text/csv")]
    [:a {:download filename} 
        :href href 
        :target "_self"]
    "Download"))

(defn app []
   (fn []
    [:div.container
      [kb/keyboard-listener]

      [:div.row.mt-3 
        [drag-and-drop]] 

      [:div.modals
        [kb/kb-action "esc" #(close-modals (r/cursor state [:modals]))]
        
        [modal {:shortcut "ctrl-/"} (r/cursor state [:modals :shortcut-modal])
          "Shortcuts"
          [:p "shortcut modal"]]

        [modal {:shortcut "ctrl-a"} (r/cursor state [:modals :payment-modal])
          "Add new Payment"  
          [payment-form]]]

      #_[:div.row.mt-5
         [:button.btn.btn-primary.btn-block {:on-click #(reset! (r/cursor state [:modals :payment-modal]) true)} "NEW PAYMENT"]]
      
      [:div.row.mt-5
        [payments-container]]

      #_[:div.row 
         [download-link "filename.csv" "Download"]]]))

(defn ^:export render []
  (r/render [app]
            (js/document.getElementById "app")))

(defn ^:after-load re-render []
  (render))

(defonce start-up (do (render) true))
