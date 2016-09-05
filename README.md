# geewiz

[![Build Status](https://travis-ci.org/juhofriman/geewiz.svg?branch=master)](https://travis-ci.org/juhofriman/geewiz)
[![Clojars Project](https://img.shields.io/clojars/v/geewiz.svg)](https://clojars.org/geewiz)

Geewiz is one mans humble attempt to build utility library for constructing and modeling [GraphQL](http://graphql.org/) like interfaces and services. It's at really early stage and does support only fetching data - mutations will eventually be implemented.

The whole "great idea" is using geewiz should be datasource agnostic. It can be backed with relational database, nosql database, xml, json, in memory data and so on. It's all about modeling and creating functions to materialize that data matching the model.

## Usage

TODO

## Example project

Under examples you can find h2-geewiz project which you can just kick in REPL. It sets up really simple in memory H2 database as a datasource and hooks couple of handlers for mapping queries to data. Check out sources to find out how mappings are made.

```clojure
(use 'h2-geewiz.core :reload)

; Querying plain zoo
user=> (execute "{zoo(id:1) { name }}")
{:name "Korkeasaari"}
; Another zoo
user=> (execute "{zoo(id:2) { name }}")
{:name "Berlin Zoo"}
; Projecting some fields...
user=> (execute "{zoo(id:2) { name, id }}")
{:name "Berlin Zoo", :id 2}
; ... in different order (doesn't really make sense, but hey! It's possible!)
user=> (execute "{zoo(id:2) { id, name }}")
{:id 2, :name "Berlin Zoo"}
; Projecting dependent data (animals in zoo)
user=> (execute "{zoo(id:2) { id, name, animal() { name } }}")
{:id 2, :name "Berlin Zoo", :animal ({:name "Lion"} {:name "Porcupine"} {:name "Elephant"} {:name "Rhinoceros"})}
; Projecting nested dependent data
user=> (execute "{zoo(id:1) { id, name, animal() { name, attendant() { name, salary } } }}")
{:id 1, :name "Korkeasaari", :animal ({:name "Leila Leijona", :attendant {:name "Kiivari Hippalinen", :salary 1283}} {:name "Kari Karhu", :attendant {:name "Kiivari Hippalinen", :salary 1283}} {:name "Marko Maasika", :attendant {:name "Simo Simpulainen", :salary 2345}} {:name "Kalle Kirahvi", :attendant {:name "Juili Kilupainen", :salary 6252}})}
```

## License

Copyright © 2016 Juho Friman

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
