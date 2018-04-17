(ns loggy.feed
  (:require [loggy.database :as db]
            [loggy.view :as view])
  (:use [loggy.utils]
        [loggy.config]))

(defn render-iso-date [inst]
  (.format (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss'Z'") inst))

(defn atom-post-entry [post]
  (let [url (str (:host @config) "/post/" (:id post))]
    [:entry {}
     [:title {} (:title post)]
     [:link {:rel "alternate" :type "text/html" :href url}]
     [:id {} url]
     [:published {} (render-iso-date (:created post))]
     [:author {} [:name {} (:name @config)]]
     [:content {:type "text/html" :href url}
       (view/cut-body (:body post))]]))

(defn atom-feed [post-ids]
  (let [posts (map db/get-post post-ids)
        updated (or (db/max-post-date posts)
                    (db/now))
        author (:name @config)
        host (:host @config)]
    (xmlize
     [:feed {:xmlns "http://www.w3.org/2005/Atom" :xml:lang "ru"}
      [:title {} (:title @config)]
      [:link {:type "application/atom+xml" :href (str host "/feed.xml") :rel "self"}]
      [:link {:type "text/html" :href (str host "/") :rel "alternative"}]
      [:id {} host]
      [:updated {} (render-iso-date updated)]
      [:author {} [:name {} author]]
      (for [post posts]
        (atom-post-entry post))])))
