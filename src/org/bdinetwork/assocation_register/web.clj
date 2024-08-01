;;; SPDX-FileCopyrightText: 2024 Jomco B.V.
;;; SPDX-FileCopyrightText: 2024 Topsector Logistiek
;;; SPDX-FileContributor: Joost Diepenmaat <joost@jomco.nl>
;;; SPDX-FileContributor: Remco van 't Veer <remco@jomco.nl>
;;;
;;; SPDX-License-Identifier: AGPL-3.0-or-later

(ns org.bdinetwork.assocation-register.web
  (:require [compojure.core :refer [GET POST defroutes]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.util.response :refer [not-found]]
            [org.bdinetwork.assocation-register.data-source :as ds]
            [org.bdinetwork.ishare.jwt :as ishare.jwt]))

(defroutes routes
  (GET "/parties" {:keys [params data-source]}
    {:body (ds/parties data-source params)
     :token-key "parties_token"})
  (constantly
   (not-found "Resource not found")))

(defn wrap-datasource
  [handler ds]
  (fn [request]
    (handler (assoc request :data-source ds))))

(defn wrap-token-response
  [handler {:keys [private-key x5c server-id]}]
  (fn [{:keys [client-id] :as request}]
    (let [client-id "EU.EORI.CLIENT"
          {:keys [body token-key] :as response} (handler request)]
      (if (and client-id token-key)
        (assoc response :body {token-key (ishare.jwt/make-jwt (assoc body
                                                                     :iss server-id
                                                                     :sub server-id ;; TODO: check this
                                                                     :aud client-id)
                                                              private-key
                                                              x5c)})
        response))))

(defn make-handler
  [data-source config]
  (-> routes
      (wrap-token-response config)
      (wrap-datasource data-source)
      (wrap-params)
      (wrap-json-response)))
