(ns loggy.database
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:use [loggy.config])
  (:use [loggy.utils])
  (:import [java.util UUID]))

(defn get-post [post-id]
  (let [path (str "data/posts/" post-id "/post.edn")]
    (some-> (io/file path)
            (safe-slurp)
            (edn/read-string))))

(defn post-ids []
  (->>
   (for [name (seq (.list (io/file "data/posts")))
         :let [child (io/file "data/posts" name)]
         :when (.isDirectory child)]
     name)
   (sort)
   (reverse)))

(defn paginate [post-ids page]
  (let [per-page (:per-page @config)]
    (vec
     (take per-page
           (drop (* per-page page) post-ids)))))

(defn now [] (java.util.Date.))

(defn save-post! [post picture]
  (let [dir (io/file (str "data/posts/" (:id post)))
        picture-name (let [in-name (:filename picture)
                           ext (second (re-matches #".*(\.[^.]+)" in-name))]
                       (when ext (str (:id post) ext)))]
    (.mkdir dir)
    (when picture-name 
      (io/copy (:tempfile picture) (io/file dir picture-name)))
    (spit (io/file dir "post.edn") (pr-str (assoc post :picture picture-name)))))

(defn init []
  (.mkdir (io/file "data/"))
  (.mkdir (io/file "data/posts")))
