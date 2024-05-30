package it.gov.pagopa.print.payment.notice.functions.client.impl;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import it.gov.pagopa.print.payment.notice.functions.client.PaymentNoticeGenerationRequestErrorClient;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequestError;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.Optional;

import static com.mongodb.MongoClientSettings.getDefaultCodecRegistry;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class PaymentNoticeGenerationRequestErrorClientImpl implements PaymentNoticeGenerationRequestErrorClient {

    private static PaymentNoticeGenerationRequestErrorClientImpl instance;

    private final MongoCollection<PaymentNoticeGenerationRequestError> mongoCollection;

    private PaymentNoticeGenerationRequestErrorClientImpl() {
        String connectionString = System.getenv("NOTICE_REQUEST_MONGODB_CONN_STRING");
        String databaseName = System.getenv("NOTICE_REQUEST_MONGO_DB_NAME");
        String collectionName = System.getenv("NOTICE_ERR_REQUEST_MONGO_COLLECTION_NAME");


        CodecProvider pojoCodecProvider = PojoCodecProvider.builder().automatic(true).build();
        CodecRegistry pojoCodecRegistry = fromRegistries(getDefaultCodecRegistry(),
                fromProviders(pojoCodecProvider));
        MongoClient mongoClient = MongoClients.create(connectionString);
        MongoDatabase database = mongoClient.getDatabase(databaseName)
                .withCodecRegistry(pojoCodecRegistry);
        mongoCollection = database.getCollection(collectionName, PaymentNoticeGenerationRequestError.class);

    }

    PaymentNoticeGenerationRequestErrorClientImpl(MongoCollection<PaymentNoticeGenerationRequestError> mongoCollection) {
        this.mongoCollection = mongoCollection;
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
        mongoCollection.updateOne(Filters.eq("_id", paymentNoticeGenerationRequestError.getId()),
                Updates.inc("numberOfAttempts", 1));
    }

    @Override
    public Optional<PaymentNoticeGenerationRequestError> findOne(String folderId) {
        return Optional.ofNullable(mongoCollection.find(Filters.eq("folderId", folderId)).first());
    }

    @Override
    public void deleteRequestError(String id) {
        mongoCollection.deleteOne(Filters.eq("folderId", id));
    }

}
