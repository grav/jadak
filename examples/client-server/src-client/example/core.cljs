(ns example.core
  (:require [reagent.core :as r]))

(defonce !app-state (r/atom nil))

(defn app []
  (let [{:keys [message]} @!app-state]
    [:div [:p (or message "hello")]
     [:input {:on-click #(-> (js/fetch "/api/hi"
                                       #js{:method "POST"
                                           :body (.-value (.-target %))})
                             (.then (fn [response]
                                      (.text response)))
                             (.then js/alert))}]
     [:button {:on-click #(-> (js/fetch "/api/hello")
                              (.then (fn [r]
                                       (.text r)))
                              (.then js/alert))}
      "Talk to the server"]]))

(defn ^:dev/after-load main []
  (r/render [app] (js/document.getElementById "app")))

