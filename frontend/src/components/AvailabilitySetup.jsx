import { useState } from 'react';
import { api } from '../api/client';

function generateTimeOptions() {
  const options = [];
  for (let h = 0; h < 24; h++) {
    for (let m = 0; m < 60; m += 30) {
      const time = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
      options.push(time);
    }
  }
  return options;
}

const TIME_OPTIONS = generateTimeOptions();

function formatTime(time) {
  const [h, m] = time.split(':');
  const hour = parseInt(h, 10);
  const ampm = hour >= 12 ? 'PM' : 'AM';
  const display = hour === 0 ? 12 : hour > 12 ? hour - 12 : hour;
  return `${display}:${m} ${ampm}`;
}

function timeToMinutes(time) {
  if (!time) return 0;
  const [h, m] = time.split(':').map(Number);
  return h * 60 + m;
}

function getHoursDiff(start, end) {
  return (timeToMinutes(end) - timeToMinutes(start)) / 60;
}

function getOverlapErrors(slots) {
  const errors = new Array(slots.length).fill(null);
  const minutes = slots.map(s => ({
    start: timeToMinutes(s.startTime),
    end: timeToMinutes(s.endTime)
  }));

  for (let i = 0; i < minutes.length; i++) {
    for (let j = i + 1; j < minutes.length; j++) {
      if (minutes[i].start < minutes[j].end && minutes[j].start < minutes[i].end) {
        errors[i] = "Overlaps with another slot";
        errors[j] = "Overlaps with another slot";
      }
    }
  }
  return errors;
}

function TimeSlotRow({ slot, onChange, onRemove, showRemove, error }) {
  const diff = getHoursDiff(slot.startTime, slot.endTime);
  const isValid = diff >= 1;

  return (
    <div className="mb-4">
      <div className="flex items-center gap-2">
        <div className="flex-1">
          <select
            value={slot.startTime}
            onChange={(e) => onChange({ ...slot, startTime: e.target.value })}
            className="w-full border border-gray-300 rounded-md px-2 py-1.5 text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white"
          >
            {TIME_OPTIONS.map((t) => (
              <option key={t} value={t}>{formatTime(t)}</option>
            ))}
          </select>
        </div>

        <span className="text-gray-400 text-xs font-medium uppercase">to</span>

        <div className="flex-1">
          <select
            value={slot.endTime}
            onChange={(e) => onChange({ ...slot, endTime: e.target.value })}
            className="w-full border border-gray-300 rounded-md px-2 py-1.5 text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white"
          >
            {TIME_OPTIONS.map((t) => (
              <option key={t} value={t}>{formatTime(t)}</option>
            ))}
          </select>
        </div>

        {showRemove && (
          <button
            onClick={onRemove}
            className="p-1.5 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded-md transition-colors"
            aria-label="Remove slot"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/></svg>
          </button>
        )}
      </div>

      <div className="mt-1 flex justify-between items-center px-1">
        {error ? (
          <span className="text-[11px] text-red-600 font-medium">{error}</span>
        ) : !isValid ? (
          <span className="text-[11px] text-red-600 font-medium">End must be after start (min 1h)</span>
        ) : (
          <span className="text-[11px] text-green-600 font-medium">{diff}h duration</span>
        )}
      </div>
    </div>
  );
}

function DayTypeSection({ label, slots, onChange, errors, maxSlots = 4 }) {
  const addSlot = () => {
    if (slots.length < maxSlots) {
      const lastSlot = slots[slots.length - 1];
      let newStart = '09:00';
      let newEnd = '10:00';
      
      if (lastSlot) {
        const lastEndMin = timeToMinutes(lastSlot.endTime);
        if (lastEndMin + 60 <= 23 * 60 + 30) {
            const h = Math.floor((lastEndMin) / 60);
            const m = lastEndMin % 60;
            newStart = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
            const eh = Math.floor((lastEndMin + 60) / 60);
            const em = (lastEndMin + 60) % 60;
            newEnd = `${String(eh).padStart(2, '0')}:${String(em).padStart(2, '0')}`;
        }
      }

      onChange([...slots, { startTime: newStart, endTime: newEnd }]);
    }
  };

  const updateSlot = (index, newSlot) => {
    const newSlots = [...slots];
    newSlots[index] = newSlot;
    onChange(newSlots);
  };

  const removeSlot = (index) => {
    onChange(slots.filter((_, i) => i !== index));
  };

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-5">
      <div className="flex items-center justify-between mb-5">
        <div>
          <h3 className="font-bold text-gray-900 text-lg">{label}</h3>
          <p className="text-xs text-gray-500">Up to {maxSlots} time windows</p>
        </div>
        <div className="bg-gray-100 px-2 py-1 rounded text-[10px] font-bold text-gray-600 uppercase tracking-wider">
          {slots.length} / {maxSlots}
        </div>
      </div>

      <div className="space-y-2">
        {slots.map((slot, index) => (
          <TimeSlotRow
            key={index}
            slot={slot}
            error={errors[index]}
            onChange={(newSlot) => updateSlot(index, newSlot)}
            onRemove={() => removeSlot(index)}
            showRemove={slots.length > 1}
          />
        ))}
      </div>

      {slots.length < maxSlots && (
        <button
          onClick={addSlot}
          className="mt-2 w-full py-2.5 border-2 border-dashed border-gray-200 rounded-lg text-sm font-medium text-gray-500 hover:border-blue-300 hover:text-blue-600 hover:bg-blue-50 transition-all flex items-center justify-center gap-2"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          Add another window
        </button>
      )}
    </div>
  );
}

