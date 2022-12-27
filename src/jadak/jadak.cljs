(ns ^:figwheel-always jadak.jadak
  (:require [bidi.bidi]
            [clojure.string :as s]
            [cljs.reader]
            [goog.crypt.base64]
            [http]))

;; verify - authentication

(defn- decode-auth-header [auth-header]
  (let [[_ b64] (re-matches #"Basic (.+)" auth-header)
        [_ username password] (->> (goog.crypt.base64/decodeString b64)
                                   (re-matches #"(.+):(.+)"))]
    [username password]))

(defmulti verify
          "Multimethod that allows new schemes to be added."
          (fn [_ctx {:keys [scheme]}]
            scheme))

(defmethod verify nil
  [_ctx _access-control]
  (js/Promise.resolve nil))

(defmethod verify :cookie
  [{cookie-map :cookies}
   {{cookie-name :cookie} :authorization
    verify-fn             :verify}]
  (-> (verify-fn (get cookie-map cookie-name))
      js/Promise.resolve))

(defmethod verify "Basic"
  [{{{auth-header "authorization"} :headers}        :request
    {verify-fn :verify}                             :access-control}
   _]
  (js/Promise.resolve (and auth-header
                           (verify-fn (decode-auth-header auth-header)))))
;; validate - authorization

(defmulti validate
          (fn [_ctx _credentials {:keys [scheme] :as _auth}]
            scheme))

(defmethod validate nil
  [{{:keys [method]} :request
    :as              _ctx}
   {:keys [roles]
    :as   _creds}
   {{role method} :methods
    :as           _auth}]
  (when roles
    (roles role)))

(defn handle-auth [{{:keys [method]}       :request
                    {:keys [authorization]
                     :as   access-control} :access-control
                    :as                    ctx}]
  (if (= method :options)
    (js/Promise.resolve ctx)
    (-> (verify ctx access-control)
        (.then (fn [credentials]
                 (cond-> ctx
                         (and authorization
                              (not (validate ctx credentials authorization)))
                         (assoc
                           :response (if credentials
                                       {:status 403
                                        :body   "forbidden"}
                                       {:status 401
                                        :body   "unauthorized"}))))))))

(def resource constantly)

(defn as-resource [v]
  (resource {:methods
             {:get {:produces #{"text/plain"}
                    :response (constantly v)}}}))

(defn parse-accept [a]
  (for [s (clojure.string/split a #",\s?")]
    (let [[_ mime sub-mime _ q] (re-matches #"(.+)/([^;]+)(;(.+))?" s)]
      [mime sub-mime q])))

(defn- parse-body [s content-type]
  (when-not (empty? s)
    (let [f (case content-type
              "application/json" #(-> (js/JSON.parse %)
                                      (js->clj :keywordize-keys true))
              "application/edn" cljs.reader/read-string
              identity)]
      (f (.toString s "utf-8")))))


;; This is used to discern between Ring responses and maps that look like Ring responses (eg {:status 200, :body "hello"})
;; It seems to be how yada does it, albeit indirectly
(defrecord Response [status headers body])

(defn- cookie-str->map [cookie-str]
  (->> (clojure.string/split cookie-str #";\s?")
       (map #(clojure.string/split % #"="))
       (into {})))
(defn process-request-parameters [{:keys                 [headers path query body] :as request} routes]
  (let [{handler-fn   :handler
         route-params :route-params
         :or          {handler-fn (constantly {:response {:status 204
                                                          :body   nil}})}} (bidi.bidi/match-route routes path)
        {{path-params :path} :parameters
         access-control      :access-control
         :as                 ctx} (handler-fn)]
    (assoc ctx
      :resource {:access-control
                 {:realms {"default" access-control}}}
      :cookies (when-let [cookie-str (or (get headers "cookie")
                                         (get headers "Cookie"))]
                 (cookie-str->map cookie-str))
      :request (merge request
                      {:route-params route-params})
      :parameters {:path  (->> (for [[k v] route-params]
                                 (let [f (or (get path-params k) identity)]
                                   [k (f v)]))
                               (into {}))
                   :query query}
      :body (parse-body body request))))

(defn can-produce [produces [mime sub-mime]]
  (or (produces (str mime "/" sub-mime))
      (and (= sub-mime "*")
           (->> produces
                (filter #(= (second (re-matches #"(.+)/.*" %)) mime))
                first))
      (and (= "*" mime sub-mime)
           (first produces))
      nil))

(defn accept->content-type [produces accept]
  (->> (parse-accept accept)
       (keep (partial can-produce produces))
       first))

(defn accepts-text-plain? [accept]
  (some? (accept->content-type #{"text/plain"} accept)))

(defn is-origin-allowed [origin allowed]
  (cond
    (= "*" allowed)
    origin

    (string? allowed)
    (and (= origin allowed) origin)

    (ifn? allowed)
    (allowed origin)))

(defn- allowed-methods [methods]
  (->> methods
       keys
       (concat [:options
                (when (:get methods)
                  :head)])
       (remove nil?)
       sort
       (map (comp s/upper-case name))
       (s/join ", ")))

(defn handle-client-errors [{response                              :response
                             {:keys                         [method headers]
                              body                          :body} :request
                             methods                               :methods
                             default-produces :produces
                             :as                                   ctx}]
  (assoc ctx :response
             (or response
                 (let [accept (or (get headers "accept")
                                  (get headers "Accept")
                                  "*/*")
                       content-type (or (get headers "content-type")
                                        (get headers "Content-Type"))
                       {response-fn :response
                        produces    :produces
                        consumes    :consumes
                        :or         {produces (or default-produces #{})
                                     consumes #{}}} (get methods method)
                       produce-content-type (accept->content-type produces accept)]
                   (cond (and (not (empty? body))
                              (not (consumes content-type)))
                         {:status 415
                          :body   (str "Cannot consume " (pr-str content-type) body)}

                         (and (= method :options)
                              (not (get methods :options)))
                         {:status  200
                          :body    ""
                          :headers {"allow" (allowed-methods methods)}}

                         (and (nil? response-fn)
                              (not= method :options))

                         {:status 405
                          :body   (str "no method for " (clojure.string/upper-case (name method)) "\n")
                          ;; extra service for this specific error
                          :headers {"allow" (allowed-methods methods)}}

                         (and (nil? produce-content-type)
                              (not (#{:head :options} method)))
                         {:status 406
                          :body   (str "Cannot accept " (pr-str accept))})))))

(defn is-stream? [v]
  (instance? (.-Readable (js/require "stream")) v))

(defn handle-request [{response                              :response
                       {:keys                         [method]
                        {accept       "accept"
                         content-type "content-type"
                         origin       "origin"
                         :or          {accept "*/*"}} :headers
                        body                          :body} :request
                       methods                               :methods
                       {allow-origin :allow-origin
                        :as          access-control}         :access-control
                       :as                                   ctx}]
  (-> (if response
        (-> response
            (update :body (if (accepts-text-plain? accept)
                            identity
                            (constantly "")))
            map->Response
            js/Promise.resolve)
        (let [{response-fn :response
               produces    :produces
               consumes    :consumes
               :or         {produces #{}
                            consumes #{}}} (get methods method)
              produce-content-type (accept->content-type produces accept)]
          (-> (response-fn (assoc ctx :body (parse-body body content-type)
                                      :response (map->Response {:status 200})))
              js/Promise.resolve
              (.then (fn [response]
                       (let [{:keys [headers body]
                              :as   response} (if (instance? Response response)
                                                response
                                                (map->Response {:body response
                                                                :status 200}))]
                         (assoc response
                           :headers (merge (when produce-content-type
                                             {"content-type" produce-content-type})
                                           headers)
                           :body (cond
                                   (or
                                     (is-stream? body)
                                     (string? body))
                                   body

                                   :else
                                   (str
                                     (if (= produce-content-type "application/json")
                                       (js/JSON.stringify (clj->js body
                                                                   :keyword-fn (fn [k]
                                                                                 (str (when (namespace k)
                                                                                        (str (namespace k)
                                                                                             "/"))
                                                                                      (name k)))))
                                       body)
                                     "\n"
                                     (when (= produce-content-type "text/plain")
                                       "\n"))))))))))
      ;; todo - move this out into a `handle-cors` stage
      (.then (fn [{:keys [headers]
                   :as   response}]
               (assoc response :headers
                               (merge headers
                                      (when-let [o (and origin
                                                        (is-origin-allowed origin allow-origin))]
                                        {"access-control-allow-origin" o})
                                      (when origin
                                        (->> (for [[k v] (dissoc access-control :allow-origin)
                                                   :when (or (boolean? v)
                                                             (string? v))]
                                               [(str "access-control-" (name k)) v])
                                             (into {})))))))))

(defn url->query-params [qs]
  (let [[_ q] (re-matches #".+\?(.+)" qs)]
    (->> q
         (#(clojure.string/split % #"&"))
         (map #(clojure.string/split % #"="))
         (map (fn [[k v]]
                [k (js/decodeURIComponent v)]))
         (into {}))))

(defn x-www-form-urlencoded [m]
  (->> (for [[k v] m]
         (str (name k) "=" (js/encodeURIComponent v)))
       (clojure.string/join "&")))

(defn response-for
  [routes
   method
   path
   {:keys [headers body]}
   {:keys [access-control]}]
  (-> (process-request-parameters {:path    path
                                   :query   (url->query-params path)
                                   :headers headers
                                   :body    body
                                   :method  method} routes)
      (handle-auth)
      (.then handle-client-errors)
      (.then handle-request)
      (.then (fn [{:keys [status body]}]
               {:status status
                :body   body}))))

(defn parse-http-request [req]
  (js/Promise.
    (fn [resolve _reject]
      (let [body #js []]
        ^js (.on req "data"
                 (fn [chunk]
                   (.push body chunk)))
        ^js (.on req "end"
                 (fn []
                   (let [req-map {:headers (js->clj (.-headers req))
                                  :path    (.-url req)
                                  :query   (url->query-params (.-url req))
                                  :method  (keyword (clojure.string/lower-case (.-method req)))
                                  :body    (.toString (js/Buffer.concat body))}]
                     (resolve req-map))))))))

(defn listener [routes {:keys [port]}]
  (let [s (http/createServer (fn [req res]
                               (-> (parse-http-request req)
                                   (.then #(process-request-parameters % routes))
                                   (.then #(handle-auth %))
                                   (.then handle-client-errors)
                                   (.then handle-request)
                                   (.then (fn [{:keys [status body headers]}]
                                            (if (is-stream? body)
                                              (do
                                                (let [s body]
                                                  (.on s "open" (fn []
                                                                  ^js (.writeHead res
                                                                                  status
                                                                                  (clj->js headers))
                                                                  (.pipe s res)))
                                                  (.on s "error" (fn [error]
                                                                   (println 'error error)
                                                                   (set! (.-statusCode res) 500)
                                                                   (.end res "error!")))))

                                              (do
                                                ^js (.writeHead res
                                                               status
                                                               (clj->js (merge headers
                                                                               (when body {"content-length" (js/Buffer.byteLength body)}))))
                                                (when (seq body)
                                                  (.write res body))
                                                (.end res)))))
                                   (.catch (fn [ex]
                                             (js/console.error ex)
                                             (let [body (str (.-message ex) "\n")]
                                               ^js (.writeHead res
                                                              500
                                                              (clj->js {"content-length" (js/Buffer.byteLength body)}))
                                               (.write res body)
                                               (.end res)))))))]
    (.listen s port)
    {:server s
     :close  (fn [] (.close s))
     :port   port}))

