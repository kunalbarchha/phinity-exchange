public class ToDo {

    /*
    TODO:
    1. store user sessions in redis --- Done. Need updates for KYC and AML.
    2. create /personal response object. -- done.
    3. write scheduler to check User object in mongo and redis. Handle if any mismatch. --- pending
    4. update User data in KYC, personal/, and POA update. -- done
    5. upload user documents to S3.
    5. Store KYC response from complyCube and other providers for record keeping. -- done
    6. validate IP at login against whitelisted IP.
    7. Registration validation -> Check if mobile already registered. Mobile number has to be unique for all users.
    8. Keep user KYCStatus PENDING as long as documents are not uploaded. When submitted, set INITIATED_BY_USER.
    9. Strict security for communication through Kafka
    10. Maker and Taker fee are set whichever is highest by order-service. After execution actual status will be
    identified by matching-service. In case of excess fee, refund the exceeding fee back to user.
    11. Create Document of all ENV variables in sections for each service.
    12. Compile JAR Snapshot versioning should be dynamic instead of hardcoded 1.0
     */
}
