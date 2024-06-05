package it.gov.pagopa.print.payment.notice.functions.client.impl;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import it.gov.pagopa.print.payment.notice.functions.client.PaymentNoticeGenerationRequestErrorClient;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequestError;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.Optional;

import static com.mongodb.MongoClientSettings.getDefaultCodecRegistry;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class PaymentNoticeGenerationRequestErrorClientImpl implements PaymentNoticeGenerationRequestErrorClient {

    private static PaymentNoticeGenerationRequestErrorClientImpl instance;

    private static MongoClient mongoClient;
    private static final CodecRegistry pojoCodecRegistry = fromRegistries(getDefaultCodecRegistry(),
            fromProviders(PojoCodecProvider.builder().automatic(true).build()));


    private PaymentNoticeGenerationRequestErrorClientImpl() {
        createClient();
    }

    private static void createClient() {
        String connectionString = System.getenv("NOTICE_REQUEST_MONGODB_CONN_STRING");
        mongoClient = MongoClients.create(connectionString);
    }

    PaymentNoticeGenerationRequestErrorClientImpl(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    public MongoCollection<PaymentNoticeGenerationRequestError> getMongoCollection() {
        String databaseName = System.getenv("NOTICE_REQUEST_MONGO_DB_NAME");
        String collectionName = System.getenv("NOTICE_ERR_REQUEST_MONGO_COLLECTION_NAME");
        if (mongoClient == null) {
            createClient();
        }
        try {
            MongoDatabase database = mongoClient.getDatabase(databaseName)
                    .withCodecRegistry(pojoCodecRegistry);
            return database.getCollection(collectionName, PaymentNoticeGenerationRequestError.class);
        } catch (Exception e) {
            mongoClient.close();
            mongoClient = null;
            throw new RuntimeException("Error recovering db", e);
        }
    }

    public static PaymentNoticeGenerationRequestErrorClientImpl getInstance() {
        if (instance == null) {
            instance = new PaymentNoticeGenerationRequestErrorClientImpl();
        }

        return instance;
    }

    @Override
    public void updatePaymentGenerationRequestError(
            PaymentNoticeGenerationRequestError paymentNoticeGenerationRequestError) {
        getMongoCollection().updateOne(Filters.eq("_id",
                        paymentNoticeGenerationRequestError.getId()),
                Updates.inc("numberOfAttempts", 1));
    }

    @Override
    public Optional<PaymentNoticeGenerationRequestError> findOne(String folderId) {
        return Optional.ofNullable(getMongoCollection().find(
                Filters.eq("folderId", folderId)).first());
    }

    @Override
    public void deleteRequestError(String id) {
        getMongoCollection().deleteOne(Filters.eq("folderId", id));
    }

    @Override
    public String save(
            PaymentNoticeGenerationRequestError paymentNoticeGenerationRequestError) {
        return getMongoCollection().insertOne(paymentNoticeGenerationRequestError)
                .getInsertedId().asString().getValue();
    }

}
