(ns hackerthreads.core
	(:require-macros [cljs.core.async.macros :as asyncm :refer (go go-loop)])

	(:require [reagent.core :as r :refer [atom]]
						[reagent.session :as session]
						[cljs.core.async :as async :refer (<! >! put! chan)]
						[taoensso.sente  :as sente :refer (cb-success?)]
     				[secretary.core :as secretary :include-macros true]
						[accountant.core :as accountant]))

(defonce posts (r/atom []))

;; -------------------------
;;; Add this: --->
(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket! "/chsk" ; Note the same path as before
       { :type :ws })]
  (def chsk       chsk)
  (def ch-chsk    ch-recv) ; ChannelSocket's receive channel
  (def chsk-send! send-fn) ; ChannelSocket's send API fn
  (def chsk-state state))   ; Watchable, read-only atom)

;; (go (println (<! ch-chsk)))
;; (chsk-send! (pr-str { :foo "bar" }))
(defn sub-posts []
  "Adds a subscription to new posts"
 	(chsk-send! ; Using Sente
	  [:hackerthreads/add-live {:subid :posts :q "query Z { posts { db/ident post/title post/body } }" }]))

(defmulti update-event #(-> % second :subid))
(defmethod update-event :posts [event] (reset! posts (-> event second :data :posts)))

(defmulti recv-event first)
(defmethod recv-event :hackerthreads/add-live-update [event] (update-event event))
(defmethod recv-event :default [event] (println "Unhandled recv event:" event))

(defmulti event :id)
(defmethod event :chsk/recv [{:keys [event]}] (recv-event (second event)))
(defmethod event :default [{:keys [event]}] (println "Unhandled WS event:" event))

(sente/start-chsk-router! ch-chsk event)

(add-watch chsk-state :watcher (fn [_ _ _ {:keys [open?]}]
                                 (if open?
                                   (do
                                     (sub-posts)
                                     (remove-watch chsk-state :watcher)))))

(defn make-post [post]
  )
(defmulti posts-list count)
(defmethod posts-list 0 [_] [:ul nil])
(defmethod posts-list :default [posts]
  [:ul (map-indexed
        (fn [idx post]
          [:li {:key idx}
           [:b (:post/title post)] " - " (:post/body post)])
          posts)])

(defn home-page []
  [:div [:h2 (str (count @posts) " posts")]
   [:div [:a {:href "/about"} "go to about page"]]

   [posts-list @posts]
   ])

(defn about-page []
  [:div [:h2 "About hackerthreads"]
   [:div [:a {:href "/"} "go to the home page"]]])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(secretary/defroute "/about" []
  (session/put! :current-page #'about-page))

;; -------------------------
;; Initialize app

(defn mount-root []
  (r/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!
    {:nav-handler
     (fn [path]
       (secretary/dispatch! path))
     :path-exists?
     (fn [path]
       (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (mount-root))
