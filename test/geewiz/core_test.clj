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
        (is (thrown? IllegalArgumentException (geewiz-query "{ animal() {}}"))))

    (testing "executing handler via query"
        ; just a dummy handler returning something predictable
        (geewiz-handler :animal (fn [_ _] {:type :dog :name "Rufus"}))

        (is (= "Rufus" (:name (geewiz-query "{animal(id: 123) { name }}")))))

    (testing "executing handler with constraints"

        (geewiz-handler
          :animal
          (fn [[constraintKey constraintValue] _]
            {:type :dog :id constraintValue :name "Rufus"}))

        (is (= 123 (:id (geewiz-query "{ animal(id: 123) { id, name }}")))))

    (testing "executing query as string (this should be the the way to execute quesries)"

        (geewiz-handler
          :animal
          (fn [[constraintKey constraintValue] _]
            {:type :dog :id constraintValue :name "Rufus"}))

        (is (= 123 (:id (geewiz-query "{ animal(id: 123) {id}} ")))))

    (testing "executing handler with constraints and fields"

        (geewiz-handler
          :animal
          (fn [[constraintKey constraintValue] fields]
            {:type :dog :id constraintValue :name "Rufus" :breed "Terrier"}))

        (let [result (geewiz-query "{ animal(id: 123) { name, breed }}")]
            (is (= #{:name :breed} (set (keys result))))))

    (testing "executing nested dependent queries"

        (geewiz-handler
          :zoo (fn [_ _] {:name "Korkeasaari" :id 1234}))

        (geewiz-handler
          :address
          [:zoo :id]
          (fn [[_ id] _] {:streetAddress "Something" :postalCode 12345 :id id}))

        (let [result (geewiz-query "{ zoo(id: 1234) { name, id, address() { streetAddress, postalCode, id} }}")]
            (is (= "Korkeasaari" (:name result)))
            (is (= "Something" (get-in result [:address :streetAddress])))
            (is (= 1234 (get-in result [:address :id])))
            (is (= 12345 (get-in result [:address :postalCode])))))

    (testing "handlers returning collections"

        (geewiz-handler
          :zoo
          (fn [_ _] {:name "Korkeasaari" :id 1234}))

        (geewiz-handler
          :animals
          [:zoo :id]
          (fn [[_ id] _]
            [{:name "Dumbo" :breed "Elephant"},
             {:name "John" :breed "Pig"},
             {:name "Eric" :breed "Mule"}]))

        (let [result (geewiz-query "{ zoo(id: 1234) {name, id, animals(){ name, breed }}}")]
          (is (sequential? (get result :animals)))
          (is (= "Dumbo" (-> (get result :animals) first :name)))))

    (testing "handlers must support different contexts (wat?)"

        (geewiz-handler
          :zoo
          (fn [_ _] {:name "Korkeasaari" :id 999}))

        (geewiz-handler
          :political-party
          (fn [_ _] {:name "Intolerants" :id 111}))

        (geewiz-handler
          :people
          (fn [[_ id] _] [{:name "John" :id 1 :id-zoo id} {:name "Mark" :id 2 :id-party id}]))

        (geewiz-handler
          :people [:zoo :id]
          (fn [[_ id-zoo] _] [{:name "John" :id 1 :id-zoo id-zoo}]))

        (geewiz-handler
          :people [:political-party :id]
          (fn [[_ id-party] _] [{:name "Mark" :id 2 :id-party id-party}]))

        ; Should support all of these ->
        ; { people() {}}
        ; { zoo(id: 999) { people() { name }}}
        ; { political-party(id: 111) { people() { name }}}

        (let [result (geewiz-query "{ people() { name }}")]
          (is (= (sequential? result))))

        (let [result (geewiz-query "{ zoo(id: 999) { people() { name, id-zoo }}}")]
          (is (= "John" (:name (first (:people result)))))
          (is (= 999  (:id-zoo (first (:people result))))))

        (let [result (geewiz-query "{ political-party() { name, people() { name, id-party }}}")]
          (is (= "Mark" (:name (first (:people result)))))
          (is (= 111 (:id-party (first (:people result))))))))

(deftest registering-types
    (testing "no types is no types"
        (is (empty? (geewiz-types))))

    (testing "registering types"

        (geewiz-handler
          :zoo
          "Zoo is a the place for imprisoning animals"
          (fn [_ _] {}))

        (geewiz-handler
          :animal
          "Animal is an animal"
          (fn [_ _] {}))

        (geewiz-handler
          :animal
          "Animal in zoo joinen with zoo id"
          [:zoo :id]
          (fn [_ _] {}))

        (let [types (geewiz-types)
              zootype (get types :zoo)
              animaltype (get types :animal)
              animal-in-zoo (get-in types [:animal :deps :zoo])]
            (is (contains? types :zoo))
            (is (contains? types :animal))
            (is (= "Zoo is a the place for imprisoning animals" (:description zootype)))
            (is (= "Animal is an animal" (:description animaltype)))
            (is (= "Animal in zoo joinen with zoo id" (:description animal-in-zoo))))))
