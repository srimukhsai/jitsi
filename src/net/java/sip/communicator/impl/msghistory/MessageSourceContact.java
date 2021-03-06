/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.msghistory;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.muc.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.util.*;

import java.util.*;

/**
 * Represents a contact source displaying a recent message for contact.
 * @author Damian Minkov
 */
public class MessageSourceContact
    extends DataObject
    implements SourceContact,
               Comparable<MessageSourceContact>
{
    /**
     * The parent service.
     */
    private final MessageSourceService service;

    /**
     * The address.
     */
    private String address = null;

    /**
     * The display name.
     */
    private String displayName = null;

    /**
     * The protocol provider.
     */
    private ProtocolProviderService ppService = null;

    /**
     * The status. Will reuse global status offline.
     */
    private PresenceStatus status = GlobalStatusEnum.OFFLINE;

    /**
     * The image.
     */
    private byte[] image = null;

    /**
     * The message content.
     */
    private String messageContent = null;

    /**
     * A list of all contact details.
     */
    private final List<ContactDetail> contactDetails
        = new LinkedList<ContactDetail>();

    /**
     * The contact instance.
     */
    private Contact contact = null;

    /**
     * The room instance.
     */
    private ChatRoom room = null;

    /**
     * The timestamp.
     */
    private Date timestamp = null;

    /**
     * The protocol provider.
     * @return the protocol provider.
     */
    public ProtocolProviderService getProtocolProviderService()
    {
        return ppService;
    }

    /**
     * Constructs <tt>MessageSourceContact</tt>.
     * @param source the source event.
     * @param service the message source service.
     */
    MessageSourceContact(EventObject source,
                         MessageSourceService service)
    {
        update(source);

        if(source instanceof MessageDeliveredEvent
            || source instanceof MessageReceivedEvent)
        {
            initDetails(false);
        }
        else if(source instanceof ChatRoomMessageDeliveredEvent
                || source instanceof ChatRoomMessageReceivedEvent)
        {
            initDetails(true);
        }

        this.service = service;

        updateMessageContent();
    }

    /**
     * Make sure the content of the message is not too long,
     * as it will fill up tooltips and ui components.
     */
    private void updateMessageContent()
    {
        if(this.messageContent != null
            && this.messageContent.length() > 60)
        {
            // do not display too long texts
            this.messageContent = this.messageContent.substring(0, 60);
            this.messageContent += "...";
        }
    }

    /**
     * Updates fields.
     * @param source the event object
     */
    void update(EventObject source)
    {
        if(source instanceof MessageDeliveredEvent)
        {
            MessageDeliveredEvent e = (MessageDeliveredEvent)source;

            this.contact = e.getDestinationContact();

            this.address = contact.getAddress();
            this.displayName = contact.getDisplayName();
            this.ppService = contact.getProtocolProvider();
            this.image = contact.getImage();
            this.status = contact.getPresenceStatus();
            this.messageContent = e.getSourceMessage().getContent();
            this.timestamp = e.getTimestamp();
        }
        else if(source instanceof MessageReceivedEvent)
        {
            MessageReceivedEvent e = (MessageReceivedEvent)source;

            this.contact = e.getSourceContact();

            this.address = contact.getAddress();
            this.displayName = contact.getDisplayName();
            this.ppService = contact.getProtocolProvider();
            this.image = contact.getImage();
            this.status = contact.getPresenceStatus();
            this.messageContent = e.getSourceMessage().getContent();
            this.timestamp = e.getTimestamp();
        }
        else if(source instanceof ChatRoomMessageDeliveredEvent)
        {
            ChatRoomMessageDeliveredEvent e
                = (ChatRoomMessageDeliveredEvent)source;

            this.room = e.getSourceChatRoom();

            this.address = room.getIdentifier();
            this.displayName = room.getName();
            this.ppService = room.getParentProvider();
            this.image = null;
            this.status = room.isJoined()
                ? ChatRoomPresenceStatus.CHAT_ROOM_ONLINE
                : ChatRoomPresenceStatus.CHAT_ROOM_OFFLINE;
            this.messageContent = e.getMessage().getContent();
            this.timestamp = e.getTimestamp();
        }
        else if(source instanceof ChatRoomMessageReceivedEvent)
        {
            ChatRoomMessageReceivedEvent e
                = (ChatRoomMessageReceivedEvent)source;

            this.room = e.getSourceChatRoom();

            this.address = room.getIdentifier();
            this.displayName = room.getName();
            this.ppService = room.getParentProvider();
            this.image = null;
            this.status = room.isJoined()
                ? ChatRoomPresenceStatus.CHAT_ROOM_ONLINE
                : ChatRoomPresenceStatus.CHAT_ROOM_OFFLINE;
            this.messageContent = e.getMessage().getContent();
            this.timestamp = e.getTimestamp();
        }

        updateMessageContent();
    }

    /**
     * Updates fields.
     * @param msc the object
     */
    void update(MessageSourceContact msc)
    {
        this.contact = msc.contact;

        this.address = contact.getAddress();
        this.displayName = contact.getDisplayName();
        this.ppService = contact.getProtocolProvider();
        this.image = contact.getImage();
        this.status = contact.getPresenceStatus();
        this.messageContent = msc.messageContent;
        this.timestamp = msc.timestamp;

        updateMessageContent();
    }

    @Override
    public String toString()
    {
        return "MessageSourceContact{" +
            "address='" + address + '\'' +
            ", ppService=" + ppService +
            '}';
    }

    /**
     * We will the details for this source contact.
     * Will skip OperationSetBasicInstantMessaging for chat rooms.
     * @param isChatRoom is current source contact a chat room.
     */
    private void initDetails(boolean isChatRoom)
    {
        ContactDetail contactDetail =
            new ContactDetail(
                    this.address,
                    this.displayName);

        Map<Class<? extends OperationSet>, ProtocolProviderService>
            preferredProviders;

        ProtocolProviderService preferredProvider
            = this.ppService;

        if (preferredProvider != null)
        {
            preferredProviders
                = new Hashtable<Class<? extends OperationSet>,
                                ProtocolProviderService>();

            LinkedList<Class<? extends OperationSet>> supportedOpSets
                = new LinkedList<Class<? extends OperationSet>>();

            for(Class<? extends OperationSet> opset
                    : preferredProvider.getSupportedOperationSetClasses())
            {
                // skip opset IM as we want explicitly muc support
                if(opset.equals(OperationSetPresence.class)
                    || opset.equals(OperationSetPersistentPresence.class)
                    || (isChatRoom
                        && opset.equals(
                                OperationSetBasicInstantMessaging.class)))
                {
                    continue;
                }

                preferredProviders.put(opset, preferredProvider);

                supportedOpSets.add(opset);
            }

            contactDetail.setPreferredProviders(preferredProviders);

            contactDetail.setSupportedOpSets(supportedOpSets);
        }

        contactDetails.add(contactDetail);
    }

    @Override
    public String getDisplayName()
    {
        if(this.displayName != null)
            return this.displayName;
        else
            return MessageHistoryActivator.getResources()
                .getI18NString("service.gui.UNKNOWN");
    }

    @Override
    public String getContactAddress()
    {
        if(this.address != null)
            return this.address;

        return null;
    }

    @Override
    public ContactSourceService getContactSource()
    {
        return service;
    }

    @Override
    public String getDisplayDetails()
    {
        return messageContent;
    }

    /**
     * Returns a list of available contact details.
     * @return a list of available contact details
     */
    @Override
    public List<ContactDetail> getContactDetails()
    {
        return new LinkedList<ContactDetail>(contactDetails);
    }

    /**
     * Returns a list of all <tt>ContactDetail</tt>s supporting the given
     * <tt>OperationSet</tt> class.
     * @param operationSet the <tt>OperationSet</tt> class we're looking for
     * @return a list of all <tt>ContactDetail</tt>s supporting the given
     * <tt>OperationSet</tt> class
     */
    @Override
    public List<ContactDetail> getContactDetails(
        Class<? extends OperationSet> operationSet)
    {
        List<ContactDetail> res = new LinkedList<ContactDetail>();

        for(ContactDetail det : contactDetails)
        {
            if(det.getPreferredProtocolProvider(operationSet) != null)
                res.add(det);
        }

        return res;
    }

    /**
     * Returns a list of all <tt>ContactDetail</tt>s corresponding to the given
     * category.
     * @param category the <tt>OperationSet</tt> class we're looking for
     * @return a list of all <tt>ContactDetail</tt>s corresponding to the given
     * category
     */
    @Override
    public List<ContactDetail> getContactDetails(
        ContactDetail.Category category)
        throws OperationNotSupportedException
    {
        // We don't support category for message source history details,
        // so we return null.
        throw new OperationNotSupportedException(
            "Categories are not supported for message source contact history.");
    }

    /**
     * Returns the preferred <tt>ContactDetail</tt> for a given
     * <tt>OperationSet</tt> class.
     * @param operationSet the <tt>OperationSet</tt> class, for which we would
     * like to obtain a <tt>ContactDetail</tt>
     * @return the preferred <tt>ContactDetail</tt> for a given
     * <tt>OperationSet</tt> class
     */
    @Override
    public ContactDetail getPreferredContactDetail(
        Class<? extends OperationSet> operationSet)
    {
        return contactDetails.get(0);
    }

    @Override
    public byte[] getImage()
    {
        return image;
    }

    @Override
    public boolean isDefaultImage()
    {
        return image == null;
    }

    @Override
    public void setContactAddress(String contactAddress)
    {}

    @Override
    public PresenceStatus getPresenceStatus()
    {
        return status;
    }

    /**
     * Sets current status.
     * @param status
     */
    public void setStatus(PresenceStatus status)
    {
        this.status = status;
    }

    @Override
    public int getIndex()
    {
        return service.getIndex(this);
    }

    /**
     * The contact.
     * @return the contact.
     */
    public Contact getContact()
    {
        return contact;
    }

    /**
     * The room.
     * @return the room.
     */
    public ChatRoom getRoom()
    {
        return room;
    }

    /**
     * The timestamp of the message.
     * @return the timestamp of the message.
     */
    public Date getTimestamp()
    {
        return timestamp;
    }

    /**
     * Compares two MessageSourceContacts.
     * @param o the object to compare with
     * @return 0, less than zero, greater than zero, if equals, less or greater.
     */
    @Override
    public int compareTo(MessageSourceContact o)
    {
        if(o == null
            || o.getTimestamp() == null)
            return 1;

        return o.getTimestamp()
            .compareTo(getTimestamp());
    }

    @Override
    public boolean equals(Object o)
    {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        MessageSourceContact that = (MessageSourceContact) o;

        if(!address.equals(that.address)) return false;
        if(!ppService.equals(that.ppService)) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = address.hashCode();
        result = 31 * result + ppService.hashCode();
        return result;
    }
}
