import { useEffect, useState, useRef, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import { useAuth } from './AuthContext.js';
import { api } from '../api/client';
import { ChatContext } from './ChatContext.js';

export function ChatProvider({ children }) {
  const { token, user } = useAuth();
  const [conversations, setConversations] = useState([]);
  const [activeConversationId, setActiveConversationId] = useState(null);
  const [newConversationUser, setNewConversationUser] = useState(null);
  const [messages, setMessages] = useState({}); // conversationId -> [message]
  const [pagination, setPagination] = useState({}); // conversationId -> { page, last }
  const [unreadConversationIds, setUnreadConversationIds] = useState(new Set());
  const [isConnected, setIsConnected] = useState(false);
  const [isChatOpen, setIsChatOpen] = useState(false);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [pairingSessions, setPairingSessions] = useState([]);
  const clientRef = useRef(null);

  // Refs to prevent the WebSocket connection from restarting when UI state changes
  const incomingMessageHandlerRef = useRef(null);
  const fetchPairingSessionsRef = useRef(null);
  const activeConversationIdRef = useRef(activeConversationId);
  const isChatOpenRef = useRef(isChatOpen);
  const userRef = useRef(user);

  const fetchConversations = useCallback(async () => {
    if (!token) return;
    try {
      const response = await api.get('/api/chat/conversations?page=0&size=50');
      const data = response?.content || response || [];
      setConversations(data);
      
      const initialUnread = new Set();
      if (Array.isArray(data)) {
        data.forEach(conv => {
          if (conv.unreadCount > 0 || conv.unread) initialUnread.add(conv.id);
        });
      }
      setUnreadConversationIds(initialUnread);
    } catch (err) {
      console.error('[Chat] Failed to fetch conversations:', err);
    }
  }, [token]);

  const fetchPairingSessions = useCallback(async () => {
    if (!token) return;
    try {
      console.log('[Chat] Refreshing pairing sessions...');
      const response = await api.get('/api/pairing/sessions?size=100');
      const data = response?.content || [];
      console.log(`[Chat] Synced ${data.length} pairing sessions`);
      setPairingSessions(data);
    } catch (err) {
      console.error('[Chat] Failed to fetch pairing sessions:', err);
    }
  }, [token]);

  useEffect(() => {
    activeConversationIdRef.current = activeConversationId;
    isChatOpenRef.current = isChatOpen;
    userRef.current = user;
    fetchPairingSessionsRef.current = fetchPairingSessions;
  }, [activeConversationId, isChatOpen, user, fetchPairingSessions]);

  // Initial fetch only
  useEffect(() => {
    if (token) {
      fetchConversations();
      fetchPairingSessions();
    }
  }, [token, fetchConversations, fetchPairingSessions]);

  const updateSessionStatus = async (sessionId, status) => {
    try {
      console.log(`[Chat] Updating session ${sessionId} to ${status}`);
      await api.patch(`/api/pairing/${sessionId}/status?status=${status}`);
      await fetchPairingSessions();
    } catch (err) {
      // Handle redundant updates (400) gracefully
      console.warn(`[Chat] Session update error (likely already updated):`, err.message);
      await fetchPairingSessions();
    }
  };

  const fetchMessages = useCallback(async (conversationId, page = 0) => {
    if (!token || !conversationId || conversationId.startsWith('new-')) return;
    
    setLoadingMessages(true);
    try {
      const response = await api.get(`/api/chat/conversations/${conversationId}/messages?page=${page}&size=50`);
      const newMessages = response?.content || [];
      const isLast = response?.last ?? true;

      setMessages(prev => {
        const existing = prev[conversationId] || [];
        if (page === 0) return { ...prev, [conversationId]: newMessages };
        return { ...prev, [conversationId]: [...newMessages, ...existing] };
      });

      setPagination(prev => ({
        ...prev,
        [conversationId]: { page, last: isLast }
      }));
      
      if (page === 0) {
        setUnreadConversationIds(prev => {
          if (!prev.has(conversationId)) return prev;
          const next = new Set(prev);
          next.delete(conversationId);
          return next;
        });
      }
    } catch (err) {
      console.error(`[Chat] Failed to fetch history:`, err);
    } finally {
      setLoadingMessages(false);
    }
  }, [token]);

  const loadMoreMessages = useCallback(async () => {
    const convId = activeConversationId;
    if (!convId || convId.startsWith('new-')) return;
    const currentPager = pagination[convId] || { page: 0, last: false };
    if (currentPager.last) return;
    await fetchMessages(convId, currentPager.page + 1);
  }, [activeConversationId, pagination, fetchMessages]);

  const markAsRead = useCallback(async (convId) => {
    if (!convId || convId.startsWith('new-')) return;
    setUnreadConversationIds(prev => {
      if (!prev.has(convId)) return prev;
      api.post(`/api/chat/conversations/${convId}/read`).catch(() => {});
      const next = new Set(prev);
      next.delete(convId);
      return next;
    });
  }, []);

  const handleIncomingMessage = useCallback((msg) => {
    const convId = msg.conversationId;
    if (!convId) return;

    setMessages(prev => {
      const existing = prev[convId] || [];
      if (existing.find(m => m.id === msg.id)) return prev;
      const filtered = existing.filter(m => {
        if (!m.isOptimistic) return true;
        const isSameSender = m.senderId === msg.senderId || (userRef.current?.id && m.senderId === userRef.current.id);
        return !(isSameSender && m.content === msg.content);
      });
      return { ...prev, [convId]: [...filtered, msg] };
    });

    setConversations(prev => {
      const other = prev.filter(c => c.id !== convId);
      const current = prev.find(c => c.id === convId);
      if (!current) {
        fetchConversations();
        return prev;
      }
      return [{ ...current, lastMessage: msg.content, lastMessageAt: msg.createdAt }, ...other];
    });

    if (newConversationUser && (msg.senderId === newConversationUser.id || msg.recipientId === newConversationUser.id)) {
      setActiveConversationId(convId);
      setNewConversationUser(null);
    }

    const currentUserId = userRef.current?.id;
    const isFromMe = msg.senderId === currentUserId || (userRef.current?.displayName && msg.senderDisplayName === userRef.current.displayName);
    const isActuallyViewing = isChatOpenRef.current && activeConversationIdRef.current === convId;
    
    if (!isActuallyViewing && !isFromMe) {
      setUnreadConversationIds(prev => {
        const next = new Set(prev);
        next.add(convId);
        return next;
      });
    } else if (isActuallyViewing && !isFromMe) {
      api.post(`/api/chat/conversations/${convId}/read`).catch(() => {});
    }
  }, [fetchConversations, newConversationUser]);

  useEffect(() => {
    incomingMessageHandlerRef.current = handleIncomingMessage;
  }, [handleIncomingMessage]);

  useEffect(() => {
    if (isChatOpen && activeConversationId) {
      Promise.resolve().then(() => markAsRead(activeConversationId));
    }
  }, [isChatOpen, activeConversationId, markAsRead]);

  // Main STOMP Connection - rely strictly on WebSockets
  useEffect(() => {
    if (!token) return;

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.host;
    const brokerURL = `${protocol}//${host}/ws?token=${encodeURIComponent(token)}`;

    const client = new Client({
      brokerURL,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    client.onConnect = () => {
      console.log('✅ Chat connected via WebSocket');
      setIsConnected(true);
      
      // Subscribe to personal messages
      client.subscribe('/user/queue/messages', (message) => {
        if (incomingMessageHandlerRef.current) {
          incomingMessageHandlerRef.current(JSON.parse(message.body));
        }
      });

      // Subscribe to pairing session updates
      client.subscribe('/user/queue/pairing', () => {
        console.log('🔄 Pairing update received via WebSocket');
        // Add a small 500ms delay to ensure DB has committed before refetching
        setTimeout(() => {
          if (fetchPairingSessionsRef.current) {
            fetchPairingSessionsRef.current();
          }
        }, 500);
      });
    };

    client.onDisconnect = () => {
      console.warn('❌ WebSocket disconnected');
      setIsConnected(false);
    };

    client.activate();
    clientRef.current = client;
    return () => client.deactivate();
  }, [token]);

  const sendMessage = async (recipientId, content) => {
    const optimisticMsg = {
      id: 'opt-' + Date.now(),
      conversationId: activeConversationId,
      senderId: user?.id,
      content,
      createdAt: new Date().toISOString(),
      isOptimistic: true
    };

    if (activeConversationId) {
      setMessages(prev => ({
        ...prev,
        [activeConversationId]: [...(prev[activeConversationId] || []), optimisticMsg]
      }));
    }

    if (clientRef.current && isConnected) {
      try {
        clientRef.current.publish({
          destination: '/app/chat.send',
          body: JSON.stringify({ recipientId, content }),
        });
        return;
      } catch {
        console.warn('[Chat] WS publish failed, falling back to REST');
      }
    }

    try {
      const msg = await api.post('/api/chat/messages', { recipientId, content });
      handleIncomingMessage(msg);
    } catch {
      if (activeConversationId) {
        setMessages(prev => ({
          ...prev,
          [activeConversationId]: (prev[activeConversationId] || []).filter(m => m.id !== optimisticMsg.id)
        }));
      }
    }
  };

  const selectConversation = useCallback(async (convId) => {
    setActiveConversationId(convId);
    await fetchMessages(convId, 0);
  }, [fetchMessages]);

  const openConversation = async (otherUserId, displayName) => {
    setIsChatOpen(true);
    let conversation = conversations.find(c => c.otherUserId === otherUserId);
    if (!conversation) {
      setNewConversationUser({ id: otherUserId, displayName });
      setActiveConversationId('new-' + otherUserId);
    } else {
      setNewConversationUser(null);
      await selectConversation(conversation.id);
    }
  };

  const pendingIncomingRequestsCount = pairingSessions.filter(
    s => s.status === 'PENDING' && s.requesteeId === user?.id
  ).length;

  return (
    <ChatContext.Provider value={{
      conversations,
      activeConversationId,
      setActiveConversationId: selectConversation,
      newConversationUser,
      messages,
      pagination,
      unreadConversationIds,
      sendMessage,
      isConnected,
      openConversation,
      fetchMessages,
      loadMoreMessages,
      isChatOpen,
      setIsChatOpen,
      toggleChat: () => setIsChatOpen(prev => !prev),
      unreadCount: unreadConversationIds.size + pendingIncomingRequestsCount,
      loadingMessages,
      pairingSessions,
      updateSessionStatus,
      fetchPairingSessions
    }}>
      {children}
    </ChatContext.Provider>
  );
}
