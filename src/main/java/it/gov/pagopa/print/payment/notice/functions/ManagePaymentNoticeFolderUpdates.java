package it.gov.pagopa.print.payment.notice.functions;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.Cardinality;
import com.microsoft.azure.functions.annotation.EventHubTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.mongodb.client.*;
import com.mongodb.client.internal.MongoClientImpl;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequest;
import it.gov.pagopa.print.payment.notice.functions.service.NoticeFolderService;
import it.gov.pagopa.print.payment.notice.functions.service.impl.NoticeFolderServiceImpl;
import org.apache.commons.io.FileUtils;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static java.util.Arrays.asList;

/**
 * Azure Functions with Azure Queue trigger.
 */
public class ManagePaymentNoticeFolderUpdates {

    private final Logger logger = LoggerFactory.getLogger(ManagePaymentNoticeFolderUpdates.class);

    private static final String WORKING_DIRECTORY_PATH = System.getenv().getOrDefault("WORKING_DIRECTORY_PATH", "");

    private static final String PATTERN_FORMAT = "yyyy.MM.dd.HH.mm.ss";

    private final NoticeFolderService noticeFolderService;

    public ManagePaymentNoticeFolderUpdates() {
        this.noticeFolderService = new NoticeFolderServiceImpl();
    }

    ManagePaymentNoticeFolderUpdates(NoticeFolderService noticeFolderService) {
        this.noticeFolderService = noticeFolderService;
    }

    /**
     * This function will be invoked when a Queue trigger occurs
     *
     */
    @FunctionName("ManagePaymentNoticeFolderUpdatesProcess")
    public void processGenerateReceipt(
            @EventHubTrigger(
                    name = "PaymentNoticeRequest",
                    eventHubName = "", // blank because the value is included in the connection string
                    connection = "NOTICE_EVENTHUB_CONN_STRING",
                    cardinality = Cardinality.MANY)
            List<PaymentNoticeGenerationRequest> requestMsg,
            @BindingName(value = "PropertiesArray") Map<String, Object>[] properties,
            final ExecutionContext context) throws IOException {

        requestMsg.stream().filter(item -> (
                Objects.equals(
                        item.getNumberOfElementsProcessed() + item.getNumberOfElementsFailed(),
                        item.getNumberOfElementsTotal()))
                )
                .forEach(item -> {
                    try {
                        noticeFolderService.manageFolder(item);
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);

                    }
                });

    }

}
