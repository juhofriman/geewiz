(ns geewiz.parser-test
  (:require [clojure.test :refer :all]
            [geewiz.parser :refer :all]))

(deftest syntax-tests
    (testing "throws"
        (is (thrown? IllegalArgumentException (parse "24frvr"))))
    (testing "simple"
        (is (= {:type :animal :handler-arguments [] :fields []}
               (parse "{ animal() {}}")))
        (is (= {:type :animal :handler-arguments [:id 123] :fields [:breed]}
               (parse "{ animal(id: 123) { breed }}")))
        (is (= {:type :animal :handler-arguments [:id 123 :name "jeppe"] :fields [:name :breed]}
               (parse "{ animal(id: 123, name: 'jeppe') { name, breed }}"))))

    (testing "allowed special characters in names"
        (is (= {:type :animal :handler-arguments [] :fields [ :fav_food ]}
               (parse "{ animal() { fav_food }}")))
        (is (= {:type :animal :handler-arguments [] :fields [ :fav-food ]}
               (parse "{ animal() { fav-food }}"))))

    (testing "nested"
        (is (= {:type
                :animal
                :handler-arguments [:id 1]
                :fields [
                    :name
                    {:type :legs
                     :handler-arguments []
                     :fields [:headLeft :headRight :tailLeft :tailRight]}]}
               (parse "{ animal(id: 1) { name, legs() { headLeft, headRight, tailLeft, tailRight }}}")))))
