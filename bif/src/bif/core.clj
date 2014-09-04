(ns bif.core
  (:require [taoensso.sente :as sente]
            [bif.twitter-stream :as twitter]
            [hiccup.core :as h]
            [compojure.core :refer [defroutes GET POST]]
            [org.httpkit.server :as server]
            [ring.middleware.resource :refer [wrap-resource]]
            [clojure.core.async :as a]
            [cemerick.austin.repls :refer (browser-connected-repl-js)]))

(let [{:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn
              connected-uids]}
      (sente/make-channel-socket! {})]
  (def ring-ajax-post ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk ch-recv)
  (def chsk-send! send-fn)
  (def connected-uids connected-uids))

(defonce stream (twitter/stream "ukraine"))
(defonce stream-mult (a/mult (:ch stream)))

(defn init-counter [] (atom {:total 0 :count 0}))
(defonce counter (init-counter))

(defn mean
  []
  (let [{:keys [total count]} @counter]
    (if (< 0 count) (/ total count) 0)))

(defroutes routes
  (GET "/" []
    (h/html
     [:h1#title "БИФ 2014"]
     [:p "Привет! Интересует средняя длинна пойманных нами твитов? "
      [:a {:href "/count"} "Это тут!"]]))

  (GET "/count" []
    (h/html
     [:h1#title "Считаем слова"]
     [:h2.mean "Средний твит: " (float (mean))]
     [:p (format "Всего твитов: %s; всего символов: %s" (:count @counter) (:total @counter))]))

  (GET "/game" []
    (h/html
     [:html
      [:head
       [:script {:src "/js/react-0.11.1.min.js" :type "text/javascript"}]
       [:meta {:name "viewport" :content "width=device-width, initial-scale=0.67, maximum-scale=0.67, user-scalable=no"}]
       [:link {:href "css/style.css" :rel "stylesheet" :type "text/css"}]]
      [:body
       [:div.contanier [:div#board-area]]
       [:script {:src "/js/out/goog/base.js" :type "text/javascript"}]
       [:script {:src "/js/main.js" :type "text/javascript"}]
       [:script {:type "text/javascript"} "goog.require(\"bif.core\");"]
       [:script (browser-connected-repl-js)]]]))

  (GET  "/chsk" req (ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ring-ajax-post req)))

(def app
  (-> routes
      (wrap-resource "public/")))

(defonce http-server (atom nil))

(defn stop-http-server! []
  (when-let [current-server @http-server]
    (current-server :timeout 100)))

(defn start-http-server! []
  (stop-http-server!)
  (let [port 8300
        s (server/run-server (var app) {:port port})
        uri (format "http://localhost:%s/" port)]
    (prn "Http-kit server is running at `%s`" uri)
    (.browse (java.awt.Desktop/getDesktop)
             (java.net.URI. uri))
    (reset! http-server s)))

(defonce counter-process (atom nil))

(defn make-counter-process
  [source counter]
  (let [process (a/go-loop []
                  (when-let [v (a/<! source)]
                    (swap! counter #(-> %
                                        (update-in [:total] + (count (:text v)))
                                        (update-in [:count] inc)))
                    (recur)))]
    (fn []
      (a/close! source)
      (a/<!! process))))

(defn stop-counter-process!
  []
  (when-let [p @counter-process]
    (p)))

(defn start-counter-process!
  []
  (stop-counter-process!)
  (let [ch (a/chan (a/dropping-buffer 100))]
    (a/tap stream-mult ch)
    (reset! counter-process (make-counter-process ch counter))))

(defonce pub-process (atom nil))

(defn make-pub-process
  [source]
  (let [process
        (a/go-loop []
          (when-let [v (a/<! source)]
            (doseq [uid (:any @connected-uids)]
              (chsk-send! uid
                          [:game/quote {:image (:image v)
                                        :quote (:text v)}]))
            (recur)))]
    (fn []
      (a/close! source)
      (a/<!! process))))

(defn stop-pub-process!
  []
  (when-let [p @pub-process]
    (p)))

(defn start-pub-process!
  []
  (stop-pub-process!)
  (let [ch (a/chan (a/dropping-buffer 100))]
    (a/tap stream-mult ch)
    (reset! pub-process (make-pub-process ch))))

(comment
  (def repl-env (reset! cemerick.austin.repls/browser-repl-env
                        (cemerick.austin/repl-env)))
  (cemerick.austin.repls/cljs-repl repl-env))
