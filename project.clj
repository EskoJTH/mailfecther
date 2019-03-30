(defproject mailfetcher "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [clj-http "3.9.1"]
                 [com.google.http-client/google-http-client "1.25.0"]
 ;;                [com.google.http-client/google-http-client-jackson2 "1.25.0"]
                 [com.google.api-client/google-api-client "1.25.0"]
                 ;; https://mvnrepository.com/artifact/com.google.auth/google-auth-library-credentials
                 [com.google.auth/google-auth-library-credentials "0.12.0"]
                 [com.google.oauth-client/google-oauth-client-jetty "1.25.0"]
                 [org.clojure/data.json "0.2.6"]
                 [tupelo "0.9.112"]]
  :main ^:skip-aot mailfetcher.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
