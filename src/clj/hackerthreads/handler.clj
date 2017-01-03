(ns hackerthreads.handler
  (:require [compojure.core :refer [GET POST defroutes]]
            [compojure.route :refer [not-found resources]]
            [hiccup.page :refer [include-js include-css html5]]
            [hackerthreads.middleware :refer [wrap-middleware]]
            [config.core :refer [env]]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer (get-sch-adapter)]
            [hackerthreads.datomic :refer [conn]]
            [hackerthreads.graphql :refer [query]]
            [datomic.api :as d]
            [clojure.core.async :refer [go-loop close!]]))

(def mount-target
  [:div#app
      [:h3 "ClojureScript has not been compiled!"]
      [:p "please run "
       [:b "lein figwheel"]
       " in order to start the compiler"]])

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))])

(defn loading-page []
  (html5
    (head)
    [:body {:class "body-container"}
     mount-target
     (include-js "/js/app.js")]))

(defn gen-uuid [] (str (java.util.UUID/randomUUID)))

(let [{:keys [ch-recv send-fn connected-uids
              ajax-post-fn ajax-get-or-ws-handshake-fn]}
      (sente/make-channel-socket! (get-sch-adapter) {:user-id-fn (fn [req] (gen-uuid))})]

  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv) ; ChannelSocket's receive channel
  (def chsk-send!                    send-fn) ; ChannelSocket's send API fn
  (def connected-uids                connected-uids))

(def live-queries (atom {}))

(defn send-query-update [uid]
  (let [{:keys [subid q]} (get @live-queries uid)]
    (chsk-send! uid [:hackerthreads/add-live-update {:subid subid :data (:data (query q))}])))

(defn add-live [uid {:as data :keys [q subid]}]
  (swap! live-queries assoc uid data)
  (send-query-update uid)
  subid)

(defonce loop-chan (atom nil))

(defn watch-db []
  (println "Starting to watch DB.")
  (go-loop [db (.take (d/tx-report-queue conn))]
    (doseq [uid (keys @live-queries)] (send-query-update uid))
    (recur (.take (d/tx-report-queue conn)))))

(swap! loop-chan (fn [chan] (if-not chan (watch-db))))

(defmulti event :id)
(defmethod event :default [{:keys [event]}]
  (println "Unhandled" event))
(defmethod event :hackerthreads/add-live [{:as ev-msg :keys [event uid ?data ?reply-fn]}]
  (add-live uid ?data))


(sente/start-chsk-router! ch-chsk event)

(defroutes routes
  (GET "/" [] (loading-page))
  (GET "/about" [] (loading-page))
  (GET "/chsk" req (ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ring-ajax-post req))

  (resources "/")
  (not-found "Not Found"))

(def app (wrap-middleware #'routes))
