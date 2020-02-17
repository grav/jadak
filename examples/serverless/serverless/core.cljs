(ns serverless.core
  (:require [jadak.jadak :as jadak]
            [jadak.serverless]))

(defonce appstate (atom {(random-uuid) {:title "buy tickets" :done? false}}))

(defn template [body]
  (str "<html>
  <script>
    function api(url, body){
      fetch(url, {body: body, method: 'POST'})
      .then(res => res.text())
      .then(body => document.getElementById('app').innerHTML=body)
    }
  </script>
  <h2>Short-term Todo</h2>
  <div id='app'>" body "</div>
  New item: <input id='new'><button onclick='api(\"api/new\", document.getElementById(\"new\").value)'>Submit</button>
  </html>"))


(defn render [items]
  (str "<ul>"
       (->> (for [[id {:keys [title done]}] (->> items
                                                 (sort-by (comp :date second)))]
              (str "<li><label><input type='checkbox'" (when done " checked") " onclick='api(\"api/done/" (str id) "\")'>" title "</label></li>"))
            (clojure.string/join "\n"))
       "</ul>"))


(def routes ["/" [["app" (jadak/resource {:methods {:get {:produces #{"text/html;charset=UTF-8"}
                                                          :response (fn [_] (template
                                                                              (render @appstate)))}}})]
                  ["api/" [["new" (jadak/resource {:methods {:post {:consumes #{"text/plain;charset=UTF-8"}
                                                                       :produces #{"text/html"}
                                                                       :response (fn [{{:keys [body]} :request}]
                                                                                   (swap! appstate assoc (random-uuid) {:title body
                                                                                                                        :date  (js/Date.)
                                                                                                                        :done  false})
                                                                                   (render @appstate))}}})]

                           [["done/" :id] (jadak/resource {:methods {:post {:produces #{"text/html"}
                                                                            :response (fn [{{{:keys [id]} :route-params} :request}]
                                                                                        (swap! appstate update-in [(uuid id) :done] not)
                                                                                        (render @appstate))}}})]]]]])

(defn -main []
  (println "Starting server")
  (jadak/listener routes {:port 6789}))

(defn aws-lambda-main [opts]
  (jadak.serverless/aws-lambda routes opts))