(ns mailfetcher.core
  (:require [clojure.string :as s])
  (:require [clj-http.client :as http])
  (:require [clojure.data.json :as json])
  (:require [clojure.set :as sets])
  (:require [tupelo.core :as t])
  (:require [clojure.core.reducers :as r])
  (:import [java.io FileReader])
  (:import [java.io FileInputStream])
  (:import [com.google.api.client.extensions.java6.auth.oauth2 AuthorizationCodeInstalledApp])
  (:import [com.google.api.client.extensions.jetty.auth.oauth2 LocalServerReceiver$Builder])
  (:import [com.google.api.client.googleapis.auth.oauth2 GoogleAuthorizationCodeFlow$Builder])
  (:import [com.google.api.client.googleapis.auth.oauth2 GoogleAuthorizationCodeFlow])
  (:import [com.google.api.client.googleapis.auth.oauth2 GoogleClientSecrets])
  (:import [com.google.api.client.http HttpTransport])
  ;;(:import [com.google.api.client.])
  ;;(:import [com.google.api.client.json JsonFactory])
  (:import [com.google.api.client.googleapis.apache GoogleApacheHttpTransport])
  (:import [com.google.api.client.json.jackson2 JacksonFactory])
  (:import [com.google.api.client.googleapis.auth.oauth2 GoogleCredential$Builder])
  (:import [com.google.api.client.util.store FileDataStoreFactory])
  (:import [System])
  (:import [java.io File])
  (:gen-class))

;;relative path:
(def relPath (System/getProperty "user.dir"))
;;These need to be filled in.

(def userID "User id written from the google api. Long string containing letters and numbers.")
(def client_secret "Location of the client secret from the google api.")
(def file "location of the output file. Has to be created.")



(def storeF (new FileDataStoreFactory (new File (str relPath "/creds"))))
(def dataStore (. storeF getDataStore "credsDataStoreFile"))





