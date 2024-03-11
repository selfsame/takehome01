(ns gravie-test.core
  (:require
    [reagent.core :as r]
    [reagent.dom :as rd]
    [ajax.core :refer [GET POST]]
    [cljs.pprint :as pprint]))

(declare search!)

(defonce state (r/atom {
  :view :search
  :cart #{}
  :working false
  :search-term nil
  :page 0
  :search-results nil
  :games/by-id {}}))

(add-watch state :page-change
  (fn [k r o n]
    (when (not= (:page o) (:page n))
      (search! (:search-term @state)))))

(defn price [game]
  (let [digits (str (js/Math.abs (hash (:name game))))]
    (apply str (flatten [(take 1 digits) "." (take 2 (drop 1 digits))]))))

(defn search! [term]
  (swap! state assoc :working true)
  (GET (str "api/search?filter=name:" term 
    "&offset=" (* (:page @state) 10)
    "&field_list=id,name,image,deck,original_release_date&limit=10&sort=original_release_date:desc")
    :response-format :json
    :keywords? true
    :handler (fn [res] 
               ;TODO store in games/by-id
               (swap! state update :games/by-id merge 
                 (into {} (map (juxt :id identity) (:results res))))
               (swap! state assoc :search-results res)
               (swap! state assoc :working false))))

(defn header-ui []
  (let [cnt (count (:cart @state))]
    [:div.header
      [:div.section
        [:div.logotype {:on-click (fn [e] (swap! state assoc :view :search))} 
          "GameRenter"]
        [:div.cart {:on-click (fn [e] (swap! state assoc :view :checkout))} 
          "Checkout" (when (pos? cnt) (str " (" cnt ")"))]]]))

(defn game-ui [o]
  (let [rented? (contains? (:cart @state) (:id o))]
    [:div.game
      [:div.icon 
        [:img {:src (-> o :image :medium_url)}]]

      [:div.info 
        [:p.name (:name o)]
        [:p.date (re-find #"^\d+" (or (:original_release_date o) ""))]
        [:p.description (:deck o)]]
      [:div.controls
        [:div.button 
          {:class (if rented? "remove")
            :on-click (fn [e] (swap! state update :cart (if rented? disj conj) (:id o)))}
          (if rented? 
            "remove from cart"
            (str "rent for $" (price o)))]]]))

(defn pagination []
  (let [page-cnt (js/Math.ceil (/ (:number_of_total_results (:search-results @state)) 10))]
    [:div.pagination
      (if (= (:page @state) 0)
        [:div.arrow.disabled "<"]
        [:div.arrow 
          {:on-click (fn [e] (swap! state update :page dec))}
          "<"])
      [:div "page " (inc (:page @state)) " of " page-cnt]
      (if (>= (inc (:page @state)) page-cnt)
        [:div.arrow.disabled ">"]
        [:div.arrow 
          {:on-click (fn [e] (swap! state update :page inc))}
          ">"])]))

(defn search-ui []
  (let [search-fn (fn [] 
                    (swap! state assoc :page 0)
                    (search! (:search-term @state)))]
    [:div.section
      [:div.search
        [:input 
        {:value (:search-term @state)
          :on-change (fn [e] (swap! state assoc :search-term (-> e .-target .-value )))
          :on-key-up (fn [e] (when (or (= (.-key e) "Enter")
                                      (= (.-keyCode e) 13))
                              (search-fn)))}]
        [:div.button {:on-click (fn [e] (search-fn))} "SEARCH"]]
      (if (:working @state)
        [:h1 "working.."]
        [:div
          [pagination]
          (let [games (-> @state :search-results :results)]
            (if (empty? games)
              [:h1 "No Results"]
              (into [:div.results]
                (map game-ui games))))
          [pagination]])]))

(defn cart-item [o]
  [:div.cart-item.flexrow
    [:p [:button {:on-click (fn [e] (swap! state update :cart disj (:id o)))} "x"]
        (:name o)]
    [:p "$" (price o)]])

(defn checkout-ui []
  (let [games (map #(get-in @state [:games/by-id %]) (:cart @state))]
    [:div.section.checkout
      [:h3 "Checkout"]
      (into [:div.items]
        (map cart-item games))
      [:div.flexrow
        [:p "TOTAL"]
        [:p (str "$" (reduce + (map (comp js/parseFloat price) games)))]]
      [:div.button 
        {:on-click (fn [e] (swap! state merge {:cart #{} :view :complete}))}
        "CHECKOUT"]]))

(defn complete-ui []
  [:div.section
    [:h1 "Thank You!"]
    [:div.button {:on-click (fn [e] (swap! state assoc :view :search))} "back to search"]])

(defn root-ui []
  [:div
    [header-ui]
    (case (:view @state)
      :search [search-ui]
      :checkout [checkout-ui]
      :complete [complete-ui]
      [:p "unknown view: " (:view @state)])])

(rd/render [root-ui] (js/document.querySelector "#app"))

(defonce _
  (search! ""))