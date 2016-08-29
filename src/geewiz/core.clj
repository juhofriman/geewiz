(ns geewiz.core)

(def handlers (atom {}))
(def types (atom {}))

(defn- arg-count
    "Returns arity of f"
    [f]
    {:pre [(instance? clojure.lang.AFunction f)]}
    (-> f class .getDeclaredMethods first .getParameterTypes alength))

(defn geewiz-handler
    "Registers handler for type"
    [entity body]
    (if (= 2 (arg-count body))
        (swap! handlers assoc entity body)
        (throw (IllegalArgumentException. (str "Geewiz handler must be fn of 2 args. First constraints vector second fields vector.")))))

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
        (reset! types {})))

(declare geewiz-query)

(defn- apply-sub-handlers
    [result sub-handlers]
    (reduce
        (fn [acc {type :type :as handler}]
            (assoc acc type (geewiz-query handler)))
        result
        sub-handlers))

(defn filter-result
    [result fields]
    (let [all-fields (map (fn [a] (if (associative? a) (:type a) a)) fields)]
        (select-keys (apply-sub-handlers result (filter associative? fields)) all-fields)))

(defn geewiz-query
    "Executes geewiz query. Queries should be constructed with (geewiz.parser/parse string)"
    [{type :type constraints :constraints fields :fields :or {fields [:all]}}]
    (let [handler (get @handlers type)]
        (if handler
            (filter-result (apply handler [constraints fields]) fields)
            (throw (IllegalArgumentException. (str "No handler for type" type))))))
