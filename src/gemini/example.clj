(ns gemini.example
  (:require [gemini.core :as gemini]))

;; This example shows how to use the Gemini wrapper.
;; Note: You need a valid Google Cloud Project ID and to be authenticated.

(defn run-example []
  (let [project-id "your-project-id"
        location "us-central1"
        model-name "gemini-1.5-flash"]

    (println "--- Simple Text Generation ---")
    (with-open [client (gemini/vertex-ai project-id location)]
      (let [model (gemini/generative-model client model-name)]
        ;; Single prompt
        (let [result (gemini/generate-content model "What is Clojure?")]
          (println "Result:" (:text result)))

        (println "\n--- Multi-modal Generation ---")
        ;; Multi-modal prompt
        (let [result (gemini/generate-content model ["What is in this image?"
                                                     (gemini/file-part "image/png" "gs://generativeai-downloads/images/scones.jpg")])]
          (println "Result:" (:text result)))

        (println "\n--- Chat Session ---")
        ;; Chat session
        (let [chat (gemini/start-chat model)]
          (let [resp1 (gemini/send-message chat "Hello, I am Jules.")]
            (println "Jules:" (:text resp1)))
          (let [resp2 (gemini/send-message chat "What was my name?")]
            (println "AI:" (:text resp2)))
          (println "History:" (gemini/get-history chat)))))))

(comment
  ;; To run this, you need to set your project-id and have credentials configured.
  (run-example)
  )
