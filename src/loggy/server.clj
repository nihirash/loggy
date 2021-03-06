(ns loggy.server
  (:require [immutant.web :as web]
            [compojure.core :as compojure]
            [compojure.route :as route]
            [ring.util.response]
            [ring.middleware.params]
            [ring.middleware.multipart-params]
            [ring.middleware.session :as session]
            [loggy.view :as view]
            [loggy.database :as db]
            [rum.core :as rum]
            [loggy.utils :as utils]
            [loggy.feed :as feed])
  (:use [loggy.config])
  (:gen-class))

(defn redirect [to]
  {:status 302
   :headers {"Location" to}})

(defn with-headers [handler headers]
  (fn [request]
    (some-> (handler request)
            (update :headers merge headers))))

(defn try-auth [req]
  (let [params (:params req)
        login (get params "login")
        password (get params "password")
        to (get params "to")]
    (if (and (= login (:login @config))
             (= password (:password @config)))
      (assoc (redirect (or to "/post/new")) :session true)
      (redirect  "/login"))))          

(defn posts-feed [page]
  (db/paginate (db/post-ids) (Integer/parseInt page)))

(defn html-response [body]
  {:headers {"Content-Type" "text/html; charset=utf-8"}
   :body body})

(defn wrap-authed [req handler]
    (if (= (:session req) true)
      (handler req)
      (redirect (str "/login?to=" (:uri req)))))

(defn store-post [id req]
  (wrap-authed
   req
   (fn [req]
     (let [params (:multipart-params req)
           body (get params "body")
           title (get params "title")
           picture (get params "picture")]
       (db/save-post! {:id id
                       :body body
                       :title title
                       :created (db/now)}
                      picture)) 
     (redirect "/"))))

(defn feed-page-action [page]
  (html-response
   (view/render-html
    (view/index (posts-feed page) (Integer/parseInt page)))))

(defn index-action []
  (feed-page-action "0"))

(defn page-action [page]
  (html-response
   (rum/render-static-markup
    (view/post-feed (posts-feed page)))))

(defn post-action [id]
  (html-response
   (view/render-html
    (view/post-page id))))

(defn login-page-action [req]
  (html-response
   (view/render-html
    (view/login-page (get (:params req) "to")))))

(defn logout-action []
  (-> (redirect "/")
      (assoc :session nil)))

(defn sitemap-action []
  {:status 200
   :headers {"Content-Type" "text/xml; charset=utf-8"}
   :body (view/site-map (db/post-ids))})

(defn atom-feed-action []
  {:status 200
   :headers {"Content-Type" "application/xml; charset=utf-8"}
   :body (feed/atom-feed (db/post-ids))})

(defn edit-post-form-action [id req]
  (wrap-authed req
               (fn [req]
                 (html-response (view/render-html (view/edit-post-page id))))))

(defn settings-page-action [req]
  (wrap-authed req
               (fn [req]
                 (html-response
                  (view/render-html
                   (view/edit-config-page))))))

(defn update-setting-action [req]
  (wrap-authed req
               (fn [req]
                 (let [params (:params req)]
                   (save-config! {:login (get params "login")
                                  :password (get params "password")
                                  :title (get params "title")
                                  :per-page (Integer/parseInt (get params "per-page"))
                                  :host (get params "host")
                                  :name (get params "name")
                                  :url (get params "url")
                                  :copy (get params "copy")})
                   (redirect "/config")))))

(defn robots-txt-action []
  {:headers {"Content-Type" "text/plain"}
   :body (str "User-agent: *\r\n\r\n"
              "Allow: *\r\n"
              "Sitemap: " (:host @config) "/sitemap.xml\r\n"
              "Host: " (:host @config) "\r\n")})

(compojure/defroutes routes
  ;; Posts
  (compojure/GET "/" [] (index-action))
  (compojure/GET "/nojs/:page" [page] (feed-page-action page))
  (compojure/GET "/page/:page" [page] (page-action page))
  (compojure/GET "/post/new" [] (redirect (str "/post/" (utils/time-based-uuid) "/edit")))
  (compojure/GET "/post/:id/:img" [id img] (some-> (ring.util.response/file-response (str "data/posts/" id "/" img))                                               
                                                   (update :headers merge {"Cache-Control" "max-age=3600, must-revalidate"})))
  (compojure/GET "/post/:id" [id] (post-action id))
  
  ;; Auth
  (compojure/GET "/logout" [] (logout-action))
  (compojure/GET "/login" [:as req] (login-page-action req)) 
  (compojure/POST "/login" [:as req] (try-auth req))
  
  ;; For robots
  (compojure/GET "/sitemap.xml" [] (sitemap-action))
  (compojure/GET "/robots.txt" [] (robots-txt-action))
  (compojure/GET "/feed.xml" [] (atom-feed-action))
  
  ;; Authed area
  (compojure/GET "/post/:id/edit" [id :as req] (edit-post-form-action id req))
  (compojure/POST "/post/:id/edit" [id :as req] (store-post id req))
  (compojure/POST "/config" [:as req] (update-setting-action req))
  (compojure/GET "/config" [:as req] (settings-page-action req))
  ;; End of the way
  (route/not-found {:body "404"}))

(def app
  (compojure/routes
   (-> routes
       (ring.middleware.params/wrap-params)
       (session/wrap-session)
       (ring.middleware.multipart-params/wrap-multipart-params))))

(defn -main
  "Inits app"
  [& args]
  (let [args-map (apply array-map args)
        port-str (or (get args-map "-p")
                     (get args-map "--port")
                     (System/getenv "PORT")
                     "8080")]
    (db/init)
    (read-config)
    (println "Starting web server on port" port-str)
    (web/run #'app {:host "0.0.0.0"
                    :port (Integer/parseInt port-str)})))

(comment
  (def server (-main "--port" "8080"))
  (web/stop server))
