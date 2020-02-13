(ns example-api.core
  (:require [jadak.jadak :as jadak]))

(defonce !state (atom nil))

(def routes
  ["/"
   [["" (jadak/resource {:methods {:get
                                   {:produces #{"text/html"}
                                    :response (fn []
                                                  (js/Promise.
                                                    (fn [res rej]
                                                      (.readFile (js/require "fs")
                                                                 "public/index.html"
                                                                 (fn [err data]
                                                                   (if err
                                                                     (rej err)
                                                                     (res data)))))))}}})]
    ["api/"
     [["hello" (jadak/resource {:methods {:get
                                          {:produces #{"text/plain"}
                                           :response (constantly "hi!")}}})]]]]])

(defn ^:dev/after-load main []
  (let [{{:keys [close]} :server} @!state]
    (when close
      (close))
    (swap! !state assoc :server (jadak/listener
                                  routes
                                  {:port 9876}))
    (js/console.log "(re)starting server")))