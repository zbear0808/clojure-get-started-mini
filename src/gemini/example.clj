(ns gemini.example
  (:require [gemini.core :as gemini]))

;; This example shows how to use the Gemini wrapper with an API key.

(defn run-example []
  (let [api-key "YOUR_API_KEY"] ; Replace with your actual API key

    (println "--- Simple Text Generation ---")
    ;; Single prompt
    (try
      (let [result (gemini/generate-content api-key "What is Clojure?")]
        (println "Result:" (:text result)))
      (catch Exception e
        (println "Error (likely invalid API key):" (.getMessage e))))

    (println "\n--- Multi-modal Generation ---")
    ;; Multi-modal prompt with inline image (placeholder)
    (try
      (let [result (gemini/generate-content api-key
                                            ["What is in this image?"
                                             (gemini/inline-part "image/png" (byte-array [1 2 3]))])]
        (println "Result:" (:text result)))
      (catch Exception e
        (println "Error:" (.getMessage e))))))

(comment
  ;; To run this, you need to set your api-key.
  (run-example)
  )
