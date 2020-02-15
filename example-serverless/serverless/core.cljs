(ns serverless.core
  (:require [jadak.jadak :as jadak]))

(def routes ["/" [["app" (jadak/resource {:methods {:get {:produces #{"text/html"}
                                                          :response (constantly "<div>hello</div>")}}})]]])

(defn -main []
  (println "Starting server")
  (jadak/listener routes {:port 6789}))

