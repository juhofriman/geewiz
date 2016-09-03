(ns geewiz.core
  (:require [geewiz.parser :as parser]))

(def handlers (atom {}))
(def types (atom {}))
(def deps (atom {}))

(def g {
  :zoo {
    :handler (fn [])
  }
  :party {
    :handler (fn [])
  }
  :people {
    :handler (fn [])
    :deps {
      :zoo {
        :handler (fn [])
        :arguments [:id]
      }
      :party {
        :handler (fn [])
        :arguments [:id]
      }
    }
  }
})

(defn- arg-count
    "Returns arity of f"
    [f]
    {:pre [(instance? clojure.lang.AFunction f)]}
    (-> f class .getDeclaredMethods first .getParameterTypes alength))

(defn geewiz-handler
    "Registers handler for type"
    ([entity body] (geewiz-handler entity [] body))
    ([entity [dependent-entity & arguments :as dependencies] body]
        (if (= 2 (arg-count body))
            (if (empty? dependencies)
              (swap! handlers assoc-in [entity :handler] body)
              (do
                (swap! handlers assoc-in [entity :deps dependent-entity :handler] body)
                (swap! handlers assoc-in [entity :deps dependent-entity :arguments] dependencies)))
            (throw (IllegalArgumentException.
                (str "Geewiz handler must be fn of 2 args. First constraints vector second fields vector."))))))

(defn geewiz-register-type
    "Register type"
    [type description]
    (swap! types assoc type description))

(defn geewiz-types
    "Returns all registered types"
    []
    (reduce (fn [acc [k v]]
        (if (fn? v)
            (assoc acc k (apply v []))
            (assoc acc k v))) {} @types))

(defn reset-handlers!
    []
    (do
        (reset! handlers {})
        (reset! deps {})
        (reset! types {})))

(declare iterate-query)

(defn- apply-sub-handlers
  [parent-type result sub-handlers]
  (reduce
    (fn [acc {type :type :as handler}]
      (assoc acc type (iterate-query handler parent-type result)))
    result
    sub-handlers))

(defn filter-result-object
    [type result fields]
    (let [all-fields (map (fn [a] (if (associative? a) (:type a) a)) fields)]
        (select-keys (apply-sub-handlers type result (filter associative? fields)) all-fields)))

(defn filter-result
  [type result fields]
  (if (sequential? result)
    (map #(filter-result-object type % fields) result)
    (filter-result-object type result fields)))

(defn create-constraints
  [constraints deps parent]
  (concat constraints (flatten (map (fn [[parentType field]] [field (get parent field)]) (partition 2 deps)))))

(defn- iterate-query
  [{type :type constraints :constraints fields :fields :or {fields [:all]}} parent-type parent]
   (let [type-def (get @handlers type)
         handler (if parent-type (get-in type-def [:deps parent-type :handler]) (:handler type-def))
         deps (if parent-type (get-in type-def [:deps parent-type :arguments]) [])]
      (if handler
          (filter-result type (apply handler [(create-constraints constraints deps parent) fields]) fields)
          (throw (IllegalArgumentException. (str "No handler for type" type))))))


(defn geewiz-query
    "Executes geewiz query. Queries should be constructed with (geewiz.parser/parse string)"
    [query]
    (iterate-query
      (if (string? query) (parser/parse query) query)
      nil
      nil))
