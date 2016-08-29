(ns geewiz.parser
    (:require [instaparse.core :as insta]))

(def grammar "
query = <WS?> <LEFT-BRACE> <WS?> entityQuery <WS?> <RIGHT-BRACE> <WS?>

entityQuery = entityName <LEFT-ARGS> constraints <RIGHT-ARGS> <WS?> <LEFT-BRACE> <WS?> fields <WS?> <RIGHT-BRACE>

constraints = ( constraint (<SEPARATOR> constraint)* )?
constraint = constrainKey <CONSTRAINT-SEPARATOR> constraintValue
constrainKey = NAME-STRING
constraintValue = stringValue | integerValue

fields = (field (<SEPARATOR> <WS?> field)*)?
field = (NAME-STRING | entityQuery)

entityName= NAME-STRING
fieldValue = STRING | INTEGER

stringValue = STRING
integerValue = INTEGER

<LEFT-BRACE> = '{'
<RIGHT-BRACE> = '}'
<LEFT-ARGS> = '('
<RIGHT-ARGS> = ')'
<SEPARATOR> = ',' <WS?>
<CONSTRAINT-SEPARATOR> = ':' <WS?>
<WS> = ' ' | '\t' | '\n' | '\r'
<NAME-STRING> = #'[a-zA-Z0-9]*'
<STRING> = <STRING-SEPARATOR> #'[a-zA-Z0-9]*' <STRING-SEPARATOR>
<STRING-SEPARATOR> = '\\''
<INTEGER> = #'[0-9]*'
")

(def ql-parser (insta/parser grammar))

(defn parse-raw [i] (ql-parser i))

(declare map-to-geewiz)

(defn- map-fields [[_ & fields]]
    (vec (map #(if (-> % second vector?) (map-to-geewiz (second %)) ((comp keyword second) %)) fields)))

(defn map-constraint [[_ [_ key] [_ [type val]]]]
    (let [key-as-keyword (keyword key)]
        (case type
            :stringValue [key-as-keyword val]
            :integerValue [key-as-keyword (read-string val)])))

(defn- map-constraints [[_ & constraints]]
    (vec (flatten (map map-constraint constraints))))

(defn- map-to-geewiz [[_ [_ entityName] constraints fields]]
    {:type (keyword entityName)
     :constraints (map-constraints constraints)
     :fields (map-fields fields)})

(defn parse [i] (map-to-geewiz (second (parse-raw i))))
