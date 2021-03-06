(ns metabase.cmd.load-from-h2-test
  (:require [expectations :refer [expect]]
            [metabase.cmd.load-from-h2 :as load-from-h2]
            [metabase.util :as u]
            [toucan.models :as models]))

;; Make sure load-from-h2 works with or without `file:` prefix
(expect
  {:classname   "org.h2.Driver"
   :subprotocol "h2"
   :subname     "file:/path/to/metabase.db;IFEXISTS=TRUE"
   :type        :h2}
  (#'load-from-h2/h2-details "/path/to/metabase.db"))

(expect
  {:classname "org.h2.Driver"
   :subprotocol "h2"
   :subname     "file:/path/to/metabase.db;IFEXISTS=TRUE"
   :type        :h2}
  (#'load-from-h2/h2-details "file:/path/to/metabase.db"))


;; Check to make sure we're migrating all of our entities.
;; This fetches the `metabase.cmd.load-from-h2/entities` and compares it all existing entities

(defn- migrated-model-names []
  (set (map :name @(resolve 'metabase.cmd.load-from-h2/entities))))

(def ^:private models-to-exclude
  "Models that should *not* be migrated in `load-from-h2`."
  #{"TaskHistory"
    "Query"
    "QueryCache"
    "QueryExecution"})

(defn- all-model-names []
  (set (for [ns       @u/metabase-namespace-symbols
             :when    (or (re-find #"^metabase\.models\." (name ns))
                          (= (name ns) "metabase.db.migrations"))
             :when    (not (re-find #"test" (name ns)))
             [_ varr] (do (require ns)
                          (ns-interns ns))
             :let     [{model-name :name, :as model} (var-get varr)]
             :when    (and (models/model? model)
                           (not (contains? models-to-exclude model-name)))]
         model-name)))

(expect
  (all-model-names)
  (migrated-model-names))
