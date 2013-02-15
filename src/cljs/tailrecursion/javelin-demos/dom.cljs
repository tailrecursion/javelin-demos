;   Copyright (c) Alan Dipert and Micha Niskin. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns tailrecursion.javelin-demos.dom
  "Utilities for massaging strings and interacting with the DOM."
  (:require [clojure.browser.event :as event]
            [goog.dom.forms        :as form])
  (:require-macros [tailrecursion.javelin.macros :refer [cell with-let]]))

(defn by-id
  "If id-or-elem is a string, returns the element with the specified
  id. Otherwise returns id-or-elem which is presumably an element.

  If id-or-elem is a string, it may optionally start with #.  Throws
  an exception if the element is not found."
  [id-or-elem]
  {:post [(identity %)]}
  (if (string? id-or-elem)
    (.getElementById js/document (if (= (first id-or-elem) "#")
                                   (.slice id-or-elem 1)
                                   id-or-elem))
    id-or-elem))

(defn- aset-in
  "Sets the value in a nested JavaScript array, where ks is a sequence
  of fields."
  [arr ks value]
  (aset (reduce #(aget %1 %2) arr (butlast ks)) (last ks) value))

(defn insert!
  "Inserts string s as the value of the attribute fld in element
  id-or-elem.  fld defaults to 'value'.  Provided additional flds,
  inserts the value at the specified path a la assoc-in."
  ([id-or-elem s]
     (insert! id-or-elem s "value"))
  ([id-or-elem s fld & flds]
     (let [elem (by-id id-or-elem)]
       (aset-in elem (list* fld flds) s))))

(defn html!
  "Applies the strings s and more to format-string using
  cljs.core/format, and inserts the resultant string into the HTML
  element at id-or-elem."
  [id-or-elem format-string s & more]
  (aset-in (by-id id-or-elem)
           ["innerHTML"]
           (apply format format-string s more)))

(defn parse-float
  "Attempts to parse s with js/parseFloat.  If parsing fails, returns
  default."
  [default s]
  (let [n (js/parseFloat s)]
    (if (js/isNaN n) default n)))

(defn parse-int
  "Attempts to parse s with js/parseInt.  If parsing fails, returns
  default."
  [default s]
  (let [n (js/parseInt s)]
    (if (js/isNaN n) default n)))

(defn form-cell
  "Returns an input cell backed by the form input id-or-elem.
  Understands the following additional options:

  :default - The default value of the resulting input cell. This value
  is also inserted as the form element's initial value.  Defaults to nil.

  :type - The type to parse the form input's value string as.  Known
  types are: :string, :int, :float.  Defaults to :string.

  :triggers - A set of strings or keywords of event types that should
  trigger reset of the input cell with the form element value.  Event
  types are those known to clojure.browser.events.  For a
  comprehensive list, see
  http://closure-library.googlecode.com/svn/docs/closure_goog_events_eventtype.js.source.html.
  Defaults to #{\"input\"}."
  [id-or-elem & {:keys [default triggers
                        type]
                 :or {triggers #{"input"}
                      type :string}}]
  (let [parsers {:string identity
                 :float (partial parse-float default)
                 :int (partial parse-int default)}
        elem (by-id id-or-elem)]
    (insert! elem (str default))
    (with-let [in-cell (cell 'default)]
      (doseq [t triggers]
        (event/listen elem
                      t
                      #(reset! in-cell ((parsers type) (form/getValue elem))))))))