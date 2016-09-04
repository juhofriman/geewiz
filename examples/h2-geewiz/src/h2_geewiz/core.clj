(ns h2-geewiz.core
  (:require
    [clojure.java.jdbc :as sql]
    [geewiz.core :as geewiz]))

(def db-spec
  {:classname "org.h2.Driver"
   :subprotocol "h2:mem"
   :subname "db/my-webapp;DB_CLOSE_DELAY=-1"})

(defn drop-table [table-key]
  (try
    (sql/drop-table table-key)
    (catch Exception e (println (.getMessage e)))))

(defn create-table [table-key & specs]
  (try
    (apply (partial sql/create-table table-key) specs)
    (catch Exception e (println (.getMessage e)))))

(sql/with-connection db-spec
  (do
    (drop-table :animal)
    (drop-table :attendant)
    (drop-table :zoo)

    (create-table :zoo
      [:id "bigint primary key auto_increment"]
      [:name "varchar(500)"])
    (create-table :attendant
      [:id "bigint primary key auto_increment"]
      [:name "varchar(500)"]
      [:salary "integer"]
      [:id_zoo "integer references zoo(id)"])
    (create-table :animal
      [:id "bigint primary key auto_increment"]
      [:name "varchar(500)"]
      [:fav_food "varchar(500)"]
      [:id_zoo "integer references zoo(id)"]
      [:id_attendant "integer references attendant(id)"])


    (sql/insert-records :zoo
      {:id 1 :name "Korkeasaari"}
      {:id 2 :name "Berlin Zoo"})

    (sql/insert-records :attendant
      {:id 1 :name "Kiivari Hippalinen" :salary 1283 :id_zoo 1}
      {:id 2 :name "Simo Simpulainen" :salary 2345 :id_zoo 1}
      {:id 3 :name "Juili Kilupainen" :salary 6252 :id_zoo 1}
      {:id 4 :name "Horst Zuggenberg" :salary 1543 :id_zoo 2}
      {:id 5 :name "Adolf Arhip" :salary 1324 :id_zoo 2}
      {:id 6 :name "Liza Murkel" :salary 2134 :id_zoo 2}))

    (sql/insert-records :animal
      {:id 1 :name "Leila Leijona" :fav_food "Humans" :id_zoo 1 :id_attendant 1}
      {:id 2 :name "Kari Karhu" :fav_food "Berries" :id_zoo 1 :id_attendant 1}
      {:id 3 :name "Marko Maasika" :fav_food "Dirt" :id_zoo 1 :id_attendant 2}
      {:id 4 :name "Kalle Kirahvi" :fav_food "Leaves" :id_zoo 1 :id_attendant 3}
      {:id 5 :name "Lion" :fav_food "Humans" :id_zoo 2 :id_attendant 4}
      {:id 6 :name "Porcupine" :fav_food "Humans" :id_zoo 2 :id_attendant 5}
      {:id 7 :name "Elephant" :fav_food "Humans" :id_zoo 2 :id_attendant 6}
      {:id 8 :name "Rhinoceros" :fav_food "Dunno" :id_zoo 2 :id_attendant 5}))

(defn get-zoo [[_ id] _]
  (sql/with-connection db-spec
    (sql/with-query-results res
                  ["select * from zoo where id = ?" id]
                  (first (doall res)))))

(defn get-animal [[_ id] _]
  (sql/with-connection db-spec
    (sql/with-query-results res
                  ["select * from animal where id = ?" id]
                  (first (doall res)))))

(defn get-animals-of-zoo [[_ zoo-id] _]
  (sql/with-connection db-spec
    (sql/with-query-results res
                  ["select * from animal where id_zoo = ?" zoo-id]
                  (doall res))))

(defn get-attendants-for-zoo [[_ zoo-id] _]
  (sql/with-connection db-spec
    (sql/with-query-results res
                  ["select * from attendant where id_zoo = ?" zoo-id]
                  (doall res))))

(defn get-attendant-of-animal [[_ id] _]
  (sql/with-connection db-spec
    (sql/with-query-results res
                  ["select * from attendant where id = ?" id]
                  (first (doall res)))))

(geewiz/geewiz-handler :zoo "Zoo is a zoo" get-zoo)

(geewiz/geewiz-handler :animal "Animal is an animal" get-animal)

(geewiz/geewiz-handler :animal "Animal in zoo is an animal in zoo" [:zoo :id] get-animals-of-zoo)

(geewiz/geewiz-handler :attendant "Attendant for animal" [:animal :id_attendant] get-attendant-of-animal)

(geewiz/geewiz-handler :attendant "Attendants in zoo" [:zoo :id] get-attendants-for-zoo)

(defn execute
  [query]
  (geewiz/geewiz-query query))
