(defproject bif "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [twitter-api "0.7.6"]
                 [com.taoensso/sente "0.15.1"]
                 [org.clojure/clojurescript "0.0-2311"]
                 [org.clojure/core.async "0.1.338.0-5c5012-alpha"]
                 [sablono "0.2.22"]
                 [clj-http "1.0.0"]
                 [clj-oauth "1.5.1"]
                 [cheshire "5.3.1"]
                 [compojure "1.1.8"]
                 [javax.servlet/servlet-api "2.5"]
                 [hiccup "1.0.5"]
                 [http-kit "2.1.16"]
                 [quiescent "0.1.4"]
                 [org.twitter4j/twitter4j-stream "4.0.2"]]
  :plugins [[lein-cljsbuild "1.0.3"]]
  :profiles {:dev {:plugins [[com.cemerick/austin "0.1.5"]]}}
  :cljsbuild {:builds
              {:client {:source-paths ["src-cljs"]
                        :compiler
                        {:optimizations :none
                         :output-dir "resources/public/js/out"
                         :output-to "resources/public/js/main.js"
                         :pretty-print true}}}})
