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
            [goog.dom.forms        :as form]
            [goog.dom.classes      :as classes])
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

(defn input-to
  "Attaches form input at id-or-elem to the existing input cell backbone.

  If ks is empty, resets backbone to the input value.  If ks is
  non-empty, swaps backbone with assoc-in using ks as the path.

  Understands the following additional options:

  :insert-default? - If true, immediately insert the default value
  into the backing input.  Defaults to false.

  :type - The type to parse the form input's value string as.  Known
  types are: :string, :int, :float.  Defaults to :string.

  :triggers - A set of strings or keywords of event types that should
  trigger backbone mutation.  Event types are those known to
  clojure.browser.events.  For a comprehensive list, see
  http://closure-library.googlecode.com/svn/docs/closure_goog_events_eventtype.js.source.html.
  Defaults to #{\"input\"}.

  :validator - Function of a single argument that is applied to every
  new input value.  If it returns true, the new input value is added
  to the backbone.  The validator is applied to the existing backbone
  value once when this function is called, but its return value is
  discarded."
  [backbone ks id-or-elem
   & {:keys [triggers type
             insert-default? validator]
      :or {triggers #{"input"}
           type :string
           insert-default? true
           validator (constantly true)}}]
  (let [default (if (seq ks) (get-in @backbone ks) @backbone)
        parsers {:string identity
                 :float (partial parse-float default)
                 :int (partial parse-int default)}
        elem (by-id id-or-elem)
        update! #(if (seq ks)
                   (swap! backbone assoc-in ks %)
                   (reset! backbone %))]
    (validator default)
    (if insert-default? (insert! elem (str default)))
    (doseq [t triggers]
      (event/listen elem
                    t
                    #(let [newv ((parsers type) (form/getValue elem))]
                       (update! (if (validator newv) newv default)))))))

(defn form-cell
  "Creates and returns a cell backed by the form input id-or-elem.
  Takes the same options as form-cell plus:

  :default - The initial value of the created cell. Defaults to the
  empty string."
  [id-or-elem & opts]
  (let [default (get (apply hash-map opts) :default "")]
    (with-let [in-cell (cell 'default)]
      (apply input-to in-cell [] id-or-elem opts))))

(defn add-remove!
  "Adds the classes in add-classes to id-or-elem and removes the
  classes in remove-classes from id-or-elem.

  Returns the modified element."
  [id-or-elem add-classes remove-classes]
  (with-let [elem (by-id id-or-elem)]
    (doseq [add add-classes]
      (classes/add elem (or add "")))
    (doseq [remove remove-classes]
      (classes/remove elem (or remove "")))))

(defn validator
  "For use with the input-to and form-cell :validator option.

  Returns a function that when invoked with a value, applies pred.
  If (pred v) and good-class was specified, adds the class to
  id-or-elem and removes bad-class.  If (not (pred v)), adds bad-class
  and removes good-class."
  [id-or-elem pred bad-class & [good-class]]
  (fn [v]
    (let [elem (by-id id-or-elem)]
      (with-let [valid? (pred v)]
        (if valid?
          (add-remove! elem [good-class] [bad-class])
          (add-remove! elem [bad-class] [good-class]))))))