export default function ErrorMessage({ message, onClear }) {
  if (!message) return null;

  return (
    <div className="group relative bg-white border border-red-100 rounded-xl p-3 shadow-sm shadow-red-50 mb-5 animate-in fade-in slide-in-from-top-2 duration-300">
      <div className="flex items-start gap-3">
        <div className="flex-1 pr-6">
          <p className="text-sm text-red-600 font-medium">
            {message}
          </p>
        </div>

        {onClear && (
          <button
            onClick={onClear}
            className="absolute top-2.5 right-2.5 p-1.5 rounded-lg text-red-300 hover:text-red-500 hover:bg-red-50 transition-all opacity-0 group-hover:opacity-100 focus:opacity-100"
            aria-label="Clear error"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        )}
      </div>
    </div>
  );
}
