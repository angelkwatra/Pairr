import { useState, useEffect } from 'react';
import { api } from '../api/client';

const PROFICIENCY_LEVELS = ['BEGINNER', 'AMATEUR', 'INTERMEDIATE', 'EXPERT'];

const PROFICIENCY_LABELS = {
  BEGINNER: 'Beginner',
  AMATEUR: 'Amateur',
  INTERMEDIATE: 'Intermediate',
  EXPERT: 'Expert',
};

export default function SkillSetup({ onComplete, existingSkills = [], isEditing = false }) {
  const [skills, setSkills] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  // Map of skillId -> proficiency
  const [selected, setSelected] = useState(() => {
    const map = {};
    existingSkills.forEach((us) => {
      map[us.skill.id] = us.proficiencyLevel;
    });
    return map;
  });
  const [expandedCategory, setExpandedCategory] = useState(null);

  useEffect(() => {
    api.get('/api/skills').then((data) => {
      setSkills(data);
      setLoading(false);
    }).catch((err) => {
      setError(err.message);
      setLoading(false);
    });
  }, []);

  // Group skills by category
  const grouped = skills.reduce((acc, skill) => {
    const cat = skill.categoryName || 'Other';
    if (!acc[cat]) acc[cat] = { id: skill.categoryId, skills: [] };
    acc[cat].skills.push(skill);
    return acc;
  }, {});

  const selectedCount = Object.keys(selected).length;

  const toggleSkill = (skillId) => {
    setSelected((prev) => {
      const next = { ...prev };
      if (next[skillId]) {
        delete next[skillId];
      } else {
        next[skillId] = 'BEGINNER';
      }
      return next;
    });
  };

  const setProficiency = (skillId, level) => {
    setSelected((prev) => ({ ...prev, [skillId]: level }));
  };

  const handleSubmit = async () => {
    if (selectedCount < 3) return;
    setSubmitting(true);
    setError('');

    try {
      const allSkills = Object.entries(selected)
        .map(([skillId, proficiency]) => ({ skillId, proficiency }));

      await api.post('/api/user/skills', allSkills);
      onComplete();
    } catch (err) {
      setError(err.message || 'Failed to save skills');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-16">
        <div className="text-gray-500">Loading skills...</div>
      </div>
    );
  }

  return (
    <div className="w-full max-w-2xl mx-auto">
      <div className="text-center mb-6">
        <h2 className="text-2xl font-bold mb-2">Select Your Skills</h2>
        <p className="text-gray-500 text-sm">
          Choose at least 3 skills and set your proficiency level for each.
        </p>
      </div>

      {error && (
        <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded text-red-700 text-sm">
          {error}
        </div>
      )}

      <div className="space-y-2 mb-6">
        {Object.entries(grouped).map(([categoryName, { skills: catSkills }]) => {
          const isExpanded = expandedCategory === categoryName;
          const selectedInCategory = catSkills.filter((s) => selected[s.id]).length;
          return (
            <div key={categoryName} className="bg-white rounded-lg shadow-sm border">
              <button
                type="button"
                onClick={() => setExpandedCategory(isExpanded ? null : categoryName)}
                className="w-full flex items-center justify-between px-4 py-3 text-left hover:bg-gray-50 rounded-lg"
              >
                <div className="flex items-center gap-2">
                  <span className="font-medium">{categoryName}</span>
                  {selectedInCategory > 0 && (
                    <span className="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded-full">
                      {selectedInCategory} selected
                    </span>
                  )}
                </div>
                <svg
                  className={`w-4 h-4 text-gray-400 transition-transform ${isExpanded ? 'rotate-180' : ''}`}
                  fill="none" viewBox="0 0 24 24" stroke="currentColor"
                >
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                </svg>
              </button>

              {isExpanded && (
                <div className="px-4 pb-3 space-y-2">
                  {catSkills.map((skill) => {
                    const isSelected = !!selected[skill.id];
                    return (
                      <div
                        key={skill.id}
                        className={`flex items-center justify-between p-3 rounded-lg border transition-colors ${
                          isSelected
                            ? 'border-blue-300 bg-blue-50'
                            : 'border-gray-200 hover:border-gray-300'
                        }`}
                      >
                        <button
                          type="button"
                          onClick={() => toggleSkill(skill.id)}
                          className="flex items-center gap-2 text-left flex-1"
                        >
                          <div className={`w-5 h-5 rounded border-2 flex items-center justify-center flex-shrink-0 ${
                            isSelected ? 'bg-blue-600 border-blue-600' : 'border-gray-300'
                          }`}>
                            {isSelected && (
                              <svg className="w-3 h-3 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
                              </svg>
                            )}
                          </div>
                          <span className={`text-sm ${isSelected ? 'font-medium' : ''}`}>
                            {skill.name}
                          </span>
                        </button>

                        {isSelected && (
                          <select
                            value={selected[skill.id]}
                            onChange={(e) => setProficiency(skill.id, e.target.value)}
                            className="text-xs border rounded px-2 py-1 bg-white focus:outline-none focus:ring-1 focus:ring-blue-500"
                          >
                            {PROFICIENCY_LEVELS.map((level) => (
                              <option key={level} value={level}>
                                {PROFICIENCY_LABELS[level]}
                              </option>
                            ))}
                          </select>
                        )}
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          );
        })}
      </div>

      <div className="sticky bottom-0 bg-gray-50 pt-4 pb-2">
        <div className="flex items-center justify-between mb-3">
          <span className={`text-sm ${selectedCount >= 3 ? 'text-green-600' : 'text-gray-500'}`}>
            {selectedCount} skill{selectedCount !== 1 ? 's' : ''} selected
            {selectedCount < 3 && ` (need at least 3)`}
          </span>
        </div>
        <button
          onClick={handleSubmit}
          disabled={selectedCount < 3 || submitting}
          className="w-full bg-blue-600 text-white rounded-lg py-2.5 text-sm font-medium hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {submitting ? 'Saving...' : (isEditing ? 'Save Changes' : 'Continue')}
        </button>
      </div>
    </div>
  );
}
