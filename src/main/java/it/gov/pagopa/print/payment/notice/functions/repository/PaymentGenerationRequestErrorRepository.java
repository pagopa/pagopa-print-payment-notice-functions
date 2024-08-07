package it.gov.pagopa.print.payment.notice.functions.repository;

import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequestError;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentGenerationRequestErrorRepository extends MongoRepository<PaymentNoticeGenerationRequestError, String> {


}
