(ns gemini.core
  (:import [com.google.cloud.vertexai VertexAI]
           [com.google.cloud.vertexai.generativeai GenerativeModel ResponseHandler ContentMaker PartMaker ChatSession]
           [com.google.cloud.vertexai.api GenerateContentResponse Candidate Content Part FileData Blob]
           [com.google.protobuf ByteString]))

(defn vertex-ai
  "Creates a VertexAI client.
   Params:
   - project-id: Google Cloud project ID.
   - location: Google Cloud region (e.g., \"us-central1\")."
  [project-id location]
  (VertexAI. project-id location))

(defn generative-model
  "Creates a GenerativeModel instance.
   Params:
   - client: A VertexAI instance.
   - model-name: Model name (e.g., \"gemini-1.5-flash\")."
  [client model-name]
  (GenerativeModel. model-name client))

(defn- part->map [^Part part]
  (cond
    (.hasText part) {:text (.getText part)}
    (.hasInlineData part) {:inline-data {:mime-type (-> part .getInlineData .getMimeType)
                                         :data (-> part .getInlineData .getData)}}
    (.hasFileData part) {:file-data {:mime-type (-> part .getFileData .getMimeType)
                                     :file-uri (-> part .getFileData .getFileUri)}}
    :else {:unknown (str part)}))

(defn- content->map [^Content content]
  {:role (.getRole content)
   :parts (mapv part->map (.getPartsList content))})

(defn- candidate->map [^Candidate candidate]
  {:content (content->map (.getContent candidate))
   :finish-reason (str (.getFinishReason candidate))
   :safety-ratings (mapv (fn [r]
                           {:category (str (.getCategory r))
                            :probability (str (.getProbability r))
                            :blocked? (.getBlocked r)})
                         (.getSafetyRatingsList candidate))})

(defn response->map
  "Converts a GenerateContentResponse to a Clojure map."
  [^GenerateContentResponse response]
  {:text (try (ResponseHandler/getText response) (catch Exception _ nil))
   :candidates (mapv candidate->map (.getCandidatesList response))
   :usage-metadata (if (.hasUsageMetadata response)
                     (let [usage (.getUsageMetadata response)]
                       {:prompt-token-count (.getPromptTokenCount usage)
                        :candidates-token-count (.getCandidatesTokenCount usage)
                        :total-token-count (.getTotalTokenCount usage)})
                     {})})

(defn- ->content
  "Converts input to a single Content object or String."
  [contents]
  (cond
    (string? contents) contents
    (instance? Content contents) contents
    (sequential? contents) (ContentMaker/fromMultiModalData (into-array Object contents))
    :else contents))

(defn- ->content-list
  "Converts input to a List of Content objects, a single Content object, or a String."
  [contents]
  (if (and (sequential? contents) (every? #(instance? Content %) contents))
    (java.util.ArrayList. contents)
    (->content contents)))

(defn generate-content
  "Generates content.
   Params:
   - model: GenerativeModel instance.
   - contents: A string prompt, a Content object, or a list of parts/Content."
  [model contents]
  (let [input (->content-list contents)
        response (cond
                   (string? input) (.generateContent model ^String input)
                   (instance? java.util.List input) (.generateContent model ^java.util.List input)
                   :else (.generateContent model ^Content input))]
    (response->map response)))

(defn generate-content-stream
  "Generates content as a stream. Returns a sequence of response maps."
  [model contents]
  (let [input (->content-list contents)
        stream (cond
                 (string? input) (.generateContentStream model ^String input)
                 (instance? java.util.List input) (.generateContentStream model ^java.util.List input)
                 :else (.generateContentStream model ^Content input))]
    (map response->map (iterator-seq (.iterator stream)))))

(defn start-chat
  "Starts a chat session."
  [model]
  (.startChat model))

(defn send-message
  "Sends a message in a chat session."
  [chat contents]
  (let [input (->content contents)
        response (if (string? input)
                   (.sendMessage chat ^String input)
                   (.sendMessage chat ^Content input))]
    (response->map response)))

(defn send-message-stream
  "Sends a message in a chat session as a stream."
  [chat contents]
  (let [input (->content contents)
        stream (if (string? input)
                 (.sendMessageStream chat ^String input)
                 (.sendMessageStream chat ^Content input))]
    (map response->map (iterator-seq (.iterator stream)))))

(defn get-history
  "Returns the chat history as a list of content maps."
  [chat]
  (mapv content->map (.getHistory chat)))

;; Helpers for creating parts

(defn file-part
  "Creates a Part from a GCS URI."
  [mime-type uri]
  (-> (Part/newBuilder)
      (.setFileData (-> (FileData/newBuilder)
                        (.setMimeType mime-type)
                        (.setFileUri uri)
                        .build))
      .build))

(defn inline-part
  "Creates a Part from inline data (byte array)."
  [mime-type ^bytes data]
  (-> (Part/newBuilder)
      (.setInlineData (-> (Blob/newBuilder)
                          (.setMimeType mime-type)
                          (.setData (ByteString/copyFrom data))
                          .build))
      .build))
