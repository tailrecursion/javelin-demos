;   Copyright (c) Alan Dipert and Micha Niskin. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns tailrecursion.javelin-demos.form-validation
  (:require [tailrecursion.javelin-demos.dom :refer [input-to by-id validator]]
            [clojure.browser.event :as event])
  (:require-macros [tailrecursion.javelin.macros :refer [cell]]))

(def preds
  {:name     #(not (empty? %))
   :phone    #(= 10 (count (re-seq #"\d" %)))
   :age      #(> % 0)
   :email    #(boolean (re-matches #".+@.+\..+" %))
   :password #(>= (count %) 5)})

(defn validate
  [id-or-elem pred]
  (validator id-or-elem pred "bad" "good"))

(defn ^:export start []

  (let [form (cell '{:name ""
                     :phone ""
                     :age 0
                     :email ""
                     :password ""})
        valid? (cell (every? identity (map (fn [[k v]] ((preds k) v)) form)))]

    (input-to form [:name] "#name"
              :validator (validate "#name" (preds :name)))
    (input-to form [:phone] "#phone"
              :validator (validate "#phone" (preds :phone)))
    (input-to form [:age] "#age"
              :type :int
              :insert-default? false
              :validator (validate "#age" (preds :age)))
    (input-to form [:email] "#email"
              :validator (validate "#email" (preds :email)))
    (input-to form [:password] "#password"
              :validator (validate "#password" (preds :password)))

    (cell (aset (by-id "#submit") "disabled" (not valid?)))

    (event/listen (by-id "#form")
                  "submit"
                  #(do (js/alert (format "Good job dood! Your info: %s" (pr-str @form))) false))

    (.focus (by-id "#name"))))
