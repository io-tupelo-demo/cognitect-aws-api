(ns tst.demo.core
  (:use demo.core tupelo.core tupelo.test)
  (:require
    [cognitect.aws.client.api :as aws]
    [tupelo.string :as str]
    [clojure.walk :as walk]))

(dotest
  ; Create a client:
  (let [s3-client (aws/client {:api :s3
                               ; :region "us-west-1" ; #todo not working yet
                               })]
    (is= cognitect.aws.client.Client (type s3-client))

    ; Tell the client to let you know when you get the args wrong:
    (is= true (aws/validate-requests s3-client true))

    ; Look up docs for an operation:
    (let [text (with-out-str (aws/doc s3-client :CreateBucket))] ; prints an HTTP format string
      (is (str/contains-str? text "<p>Creates a new S3 bucket. To create a bucket, you must")))

    (let [result (aws/invoke s3-client {:op :ListBuckets})

          req    (:http-request (meta result)) ; http-request and http-response are in the metadata
          resp   (:http-response (meta result))]
      (comment      ; sample output
        {:Buckets []
         :Owner   {:DisplayName "your-diaplayname"
                   :ID          "e25e3c1xxxxxxxxxxxxxxxxxxxxxx902aa063fb9a41a0c9f7ebda21b4c3b7b93"}})
      (is= (map-vals result (const->fn nil))
        {:Buckets nil
         :Owner   nil})
      (when false
        (spyx-pretty req)
        (spyx-pretty resp))
      )

    ; delete any leftovers!
    (when false
      (aws/invoke s3-client {:op :DeleteBucket :request {:Bucket                    "dbxs-tmp-220302-2046"
                                                         :CreateBucketConfiguration {:LocationConstraint "us-west-1"}
                                                         }}))
    (let [create-result (aws/invoke s3-client {:op      :CreateBucket
                                               :request {:Bucket                    "dbxs-tmp-220302-2046"
                                                         :CreateBucketConfiguration {:LocationConstraint "us-west-1"}
                                                         }})
          list-result   (aws/invoke s3-client {:op :ListBuckets})
          delete-result (aws/invoke s3-client {:op      :DeleteBucket
                                               :request {:Bucket                    "dbxs-tmp-220302-2046"
                                                         :CreateBucketConfiguration {:LocationConstraint "us-west-1"}
                                                         }})
          ]
      (is= create-result {:Location "http://dbxs-tmp-220302-2046.s3.amazonaws.com/"})
      (is (submatch? {:Buckets
                      [{;:CreationDate #inst "2022-03-03T04:56:55.000-00:00",
                        :Name "dbxs-tmp-220302-2046"}],
                      :Owner {;:DisplayName "my-disp",
                              ;:ID "e25e3cd2e7110d47024f43e3f0eb1902aa063fb9a41a0c9f7ebda21b4c3b7b93"
                              }}
            list-result))
      (is= {} delete-result)
      )

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
