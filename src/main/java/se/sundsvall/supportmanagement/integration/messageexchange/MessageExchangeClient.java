package se.sundsvall.supportmanagement.integration.messageexchange;

import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static se.sundsvall.supportmanagement.integration.messageexchange.configuration.MessageExchangeConfiguration.CLIENT_ID;

import feign.QueryMap;
import generated.se.sundsvall.messageexchange.Conversation;
import generated.se.sundsvall.messageexchange.Message;
import generated.se.sundsvall.messageexchange.PageConversation;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.integration.messageexchange.configuration.MessageExchangeConfiguration;

@FeignClient(name = CLIENT_ID, url = "${integration.messageexchange.url}", configuration = MessageExchangeConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface MessageExchangeClient {

	/******************
	 * CONVERSATIONS
	 ******************/

	/**
	 * Create a conversation.
	 *
	 * @param  municipalityId the id of the municipality
	 * @param  namespace      the namespace
	 * @param  conversation   the Conversation to create
	 * @return
	 */
	@PostMapping(path = "/{municipalityId}/{namespace}/conversations", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> createConversation(
		@PathVariable("municipalityId") String municipalityId,
		@PathVariable("namespace") String namespace,
		@RequestBody Conversation conversation);

	/**
	 * Get a pageable list of conversations.
	 *
	 * @param  municipalityId the id of the municipality
	 * @param  namespace      the namespace
	 * @param  filter         the filter to use
	 * @param  pageable       the pageable object
	 * @return                a PageConversation object.
	 */
	@GetMapping(path = "/{municipalityId}/{namespace}/conversations", produces = APPLICATION_JSON_VALUE)
	ResponseEntity<PageConversation> getConversations(
		@RequestHeader(Identifier.HEADER_NAME) String identifier,
		@PathVariable("municipalityId") String municipalityId,
		@PathVariable("namespace") String namespace,
		@RequestParam("filter") String filter,
		@QueryMap Pageable pageable);

	/**
	 * Get a conversation by id.
	 *
	 * @param  municipalityId the id of the municipality
	 * @param  namespace      the namespace
	 * @param  conversationId the conversationId
	 * @return                a PageConversation object.
	 */
	@GetMapping(path = "/{municipalityId}/{namespace}/conversations/{conversationId}", produces = APPLICATION_JSON_VALUE)
	ResponseEntity<Conversation> getConversationById(
		@PathVariable("municipalityId") String municipalityId,
		@PathVariable("namespace") String namespace,
		@PathVariable("conversationId") String conversationId);

	/**
	 * Update a conversation by id.
	 *
	 * @param  municipalityId the id of the municipality
	 * @param  namespace      the namespace
	 * @param  conversationId the conversationId
	 * @param  conversation   the Conversation to update
	 * @return                a PageConversation object.
	 */
	@PatchMapping(path = "/{municipalityId}/{namespace}/conversations/{conversationId}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	ResponseEntity<Conversation> updateConversationById(
		@PathVariable("municipalityId") String municipalityId,
		@PathVariable("namespace") String namespace,
		@PathVariable("conversationId") String conversationId,
		@RequestBody Conversation conversation);

	/******************
	 * MESSAGES
	 ******************/

	/**
	 * Create message with attachments.
	 *
	 * @param  municipalityId the id of the municipality
	 * @param  namespace      the namespace
	 * @param  conversationId the conversationId
	 * @param  message        the json message
	 * @param  attachments    the list of attachments
	 * @return
	 */
	@PostMapping(path = "/{municipalityId}/{namespace}/conversations/{conversationId}/messages", consumes = MULTIPART_FORM_DATA_VALUE, produces = ALL_VALUE)
	ResponseEntity<Void> createMessage(
		@PathVariable("municipalityId") String municipalityId,
		@PathVariable("namespace") String namespace,
		@PathVariable("conversationId") String conversationId,
		@RequestPart("message") final Message message,
		@RequestPart(value = "attachments", required = false) final List<MultipartFile> attachments);

	/**
	 * Get a pageable list of messages.
	 *
	 * @param  municipalityId the id of the municipality
	 * @param  namespace      the namespace
	 * @param  conversationId the conversationId
	 * @param  pageable       the pageable object
	 * @return                a PageMessage object.
	 */
	@GetMapping(path = "/{municipalityId}/{namespace}/conversations/{conversationId}/messages", produces = APPLICATION_JSON_VALUE)
	ResponseEntity<Page<Message>> getMessages(
		@PathVariable("municipalityId") String municipalityId,
		@PathVariable("namespace") String namespace,
		@PathVariable("conversationId") String conversationId,
		@QueryMap Pageable pageable);

	/**
	 * Get a message attachment.
	 *
	 * @param  municipalityId the id of the municipality
	 * @param  namespace      the namespace
	 * @param  conversationId the conversationId
	 * @param  messageId      the messageId
	 * @param  attachmentId   the attachmentId
	 * @return                a PageMessage object.
	 */
	@GetMapping(path = "/{municipalityId}/{namespace}/conversations/{conversationId}/messages/{messageId}/attachments/{attachmentId}", produces = ALL_VALUE)
	ResponseEntity<InputStreamResource> getMessageAttachment(
		@PathVariable("municipalityId") String municipalityId,
		@PathVariable("namespace") String namespace,
		@PathVariable("conversationId") String conversationId,
		@PathVariable("messageId") String messageId,
		@PathVariable("attachmentId") String attachmentId);
}
