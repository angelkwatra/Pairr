import { useEffect, useRef } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.js';

export default function OAuthCallback() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();
  const processedRef = useRef(false);

  useEffect(() => {
    // Prevent double processing if effect runs twice in StrictMode
    if (processedRef.current) return;

    const params = new URLSearchParams(location.search);
    const token = params.get('token');

    if (token) {
      processedRef.current = true;
      login(token, {});
      
      // Redirect to dashboard immediately
      navigate('/', { replace: true });
    } else if (location.search) {
      // Only error if there actually is a query string but no token
      console.error("OAuth failed: No token received from backend");
      navigate('/login', { replace: true });
    }
  }, [login, navigate, location.search]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="flex flex-col items-center gap-4">
        <div className="w-12 h-12 border-4 border-blue-100 border-t-blue-600 rounded-full animate-spin"></div>
        <p className="text-gray-600 font-medium animate-pulse">Completing secure login...</p>
      </div>
    </div>
  );
}
