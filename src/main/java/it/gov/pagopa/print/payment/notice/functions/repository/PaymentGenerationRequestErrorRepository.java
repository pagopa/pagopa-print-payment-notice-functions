package it.gov.pagopa.print.payment.notice.functions.repository;

import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequestError;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentGenerationRequestErrorRepository
        extends MongoRepository<PaymentNoticeGenerationRequestError, String> {

    /*
     * CompressionService creates a new ErrorEvent after every compression
     * failure without carrying the persisted error id.
     *
     * Reuse the existing compression error for the same folder so that the
     * retry counter stored in Mongo remains the source of truth.
     *
     * Ordering by numberOfAttempts also makes the lookup safer in case legacy
     * duplicate error records already exist for the same folder.
     */
    Optional<PaymentNoticeGenerationRequestError>
            findTopByFolderIdAndCompressionErrorTrueOrderByNumberOfAttemptsDesc(
                    String folderId);

    /*
     * Check the retry limit and increment the counter atomically.
     *
     * This prevents concurrent or duplicate deliveries from both acquiring
     * the same retry attempt.
     */
    @Query("{ '_id': ?0, 'numberOfAttempts': { '$lt': ?1 } }")
    @Update("{ '$inc': { 'numberOfAttempts': 1 } }")
    long incrementNumberOfAttemptsIfBelowMax(
            String id,
            int maxRetries);
}