export default function AvailabilitySetup({ onComplete, existingAvailability = [], isEditing = false }) {
  const trimTime = (t) => t ? t.substring(0, 5) : null;

  const initialWeekday = existingAvailability
    .filter((a) => a.dayType === 'WEEKDAY')
    .map((a) => ({ startTime: trimTime(a.startTime), endTime: trimTime(a.endTime) }));

  const initialWeekend = existingAvailability
    .filter((a) => a.dayType === 'WEEKEND')
    .map((a) => ({ startTime: trimTime(a.startTime), endTime: trimTime(a.endTime) }));

  const [weekdaySlots, setWeekdaySlots] = useState(
    initialWeekday.length > 0 ? initialWeekday : [{ startTime: '18:00', endTime: '21:00' }]
  );
  const [weekendSlots, setWeekendSlots] = useState(
    initialWeekend.length > 0 ? initialWeekend : [{ startTime: '10:00', endTime: '17:00' }]
  );

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const weekdayErrors = getOverlapErrors(weekdaySlots);
  const weekendErrors = getOverlapErrors(weekendSlots);

  const allWeekdayValid = weekdaySlots.every((s, i) => getHoursDiff(s.startTime, s.endTime) >= 1 && !weekdayErrors[i]);
  const allWeekendValid = weekendSlots.every((s, i) => getHoursDiff(s.startTime, s.endTime) >= 1 && !weekendErrors[i]);

  const canSubmit = allWeekdayValid && allWeekendValid;

  const handleSubmit = async () => {
    if (!canSubmit) return;
    setSubmitting(true);
    setError('');

    try {
      const payload = [
        ...weekdaySlots.map((s) => ({ ...s, dayType: 'WEEKDAY' })),
        ...weekendSlots.map((s) => ({ ...s, dayType: 'WEEKEND' })),
      ];
      await api.post('/api/user/availability', payload);
      onComplete();
    } catch (err) {
      setError(err.message || 'Failed to save availability');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="w-full max-w-lg mx-auto">
      <div className="text-center mb-8">
        <h2 className="text-3xl font-extrabold text-gray-900 mb-3">Set Your Availability</h2>
        <p className="text-gray-600 text-sm max-w-sm mx-auto">
          When are you available for peer programming? Add up to 4 windows for weekdays and weekends.
        </p>
      </div>

      {error && (
        <div className="mb-6 p-4 bg-red-50 border-l-4 border-red-400 rounded-r-lg text-red-700 text-sm animate-pulse">
          <div className="flex">
            <svg className="h-5 w-5 text-red-400 mr-2" viewBox="0 0 20 20" fill="currentColor">
              <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
            </svg>
            {error}
          </div>
        </div>
      )}

      <div className="space-y-6 mb-8">
        <DayTypeSection
          label="Weekday Availability"
          slots={weekdaySlots}
          errors={weekdayErrors}
          onChange={setWeekdaySlots}
        />
        <DayTypeSection
          label="Weekend Availability"
          slots={weekendSlots}
          errors={weekendErrors}
          onChange={setWeekendSlots}
        />
      </div>

      <button
        onClick={handleSubmit}
        disabled={!canSubmit || submitting}
        className="w-full bg-blue-600 hover:bg-blue-700 text-white rounded-xl py-3.5 text-base font-bold shadow-lg shadow-blue-200 transition-all transform active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed disabled:shadow-none"
      >
        {submitting ? (
          <span className="flex items-center justify-center gap-2">
            <svg className="animate-spin h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            Saving...
          </span>
        ) : (isEditing ? 'Save Changes' : 'Complete Setup')}
      </button>
    </div>
  );
}
