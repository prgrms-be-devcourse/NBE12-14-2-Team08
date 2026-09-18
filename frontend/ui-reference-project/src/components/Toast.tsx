'use client';

import React from 'react';
import { useApp } from '../context/AppContext';
import { CheckCircle2, AlertCircle } from 'lucide-react';

export const Toast: React.FC = () => {
  const { toast } = useApp();
  if (!toast) return null;

  const isAlert = toast.includes('실패') || toast.includes('초과') || toast.includes('탈퇴') || toast.includes('주의');

  return (
    <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-50 animate-bounce transition-all duration-300">
      <div
        className={`flex items-center gap-2.5 px-5 py-3 rounded-full shadow-xl text-sm font-semibold text-white ${
          isAlert ? 'bg-rose-500' : 'bg-emerald-600'
        }`}
      >
        {isAlert ? <AlertCircle className="w-5 h-5 shrink-0" /> : <CheckCircle2 className="w-5 h-5 shrink-0" />}
        <span>{toast}</span>
      </div>
    </div>
  );
};
