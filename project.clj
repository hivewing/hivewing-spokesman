(defproject hivewing-spokesman "0.1.0-SNAPSHOT"
  :description "The way hivewing talks to the outside world"
  :url "http://spokesman.hivewing.io"
  :dependencies [
                 [org.clojure/clojure "1.7.0-alpha5"]
                 [hivewing-core "0.1.3-SNAPSHOT"]
                 [clj-http "1.0.1"]
                 ]
  :main ^:skip-aot hivewing-spokesman.core
  :plugins [[s3-wagon-private "1.1.2"]
            [lein-environ "1.0.0"]]
  :repositories [["hivewing-core" {:url "s3p://clojars.hivewing.io/hivewing-core/releases"
                                   :username "AKIAJCSUM5ZFGI7DW5PA"
                                   :passphrase "UcO9VGAaGMRuJZbgZxCiz0XuHmB1J0uvzt7WIlJK"}]]
  :uberjar-name "hivewing-spokesman-%s.uber.jar"
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
