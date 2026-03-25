import { useChat } from '../../context/ChatContext.js';
import ChatList from './ChatList';
import ChatWindow from './ChatWindow';

export default function ChatWidget() {
  const { isChatOpen, toggleChat, activeConversationId, setActiveConversationId, unreadCount } = useChat();

  if (!isChatOpen) {
    return (
      <button
        onClick={toggleChat}
        className="fixed bottom-6 right-6 w-14 h-14 bg-blue-600 hover:bg-blue-700 text-white rounded-full shadow-lg flex items-center justify-center transition-transform hover:scale-105 z-50 group"
      >
        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>
        
        {unreadCount > 0 && (
          <span className="absolute -top-1 -right-1 flex h-6 w-6">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-red-400 opacity-75"></span>
            <span className="relative inline-flex rounded-full h-6 w-6 bg-red-500 text-[10px] font-bold items-center justify-center border-2 border-white shadow-sm">
              {unreadCount}
            </span>
          </span>
        )}
      </button>
    );
  }

  return (
    <div className="fixed inset-0 z-50 flex flex-col md:items-end md:justify-end pointer-events-none">
      {/* Backdrop for mobile only */}
      <div 
        className="absolute inset-0 bg-black/40 backdrop-blur-sm md:hidden pointer-events-auto" 
        onClick={toggleChat}
      ></div>

      <div className="bg-white w-full h-full md:w-[85vw] md:max-w-[850px] md:h-[80vh] md:max-h-[650px] md:m-6 md:rounded-2xl shadow-2xl flex overflow-hidden relative animate-in slide-in-from-bottom-10 fade-in duration-200 pointer-events-auto border border-gray-100">
        
        {/* Chat List (Conversations) */}
        <div className={`w-full md:w-[320px] flex flex-col border-r border-gray-100 ${activeConversationId ? 'hidden md:flex' : 'flex'}`}>
          <ChatList onSelect={() => {}} />
          
          {/* Close button for mobile in the list view */}
          <button 
            onClick={toggleChat}
            className="md:hidden p-4 text-center text-sm font-bold text-gray-500 bg-gray-50 border-t border-gray-100 hover:text-gray-700 active:bg-gray-100"
          >
            Close Messages
          </button>
        </div>

        {/* Chat Window (Messages) */}
        <div className={`flex-1 bg-gray-50 flex flex-col ${!activeConversationId ? 'hidden md:flex' : 'flex'}`}>
          {activeConversationId ? (
            <ChatWindow onClose={() => setActiveConversationId(null)} />
          ) : (
            <div className="flex-1 flex items-center justify-center text-gray-400 text-sm flex-col p-12 text-center">
              <div className="w-20 h-20 bg-gray-100 rounded-full flex items-center justify-center mb-6 shadow-inner">
                <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" className="opacity-30"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>
              </div>
              <h3 className="text-gray-900 font-bold text-lg mb-2">Your Conversations</h3>
              <p className="max-w-[240px] leading-relaxed">
                Select a partner from the left to start collaborating on your next big project.
              </p>
            </div>
          )}
        </div>

        {/* Close button for desktop - fixed at top right of widget */}
        <button 
          onClick={toggleChat}
          className="absolute top-4 right-4 p-2 bg-white/80 backdrop-blur shadow-sm border border-gray-100 hover:bg-white hover:border-gray-300 rounded-xl text-gray-500 hidden md:flex items-center justify-center transition-all z-20 group"
          title="Close Chat"
        >
          <svg className="group-hover:rotate-90 transition-transform duration-200" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
        </button>
      </div>
    </div>
  );
}
