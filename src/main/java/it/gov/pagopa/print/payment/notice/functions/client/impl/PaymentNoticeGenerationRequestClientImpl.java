package it.gov.pagopa.print.payment.notice.functions.client.impl;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import it.gov.pagopa.print.payment.notice.functions.client.PaymentNoticeGenerationRequestClient;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequest;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.Optional;

import static com.mongodb.MongoClientSettings.getDefaultCodecRegistry;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class PaymentNoticeGenerationRequestClientImpl implements PaymentNoticeGenerationRequestClient {

    private static PaymentNoticeGenerationRequestClientImpl instance;

    private final MongoCollection<PaymentNoticeGenerationRequest> mongoCollection;

    private PaymentNoticeGenerationRequestClientImpl() {
        String connectionString = System.getenv("NOTICE_REQUEST_MONGODB_CONN_STRING");
        String databaseName = System.getenv("NOTICE_REQUEST_MONGO_DB_NAME");
        String collectionName = System.getenv("NOTICE_REQUEST_MONGO_COLLECTION_NAME");


        CodecProvider pojoCodecProvider = PojoCodecProvider.builder().automatic(true).build();
        CodecRegistry pojoCodecRegistry = fromRegistries(getDefaultCodecRegistry(),
                fromProviders(pojoCodecProvider));
        MongoClient mongoClient = MongoClients.create(connectionString);
        MongoDatabase database;
        try {
            database = mongoClient.getDatabase(databaseName)
                    .withCodecRegistry(pojoCodecRegistry);
        } catch (Exception e) {
            mongoClient.close();
            throw e;
        }
        mongoCollection = database.getCollection(collectionName, PaymentNoticeGenerationRequest.class);

    }

    PaymentNoticeGenerationRequestClientImpl(MongoCollection<PaymentNoticeGenerationRequest> mongoCollection) {
        this.mongoCollection = mongoCollection;
    }

    public static PaymentNoticeGenerationRequestClientImpl getInstance() {
        if(instance == null) {
            instance = new PaymentNoticeGenerationRequestClientImpl();
        }

        return instance;
    }

    @Override
    public Optional<PaymentNoticeGenerationRequest> findById(String folderId) {
        return Optional.ofNullable(mongoCollection.find(Filters.eq("_id", folderId)).first());
    }

    @Override
    public void updatePaymentGenerationRequest(PaymentNoticeGenerationRequest paymentNoticeGenerationRequest) {
        mongoCollection.updateOne(Filters.eq("_id", paymentNoticeGenerationRequest.getId()),
                Updates.set("status", paymentNoticeGenerationRequest.getStatus().name()));
    }

}
