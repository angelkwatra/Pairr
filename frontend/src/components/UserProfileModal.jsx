import { useState, useEffect } from 'react';
import { api } from '../api/client';

const PROFICIENCY_LABELS = {
  BEGINNER: 'Beginner',
  AMATEUR: 'Amateur',
  INTERMEDIATE: 'Intermediate',
  EXPERT: 'Expert',
};

function formatTimeDisplay(time) {
  if (!time) return '';
  const parts = time.split(':');
  const h = parseInt(parts[0], 10);
  const m = parts[1];
  const ampm = h >= 12 ? 'PM' : 'AM';
  const display = h === 0 ? 12 : h > 12 ? h - 12 : h;
  return `${display}:${m} ${ampm}`;
}

export default function UserProfileModal({ userId, onClose }) {
  const [profile, setProfile] = useState(null);
  const [skills, setSkills] = useState([]);
  const [availability, setAvailability] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    async function fetchUserData() {
      setLoading(true);
      setError('');
      try {
        const [profileData, skillsData, availabilityData] = await Promise.all([
          api.get(`/api/users/${userId}`),
          api.get(`/api/users/${userId}/skills`),
          api.get(`/api/users/${userId}/availability`),
        ]);
        setProfile(profileData);
        setSkills(skillsData);
        setAvailability(availabilityData);
      } catch (err) {
        setError(err.message || 'Failed to load profile');
      } finally {
        setLoading(false);
      }
    }

    if (userId) fetchUserData();
  }, [userId]);

  if (!userId) return null;

  return (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-[60] p-4">
      <div className="bg-white rounded-3xl shadow-2xl w-full max-w-lg overflow-hidden animate-in fade-in zoom-in duration-200">
        {/* Header/Cover */}
        <div className="h-24 bg-gradient-to-r from-blue-600 to-indigo-700 relative">
          <button 
            onClick={onClose}
            className="absolute top-4 right-4 p-2 bg-black/20 hover:bg-black/40 rounded-full text-white transition-colors"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
          </button>
        </div>

        <div className="px-6 pb-8 relative">
          {/* Avatar */}
          <div className="absolute -top-12 left-6">
            <div className="w-24 h-24 bg-white rounded-2xl p-1 shadow-lg">
              <div className="w-full h-full bg-gray-100 rounded-xl flex items-center justify-center text-3xl font-bold text-blue-600 border border-gray-50">
                {profile?.displayName?.charAt(0).toUpperCase() || '?'}
              </div>
            </div>
          </div>

          {/* User Info */}
          <div className="pt-14">
            {loading ? (
              <div className="py-12 flex flex-col items-center justify-center space-y-4">
                <div className="w-10 h-10 border-4 border-blue-100 border-t-blue-600 rounded-full animate-spin"></div>
              </div>
            ) : error ? (
              <div className="py-8 text-center text-red-600 bg-red-50 rounded-xl border border-red-100 italic">
                {error}
              </div>
            ) : (
              <div className="space-y-6">
                <div>
                  <div className="flex items-baseline gap-2 flex-wrap">
                    <h2 className="text-2xl font-black text-gray-900 leading-tight">
                      {profile.displayName}
                    </h2>
                    {profile.username && (
                      <span className="text-sm font-bold text-blue-600/60 bg-blue-50/50 px-2 py-0.5 rounded-lg">
                        @{profile.username}
                      </span>
                    )}
                  </div>
                  <div className="flex items-center gap-2 mt-1 flex-wrap">
                    <div className="flex items-center text-yellow-500">
                      <svg className="w-4 h-4 fill-current" viewBox="0 0 20 20"><path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" /></svg>
                      <span className="ml-1 text-sm font-bold">{profile.overallRating?.toFixed(1) || 'No ratings'}</span>
                    </div>
                    <span className="text-gray-300">•</span>
                    <div className="flex items-center gap-1.5 bg-green-50 text-green-700 px-2 py-0.5 rounded-lg border border-green-100 shadow-sm">
                      <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
                      <span className="text-[10px] font-black uppercase tracking-tight">{profile.completedSessionsCount || 0} Verified Sessions</span>
                    </div>
                    <span className="text-gray-300">•</span>
                    <span className="text-xs font-bold text-gray-400 uppercase tracking-widest">
                      Member since {new Date(profile.createdAt).getFullYear()}
                    </span>
                  </div>
                </div>

                {/* Skills Section */}
                <section>
                  <h3 className="text-[10px] font-black text-gray-400 uppercase tracking-[0.2em] mb-3">Skills & Expertise</h3>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                    {skills.map((us) => (
                      <div key={us.skill.id} className="p-3 rounded-xl border border-gray-100 bg-gray-50/50 flex flex-col">
                        <span className="text-sm font-bold text-gray-800">{us.skill.name}</span>
                        <div className="flex items-center gap-2 mt-1">
                          <span className="text-[10px] font-bold text-blue-600 bg-blue-50 px-1.5 py-0.5 rounded uppercase tracking-wider">
                            {PROFICIENCY_LABELS[us.proficiencyLevel]}
                          </span>
                          {us.rating > 0 && (
                            <span className="text-[10px] text-yellow-600 font-bold">
                              {us.rating.toFixed(1)} ★
                            </span>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                </section>

                {/* Availability Section */}
                <section>
                  <h3 className="text-[10px] font-black text-gray-400 uppercase tracking-[0.2em] mb-3">Availability</h3>
                  <div className="space-y-4">
                    {['WEEKDAY', 'WEEKEND'].map(type => {
                      const slots = availability.filter(a => a.dayType === type);
                      if (slots.length === 0) return null;
                      return (
                        <div key={type} className="flex gap-3">
                          <div className={`w-1 rounded-full ${type === 'WEEKDAY' ? 'bg-blue-500' : 'bg-indigo-500'}`}></div>
                          <div className="flex-1">
                            <span className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">{type === 'WEEKDAY' ? 'Weekdays' : 'Weekends'}</span>
                            <div className="flex flex-wrap gap-2 mt-1">
                              {slots.map((s, i) => (
                                <span key={i} className="text-xs font-medium text-gray-700 bg-gray-100 px-2 py-1 rounded-lg">
                                  {formatTimeDisplay(s.startTime)} - {formatTimeDisplay(s.endTime)}
                                </span>
                              ))}
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </section>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
