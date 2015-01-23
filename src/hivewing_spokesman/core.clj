(ns hivewing-spokesman.core
  (:require
            [taoensso.timbre :as logger]
            [hivewing-core.configuration :as config]
            [hivewing-core.spokesman :as spokesman]
            [amazonica.aws.sqs :as sqs]
            [amazonica.aws.simpleemail :as ses]
            [clojure.data.json :as json]
            [clj-http.client :as http])
  (:gen-class))

(defn process-email-send
  [{to :to
    subject :subject
    from :from
    text :text
    html :html
    :as data}]

  (logger/info "Sending email" subject to from text html)

  (comment ses/send-email config/ses-aws-credentials
      "if you send an email here it succeeds"
      "success@simulator.amazonses.com"
      :destination {:to-addresses ["example@example.com"]}
        :source "no-reply@example.com"
        :message {:subject "Test Subject"
          :body {:html "testing 1-2-3-4"
                                :text "testing 1-2-3-4"}}))

(comment
  (def target "http://ziplist.com/apple")
  (def data {:apple "pear"})
  (def headers {"X-Hivewing" "pears"})
  (process-post-hook {:target target :headers headers :data data})
  )

(defn process-post-hook
  [{target :target
    headers :headers
    data :data}]

  (logger/info "Sending POST-hook" target headers data)
  (let [response (http/post target {
          :headers headers
          :body (json/write-str data)
          :content-type :json
          :force-redirects true
          :socket-timeout 500  ;; in milliseconds
          :conn-timeout   500  ;; in milliseconds
          :client-params {"http.protocol.allow-circular-redirects" false
                          "http.useragent" "hivewing-spokesman/1.0"}})]
    (logger/info "Response: " target " => " (:status response))))

(defn process-incoming-message
  "The messages are received by the system and processed here"
  [msg]
  (doseq [msg-key (keys msg)]
    (let [data (get msg msg-key)]
      (logger/info "Processing " msg-key " : " data)
      (try
        (case msg-key
          :email (process-email-send data)
          :post-hook (process-post-hook data)
          )
        (catch Exception ex
          (logger/error (str "Error: " (.getMessage ex))))))))

(defn -main
  "Start up the subscribe loop and try to process any incoming messages"
  [& args]
  (logger/info "Starting hivewing-spokesman process")
  (let [incoming-queue (spokesman/spokesman-sqs-queue)]
    (logger/info "Incoming queue: " incoming-queue)
    (while true
      (try
        (let [msgs (:messages (sqs/receive-message config/sqs-aws-credentials
                                       :queue-url incoming-queue
                                       :wait-time-seconds 1
                                       :max-number-of-messages 10
                                       :delete false))]
          (if (empty? msgs)
            (Thread/sleep 500)
            (do
              (logger/info "Received " (count msgs) " messages")
              (logger/info "received " msgs)

              (doseq [packed-msg msgs]
                ; Unpack it - it's just prn-str for now.
                (let [msg (read-string (:body packed-msg))]
                  ; Process
                  (process-incoming-message msg)
                  ; Delete
                  (sqs/delete-message config/sqs-aws-credentials incoming-queue (:receipt-handle packed-msg)))))))
        (catch Exception e (logger/error "Exception: " (.getMessage e)))))))
