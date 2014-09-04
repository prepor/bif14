(ns bif.twitter-stream
  (:require ;; [cheshire.core :as json]
            ;; [twitter.oauth :as oauth]
            ;; [twitter.callbacks.handlers :as hanlders]
            [clojure.core.async :as a]
            ;; [clojure.java.io :as io]
            )
  (:import [twitter4j TwitterStreamFactory FilterQuery StatusListener]
           [twitter4j.conf ConfigurationBuilder])
  )

(def config (-> (doto (ConfigurationBuilder.)
                  (.setDebugEnabled true)
                  (.setOAuthConsumerKey "HSRccMv9SEriF8ioai2lcJuKW")
                  (.setOAuthConsumerSecret "mPDvxNCzcgQVLdy8DjfhB7yxArBhW8SigtBEjf7jNHQKgRapCf")
                  (.setOAuthAccessToken "14333001-bI26eZq47LrqeMvojzQWTLAcNTNzGlgIOGbm9Adlu")
                  (.setOAuthAccessTokenSecret "u19GY7JslDzC1gExSfInqPAOhP35BP1Mjl2V7DBXKdc8v"))
                (.build)))

(def factory (TwitterStreamFactory. config))

(defn stream
  [track]
  (let [stream (.getInstance factory)
        out (a/chan (a/sliding-buffer 100))
        query (doto (FilterQuery.)
                (.track (into-array [track])))
        listener (reify StatusListener
                   (onStatus [_ status]
                     (a/put! out {:image (-> status (.getUser) (.getProfileImageURL))
                                  :text (.getText status)}))
                   (onDeletionNotice [_ _])
                   (onScrubGeo [_ _ _])
                   (onStallWarning [_ _])
                   (onTrackLimitationNotice [_ _]))]
    (.addListener stream listener)
    (.filter stream query)
    {:close (fn [] (.shutdown stream)) :ch out}))

;; (statuses-filter :params {:track "Borat"}
;;                  :oauth-creds my-creds
;;                  :callbacks *custom-streaming-callback*)

;; (def my-creds (oauth/make-oauth-creds "HSRccMv9SEriF8ioai2lcJuKW"
;;                                       "mPDvxNCzcgQVLdy8DjfhB7yxArBhW8SigtBEjf7jNHQKgRapCf"
;;                                       "14333001-bI26eZq47LrqeMvojzQWTLAcNTNzGlgIOGbm9Adlu"
;;                                       "u19GY7JslDzC1gExSfInqPAOhP35BP1Mjl2V7DBXKdc8v"))

;; (def ^:dynamic
;;   *custom-streaming-callback*
;;   )


;; (def token "14333001-bI26eZq47LrqeMvojzQWTLAcNTNzGlgIOGbm9Adlu")
;; (def secret "u19GY7JslDzC1gExSfInqPAOhP35BP1Mjl2V7DBXKdc8v")

;; (def url "https://stream.twitter.com/1.1/statuses/filter.json")

;; (defn stream
;;   [track]
;;   (let [out (a/chan (a/sliding-buffer 100))
;;         clb (AsyncStreamingCallback.
;;              (a/put! out %)
;;              (comp println handlers/response-return-everything)
;;              hanlders/exception-print)])
;;   (let [params {:track track}
;;         credentials (time (oauth/credentials consumer
;;                                              token secret
;;                                              :get url params))
;;         res (time (http/get url {:query-params (merge params credentials) :as :stream}))
;;         stream (:body res)
;;         reader (io/reader stream)
;;         out (a/chan (a/sliding-buffer 100))]
;;     (prn "Stream started" track)
;;     (a/thread
;;       (try
;;         (doseq [t (json/parsed-seq reader true)]
;;           ;; (prn "Tweet" (:text t))
;;           (a/>!! out t))
;;         (catch java.io.IOException e
;;           (a/close! out)))
;;       (prn "Stream closed!"))
;;     {:close (fn [] (.close reader)) :ch out}))
