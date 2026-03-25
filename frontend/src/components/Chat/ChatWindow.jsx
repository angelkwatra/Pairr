import { useState, useEffect, useRef, useMemo } from 'react';
import { useChat } from '../../context/ChatContext.js';
import { useAuth } from '../../context/AuthContext.js';
import UserProfileModal from '../UserProfileModal';
import RatingModal from './RatingModal';

export default function ChatWindow({ onClose }) {
  const { 
    activeConversationId, 
    newConversationUser,
    messages, 
    sendMessage, 
    conversations, 
    fetchMessages,
    loadMoreMessages,
    pagination,
    loadingMessages,
    pairingSessions,
    updateSessionStatus
  } = useChat();
  const { user } = useAuth();
  const [inputText, setInputText] = useState('');
  const [viewUserProfileId, setViewUserProfileId] = useState(null);
  const [showRatingModal, setShowRatingModal] = useState(null);
  const messagesEndRef = useRef(null);
  const scrollContainerRef = useRef(null);
  const [prevScrollHeight, setPrevScrollHeight] = useState(0);
  
  // Find current conversation details
  let currentConv = conversations.find(c => c.id === activeConversationId);
  let recipientId = currentConv?.otherUserId;
  let recipientName = currentConv?.otherUserDisplayName;
  
  // Handle new conversation case (optimistic ID)
  if (!currentConv && activeConversationId?.startsWith('new-')) {
    recipientId = activeConversationId.split('new-')[1];
    recipientName = newConversationUser?.displayName || "New Chat"; 
  }

  // Find the most recent session for this user pair (including COMPLETED)
  const sessionForBanner = [...pairingSessions]
    .sort((a, b) => new Date(b.requestedAt) - new Date(a.requestedAt))
    .find(s => 
      (s.requesterId === recipientId || s.requesteeId === recipientId) && 
      s.status !== 'CANCELLED'
    );

  const isChatEnabled = sessionForBanner?.status === 'ACCEPTED';

  const currentMessages = useMemo(() => 
    messages[activeConversationId] || [], 
    [messages, activeConversationId]
  );

  const currentPagination = pagination[activeConversationId] || { page: 0, last: true };

  useEffect(() => {
    if (activeConversationId && !activeConversationId.startsWith('new-')) {
      fetchMessages(activeConversationId);
    }
  }, [activeConversationId, fetchMessages]);

  // Adjust scroll position after loading older messages
  useEffect(() => {
    if (scrollContainerRef.current && prevScrollHeight > 0) {
      const newScrollHeight = scrollContainerRef.current.scrollHeight;
      scrollContainerRef.current.scrollTop = newScrollHeight - prevScrollHeight;
      requestAnimationFrame(() => {
        setPrevScrollHeight(0);
      });
    }
  }, [currentMessages, prevScrollHeight]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  // Initial scroll to bottom when conversation opens or new message arrives
  useEffect(() => {
    if (currentMessages.length > 0 && prevScrollHeight === 0) {
      // Only scroll to bottom if we are not loading older messages
      scrollToBottom();
    }
  }, [currentMessages, activeConversationId, prevScrollHeight]);

  const handleLoadMore = async () => {
    if (scrollContainerRef.current) {
      setPrevScrollHeight(scrollContainerRef.current.scrollHeight);
    }
    await loadMoreMessages();
  };

  const handleSend = (e) => {
    e.preventDefault();
    if (!inputText.trim() || !isChatEnabled) return;
    
    if (recipientId) {
      sendMessage(recipientId, inputText);
      setInputText('');
    }
  };

  const openRating = () => {
    setShowRatingModal({
      ...sessionForBanner,
      otherUserId: recipientId,
      otherUserDisplayName: recipientName
    });
  };

  const handleCompleteSession = async () => {
    await updateSessionStatus(sessionForBanner.id, 'COMPLETED');
    // Only prompt for rating if the user hasn't already rated during the ACCEPTED phase
    if (!sessionForBanner.ratedByCurrentUser) {
      openRating();
    }
  };

  if (!activeConversationId) return null;

  return (
    <div className="flex flex-col h-full bg-white relative">
      {/* Header */}
      <div className="p-3 border-b border-gray-100 flex items-center justify-between bg-white shadow-sm z-10">
        <div className="flex items-center gap-3">
          <button onClick={onClose} className="md:hidden text-gray-500 hover:text-gray-700">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
          </button>
          <button 
            onClick={() => setViewUserProfileId(recipientId)}
            className="flex items-center gap-3 text-left hover:bg-gray-50 p-1 pr-3 rounded-xl transition-colors"
          >
            <div className="w-8 h-8 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-full flex items-center justify-center text-white font-bold text-sm shadow-inner">
              {recipientName ? recipientName.charAt(0).toUpperCase() : '?'}
            </div>
            <div>
              <h3 className="font-bold text-gray-900 text-sm">{recipientName || 'Chat'}</h3>
              <p className="text-[9px] text-blue-600 font-bold uppercase tracking-wider">View Profile</p>
            </div>
          </button>
        </div>
      </div>

      {/* Pairing Session Status Bar */}
      {sessionForBanner && (
        <div className={`border-b px-4 py-2 flex items-center justify-between animate-in fade-in slide-in-from-top-2 ${
          sessionForBanner.status === 'COMPLETED' ? 'bg-gray-50 border-gray-100' : 'bg-blue-50/80 backdrop-blur-sm border-blue-100'
        }`}>
          <div className="flex items-center gap-2">
            <div className={`w-2 h-2 rounded-full ${
              sessionForBanner.status === 'PENDING' ? 'bg-amber-400 animate-pulse' : 
              sessionForBanner.status === 'ACCEPTED' ? 'bg-green-500 animate-pulse' : 
              'bg-gray-400'
            }`}></div>
            <p className="text-[10px] font-black uppercase tracking-tight text-blue-900">
              {sessionForBanner.status === 'PENDING' ? 'Request Pending' : 
               sessionForBanner.status === 'ACCEPTED' ? 'Active Session' : 
               'Session Completed'}
              <span className="mx-1 text-blue-300">|</span>
              <span className="text-blue-600">{sessionForBanner.skillName}</span>
            </p>
          </div>
          
          <div className="flex gap-2">
            {sessionForBanner.status === 'PENDING' && sessionForBanner.requesteeId === user?.id && (
              <button 
                onClick={() => updateSessionStatus(sessionForBanner.id, 'ACCEPTED')}
                className="bg-blue-600 text-white text-[9px] font-black uppercase px-3 py-1 rounded-lg hover:bg-blue-700 transition-colors shadow-sm"
              >
                Accept
              </button>
            )}
            {sessionForBanner.status === 'ACCEPTED' && (
              <>
                {!sessionForBanner.ratedByCurrentUser && (
                  <button 
                    onClick={openRating}
                    className="bg-white border border-blue-200 text-blue-600 text-[9px] font-black uppercase px-3 py-1 rounded-lg hover:bg-blue-50 transition-colors shadow-sm"
                  >
                    Rate Partner
                  </button>
                )}
                <button 
                  onClick={handleCompleteSession}
                  className="bg-green-600 text-white text-[9px] font-black uppercase px-3 py-1 rounded-lg hover:bg-green-700 transition-colors shadow-sm"
                >
                  End Session
                </button>
              </>
            )}
            {sessionForBanner.status === 'COMPLETED' && !sessionForBanner.ratedByCurrentUser && (
              <button 
                onClick={openRating}
                className="bg-blue-600 text-white text-[9px] font-black uppercase px-3 py-1 rounded-lg hover:bg-blue-700 transition-colors shadow-sm"
              >
                Rate Partner
              </button>
            )}
            {sessionForBanner.status !== 'COMPLETED' && (
              <button 
                onClick={() => updateSessionStatus(sessionForBanner.id, 'CANCELLED')}
                className="text-[9px] font-black uppercase px-2 py-1 text-red-400 hover:text-red-600 transition-colors"
              >
                Cancel
              </button>
            )}
          </div>
        </div>
      )}

      {/* Messages */}
      <div 
        ref={scrollContainerRef}
        className="flex-1 overflow-y-auto p-4 space-y-3 bg-gray-50/50 relative"
      >
        {!currentPagination.last && (
          <div className="flex justify-center pb-4">
            <button 
              onClick={handleLoadMore}
              disabled={loadingMessages}
              className="text-xs font-bold text-blue-600 bg-white px-4 py-1.5 rounded-full shadow-sm border border-gray-100 hover:bg-blue-50 transition-all disabled:opacity-50"
            >
              {loadingMessages ? 'Loading...' : 'Load older messages'}
            </button>
          </div>
        )}

        {loadingMessages && currentMessages.length === 0 ? (
          <div className="py-12 flex flex-col items-center justify-center space-y-4">
            <div className="w-8 h-8 border-2 border-blue-100 border-t-blue-600 rounded-full animate-spin"></div>
            <p className="text-xs text-gray-400 italic">Syncing messages...</p>
          </div>
        ) : currentMessages.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-gray-400 text-xs text-center p-8">
            <div className="w-12 h-12 bg-gray-100 rounded-full flex items-center justify-center mb-4 text-gray-300">
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>
            </div>
            <p>No messages yet.</p>
            {!isChatEnabled && <p className="mt-2 text-blue-600 font-bold uppercase tracking-tighter text-[10px]">Accept request to start chatting</p>}
          </div>
        ) : (
          currentMessages.map((msg, idx) => {
            const isMe = msg.senderId === user?.id || (user?.email && msg.senderEmail === user.email);
            
            return (
              <div 
                key={msg.id || idx} 
                className={`flex ${isMe ? 'justify-end' : 'justify-start'}`}
              >
                <div 
                  className={`max-w-[80%] rounded-2xl px-4 py-2 text-sm shadow-sm ${
                    isMe 
                      ? 'bg-blue-600 text-white rounded-br-none' 
                      : 'bg-white text-gray-800 border border-gray-100 rounded-bl-none'
                  }`}
                >
                  <p>{msg.content}</p>
                  <div className={`text-[9px] mt-1 text-right ${isMe ? 'text-blue-200' : 'text-gray-400'}`}>
                    {new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                  </div>
                </div>
              </div>
            );
          })
        )}
        <div ref={messagesEndRef} />

        {/* Chat Blocked Overlay */}
        {!isChatEnabled && (
          <div className="absolute inset-0 z-20 flex items-end justify-center pb-12 px-6 pointer-events-none">
            <div className="bg-white/90 backdrop-blur-md border border-gray-100 shadow-2xl rounded-2xl p-6 text-center animate-in fade-in slide-in-from-bottom-4 duration-500 max-w-xs pointer-events-auto">
              <div className="w-12 h-12 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4 text-gray-400">
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
              </div>
              <h4 className="text-sm font-black text-gray-900 uppercase tracking-tight mb-1">
                {sessionForBanner?.status === 'COMPLETED' ? 'Session Completed' : 'Chat Locked'}
              </h4>
              <p className="text-[11px] text-gray-500 font-medium leading-relaxed">
                {sessionForBanner?.status === 'COMPLETED' 
                  ? 'This collaboration has ended. You can no longer send messages.' 
                  : 'You must have an active pairing session to message this user.'}
              </p>
            </div>
          </div>
        )}
      </div>

      {/* Input */}
      <div className={`p-3 bg-white border-t border-gray-100 ${!isChatEnabled ? 'opacity-50 cursor-not-allowed' : ''}`}>
        <form onSubmit={handleSend} className="flex gap-2">
          <input
            type="text"
            value={inputText}
            onChange={(e) => setInputText(e.target.value)}
            disabled={!isChatEnabled}
            placeholder={isChatEnabled ? "Type a message..." : "Chat is disabled"}
            className="flex-1 border border-gray-200 rounded-full px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-gray-50 disabled:bg-gray-100"
          />
          <button 
            type="submit"
            disabled={!inputText.trim() || !isChatEnabled}
            className="bg-blue-600 text-white p-2 rounded-full hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors shadow-sm"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="22" y1="2" x2="11" y2="13"></line><polygon points="22 2 15 22 11 13 2 9 22 2"></polygon></svg>
          </button>
        </form>
      </div>

      {viewUserProfileId && (
        <UserProfileModal 
          userId={viewUserProfileId} 
          onClose={() => setViewUserProfileId(null)} 
        />
      )}

      {showRatingModal && (
        <RatingModal
          session={showRatingModal}
          onComplete={() => setShowRatingModal(null)}
          onClose={() => setShowRatingModal(null)}
        />
      )}
    </div>
  );
}
