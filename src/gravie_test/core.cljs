(ns gravie-test.core
  (:require
    [reagent.core :as r]
    [reagent.dom :as rd]))


(defonce state (atom {:text "Hello world!"}))

(defn root-ui []
  [:h1 "hello world5"])

(rd/render [root-ui] (js/document.querySelector "#app"))