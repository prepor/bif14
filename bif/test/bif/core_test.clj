(ns bif.core-test
  (:require [clojure.core.async :as a]
            [bif.core :as bif]
            [clojure.test :refer :all]))

(deftest counter
  (let [source (a/chan 10)
        counter (bif/init-counter)
        process (bif/make-counter-process source counter)]
    (a/<!! (a/onto-chan source (for [t ["First tweet"
                                       "Second tweet"
                                       "Приветик!"]]
                                {:text t})))
    (process)
    (is (= {:total 32 :count 3} @counter))))
