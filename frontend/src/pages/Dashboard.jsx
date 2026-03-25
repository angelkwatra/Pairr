import { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../context/AuthContext.js';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import SkillSetup from '../components/SkillSetup';
import AvailabilitySetup from '../components/AvailabilitySetup';
import RecommendationsModal from '../components/RecommendationsModal';
import ChatWidget from '../components/Chat/ChatWidget';

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

export default function Dashboard() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [userSkills, setUserSkills] = useState(null);
  const [userAvailability, setUserAvailability] = useState(null);
  const [loading, setLoading] = useState(true);

  // Onboarding step: 'skills' | 'availability' | null (complete)
  const [onboardingStep, setOnboardingStep] = useState(null);

  // Edit mode toggles
  const [editingSkills, setEditingSkills] = useState(false);
  const [editingAvailability, setEditingAvailability] = useState(false);
  const [recommendationSkill, setRecommendationSkill] = useState(null);

  const fetchProfile = useCallback(async () => {
    try {
      const [skills, availability] = await Promise.all([
        api.get('/api/user/skills'),
        api.get('/api/user/availability'),
      ]);
      setUserSkills(skills);
      setUserAvailability(availability);

      // Determine onboarding step
      if (!skills || skills.length < 3) {
        setOnboardingStep('skills');
      } else if (!availability || availability.length === 0) {
        setOnboardingStep('availability');
      } else {
        setOnboardingStep(null);
      }
    } catch (err) {
      // If fetching fails, still try to show what we can
      console.error('Failed to fetch profile:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchProfile();
  }, [fetchProfile]);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const handleSkillsComplete = async () => {
    // Refetch skills to get updated list
    const skills = await api.get('/api/user/skills');
    setUserSkills(skills);
    if (!userAvailability || userAvailability.length === 0) {
      setOnboardingStep('availability');
    } else {
      setOnboardingStep(null);
      setEditingSkills(false);
    }
  };

  const handleAvailabilityComplete = async () => {
    const availability = await api.get('/api/user/availability');
    setUserAvailability(availability);
    setOnboardingStep(null);
    setEditingAvailability(false);
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-gray-500">Loading...</div>
      </div>
    );
  }

  // Onboarding flow
  if (onboardingStep) {
    return (
      <div className="min-h-screen bg-gray-50">
        <nav className="bg-white shadow">
          <div className="max-w-4xl mx-auto px-4 py-3 flex items-center justify-between">
            <h1 className="text-xl font-bold">Pairr</h1>
            <div className="flex flex-col items-end">
              {user?.username && <span className="text-sm font-bold text-gray-900 leading-tight">@{user.username}</span>}
              <span className="text-[10px] text-gray-500 font-medium uppercase tracking-wider">{user?.email}</span>
            </div>
          </div>
        </nav>

        {/* Progress indicator */}
        <div className="max-w-2xl mx-auto px-4 pt-8 pb-4">
          <div className="flex items-center justify-center gap-3 mb-8">
            <div className={`flex items-center gap-2 ${
              onboardingStep === 'skills' ? 'text-blue-600' : 'text-green-600'
            }`}>
              <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium ${
                onboardingStep === 'skills'
                  ? 'bg-blue-600 text-white'
                  : 'bg-green-600 text-white'
              }`}>
                {onboardingStep === 'skills' ? '1' : (
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                  </svg>
                )}
              </div>
              <span className="text-sm font-medium">Skills</span>
            </div>

            <div className="w-12 h-px bg-gray-300" />

            <div className={`flex items-center gap-2 ${
              onboardingStep === 'availability' ? 'text-blue-600' : 'text-gray-400'
            }`}>
              <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium ${
                onboardingStep === 'availability'
                  ? 'bg-blue-600 text-white'
                  : 'bg-gray-200 text-gray-500'
              }`}>
                2
              </div>
              <span className="text-sm font-medium">Availability</span>
            </div>
          </div>
        </div>

        <div className="px-4 pb-8">
          {onboardingStep === 'skills' && (
            <SkillSetup
              onComplete={handleSkillsComplete}
              existingSkills={userSkills || []}
            />
          )}
          {onboardingStep === 'availability' && (
            <AvailabilitySetup
              onComplete={handleAvailabilityComplete}
              existingAvailability={userAvailability || []}
            />
          )}
        </div>
      </div>
    );
  }

  // Main dashboard with profile management
  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="bg-white shadow">
        <div className="max-w-4xl mx-auto px-4 py-3 flex items-center justify-between">
          <h1 className="text-xl font-bold">Pairr</h1>
          <div className="flex items-center gap-4">
            <div className="flex flex-col items-end">
              {user?.username && <span className="text-sm font-bold text-gray-900 leading-tight">@{user.username}</span>}
              <span className="text-[10px] text-gray-500 font-medium uppercase tracking-wider">{user?.email}</span>
            </div>
            <button
              onClick={handleLogout}
              className="text-sm text-red-600 hover:underline"
            >
              Logout
            </button>
          </div>
        </div>
      </nav>

      <main className="max-w-4xl mx-auto px-4 py-8 space-y-6">
        {/* Skills Section */}
        <section className="bg-white rounded-lg shadow-sm border p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold">My Skills</h2>
            <button
              onClick={() => setEditingSkills(!editingSkills)}
              className="text-sm text-blue-600 hover:underline"
            >
              {editingSkills ? 'Cancel' : 'Edit'}
            </button>
          </div>

          {editingSkills ? (
            <SkillSetup
              onComplete={handleSkillsComplete}
              existingSkills={userSkills || []}
              isEditing={true}
            />
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {userSkills?.map((us) => (
                <div
                  key={us.skill.id}
                  className="group flex items-center justify-between p-3 rounded-xl border border-gray-200 hover:border-blue-300 hover:bg-blue-50/30 transition-all"
                >
                  <div className="flex-1">
                    <p className="text-sm font-bold text-gray-900">{us.skill.name}</p>
                    <div className="flex items-center gap-2 mt-0.5">
                      <span className="text-[10px] bg-gray-100 text-gray-600 px-1.5 py-0.5 rounded font-bold uppercase tracking-wider">
                        {PROFICIENCY_LABELS[us.proficiencyLevel]}
                      </span>
                      {us.rating > 0 && (
                        <span className="text-[10px] text-yellow-600 font-bold">
                          {us.rating.toFixed(1)} ★
                        </span>
                      )}
                    </div>
                  </div>
                  <button
                    onClick={() => setRecommendationSkill(us.skill)}
                    className="ml-3 px-3 py-1.5 bg-white border border-gray-200 text-blue-600 rounded-lg text-xs font-bold shadow-sm hover:bg-blue-600 hover:text-white hover:border-blue-600 transition-all opacity-0 group-hover:opacity-100 transform translate-x-2 group-hover:translate-x-0"
                  >
                    Find Partners
                  </button>
                </div>
              ))}
            </div>
          )}
        </section>

        {/* Availability Section */}
        <section className="bg-white rounded-lg shadow-sm border p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold">My Availability</h2>
            <button
              onClick={() => setEditingAvailability(!editingAvailability)}
              className="text-sm text-blue-600 hover:underline"
            >
              {editingAvailability ? 'Cancel' : 'Edit'}
            </button>
          </div>

          {editingAvailability ? (
            <AvailabilitySetup
              onComplete={handleAvailabilityComplete}
              existingAvailability={userAvailability || []}
              isEditing={true}
            />
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
              <div>
                <h3 className="text-xs font-bold text-gray-400 uppercase tracking-wider mb-3 flex items-center gap-2">
                  <div className="w-1.5 h-1.5 rounded-full bg-blue-500"></div>
                  Weekdays
                </h3>
                <div className="space-y-2">
                  {userAvailability?.filter(a => a.dayType === 'WEEKDAY').length > 0 ? (
                    userAvailability
                      .filter(a => a.dayType === 'WEEKDAY')
                      .sort((a, b) => a.startTime.localeCompare(b.startTime))
                      .map((a, idx) => (
                        <div key={idx} className="p-3 rounded-lg border border-gray-100 bg-gray-50/50">
                          <p className="text-sm text-gray-700 font-medium">
                            {formatTimeDisplay(a.startTime)} — {formatTimeDisplay(a.endTime)}
                          </p>
                        </div>
                      ))
                  ) : (
                    <p className="text-sm text-gray-400 italic">No weekday slots set</p>
                  )}
                </div>
              </div>
              <div>
                <h3 className="text-xs font-bold text-gray-400 uppercase tracking-wider mb-3 flex items-center gap-2">
                  <div className="w-1.5 h-1.5 rounded-full bg-indigo-500"></div>
                  Weekends
                </h3>
                <div className="space-y-2">
                  {userAvailability?.filter(a => a.dayType === 'WEEKEND').length > 0 ? (
                    userAvailability
                      .filter(a => a.dayType === 'WEEKEND')
                      .sort((a, b) => a.startTime.localeCompare(b.startTime))
                      .map((a, idx) => (
                        <div key={idx} className="p-3 rounded-lg border border-gray-100 bg-gray-50/50">
                          <p className="text-sm text-gray-700 font-medium">
                            {formatTimeDisplay(a.startTime)} — {formatTimeDisplay(a.endTime)}
                          </p>
                        </div>
                      ))
                  ) : (
                    <p className="text-sm text-gray-400 italic">No weekend slots set</p>
                  )}
                </div>
              </div>
            </div>
          )}
        </section>
      </main>

      {recommendationSkill && (
        <RecommendationsModal
          skill={recommendationSkill}
          onClose={() => setRecommendationSkill(null)}
        />
      )}
      <ChatWidget />
    </div>
  );
}
