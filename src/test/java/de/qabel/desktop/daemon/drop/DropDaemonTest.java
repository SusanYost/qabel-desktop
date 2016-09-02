package de.qabel.desktop.daemon.drop;


import de.qabel.core.config.Contact;
import de.qabel.core.config.Entity;
import de.qabel.core.config.Identity;
import de.qabel.core.crypto.QblECPublicKey;
import de.qabel.core.drop.DropMessage;
import de.qabel.core.drop.DropURL;
import de.qabel.core.exceptions.QblDropInvalidURL;
import de.qabel.core.exceptions.QblNetworkInvalidResponseException;
import de.qabel.core.http.DropServerHttp;
import de.qabel.core.http.MainDropConnector;
import de.qabel.core.http.MockDropServer;
import de.qabel.core.repository.entities.ChatDropMessage;
import de.qabel.core.repository.entities.DropState;
import de.qabel.core.repository.exception.EntityNotFoundException;
import de.qabel.core.repository.exception.PersistenceException;
import de.qabel.core.repository.inmemory.InMemoryChatDropMessageRepository;
import de.qabel.core.service.ChatService;
import de.qabel.core.service.MainChatService;
import de.qabel.desktop.repository.inmemory.InMemoryHttpDropConnector;
import de.qabel.desktop.ui.AbstractControllerTest;
import de.qabel.desktop.ui.actionlog.PersistenceDropMessage;
import de.qabel.desktop.ui.connector.DropConnector;
import de.qabel.desktop.ui.connector.DropPollResponse;
import kotlin.Triple;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import static de.qabel.desktop.AsyncUtils.assertAsync;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class DropDaemonTest extends AbstractControllerTest {
    private Contact c;
    private Thread daemon;
    private Identity identity;
    private Contact sender;
    private Identity senderIdentity;
    private DropDaemon dd;
    private ChatService chatService;
    private MainDropConnector dropConnector;
    private DropURL dropURL;
    private MockCoreDropConnector mockCoreDropConnector;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        identity = identityBuilderFactory.factory().withAlias("Tester").build();
        identityRepository.save(identity);
        clientConfiguration.selectIdentity(identity);
        c = getContact(identity);
        senderIdentity = identityBuilderFactory.factory().withAlias("sender").build();
        sender = getContact(senderIdentity);
        contactRepository.save(sender, identity);
        dropConnector= new MainDropConnector(new MockDropServer());
        mockCoreDropConnector = new MockCoreDropConnector();
        chatService = new MainChatService(mockCoreDropConnector, identityRepository,
            contactRepository, chatDropMessageRepository, dropStateRepository);
        dd = new DropDaemon(chatService, dropMessageRepository, contactRepository);
    }

    private void send(Contact contact, Entity sender) {
        ChatDropMessage.MessagePayload.TextMessage textMessage =
            new ChatDropMessage.MessagePayload.TextMessage("test_message");
        DropMessage dropMessage = new DropMessage(sender, textMessage.toString(),
            ChatDropMessage.MessageType.BOX_MESSAGE.getType());
        dropConnector.sendDropMessage(senderIdentity, contact, dropMessage, contact.getDropUrls().iterator().next());
    }

    @Test
    public void receiveMessagesTest() throws Exception {
        send(c, senderIdentity);

        dd.receiveMessages();

        List<PersistenceDropMessage> lst = dropMessageRepository.loadConversation(sender, identity);
        assertEquals(1, lst.size());
    }

    @Test
    public void handlesErrorsTest() throws Exception {
        dd.setSleepTime(1);
        send(c, sender);

        mockCoreDropConnector.e = new IllegalStateException("network error");

        startDaemon();

        waitUntil(() -> mockCoreDropConnector.polls > 0);
        mockCoreDropConnector.e = null;

        assertAsync(() -> dropMessageRepository.loadConversation(sender, identity), is(not(empty())));
    }

    @Test
    public void receivesMessagesForAllIdentities() throws Exception {
        Identity otherIdentity = identityBuilderFactory.factory().withAlias("tester2").build();
        Contact otherIdentitiesContact = getContact(otherIdentity);
        identityRepository.save(otherIdentity);
        contactRepository.save(sender, otherIdentity);

        send(otherIdentitiesContact, sender);

        dd.receiveMessages();

        assertThat(dropMessageRepository.loadConversation(sender, otherIdentity), is(not(empty())));
    }

    private Contact getContact(Identity otherIdentity) {
        return new Contact(otherIdentity.getAlias(), otherIdentity.getDropUrls(), otherIdentity.getEcPublicKey());
    }

    private void startDaemon() {
        daemon = new Thread(dd);
        daemon.setDaemon(true);
        daemon.start();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        if (daemon != null && daemon.isAlive()) {
            daemon.interrupt();
        }
        super.tearDown();
    }

    private class MockCoreDropConnector implements de.qabel.core.http.DropConnector {
        public RuntimeException e = null;
        int polls;

        @Override
        public void sendDropMessage(Identity identity, Contact contact, DropMessage dropMessage, DropURL dropURL) { }

        @NotNull
        @Override
        public DropServerHttp.DropServerResponse<DropMessage> receiveDropMessages(Identity identity, DropURL dropURL, DropState dropState) {
            polls++;
            if (e != null) {
                throw e;
            }
            return dropConnector.receiveDropMessages(identity, dropURL, dropState);
        }
    }
}
