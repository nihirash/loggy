(ns loggy.view
  (:require [rum.core :as rum]
            [clojure.java.io :as io]
            [endophile.hiccup :as endohiccup]
            [endophile.core :as endophile]
            [loggy.database :as db]
            [clojure.string :as str])
  (:use [loggy.config]
        [loggy.utils]))

(defn styles [] (safe-slurp (io/resource "style.css")))

(def script (safe-slurp (io/resource "script.js")))

(defn render-date [inst]
  (.format (java.text.SimpleDateFormat. "dd.MM.yyyy") inst))

(defn show-year []
  "Return year(for copyright)"
  (.format (java.text.SimpleDateFormat. "yyyy") (db/now)))

(defn cut-body [post-body]
  "Returns only first paragraph of post body"
  (first (str/split post-body #"\n")))

(defn render-markdown [body]
  (endohiccup/to-hiccup
   (endophile/mp body)))

(rum/defc post [post full?]
  "Render single post component"
  (let [post-body (if full?
                    (:body post)
                    (cut-body (:body post)))
        post-body-rendered (render-markdown post-body)]
    [:article.mainArticle
     [:.headerArticleContent
      [:.infoPost [:p (render-date (:created post))]]
      [:.headlinePost [:h2 (if full?
                             (:title post)
                             [:a {:href (str "/post/" (:id post))} (:title post)])]]
      (when (:picture post)
        [:img.imgPost {:src (str "/post/" (:id post) "/" (:picture post) )}])
      [:.textPost {:dangerouslySetInnerHTML {:__html post-body-rendered}}]
      [:.buttonPostOpen
       (if full?
         [:a {:href "/"} "На главную"]
         [:a {:href (str "/post/" (:id post))} "Читать"])]]]))

(rum/defc page [title & children]
  "Common page component"
  [:html
   [:head
    [:meta {:http-equiv "Content-Type" :content "text/html; charset=UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
    [:link {:href "https://fonts.googleapis.com/css?family=Cormorant" :rel "stylesheet"} ]
    [:link {:href "/feed.xml" :type "application/atom+xml" :rel "alternative" :title "ATOM Feed"}]
    [:title title]]
   [:body.container
    [:style {:dangerouslySetInnerHTML {:__html (str (styles))}} ]
    [:header.metaBar
     [:.headlineMetaBar
      [:a {:href (:host @config)}
       [:h1 title]]]]
    [:main.siteMain
     children]
    [:footer.footer
     [:p "(c) " (show-year) " "
      [:a.contact {:href (:url @config)} (:name @config)] (:copy @config)]]
    [:script {:dangerouslySetInnerHTML {:__html (str script)}}]]])

(defn render-html [component]
  (str "<!DOCTYPE html>\n"
       (rum/render-static-markup component)))

(rum/defc post-feed [post-ids]
  (for [post-id post-ids]
          (post (db/get-post post-id) nil)))

(rum/defc index [post-ids page-num]
  (page (:title @config)
        (post-feed post-ids)
        [:noscript
         (if (seq post-ids)
           [:a {:href (str "/nojs/" (inc page-num)) } "Ранее" ]
           [:a {:href "/"} "На главную"])]))

(rum/defc post-page [post-id] 
  (let [post-data (db/get-post post-id)]
    (page (str (:title @config))
          (post post-data true))))

(defn site-map [post-ids]
  (xmlize
   [:urlset {:xmlns "http://www.sitemaps.org/schemas/sitemap/0.9"}
    [:url {} [:loc {} (:host @config)]]
    (map (fn [id] [:url {} [:loc {} (format "%s/post/%s" (:host @config) id)]]) post-ids)]))

(rum/defc text-input [name title value]
  [:div.formItem
   [:label {:for name} title]
   [:input {:type "text"
            :name name
            :id name
            :value value
            :placeholder title}]])

(rum/defc password-input [name title]
  [:div.formItem
   [:label {:for name} title]
   [:input {:type "password"
            :name name
            :id name
            :placeholder title}]])

(rum/defc text-area [name value title]
  [:div.formItem
   [:label {:for name} title]
   [:textarea {:name name
               :id name
               :placeholder title
               :value value}]])

(rum/defc file-input [name]
  [:div.formItem
   [:input {:type "file"
            :name name}]])

(defn edit-config-page []
  (page
   "Конфигурация"
   [:br]
   [:form {:action "/config"
           :method "post"}
    (for [field (map first @config)]
      (text-input field
                  (get field-captions field)
                  (get @config field)))
    [:button.button "Изменить"]]))
   
(rum/defc login-page [to]
  (page "Авторизация"
        [:.subtitlePost [:h2 "Пожалуйста авторизуйтесь"]]
        [:form {:action "/login"
                :method "post"}
         [:input {:name "to"
                  :type "hidden"
                  :value (or to "/post/new")}]
         (text-input "login" "Login" "")
         (password-input "password" "Password")
         [:div
          [:button.button "Войти"]]]))

(rum/defc edit-post-page [post-id]
  "Using it for creating page too"
  (let [post (db/get-post post-id)
        create? (nil? post)]
    (page
     (if create? "Создание записи" (str "Редактирование записи - " (:title post)))
          [:br]
          [:form {:action (str "/post/" post-id "/edit")
                  :method "post"
                  :enctype "multipart/form-data"}
           (text-input "title" "Заголовок" (:title post))
           (file-input "picture")
           (text-area "body" (:body post) "Текст сообщения")
           [:div 
            [:button.button
             (if create? "Создать" "Редактировать")]]])))
