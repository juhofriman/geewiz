(ns geewiz.parser-test
  (:require [clojure.test :refer :all]
            [geewiz.parser :refer :all]))

(deftest syntax-tests
    (testing "simple"
        (is (= {:type :animal :constraints [] :fields []}
               (parse "{ animal() {}}")))
        (is (= {:type :animal :constraints [:id 123] :fields [:breed]}
               (parse "{ animal(id: 123) { breed }}")))
        (is (= {:type :animal :constraints [:id 123 :name "jeppe"] :fields [:name :breed]}
               (parse "{ animal(id: 123, name: 'jeppe') { name, breed }}"))))

    (testing "nested"
        (is (= {:type :animal :constraints [:id 1] :fields [:name {:type :legs :constraints [] :fields [:headLeft :headRight :tailLeft :tailRight]}]}
               (parse "{ animal(id: 1) { name, legs() { headLeft, headRight, tailLeft, tailRight }}}")))))
