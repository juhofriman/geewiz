(ns geewiz.core
  (:require [geewiz.parser :as parser]))

(def handlers (atom {}))
(def types (atom {}))

;; Example of handlers data structure
; (def g {
;  :zoo {
;     :handler (fn [])
;   }
;   :party {
;     :handler (fn [])
;   }
;   :people {
;     :handler (fn [])
;     :deps {
;       :zoo {
;         :handler (fn [])
;         :arguments [:id]
;       }
;       :party {
;         :handler (fn [])
;         :arguments [:id]
;       }
;     }
;   }
; })

; Example of internal query
; {:type :zoo,
;  :constraints [:id 1],
;  :fields
;     [:name
;      {:type :animal, :constraints [], :fields [:breed]}]}

(defn- arg-count
    "Returns arity of f"
    [f]
    {:pre [(instance? clojure.lang.AFunction f)]}
    (-> f class .getDeclaredMethods first .getParameterTypes alength))

(defn geewiz-handler
    "Registers handler for type"
    ([entity body] (geewiz-handler entity "" [] body))
    ([entity desc-or-deps body]
     (if (string? desc-or-deps)
       (geewiz-handler entity desc-or-deps [] body)
       (geewiz-handler entity "" desc-or-deps body)))
    ([entity description [dependent-entity & arguments :as dependencies] body]
     (if (= 2 (arg-count body))
         (if (empty? dependencies)
           (do
             (swap! handlers assoc-in [entity :handler] body)
             (swap! handlers assoc-in [entity :description] description))
           (do
             (swap! handlers assoc-in [entity :deps dependent-entity :handler] body)
             (swap! handlers assoc-in [entity :deps dependent-entity :description] description)
             (swap! handlers assoc-in [entity :deps dependent-entity :arguments] dependencies)))
         (throw (IllegalArgumentException.
                 (str "Geewiz handler must be fn of 2 args. First constraints vector second fields vector."))))))

(defn geewiz-types
    "Returns all registered types"
    []
    @handlers)

(defn reset-handlers!
    []
    (reset! handlers {}))

(defn get-typedef
  "Returns typedef for type"
  [type]
  (if-let [h (get @handlers type)]
    h
    (throw (IllegalArgumentException. (str "No handler for type " type)))))

; We have mutual recursion here
(declare execute-geewiz-handler)

(defn prepare-nested-query
  [parent-type parent {t :type a :handler-arguments f :fields}]
  (let [type-def (get-typedef t)
        handler (get-in type-def [:deps parent-type :handler])
        arguments (partition 2 (get-in type-def [:deps parent-type :arguments]))]
    (partial execute-geewiz-handler t handler (concat a (mapcat #(vector (first %) (get parent (second %))) arguments)) f)))

(defn process-result
  [type handler-result fields]
  (into
    {}
    (for [f fields]
      (if (associative? f)
            ; It's a nested query for type
        (let [sub-type (:type f)]
          [sub-type (apply (prepare-nested-query type handler-result f) [])])

            ; it's a captured keyword
        [f (get handler-result f)]))))

(defn execute-geewiz-handler
  [type handler handler-arguments fields]
  (let [handler-result (apply handler [handler-arguments fields])]
    (if (sequential? handler-result)
      ; Process collection of entities
      (map #(process-result type % fields) handler-result)
      ; Process single entity
      (process-result type handler-result fields))))

(defn geewiz-query
    "Executes query string"
    [query]
    (let [{t :type a :handler-arguments f :fields} (parser/parse query)]
      (execute-geewiz-handler t (-> (get-typedef t) :handler) a f)))
