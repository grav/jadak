(ns serverless.core
  (:require [jadak.jadak :as jadak]
            [clojure.pprint]))

(defonce appstate (atom {(random-uuid) {:title "hello" :done? false}}))

(defn template [body]
  (str "<html><script>function api(url){fetch(url, {method: 'POST'}).then(res => res.text()).then(body => document.getElementById('app').innerHTML=body)}</script><div id='app'>"
    body
      "</div>New: <input id='new'><button onclick='api(\"/app/new/\" + document.getElementById(\"new\").value)'>Submit</button></html>"))

(defn render [items]
  (str "<ol>"
       (->> (for [[id {:keys [title done]}] items]
              (str "<li><input type='checkbox'" (when done " checked") " onclick='api(\"/app/done/" (str id) "\")'>" title "</li>"))
            (clojure.string/join "\n"))
       "</ol>"))


(def routes ["/" [["app"
                   [["" (jadak/resource {:methods {:get {:produces #{"text/html"}
                                                         :response (fn [_] (template
                                                                             (render @appstate)))}}})]
                    [["/new/" :title] (jadak/resource {:methods {:post {:produces #{"text/html"}
                                                                        :response (fn [{{{:keys [title]} :route-params} :request}]
                                                                                    (swap! appstate assoc (random-uuid) {:title title
                                                                                                                         :date (js/Date.)
                                                                                                                         :done false})
                                                                                    (render @appstate))}}})]

                    [["/done/" :id] (jadak/resource {:methods {:post {:produces #{"text/html"}
                                                                      :response (fn [{{{:keys [id]} :route-params} :request}]
                                                                                  (swap! appstate update-in [(uuid id) :done] not)
                                                                                  (render @appstate))}}})]]]]])

(defn -main []
  (println "Starting server")
  (jadak/listener routes {:port 6789}))

