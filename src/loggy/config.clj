(ns loggy.config
  (:require [clojure.edn :as edn])
  (:use [loggy.utils]))

(def config (atom nil))

(def defaut-config {:login "admin"
                    :password "admin"
                    :title "Blog"
                    :per-page 3
                    :host "http://example.com"
                    :name "Author name"
                    :url "http://test.example.com"
                    :copy "All rights reserved"})

(def field-captions {:login "Имя пользователя"
                     :password "Ваш пароль"
                     :title "Заголовок блога"
                     :per-page "Число записей на страницу блога"
                     :host "Url-вашего блога(для построения ссылок)"
                     :name "Ваше имя(отображается на строчке копирайтов)"
                     :url "Ссылка на ваши контактные данные"
                     :copy "Строка копирайта(или другое сообщение после вашего имени)"})

(defn read-config []
  (reset! config (edn/read-string (safe-slurp "data/config.edn")))
  (when (nil? @config) (reset! config defaut-config)))

(defn save-config! [new-config]
  (reset! config new-config)
  (spit "data/config.edn" (pr-str @config)))

(read-config)
