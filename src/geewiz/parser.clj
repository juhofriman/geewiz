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
<NAME-STRING> = #'[a-zA-Z0-9_-]*'
<STRING> = <STRING-SEPARATOR> #'[a-zA-Z0-9]*' <STRING-SEPARATOR>
<STRING-SEPARATOR> = '\\''
<INTEGER> = #'[0-9]*'
")

; RAW parsed datastructure:
; user=> (parse-raw "{cat(id: 1234) {name, breed, friends() { name }}}")

;[:query
; [:entityQuery
;  [:entityName "cat"]
;  [:constraints
;   [:constraint
;    [:constrainKey "id"]
;    [:constraintValue [:integerValue "1234"]]]]
;  [:fields
;   [:field "name"]
;   [:field "breed"]
;   [:field
;    [:entityQuery
;     [:entityName "friends"]
;     [:constraints]
;     [:fields [:field "name"]]]]]]]

; Instaparser instance
(def ql-parser (insta/parser grammar))

; For mutual recursion
(declare map-to-geewiz)

(defn- map-fields
    [[_ & fields]]
    (vec (map #(if (-> % second vector?) (map-to-geewiz (second %)) ((comp keyword second) %)) fields)))

(defn- map-constraint
    [[_ [_ key] [_ [type val]]]]
    (let [key-as-keyword (keyword key)]
        (case type
            :stringValue [key-as-keyword val]
            :integerValue [key-as-keyword (read-string val)])))

(defn- map-constraints
    [[_ & constraints]]
    (vec (flatten (map map-constraint constraints))))

(defn- map-to-geewiz
    [[_ [_ entityName] constraints fields]]
    {:type (keyword entityName)
     :handler-arguments (map-constraints constraints)
     :fields (map-fields fields)})

(defn parse-raw
    "Parses input raw with instaparse. Should be used from outside only for debugging."
    [i]
    (ql-parser i))

(defn parse
    "Parses input to geewiz internal query structure"
    [i]
    (map-to-geewiz (second (parse-raw i))))
