import { useState } from 'react';
import { useChat } from '../../context/ChatContext.js';
import { useAuth } from '../../context/AuthContext.js';

export default function ChatList({ onSelect }) {
  const { 
    conversations, 
    activeConversationId, 
    setActiveConversationId, 
    unreadConversationIds, 
    pairingSessions, 
    updateSessionStatus, 
    openConversation 
  } = useChat();
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState('messages'); // 'messages' | 'history'

  const handleSelect = (id) => {
    setActiveConversationId(id);
    if (onSelect) onSelect();
  };

  const pendingRequests = pairingSessions.filter(s => s.status === 'PENDING');
  const pastSessions = pairingSessions.filter(s => s.status === 'COMPLETED' || s.status === 'CANCELLED');

  return (
    <div className="h-full flex flex-col bg-white">
      <div className="p-4 border-b border-gray-100 bg-white sticky top-0 z-10">
        <h2 className="text-xl font-black text-gray-900 tracking-tight mb-4">Chat</h2>
        <div className="flex bg-gray-100 p-1 rounded-xl shadow-inner">
          <button 
            onClick={() => setActiveTab('messages')}
            className={`flex-1 py-2 text-[10px] font-black uppercase tracking-widest rounded-lg transition-all ${
              activeTab === 'messages' ? 'bg-white text-blue-600 shadow-sm' : 'text-gray-500 hover:text-gray-700'
            }`}
          >
            Messages
          </button>
          <button 
            onClick={() => setActiveTab('history')}
            className={`flex-1 py-2 text-[10px] font-black uppercase tracking-widest rounded-lg transition-all ${
              activeTab === 'history' ? 'bg-white text-blue-600 shadow-sm' : 'text-gray-500 hover:text-gray-700'
            }`}
          >
            History
          </button>
        </div>
      </div>
      
      <div className="flex-1 overflow-y-auto">
        {activeTab === 'messages' ? (
          <>
            {/* Pending Requests Section */}
            {pendingRequests.length > 0 && (
              <div className="border-b border-gray-100 bg-blue-50/30">
                <div className="p-4 pb-2">
                  <h3 className="text-[10px] font-black uppercase tracking-widest text-blue-600 flex items-center gap-2">
                    <span className="w-1.5 h-1.5 rounded-full bg-blue-500 animate-pulse"></span>
                    Pairing Requests ({pendingRequests.length})
                  </h3>
                </div>
                <div className="px-2 pb-4 space-y-2">
                  {pendingRequests.map(session => {
                    const isIncoming = session.requesteeId === user?.id;
                    const otherPartyName = isIncoming ? session.requesterDisplayName : session.requesteeDisplayName;
                    const otherPartyId = isIncoming ? session.requesterId : session.requesteeId;

                    return (
                      <div key={session.id} className="bg-white border border-blue-100 rounded-xl p-3 shadow-sm shadow-blue-100/50">
                        <div className="flex justify-between items-start mb-2">
                          <div>
                            <p className="text-xs font-bold text-gray-900 leading-tight">{otherPartyName}</p>
                            <p className="text-[9px] text-blue-600 font-bold uppercase mt-0.5">{session.skillName}</p>
                          </div>
                          <span className={`text-[8px] px-1.5 py-0.5 rounded font-black uppercase tracking-tighter ${
                            isIncoming ? 'bg-blue-50 text-blue-600' : 'bg-gray-50 text-gray-500'
                          }`}>
                            {isIncoming ? 'Incoming' : 'Sent'}
                          </span>
                        </div>
                        
                        <div className="flex gap-2">
                          {isIncoming ? (
                            <>
                              <button 
                                onClick={() => updateSessionStatus(session.id, 'ACCEPTED')}
                                className="flex-1 bg-blue-600 text-white text-[9px] font-black uppercase py-1.5 rounded-lg hover:bg-blue-700 transition-all shadow-sm active:scale-95"
                              >
                                Accept
                              </button>
                              <button 
                                onClick={() => updateSessionStatus(session.id, 'CANCELLED')}
                                className="px-2 border border-gray-200 text-gray-400 hover:text-red-500 hover:border-red-100 rounded-lg transition-all active:scale-95"
                                title="Decline"
                              >
                                <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
                              </button>
                            </>
                          ) : (
                            <button 
                              onClick={() => updateSessionStatus(session.id, 'CANCELLED')}
                              className="flex-1 border border-gray-200 text-gray-500 text-[9px] font-black uppercase py-1.5 rounded-lg hover:bg-gray-50 transition-all active:scale-95"
                            >
                              Cancel Request
                            </button>
                          )}
                          <button 
                            onClick={() => openConversation(otherPartyId, otherPartyName)}
                            className="px-2 bg-gray-50 text-gray-600 hover:bg-blue-50 hover:text-blue-600 rounded-lg transition-all border border-gray-100 active:scale-95"
                            title="Open Chat"
                          >
                            <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}

            {conversations.length === 0 ? (
              <div className="p-12 text-center flex flex-col items-center">
                <div className="w-12 h-12 bg-gray-50 rounded-full flex items-center justify-center mb-4 text-gray-200 border border-gray-100 shadow-inner">
                  <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>
                </div>
                <p className="text-gray-400 text-sm font-medium">No conversations yet.</p>
                <p className="text-gray-300 text-[10px] mt-1 leading-relaxed">Your active pairings and messages will appear here.</p>
              </div>
            ) : (
              <div className="flex flex-col">
                {conversations.map((conv) => {
                  const isActive = activeConversationId === conv.id;
                  const isUnread = unreadConversationIds.has(conv.id);
                  
                  return (
                    <button
                      key={conv.id}
                      onClick={() => handleSelect(conv.id)}
                      className={`w-full text-left p-4 hover:bg-gray-50 transition-colors flex items-center gap-3 border-b border-gray-50 relative ${
                        isActive ? 'bg-blue-50/50' : ''
                      }`}
                    >
                      {isActive && (
                        <div className="absolute right-0 top-0 bottom-0 w-1 bg-blue-500 shadow-[0_0_8px_rgba(59,130,246,0.5)]"></div>
                      )}

                      <div className="relative flex-shrink-0">
                        <div className="w-10 h-10 bg-gradient-to-br from-gray-100 to-gray-200 rounded-full flex items-center justify-center text-gray-600 font-bold text-sm border border-gray-50 shadow-sm">
                          {conv.otherUserDisplayName ? conv.otherUserDisplayName.charAt(0).toUpperCase() : '?'}
                        </div>
                        {isUnread && (
                          <div className="absolute -top-0.5 -right-0.5 w-3 h-3 bg-blue-600 border-2 border-white rounded-full shadow-sm"></div>
                        )}
                      </div>

                      <div className="flex-1 min-w-0">
                        <div className="flex justify-between items-baseline mb-0.5">
                          <h3 className={`text-sm truncate font-bold text-gray-900 ${isActive ? 'text-blue-900' : ''}`}>
                            {conv.otherUserDisplayName}
                          </h3>
                          {conv.lastMessageAt && (
                            <span className={`text-[10px] ${isUnread ? 'text-blue-600 font-bold' : 'text-gray-400'}`}>
                              {new Date(conv.lastMessageAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}
                            </span>
                          )}
                        </div>
                        <p className={`text-xs truncate ${
                          isUnread ? 'text-gray-900 font-medium' : 'text-gray-500'
                        } ${isActive ? 'text-blue-600' : ''}`}>
                          {conv.lastMessage || 'Start a conversation'}
                        </p>
                      </div>
                    </button>
                  );
                })}
              </div>
            )}
          </>
        ) : (
          <div className="flex flex-col">
            {pastSessions.length === 0 ? (
              <div className="p-12 text-center flex flex-col items-center">
                <div className="w-12 h-12 bg-gray-50 rounded-full flex items-center justify-center mb-4 text-gray-200 border border-gray-100 shadow-inner">
                  <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
                </div>
                <p className="text-gray-400 text-sm font-medium">No history yet.</p>
                <p className="text-gray-300 text-[10px] mt-1 leading-relaxed">Completed and cancelled pairing sessions will appear here.</p>
              </div>
            ) : (
              pastSessions.map(session => {
                const isRequester = session.requesterId === user?.id;
                const otherPartyName = isRequester ? session.requesteeDisplayName : session.requesterDisplayName;
                
                return (
                  <div key={session.id} className="p-4 border-b border-gray-50 hover:bg-gray-50/50 transition-colors">
                    <div className="flex justify-between items-start mb-1">
                      <h3 className="text-sm font-bold text-gray-900">{otherPartyName}</h3>
                      <span className={`text-[8px] font-black uppercase px-1.5 py-0.5 rounded ${
                        session.status === 'COMPLETED' ? 'bg-green-50 text-green-600' : 'bg-gray-100 text-gray-400'
                      }`}>
                        {session.status}
                      </span>
                    </div>
                    <p className="text-[10px] text-blue-600 font-bold uppercase tracking-tight">{session.skillName}</p>
                    <p className="text-[9px] text-gray-400 mt-2">
                      {session.completedAt ? `Completed on ${new Date(session.completedAt).toLocaleDateString()}` : `Requested on ${new Date(session.requestedAt).toLocaleDateString()}`}
                    </p>
                  </div>
                );
              })
            )}
          </div>
        )}
      </div>
    </div>
  );
}
