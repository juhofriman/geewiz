(ns geewiz.core-test
  (:require [clojure.test :refer :all]
            [geewiz.core :refer :all]))

(defn teardown [test]
    (reset-handlers!)
    (test)
    (reset-handlers!))

(use-fixtures :each teardown)

(deftest registering-handler
    (testing "Accepts only handlers with correct arity"
        (is (thrown? IllegalArgumentException (geewiz-handler :animal (fn [])))))

    (testing "Registering dependent handlers"
        (geewiz-handler :zoo (fn [_ _] {:name "Korkeasaari" :id 123}))
        (geewiz-handler :address [:zoo :id] (fn [[id] _] { :address_for_id id}))))

(deftest testing-query
    (testing "throws for unknown type"
        ; No handler for :animal
        (is (thrown? IllegalArgumentException (geewiz-query {:type :animal}))))

    (testing "executing handler via query"
        ; just a dummy handler returning something predictable
        (geewiz-handler :animal (fn [_ _] {:type :dog :name "Rufus"}))

        (is (= "Rufus" (:name (geewiz-query {:type :animal :fields [:name]})))))

    (testing "executing handler with constraints"
        (geewiz-handler :animal (fn [[constraintKey constraintValue] _] {:type :dog :id constraintValue :name "Rufus"}))

        (is (= 123 (:id (geewiz-query {:type :animal :constraints [:id 123] :fields [:id]})))))

    (testing "executing handler with constraints and fields"
        (geewiz-handler :animal (fn [[constraintKey constraintValue] fields] {:type :dog :id constraintValue :name "Rufus" :breed "Terrier"}))

        (let [result (geewiz-query {:type :animal :constraints [:id 123] :fields [:name :breed]})]
            (is (= #{:name :breed} (set (keys result))))))

    (testing "executing nested dependent queries"

        (geewiz-handler :zoo (fn [_ _] {:name "Korkeasaari" :id 1234}))
        (geewiz-handler :address [:zoo :id] (fn [[_ id] _] {:streetAddress "Something" :postalCode 12345 :id id}))

        (let [result (geewiz-query {:type :zoo :constraints [] :fields [:name {:type :address :constraints [] :fields [:id :streetAddress :postalCode]}]})]
            (is (= "Korkeasaari" (:name result)))
            (is (= "Something" (get-in result [:address :streetAddress])))
            (is (= 1234 (get-in result [:address :id])))
            (is (= 12345 (get-in result [:address :postalCode]))))))

(deftest registering-types
    (testing "no types is no types"
        (is (empty? (geewiz-types))))

    (testing "registering static types"
        (geewiz-register-type :dog {:breed "Breed of dog" :name "Name of dog"})

        (let [types (geewiz-types) dogtype (get types :dog)]
            (is (contains? types :dog))
            (is (contains? dogtype :breed))
            (is (contains? dogtype :name))))

    (testing "registering dynamic types"
        (geewiz-register-type :dog (fn [] {:breed "Breed of dog" :name "Name of dog"}))

        (let [types (geewiz-types) dogtype (get types :dog)]
            (is (contains? types :dog))
            (is (contains? dogtype :breed))
            (is (contains? dogtype :name)))))
