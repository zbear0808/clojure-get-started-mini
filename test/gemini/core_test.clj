(ns gemini.core-test
  (:require [clojure.test :refer :all]
            [gemini.core :as g]
            [jsonista.core :as j]))

(deftest content-conversion-test
  (testing "string content"
    (is (= [{:parts [{:text "Hello"}]}]
           (#'g/->contents "Hello"))))

  (testing "vector of strings"
    (is (= [{:parts [{:text "Hello"} {:text "World"}]}]
           (#'g/->contents ["Hello" "World"]))))

  (testing "inline-part"
    (let [data (byte-array [1 2 3])
          part (g/inline-part "image/png" data)]
      (is (= "image/png" (get-in part [:inlineData :mimeType])))
      (is (= "AQID" (get-in part [:inlineData :data]))))))

(deftest json-payload-test
  (testing "mapper generates correct keys"
    (let [mapper @#'g/mapper
          payload {:contents (#'g/->contents "test")}]
      (is (= "{\"contents\":[{\"parts\":[{\"text\":\"test\"}]}]}"
             (j/write-value-as-string payload mapper))))))
