(ns geewiz.core)

(def handlers (atom {}))
(def types (atom {}))
(def deps (atom {}))

(defn- arg-count
    "Returns arity of f"
    [f]
    {:pre [(instance? clojure.lang.AFunction f)]}
    (-> f class .getDeclaredMethods first .getParameterTypes alength))

(defn geewiz-handler
    "Registers handler for type"
    ([entity body] (geewiz-handler entity [] body))
    ([entity dependencies body]
        (if (= 2 (arg-count body))
            (do
                (swap! handlers assoc entity body)
                (swap! deps assoc entity dependencies))
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

(declare geewiz-query)

(defn- apply-sub-handlers
    [result sub-handlers]
    (reduce
        (fn [acc {type :type :as handler}]
            (assoc acc type (geewiz-query handler result)))
        result
        sub-handlers))

(defn filter-result-object
    [result fields]
    (let [all-fields (map (fn [a] (if (associative? a) (:type a) a)) fields)]
        (select-keys (apply-sub-handlers result (filter associative? fields)) all-fields)))

(defn filter-result
  [result fields]
  (if (sequential? result)
    (map #(filter-result-object % fields) result)
    (filter-result-object result fields)))

(defn create-constraints [constraints deps parent]
    (concat constraints (flatten (map (fn [[parentType field]] [field (get parent field)]) (partition 2 deps)))))


(defn geewiz-query
    "Executes geewiz query. Queries should be constructed with (geewiz.parser/parse string)"
    ([query] (geewiz-query query {}))
    ([{type :type constraints :constraints fields :fields :or {fields [:all]}} parent]
     (let [handler (get @handlers type) deps (get @deps type)]
        (if handler
            (filter-result (apply handler [(create-constraints constraints deps parent) fields]) fields)
            (throw (IllegalArgumentException. (str "No handler for type" type)))))))
