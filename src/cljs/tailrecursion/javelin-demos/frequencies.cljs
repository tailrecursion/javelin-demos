;   Copyright (c) Alan Dipert and Micha Niskin. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns tailrecursion.javelin-demos.frequencies
  (:require-macros
   [tailrecursion.javelin.macros :refer [cell]])
  (:require
   [tailrecursion.javelin :refer [all! distinct! timer*]]
   [tailrecursion.javelin-demos.dom :refer [html! aset-in by-id form-cell]]
   [tailrecursion.priority-map :refer [priority-map]]))

(defn sentence
  "Returns of string of the elements of xs interleaved with commas or
  'and' as appropriate."
  [[x1 x2 & _ :as xs]]
  (case (count xs)
    0 ""
    1 (str x1)
    2 (str x1 " and " x2)
    (str (apply str (interpose ", " (butlast xs))) " and " (last xs))))

(defn histogram!
  "Inserts a table-based histogram in the element id-or-elem backed by
  the priority map pmap."
  [id-or-elem pmap & {:keys [bar-color bar-width bar-height]
                      :or {bar-color "navy", bar-width 30, bar-height 200}}]
  (let [parent (by-id id-or-elem)
        table  (.createElement js/document "table")
        bars   (.createElement js/document "tr")
        values (.createElement js/document "tr")
        max (second (first (rseq pmap)))
        scale-height (fn [n] (* (/ bar-height max) n))]
    (aset-in table ["style" "width"] "auto")
    (aset-in table ["style" "textAlign"] "center")
    (set! (.-innerHTML parent) "")
    (doseq [[n v] (into (sorted-map) pmap)]
      (let [bar-td (.createElement js/document "td")
            bar-div (.createElement js/document "div")
            val-td (.createElement js/document "td")]
        (aset-in bar-td  ["style" "verticalAlign"] "bottom")
        (aset-in bar-div ["style" "width"] (str bar-width "px"))
        (aset-in bar-div ["style" "height"] (str (scale-height v) "px"))
        (aset-in bar-div ["style" "backgroundColor"] bar-color)
        (aset-in bar-div ["style" "margin"] "auto")
        (aset-in bar-div ["innerHTML"] "&nbsp")
        (aset-in val-td  ["innerHTML"] (str n))
        (.appendChild bar-td bar-div)
        (.appendChild bars bar-td)
        (.appendChild values val-td)))
    (.appendChild table bars)
    (.appendChild table values)
    (.appendChild parent table)))

(defn ^:export start []

  (let [;; user input
        rand-max       (form-cell "#rand-max" :type :int, :default 10, :triggers #{"change"})
        slider         (form-cell "#ms" :type :int, :default 500)
        interval       (cell (if (neg? slider) slider (- 1000 slider)))

        ;; data collection
        rand           (all! (timer* interval (fn [_] (rand-int @rand-max)) (rand-int @rand-max)))
        freqs          (distinct! (cell (merge-with + ~(priority-map) {rand 1})))

        ;; analysis
        n-seen         (cell (->> freqs vals (reduce +)))
        n-even         (cell (->> freqs (filter (comp even? first)) (map second) (reduce +)))
        %-even         (cell (->> n-seen (/ n-even) (* 100) (.round js/Math)))
        most-frequent  (cell (->> (rseq freqs)
                                  (partition-by second)
                                  first
                                  (map first)
                                  sentence))
        least-frequent (cell (key (peek freqs)))
        n-distinct     (cell (count freqs))]

    ;; display
    (cell (html! "#status" "%s (interval is %s milliseconds)"
                 (if (neg? interval) "Stopped" "Running")
                 interval))
    (cell (html! "#most-frequent" "%s" most-frequent))
    (cell (html! "#n-seen" "%s" n-seen))
    (cell (html! "#percent-even" "%s%" %-even))
    (cell (html! "#least-frequent" "%s" least-frequent))
    (cell (html! "#n-distinct" "%s" n-distinct))
    (cell (histogram! "#histogram" freqs
                      :bar-color "navy",
                      :bar-width 20,
                      :bar-height 100))))