package it.gov.pagopa.print.payment.notice.functions.repository;

import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentGenerationRequestStatus;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

@Repository
public interface PaymentGenerationRequestRepository extends MongoRepository<PaymentNoticeGenerationRequest, String> {

    /**
     * Updates only the final status of the massive-generation request.
     * A partial update is used to avoid replacing the whole Mongo document. 
     */
    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'status': ?1 } }")
    long updateStatusById(
            String folderId,
            PaymentGenerationRequestStatus status);
    
}
