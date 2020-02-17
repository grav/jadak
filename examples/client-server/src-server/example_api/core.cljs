(ns example-api.core
  (:require [jadak.jadak :as jadak]))

(defonce !state (atom nil))

(defn file [path mimetype]
  (jadak/resource {:methods {:get
                             {:produces #{mimetype}
                              :response (fn []
                                          (js/Promise.
                                            (fn [res rej]
                                              (.readFile (js/require "fs")
                                                         (str "public/" path)
                                                         (fn [err data]
                                                           (if err
                                                             (rej err)
                                                             (res data)))))))}}}))

(def routes
  ["/"
   [["" (file "index.html" "text/html")]
    ["stylesheet.css" (file "stylesheet.css" "text/css")]
    ["js/"
     [["main.js" (file "js/main.js" "application/javascript")]]]
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