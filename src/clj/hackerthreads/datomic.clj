(ns hackerthreads.datomic
  (:require [io.rkn.conformity :as c]
            [datomic.api :as d]))

(def migrations (c/read-resource "migrations.edn"))
(def dburi "datomic:dev://localhost:4334/hackerthreads")
(def conn (d/connect dburi))

(comment (d/create-database dburi))
(comment (c/ensure-conforms connection migrations [:hackerthreads/migrations]))
