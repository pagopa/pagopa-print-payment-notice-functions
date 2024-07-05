package it.gov.pagopa.print.payment.notice.functions.client.impl;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import it.gov.pagopa.print.payment.notice.functions.client.PaymentNoticeGenerationRequestClient;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequest;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.Optional;

import static com.mongodb.MongoClientSettings.getDefaultCodecRegistry;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class PaymentNoticeGenerationRequestClientImpl implements PaymentNoticeGenerationRequestClient {

    private static PaymentNoticeGenerationRequestClientImpl instance;

    private static final CodecRegistry pojoCodecRegistry = fromRegistries(getDefaultCodecRegistry(),
            fromProviders(PojoCodecProvider.builder().automatic(true).build()));

    private static MongoClient mongoClient;

    private static MongoClient createClient() {
        String connectionString = System.getenv("NOTICE_REQUEST_MONGODB_CONN_STRING");
        return mongoClient != null ? mongoClient : MongoClients.create(connectionString);
    }

    public MongoCollection<PaymentNoticeGenerationRequest> getMongoCollection(MongoClient mongoClient) {
        String databaseName = System.getenv("NOTICE_REQUEST_MONGO_DB_NAME");
        String collectionName = System.getenv("NOTICE_REQUEST_MONGO_COLLECTION_NAME");
        try {
            MongoDatabase database = mongoClient.getDatabase(databaseName)
                    .withCodecRegistry(pojoCodecRegistry);
            return database.getCollection(
                    collectionName, PaymentNoticeGenerationRequest.class);
        } catch (Exception e) {
            mongoClient.close();
            throw new RuntimeException("Error recovering db", e);
        }
    }

    PaymentNoticeGenerationRequestClientImpl() {}

    PaymentNoticeGenerationRequestClientImpl(MongoClient mongoClient) {
        PaymentNoticeGenerationRequestClientImpl.mongoClient = mongoClient;
    }

    public static PaymentNoticeGenerationRequestClientImpl getInstance() {
        if(instance == null) {
            instance = new PaymentNoticeGenerationRequestClientImpl();
        }

        return instance;
    }

    @Override
    public Optional<PaymentNoticeGenerationRequest> findById(String folderId) {
        try (MongoClient mongoClient = createClient()) {
            return Optional.ofNullable(getMongoCollection(mongoClient).find(Filters.eq("_id", folderId)).first());
        }
    }

    @Override
    public void updatePaymentGenerationRequest(PaymentNoticeGenerationRequest paymentNoticeGenerationRequest) {
        try (MongoClient mongoClient = createClient()) {
            getMongoCollection(mongoClient).updateOne(Filters.eq("_id", paymentNoticeGenerationRequest.getId()),
                    Updates.set("status", paymentNoticeGenerationRequest.getStatus().name()));
        }
    }

}
