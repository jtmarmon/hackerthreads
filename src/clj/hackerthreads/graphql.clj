(ns hackerthreads.graphql
  (:require [graphql-clj.parser :as parser]
            [graphql-clj.type :as type]
            [graphql-clj.validator :as validator]
            [graphql-clj.executor :as executor]
            [hackerthreads.datomic :refer [conn]]
            [datomic.api :as d]
            [clojure.walk :refer [keywordize-keys]]))

(def schema-str (slurp "src/schema.graphql"))
(def type-schema (-> schema-str parser/parse validator/validate-schema))

(defn get-all-posts []
  (let [db (d/db conn)
        post-ids (map first (d/q '[:find ?e :in $ :where [?e :post/title _] [?e :post/body _]] db))]
    (d/pull-many db '[:post/title, :post/body] post-ids)))

(defn resolver-fn [type-name field-name]
	(cond
   ;; todo use AST to resolve only needed fields
		(and (= "QueryRoot" type-name) (= "posts" field-name)) (fn [ctx parent args] (get-all-posts))))

(defn query [query-str]
  (let [query (-> query-str parser/parse (validator/validate-statement type-schema))
        ;; execute (has nil for context)
        results (keywordize-keys (executor/execute nil type-schema resolver-fn query))]
      results))
