(ns ^:figwheel-hooks hello-world.payments
  (:require [reagent.core :as r]
            ["sepa" :as sepa]
            [hello-world.modal :refer [close-modals modal]]
            [hello-world.state :refer [state]]
            [clojure.string :as string]
            [goog.labs.format.csv :as csv]
            [cljs-drag-n-drop.core :as dnd]
            [goog.string.format :as format]  
            [clojure.string :as string]
            [reagent-keybindings.keyboard :as kb])) 

(defn iban-valid? [value]
 (if (sepa/validateIBAN value) 
  [true]
  [false "invalid iban"]))
 
(defn required [value]
  (if (string/blank? value) 
    [false "can’t be blank"] 
    [true]))

(defn on-change [form-state]
  (fn [event] 
    (let [value  (-> event .-target .-value)]
      (swap! form-state (fn [current]
                          (-> current
                              (assoc-in [:meta :pristine] false)
                              (assoc :value value)))))))
 
(defn validate [field value]
  (let [validations (get-in @field [:meta :validation :validations])]
    (swap! field 
           assoc-in [:meta :validation :errors] 
           (filter identity (mapcat (fn [validation-fn]
                                      (let [[valid msg] (validation-fn value)]
                                          (when-not valid msg)))
                                    validations)))))

(defn pristine? [field]
  (get-in @field [:meta :pristine]))

(defn class-valid? [field]
 (when-not (pristine? field)
  (if (not-empty (get-in @field [:meta :validation :errors] )) "is-invalid" "is-valid")))

(defn errors [field]  
  (get-in @field [:meta :validation :errors]))

(defn valid? [field]
  (nil? (not-empty (errors field))))

(defn field-event [field]  
  {:class     (when-not (pristine? field) (if (valid? field) ".is-valid" "is-invalid"))
   :on-blur   (fn [event] (when-not (pristine? field) (validate field (-> event .-target .-value))))
   :on-change (on-change field)})

(defonce form-fields {  :iban          {:value nil :meta { :pristine true :validation {:errors []  :validations  [iban-valid?]}}}      
                        :amount        {:value nil :meta { :pristine true :validation {:errors []  :validations  [required]}}}
                        :bic           {:value nil :meta { :pristine true :validation {:errors []  :validations  [required]}}}
                        :invoice-ref   {:value nil :meta { :pristine true :validation {:errors []  :validations  [required]}}}
                        :to-company    {:value nil :meta { :pristine true :validation {:errors []  :validations  [required]}}}}) 

(def form-state (r/atom form-fields))   

(defn form-valid? [form]
  (reduce-kv (fn [acc _ value] (and acc (empty? (get-in value [:meta :validation :errors])))) true @form))

(defn form-pristine? [form]
  (reduce-kv (fn [acc _ value] (and acc (get-in value [:meta :pristine]))) true @form))

(defn form-submit [event form]
    (.preventDefault event)
    (when (form-valid? form)
      (let [{:keys [iban to-company amount invoice-ref bic]} @form]
        (swap! state (fn [{:keys [payments] :as state}]
                       (assoc state :payments (into payments 
                                                [{:iban        (:value iban) 
                                                  :bic         (:value bic)
                                                  :invoice-ref (:value invoice-ref) 
                                                  :amount      (:value amount) 
                                                  :to-company  (:value to-company)}]))))
        (reset! form form-fields))))

(defn payment-form []
  (let [form       form-state
        iban       (r/cursor form [:iban])
        name       (r/cursor form [:to-company])
        bic-swift  (r/cursor form [:bic])
        ref        (r/cursor form [:invoice-ref])
        amount     (r/cursor form [:amount])
        #_#_res    (r/atom nil)]

    (fn []
      [:form {:on-submit #(form-submit %1 form)}

        #_[:div.form-group.mb-2.mr-sm-2
            [:input#iban-tester.form-control.bg-gradient-danger 
              {:placeholder "IBAN-tester" :type "text" 
               :on-change #(reset! res (iban-valid? (-> % .-target .-value)))}]       
            [:small.form-text.text-danger {} 
              (or (apply str (second @res)) "1")]]
        
        [:div.form-group.mb-2.mr-sm-2
          [:input#iban.form-control
            (merge {:placeholder "IBAN" :type "text" :value (:value @iban)} (field-event iban))]       
          (when-not (valid? iban)
            [:small.form-text.text-danger (apply str (errors iban))])]

        [:div.form-group.mb-2.mr-sm-2
          [:input#ref.form-control
            (merge {:placeholder "BIC/Swift" :type "text" :value (:value @bic-swift)} (field-event bic-swift))]       
          (when-not (valid? bic-swift)
            [:small.form-text.text-muted 
                (apply str (errors bic-swift))])]  

        [:div.form-group.mb-2.mr-sm-2
          [:input#name.form-control
            (merge {:placeholder "Name" :type "text" :value (:value @name)} (field-event name))]       
          (when-not (valid? name)
            [:small.form-text.text-muted 
                (apply str (errors name))])] 

        [:div.form-group.mb-2.mr-sm-2
          [:input#value.form-control
            (merge {:placeholder "Amount" :type "text" :value (:value @amount)} (field-event amount))]       
          (when-not (valid? amount)
            [:small.form-text.text-muted 
                (apply str (errors amount))])]   

        [:div.form-group.mb-2.mr-sm-2
          [:input#ref.form-control
            (merge {:placeholder "Ref" :type "text" :value (:value @ref)} (field-event ref))]       
          (when-not (valid? ref)
            [:small.form-text.text-muted 
                (apply str (errors ref))])]                                                

        [:button.btn.btn-success.mb-2.col-12 
          {:type "submit" 
           :disabled (or (form-pristine? form) 
                         (not (form-valid? form)))} "Add"]]))) 

(defn payments-table [payments on-delete]
  (let [total (reduce (fn [total item] (+ total (js/parseFloat (string/replace (:amount item) #"," ".")))) 0 payments)] 
    [:div 
      [:table.table.table-striped.table-bordered
        [:thead
          [:tr
            [:th {:scope "col"} "IBAN"]
            [:th {:scope "col"} "BIC/Swift"]
            [:th {:scope "col"} "Company"]
            [:th {:scope "col"} "Amount"]
            [:th {:scope "col"} "Invoice"]
            [:th.text-center {:scope "col" :colspan 2} "#"]]]
        [:tbody 
          (for [{:keys [iban to-company amount invoice-ref bic] :as entry} payments] 
            ^{:key (str invoice-ref)} [:tr
                                        [:td iban] 
                                        [:td bic] 
                                        [:td to-company] 
                                        [:td amount] 
                                        [:td invoice-ref]
                                        [:td
                                          [:button.btn.btn-info.btn-sm.w-100 {:on-click #(on-delete entry)} "Edit"]]
                                        [:td  
                                          [:button.btn.btn-danger.btn-sm.w-100 {:on-click #(on-delete entry)} "Delete"]]])]
        [:tfoot
          [:tr
            [:th]
            [:th]
            [:th "Total"]
            [:td (.toFixed total 2)]
            [:th]
            [:th]]]]]))

(defn payments-container []
    [:div.col-12
      [:div.col-12
        (if (empty? (:payments @state))
          [:div]
          (list 
                [:div.row.mb-5
                  [:button.btn.btn-primary.btn-block {:on-click #(reset! (r/cursor state [:modals :payment-modal]) true)} "+ Payment"]]
                
                [payments-table (:payments @state) (fn [entry]
                                                    (swap! state assoc :payments (into [] (filter #(not= entry %) (:payments @state)))))]))]])      

