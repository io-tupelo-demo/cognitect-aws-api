(ns tst.demo.core
  (:use demo.core tupelo.core tupelo.test)
  (:require
    [cognitect.aws.client.api :as aws]
    [tupelo.java-time :as time]
    [tupelo.string :as str]
    ))

(dotest
  ; Use the TUID (Time Unique ID) to make a unique bucket name `dummy-tmp-2022-0315-211839-987802432`
  (let [s3-bucket-name (format "dummy-tmp-%s" (str/clip 26 (time/tuid-str)))
        >>             (println (format "\n  s3-bucket-name => %s \n " s3-bucket-name))
        s3-client      (aws/client {:api    :s3
                                    :region :us-west-1 ; Note! must use keyword, not string!
                                    })]
    (is= cognitect.aws.client.Client (type s3-client))

    ; Tell the client to let you know when you get the args wrong:
    (is= true (aws/validate-requests s3-client true))

    ; Look up docs for an operation:
    (let [text (with-out-str (aws/doc s3-client :CreateBucket))] ; prints an HTTP format string
      (is (str/contains-str? text "<p>Creates a new S3 bucket. To create a bucket, you must")))

    (let [result (aws/invoke s3-client {:op :ListBuckets})
          ; http-request and http-response are in the metadata
          req    (:http-request (meta result))
          resp   (:http-response (meta result))]
      (comment ; sample output
        {:Buckets []
         :Owner   {:DisplayName "your-diaplayname"
                   :ID          "e25e3c1xxxxxxxxxxxxxxxxxxxxxx90yyyyyyyyyyyyyyyyyyyyyy21b4c3b7b93"}})
      (is= (map-vals result (const->fn nil))
        {:Buckets nil
         :Owner   nil})
      (when false ; for debugging
        (spyx-pretty req)
        (spyx-pretty resp))
      )

    (let [
          ; Create the bucket
          create-result (aws/invoke s3-client {:op      :CreateBucket
                                               :request {:Bucket                    s3-bucket-name
                                                         :CreateBucketConfiguration {:LocationConstraint "us-west-1"}
                                                         }})
          ; List all buckets
          list-result   (aws/invoke s3-client {:op :ListBuckets})

          ; Delete the bucket
          delete-result (aws/invoke s3-client {:op      :DeleteBucket
                                               :request {:Bucket                    s3-bucket-name
                                                         :CreateBucketConfiguration {:LocationConstraint "us-west-1"}
                                                         }})
          ]
      ; verify bucket creation
      (is= create-result {:Location (format "http://%s.s3.amazonaws.com/" s3-bucket-name)})

      ; verify new bucket name when list all buckets
      (let [all-bucket-names (set
                               (mapv :Name
                                 (grab :Buckets list-result)))]
        (is (contains-key? all-bucket-names s3-bucket-name)))

      ; the result of deleting the bucket is an empty map (no errors)
      (is= {} delete-result))

    ; how to get docstring information. Use `(println ...)` to display
    (let [getobject-resp   (with-out-str (aws/doc s3-client :GetObject))
          listbuckets-resp (with-out-str (aws/doc s3-client :ListBuckets))]
      (is (str/contains-str? getobject-resp "<p>Retrieves objects from Amazon S3. To use"))
      (is (str/contains-str? (str/whitespace-collapse getobject-resp)
            (str/whitespace-collapse
              (str/quotes->double
                " -------------------------
                  Request

                  {:IfNoneMatch string,
                   :IfUnmodifiedSince timestamp,
                   :Bucket string,
                   :Key string,
                   :SSECustomerKeyMD5 string,
                   :ResponseContentLanguage string,
                   :ResponseContentType string,
                   :PartNumber integer,
                   :IfModifiedSince timestamp,
                   :ChecksumMode [:one-of ['ENABLED']],
                   :SSECustomerKey blob,
                   :Range string,
                   :IfMatch string,
                   :RequestPayer [:one-of ['requester']],
                   :ExpectedBucketOwner string,
                   :ResponseContentEncoding string,
                   :SSECustomerAlgorithm string,
                   :ResponseCacheControl string,
                   :ResponseExpires timestamp,
                   :ResponseContentDisposition string,
                   :VersionId string}

                  Required

                  [:Bucket :Key] "))))

      (is (str/contains-str? listbuckets-resp "<p>Returns a list of all buckets owned by the authenticated sender")))

    ; Ask what ops your client can perform:
    (is= (sort (keys (aws/ops s3-client)))
      [:AbortMultipartUpload
       :CompleteMultipartUpload
       :CopyObject
       :CreateBucket
       :CreateMultipartUpload
       :DeleteBucket
       :DeleteBucketAnalyticsConfiguration
       :DeleteBucketCors
       :DeleteBucketEncryption
       :DeleteBucketIntelligentTieringConfiguration
       :DeleteBucketInventoryConfiguration
       :DeleteBucketLifecycle
       :DeleteBucketMetricsConfiguration
       :DeleteBucketOwnershipControls
       :DeleteBucketPolicy
       :DeleteBucketReplication
       :DeleteBucketTagging
       :DeleteBucketWebsite
       :DeleteObject
       :DeleteObjectTagging
       :DeleteObjects
       :DeletePublicAccessBlock
       :GetBucketAccelerateConfiguration
       :GetBucketAcl
       :GetBucketAnalyticsConfiguration
       :GetBucketCors
       :GetBucketEncryption
       :GetBucketIntelligentTieringConfiguration
       :GetBucketInventoryConfiguration
       :GetBucketLifecycle
       :GetBucketLifecycleConfiguration
       :GetBucketLocation
       :GetBucketLogging
       :GetBucketMetricsConfiguration
       :GetBucketNotification
       :GetBucketNotificationConfiguration
       :GetBucketOwnershipControls
       :GetBucketPolicy
       :GetBucketPolicyStatus
       :GetBucketReplication
       :GetBucketRequestPayment
       :GetBucketTagging
       :GetBucketVersioning
       :GetBucketWebsite
       :GetObject
       :GetObjectAcl
       :GetObjectAttributes
       :GetObjectLegalHold
       :GetObjectLockConfiguration
       :GetObjectRetention
       :GetObjectTagging
       :GetObjectTorrent
       :GetPublicAccessBlock
       :HeadBucket
       :HeadObject
       :ListBucketAnalyticsConfigurations
       :ListBucketIntelligentTieringConfigurations
       :ListBucketInventoryConfigurations
       :ListBucketMetricsConfigurations
       :ListBuckets
       :ListMultipartUploads
       :ListObjectVersions
       :ListObjects
       :ListObjectsV2
       :ListParts
       :PutBucketAccelerateConfiguration
       :PutBucketAcl
       :PutBucketAnalyticsConfiguration
       :PutBucketCors
       :PutBucketEncryption
       :PutBucketIntelligentTieringConfiguration
       :PutBucketInventoryConfiguration
       :PutBucketLifecycle
       :PutBucketLifecycleConfiguration
       :PutBucketLogging
       :PutBucketMetricsConfiguration
       :PutBucketNotification
       :PutBucketNotificationConfiguration
       :PutBucketOwnershipControls
       :PutBucketPolicy
       :PutBucketReplication
       :PutBucketRequestPayment
       :PutBucketTagging
       :PutBucketVersioning
       :PutBucketWebsite
       :PutObject
       :PutObjectAcl
       :PutObjectLegalHold
       :PutObjectLockConfiguration
       :PutObjectRetention
       :PutObjectTagging
       :PutPublicAccessBlock
       :RestoreObject
       :SelectObjectContent
       :UploadPart
       :UploadPartCopy
       :WriteGetObjectResponse])

    ))
