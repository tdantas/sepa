(ns hello-world.drag-and-drop
  (:require [reagent.core :as r]
            ["sepa" :as sepa]
            [hello-world.modal :refer [close-modals modal]]
            [hello-world.payments :refer [payments-container payment-form]]
            [hello-world.state :refer [state]]
            [clojure.string :as string]
            [goog.labs.format.csv :as csv]
            [cljs-drag-n-drop.core :as dnd]
            [goog.string.format :as format]  
            [clojure.string :as string]
            [reagent-keybindings.keyboard :as kb])) 

(defn process-file [content]
 (let [parsed (drop 1 (csv/parse content false \tab))]
   (mapv #(zipmap [:iban :bic :amount :to-company :from-company :invoice-ref] %) parsed)))

(defn uploaded [files]
  (when-some [file (when (> (alength files) 0) (aget files 0)
                                              (let [file-reader (js/FileReader.)
                                                    _           (set! (.-onload file-reader) #(swap! state assoc :payments (process-file (.-result file-reader))))]
                                                (.readAsText file-reader (aget files 0))))]))
(defn drag-and-drop []
 (let [file-content (r/atom nil)]
  (r/create-class 
      {:component-will-mount (fn [] 
                              (dnd/subscribe! js/document.documentElement ::picture
                                  {:start (fn [_] 
                                            (let [class-list (-> (js/document.querySelector ".picture")
                                                                 (.-classList))]
                                                (.add class-list "block")
                                                (.remove class-list "none")))

                                   :drop  (fn [e files]
                                            (when-some [file (when (> (alength files) 0) (aget files 0))]
                                              (let [file-reader (js/FileReader.)
                                                    _           (set! (.-onload file-reader) #(swap! state assoc :payments (process-file (.-result file-reader))))]
                                                (.readAsText file-reader (aget files 0)))))

                                   :end   (fn [_]
                                            (let [class-list (-> (js/document.querySelector ".picture")
                                                                 (.-classList))]
                                                (.add class-list "none")                  
                                                (.remove class-list "block")))}))

        :component-will-unmount (fn [] 
                                  (dnd/unsubscribe! js/document.documentElement ::picture)  
                                  (js/console.log "component will unmount"))
        :reagent-render (fn []
                          [:div.col-12 {:style {"height" "100px"}}
                            [:div.dropzone.none.picture]
                            [:div.border.col-md-12.h-100.d-inline-block.bg-light.text-center.align-middle {:on-click (fn [e] (.click (js/document.querySelector "#picture")))} [:span.oi.oi-cloud-upload.mt-3]]
                            [:input#picture 
                                  {:style { "display" "none"}
                                   :accept  "text/*"
                                   :type    "file"
                                   :on-change (fn [e]
                                               (let [files (-> e .-currentTarget .-files)]
                                                 (uploaded files)))}]])})))
