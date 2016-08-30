(ns h2-geewiz.core
  (:require
    [clojure.java.jdbc :as sql]
    [geewiz.core :as geewiz]
    [geewiz.parser :as geewiz-parser]))

(def db-spec
  {:classname "org.h2.Driver"
   :subprotocol "h2:mem"
   :subname "db/my-webapp;DB_CLOSE_DELAY=-1"})

(defn create-table [table-key & specs]
  (try
    (apply (partial sql/create-table table-key) specs)
    (catch Exception e (println (.getMessage e)))))

(sql/with-connection db-spec
  (do
    (create-table :zoo
      [:id "bigint primary key auto_increment"]
      [:name "varchar(500)"])
    (create-table :animal
      [:id "bigint primary key auto_increment"]
      [:name "varchar(500)"]
      [:id_zoo "integer references zoo(id)"]))

    (sql/insert-records :zoo
      {:id 1 :name "Korkeasaari"}
      {:id 2 :name "Berlin Zoo"})

    (sql/insert-records :animal
      {:name "Leila Leijona" :id_zoo 1}
      {:name "Simo Simpanssi" :id_zoo 1}))

(defn get-zoo [[_ id] _]
  (sql/with-connection db-spec
    (sql/with-query-results res
                  ["select * from zoo where id = ?" id]
                  (first (doall res)))))

(defn get-animals-of-zoo [[_ zoo-id] _]
  (sql/with-connection db-spec
    (sql/with-query-results res
                  ["select * from animal where id_zoo = ?" zoo-id]
                  (doall res))))


(geewiz/geewiz-handler :zoo get-zoo)

;; This does not work currently because g/core does not know how to handle sequences returned from handler
;(geewiz/geewiz-handler :animal [:zoo :id] get-animals-of-zoo)

(defn execute
  [query]
  (geewiz/geewiz-query (geewiz-parser/parse query)))
