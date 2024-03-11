(ns gravie-test.core
  (:require
    [reagent.core :as r]
    [reagent.dom :as rd]
    [ajax.core :refer [GET POST]]
    [cljs.pprint :as pprint]))

(defonce state (r/atom {
  :view :search
  :cart #{}
  :working false
  :page 0
  :search-results nil}))

(defn price [game]
  (let [digits (str (js/Math.abs (hash (:name game))))]
    (apply str (flatten [(take 1 digits) "." (take 2 (drop 1 digits))]))))

(defn search! [term]
  (swap! state assoc :working true)
  (GET (str "api/search?filter=name:" term 
    "&field_list=id,name,image,deck,original_release_date&limit=10&sort=original_release_date:desc")
    :response-format :json
    :keywords? true
    :handler (fn [res] 
               ;TODO store in games/by-id
               (swap! state assoc :search-results res)
               (swap! state assoc :working false))))

(defn header-ui []
  [:div.header
    [:div.logotype "GameRenter"]
    [:div {:on-click (fn [e] (swap! state assoc :view :search))} "Search Games"]
    [:div.cart {:on-click (fn [e] (swap! state assoc :view :checkout))} "Checkout"]])

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
  [:div.pagination
    [:div "<"]
    [:div "page " (inc (:page @state)) " of " (:number_of_total_results (:search-results @state))]
    [:div ">"]])

(defn search-ui []
  [:div.section
    [:div.search
      [:input 
       {:value (:search-term @state)
        :on-change (fn [e] (swap! state assoc :search-term (-> e .-target .-value )))
        :on-key-up (fn [e] (when (or (= (.-key e) "Enter")
                                    (= (.-keyCode e) 13))
                            (search! (:search-term @state))))}]
      [:button 
        {:on-click (fn [e] (search! (:search-term @state)))} 
        "SEARCH GAMES"]]
    (if (:working @state)
      [:h1 "working.."]
      [:div
        [pagination]
        (into [:div.results]
          (map 
            game-ui
            (-> @state :search-results :results)))
        [pagination]])])

(defn checkout-ui []
  [:div.section
    [:h3 "Checkout"]])

(defn root-ui []
  [:div
    [header-ui]
    (case (:view @state)
      :search [search-ui]
      :checkout [checkout-ui]
      [:p "unknown view: " (:view @state)])
    ;[:pre (with-out-str (pprint/pprint @state))]
    ])

(rd/render [root-ui] (js/document.querySelector "#app"))

(defonce _
  (search! ""))