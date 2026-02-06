(ns gemini.core
  (:require [jsonista.core :as j])
  (:import [java.net.http HttpClient HttpRequest HttpResponse HttpRequest$BodyPublishers HttpResponse$BodyHandlers]
           [java.net URI]
           [java.util Base64]))

(def ^:private mapper (j/object-mapper {:decode-key-fn true}))

(defonce ^:private default-client (HttpClient/newHttpClient))

(defn- ->base64 [data]
  (if (string? data)
    data
    (.encodeToString (Base64/getEncoder) data)))

(defn text-part [text]
  {:text text})

(defn inline-part [mime-type data]
  {:inlineData {:mimeType mime-type
                :data (->base64 data)}})

(defn- ->part [x]
  (cond
    (string? x) (text-part x)
    (map? x) x
    :else (throw (Exception. (str "Invalid part: " x)))))

(defn- ->content [x]
  (cond
    (string? x) {:parts [(text-part x)]}
    (map? x) (if (:parts x) x {:parts [(->part x)]})
    (sequential? x) {:parts (mapv ->part x)}
    :else (throw (Exception. (str "Invalid content: " x)))))

(defn- ->contents [x]
  (if (sequential? x)
    (if (map? (first x))
      (mapv ->content x)
      [(->content x)])
    [(->content x)]))

(defn generate-content
  "Generates content using the Gemini API.
   Params:
   - api-key: Your Google AI API key.
   - model: Model name (default: \"gemini-1.5-flash\").
   - contents: String, map, or vector of parts/contents.
   - options: Map of additional options (generationConfig, safetySettings, etc.)."
  ([api-key contents]
   (generate-content api-key "gemini-1.5-flash" contents {}))
  ([api-key model contents]
   (generate-content api-key model contents {}))
  ([api-key model contents options]
   (let [url (str "https://generativelanguage.googleapis.com/v1beta/models/" model ":generateContent?key=" api-key)
         body (merge options {:contents (->contents contents)})
         request (-> (HttpRequest/newBuilder)
                     (.uri (URI/create url))
                     (.header "Content-Type" "application/json")
                     (.POST (HttpRequest$BodyPublishers/ofString (j/write-value-as-string body mapper)))
                     .build)
         response (.send default-client request (HttpResponse$BodyHandlers/ofString))]
     (if (= 200 (.statusCode response))
       (let [data (j/read-value (.body response) mapper)]
         (let [candidates (:candidates data)
               text (get-in candidates [0 :content :parts 0 :text])]
           (assoc data :text text)))
       (throw (Exception. (str "API Error: " (.statusCode response) " " (.body response))))))))
