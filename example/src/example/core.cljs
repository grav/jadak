(ns example.core
  (:require [reagent.core :as r]))

(defonce !app-state (r/atom nil))

(defn app []
  (let [{:keys [message]} @!app-state]
    [:div [:p (or message "hello")]
     [:button {:on-click #(-> (js/fetch "http://localhost:9876/api/hello")
                              (.then (fn [r]
                                       (.text r)))
                              (.then js/alert))}
      "Talk to server"]]))

(defn ^:dev/after-load main []
  (r/render [app] (js/document.getElementById "app")))