;; ->
;; "https://www.googleapis.com/auth/gmail.readonly"
(defn getAccessCreds
  "Gets credentials from the client secret and by connecting to google."
  []
  (def trans (. GoogleApacheHttpTransport newTrustedTransport))
  (def json (new JacksonFactory))
  (def input (new FileReader client_secret))
  (def secret (. GoogleClientSecrets load json input))
  (def scopes '("https://www.googleapis.com/auth/gmail.readonly"))
  (def preflow (new GoogleAuthorizationCodeFlow$Builder trans json secret scopes))
  (. preflow setAccessType "offline")
  (. preflow setCredentialDataStore dataStore)
  (def flow (. preflow build))

  (def aServerForConnetion (.(.(new LocalServerReceiver$Builder) setPort 8888)  build))
  (def preCreds (new AuthorizationCodeInstalledApp flow aServerForConnetion))

  (. preCreds authorize "user")
  ;;(println "AccessToken:")
  ;;(println (. creds getAccessToken))
  ;;(println "RefreshToken:")
  ;;(println (. creds getRefreshToken))
  )


;; api call with client secret...... So only add the secret +++++
(defn singleCall
  "Call the server once with the clint secret. Only needs the client secret to be set."
  ([]
   (let [creds (getAccessCreds)]
     (. creds refreshToken)
     ;;(print (creds))
     (print (. creds getAccessToken))
     (def auth (str "Bearer " (. creds getAccessToken)))
     (print auth)
     (http/get "https://www.googleapis.com/gmail/v1/users/me/messages"
               {:headers  {"Authorization", auth}})
     ))
  )



(defn call
  "Calls the google api address and returns the return message."
  [address]
  (let [creds (getAccessCreds)]
    (. creds refreshToken)
    (def auth (str "Bearer " (. creds getAccessToken)))
    (http/get address
              {:headers  {"Authorization", auth}})
    )
  )


(defn myjoin [va vb]
  "Works maybe not tested. Used concat instead."
  (if va
    (recur (rest va) (cons (first va) vb))))


(defn parseMessages0
  "Calls the google api on the address and gets a list of all the message id's"
  ([address]
   
   (def rawestRespose (call address))
   (def response1 (json/read-str (:body rawestRespose) :key-fn keyword))
   (def nextPage (response1 :nextPageToken))
   

                                        ;(println response1)
   (println (str "next page token: "nextPage))
   (println (count (set (map :id (response1 :messages)))))
   
   (if nextPage
     (concat
      (into[] (parseMessages0 address nextPage))
      (into[] (response1 :messages))
      )
     (into[] (response1 :messages))
     )
   )
  ([address pageToken]
   (def pageAddress (str address "?pageToken=" pageToken))
   
   (def rawestRespose (call pageAddress))

   (def response (json/read-str (:body rawestRespose) :key-fn keyword))
   (def nextPage (response :nextPageToken))

   ;(println response)
   (println (str "next page token: "nextPage))
   (println (count (set (map :id (response :messages)))))

   (if nextPage
     (concat
      (into[] (parseMessages0 address nextPage))
      (into[] (response :messages))
      )
     (into[] (response :messages))
     )
   )
  )


(defn findSubject
  [set]
  (if (= (:name set) "Subject") (:value set)))



;;;results will be cons'd to r
;;;s contains the string to do operations on
;;;x is the target feature that is being parsed
;;;returns ((["feature:" "value"]) "leftover string")
(defn finder
  "Parses the feature x from string containing features s.
  Designed to be used with fold from outside.
  returns a list [r s] where r is the found result and s is the string after the point of the parsed resultt.
  "
  [[r s :as in] x]
  (if in
    (do
      (def match (re-find (re-pattern (str "(.*)(" x ")(.+)")) s))
      (if (not-any? empty? [(get match 2) (get match 3)])
        (do
          (println (get match 3))
          (cons
         (cons {(keyword (get match 2)) (get match 3)} r)
         [(get match 1)]
         ))))
    ))

;;test
(def firs [{:a 1} {:b 2} {:c 3} {:d 4} {:e 5}])
(def secend [{:a 10} {:b 9} {:c 8} {:d 7} {:e 6}])
(concat firs secend)


;;;parses json from the string given.
;;;s is string containing the fields
;;;xs is the fields to be parsed in revrese order of occurance.
(defn parser
  "Parses features xs from string s. If thay are found int the same order."
  ([s xs]
   (->>
    (first (reduce finder (cons [[] s] xs ) )) ;;left strict fold with accumulator?
    ;;(json/write-str)
   )
   )
  )


(defn parserStarter
  "parses from s a single instance of the parameters found."
  [s]
  (parser s ["Battery level:", "Sys.Temp:", "GSM Strength:", "DI:", "TARA:", "NET:", "Scale:"])
  )


;;gets the specific message with the id and parses data to json and timestamp.
;;(getAndParse 163bf05f0efea46f)
(defn getAndParse
  "Gets a message based on it's id.
  And then parses its content to {:data res :timeStamp header :id id} kind of form."
  [id]
   (def rawMessage (call (str "https://www.googleapis.com/gmail/v1/users/me/messages" "/" id "?format=full")))
   ;;   (println rawMessage)

   ;;parsing the raw message
   ;;parsing the data
   (def data (:snippet (json/read-str (:body rawMessage) :key-fn keyword)))
   ;;parsing timestamp
   ;;(print data)
   (as-> (:body rawMessage) in
     (json/read-str in :key-fn keyword)
     (:payload in)
     (:headers in)
     (map findSubject in)
     (filter identity in)
     (def header in)
     )
      (println header)
   ;;   (println data)
  ;;   (println (join header)

  (if (not (empty? header))
    (do
      (def hType (first (s/split (first header) #" ")))

   ;;(println data)
   
   (if (= "Report" hType)
     ;;more error handling

     (let [res (parserStarter data)]
       (if res
         {:data res :timeStamp header :id id})))
   )
    (do
      (println "failed parse for header!!!!!!")
      (println header)
      (println data)))
  )


(defn parseMessages
  "Returns all id's of the messages."
  []
    (map :id (parseMessages0 "https://www.googleapis.com/gmail/v1/users/me/messages")))


(defn addIdKey
  "not used."
  [data id]
  (conj data {id :id})
  )


(defn iter
  "component for the zip"
  [[r & rs] b]
   (let [[[x & xs]] rs]
     [(conj r [x b]) xs]
   )
  )

(defn zip
  "the usual zip function."
  [[a :as as] [b :as bs]]
  (first (r/reduce iter [[] as] bs)))

(defn sprnt
  "Helper function."
  [args]
  (println (str args))
  (identity args))



(defn loopId [l r]
  "recursive logick of getId"
  (if
      (empty? l) nil
      (do
        (->>
         (first l)
         (into [])
         (def aa))
        
        (if (= (get (get aa 2) 0) "id")
          (recur (rest aa) (concat (list (get (get aa 2) 1) r)))
          (recur (rest aa) r)))))


(defn getId [l]
  "gets id's from list l
l has to be the correct shape that arrives from the goole api."
  (loopId l '()))


(defn updateData
  "Reads the past id's from file and loads the content of the id's that aren't found there from google and saves that to the file location."
  []

  ;;(def file "/home/esko/OmatProjektit/clojure/mailfetcher/src/mailfetcher/data.json")
  ;;Defined on top of this file.

  (->>
   (slurp file)
   (json/read-str)
   (def storedJson))

  (println)
  (println "JSON from the store")
  (println storedJson)
  (println "JSON from the store")
  (println)

   (->>
   (getId storedJson)
   (def storedData)
   )

  (->>
   (parseMessages)
   (def newMessages)
   )
  
  (println (str "Stored data entries:" (count storedData)))
  (println (str "New data entries:" (count newMessages)))
  
  (def newIds (sets/difference (set newMessages) (set storedData)))
  (println (str "Id's to add: "newIds))
  (println (str "Count of new Ids to add: "(count newIds)))

  (def save (json/write-str (concat storedJson (map getAndParse newIds))))
  ;;(println save)
  (spit file save)
;;(spit file (map println       :append true)
;;(spit file (map println (println (r/foldcat (r/map getAndParse newIds)) newIds)) :append true)
  )


(defn storageLocation
  "get loaction of credential storage."
  []
  (. storeF getDataDirectory))


(defn createCreds
  "Gets new creds"
  []
  (getAccessCreds))


(defn testStoredCreds
  
  []
  (def trans (. GoogleApacheHttpTransport newTrustedTransport))
  (def json (new JacksonFactory))
  (def input (new FileReader client_secret))
  (def secret (. GoogleClientSecrets load json input))
  (def scopes '("https://www.googleapis.com/auth/gmail.readonly"))
  (def preflow (new GoogleAuthorizationCodeFlow$Builder trans json secret scopes))
  (. preflow setAccessType "offline")
  (. preflow setCredentialDataStore dataStore)
  (def flow (. preflow build))
  (let [creds (. flow loadCredential "user")]
    ;;(print (creds))
    (. creds refreshToken)
    (print (. creds getAccessToken))
    (def auth (str "Bearer " (. creds getAccessToken)))
    (print auth)
    (http/get "https://www.googleapis.com/gmail/v1/users/me/messages"
              {:headers  {"Authorization", auth}})
    )
  )


(defn setToken
  "Not used currenlly. Maybe not working."
  []
  (def token (slurp accessToken))
  (def newMessages (parseMessages "https://www.googleapis.com/gmail/v1/users/me/messages"))
  )


(defn -main
  "Parses new messages from the scale to a file called data.json"
  [& args]
  (updateData))

