(ns jadak.serverless
  (:require [jadak.jadak :as jadak]
            [clojure.string]
            [clojure.pprint]))

(defn lambda-proxy-event->jadak-ctx [event]
  (let [{method "httpMethod"
         path "path"
         query nil #_"queryStringParameters" ;; TODO
         headers "headers"
         body "body"} event]
    {:headers headers
     :path    path
     :query   query
     :method  (-> method clojure.string/lower-case keyword)
     :body    body}))

(defn jadak-response->lambda-response [{:keys [body status headers]}]
  (println 'jadak-response->lambda-response)
  (-> {:body body
       :statusCode status
       :headers headers}
      clj->js))


(defn aws-lambda [routes {:keys [event _context]}]
      (-> (js/Promise.resolve (lambda-proxy-event->jadak-ctx event))
          (.then #(jadak/process-request-parameters % routes))
          (.then jadak/handle-auth)
          (.then jadak/handle-client-errors)
          (.then jadak/handle-request)
          (.then jadak-response->lambda-response)
          #_(.then (fn [response]
                     #_(.end (:pg-client @api/state))
                     (cb nil response)))))