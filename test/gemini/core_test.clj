(ns gemini.core-test
  (:require [clojure.test :refer :all]
            [gemini.core :as g])
  (:import [com.google.cloud.vertexai.api Part]))

(deftest part-helpers-test
  (testing "file-part"
    (let [part (g/file-part "image/png" "gs://bucket/image.png")]
      (is (instance? Part part))
      (is (= "image/png" (.getMimeType (.getFileData part))))
      (is (= "gs://bucket/image.png" (.getFileUri (.getFileData part))))))

  (testing "inline-part"
    (let [data (byte-array [1 2 3])
          part (g/inline-part "image/png" data)]
      (is (instance? Part part))
      (is (= "image/png" (.getMimeType (.getInlineData part))))
      (is (= 3 (.size (.getData (.getInlineData part))))))))
