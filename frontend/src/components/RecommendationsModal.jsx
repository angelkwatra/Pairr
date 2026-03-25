import { useState, useEffect } from 'react';
import { api } from '../api/client';
import { useChat } from '../context/ChatContext.js';
import UserProfileModal from './UserProfileModal';

export default function RecommendationsModal({ skill, onClose }) {
  const [recommendations, setRecommendations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [dayType, setDayType] = useState('WEEKDAY');
  const [viewUserProfileId, setViewUserProfileId] = useState(null);
  const { openConversation, fetchPairingSessions } = useChat();

  useEffect(() => {
    async function fetchRecommendations() {
      setLoading(true);
      setError('');
      try {
        const data = await api.get(`/api/recommendations?skillId=${skill.id}&dayType=${dayType}`);
        setRecommendations(data);
      } catch (err) {
        setError(err.message || 'Failed to fetch recommendations');
      } finally {
        setLoading(false);
      }
    }

    fetchRecommendations();
  }, [skill.id, dayType]);

  const handleRequestPairing = async (e, userId) => {
    e.stopPropagation();
    try {
      await api.post('/api/pairing/request', { requesteeId: userId, skillId: skill.id });
      // Refresh the sessions list so the request shows up in the ChatList immediately
      await fetchPairingSessions();
      // Navigate them to chat so they can see their sent request
      openConversation(userId, recommendations.find(r => r.userId === userId)?.displayName);
      onClose();
    } catch (err) {
      alert(err.message || 'Failed to request pairing');
    }
  };

  return (
    <>
      <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
        <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in duration-200">
          <div className="p-6 border-b border-gray-100 flex items-center justify-between bg-gray-50/50">
            <div>
              <h2 className="text-xl font-bold text-gray-900">Recommended Partners</h2>
              <p className="text-sm text-gray-500 font-medium">For {skill.name}</p>
            </div>
            <button 
              onClick={onClose}
              className="p-2 hover:bg-white hover:shadow-sm rounded-full transition-all text-gray-400 hover:text-gray-600"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
            </button>
          </div>

          <div className="p-4 bg-gray-100/50 border-b border-gray-100 flex gap-2">
            <button
              onClick={() => setDayType('WEEKDAY')}
              className={`flex-1 py-2 text-xs font-bold rounded-lg transition-all ${
                dayType === 'WEEKDAY' 
                  ? 'bg-white text-blue-600 shadow-sm ring-1 ring-gray-200' 
                  : 'text-gray-500 hover:bg-gray-200'
              }`}
            >
              WEEKDAYS
            </button>
            <button
              onClick={() => setDayType('WEEKEND')}
              className={`flex-1 py-2 text-xs font-bold rounded-lg transition-all ${
                dayType === 'WEEKEND' 
                  ? 'bg-white text-indigo-600 shadow-sm ring-1 ring-gray-200' 
                  : 'text-gray-500 hover:bg-gray-200'
              }`}
            >
              WEEKENDS
            </button>
          </div>

          <div className="max-h-[60vh] overflow-y-auto p-4 space-y-3">
            {loading ? (
              <div className="py-12 flex flex-col items-center justify-center space-y-4">
                <div className="w-10 h-10 border-4 border-blue-100 border-t-blue-600 rounded-full animate-spin"></div>
                <p className="text-sm text-gray-500 font-medium italic">Finding the best matches...</p>
              </div>
            ) : error ? (
              <div className="py-8 px-4 text-center">
                <div className="bg-red-50 text-red-600 p-4 rounded-xl text-sm border border-red-100 font-medium">
                  {error}
                </div>
              </div>
            ) : recommendations.length === 0 ? (
              <div className="py-12 text-center">
                <div className="w-16 h-16 bg-gray-50 rounded-full flex items-center justify-center mx-auto mb-4 border border-gray-100">
                  <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-gray-300"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>
                </div>
                <p className="text-gray-500 text-sm font-medium">No partners found for this slot.</p>
                <p className="text-gray-400 text-xs mt-1">Try switching between weekday/weekend.</p>
              </div>
            ) : (
              recommendations.map((rec) => (
                <div 
                  key={rec.userId}
                  onClick={() => setViewUserProfileId(rec.userId)}
                  className="group p-4 bg-white border border-gray-100 rounded-xl hover:border-blue-200 hover:shadow-md transition-all flex items-center justify-between cursor-pointer"
                >
                  <div className="flex items-center gap-4">
                    <div className="w-12 h-12 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-full flex items-center justify-center text-white font-bold text-lg shadow-inner group-hover:scale-105 transition-transform">
                      {rec.displayName.charAt(0).toUpperCase()}
                    </div>
                    <div>
                      <h3 className="font-bold text-gray-900 leading-tight">{rec.displayName}</h3>
                      <div className="flex items-center gap-2 mt-1">
                        <div className="w-16 h-1.5 bg-gray-100 rounded-full overflow-hidden">
                          <div 
                            className="h-full bg-blue-500 transition-all duration-1000" 
                            style={{ width: `${Math.min(rec.score * 100, 100)}%` }}
                          ></div>
                        </div>
                        <span className="text-[10px] font-bold text-gray-400 uppercase tracking-tighter">
                          {Math.round(rec.score * 100)}% Match
                        </span>
                      </div>
                    </div>
                  </div>
                  <button 
                    onClick={(e) => handleRequestPairing(e, rec.userId)}
                    className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-xl text-[11px] font-black uppercase tracking-wider transition-all shadow-lg shadow-blue-100 active:scale-95"
                  >
                    Pair Now
                  </button>
                </div>
              ))
            )}
          </div>

          <div className="p-4 bg-gray-50 text-center">
            <p className="text-[10px] text-gray-400 uppercase font-bold tracking-widest">
              Ranked by availability, proficiency & ratings
            </p>
          </div>
        </div>
      </div>

      {viewUserProfileId && (
        <UserProfileModal 
          userId={viewUserProfileId} 
          onClose={() => setViewUserProfileId(null)} 
        />
      )}
    </>
  );
}
