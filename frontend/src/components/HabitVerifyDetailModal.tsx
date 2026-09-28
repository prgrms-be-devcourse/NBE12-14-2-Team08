'use client';

import { useEffect } from 'react';

interface HabitVerifyDetailModalProps {
  isOpen: boolean;
  onClose: () => void;
  verification: {
    id: number;
    habitId: number;
    habitTitle: string;
    verifyDate: string;
    status: 'PENDING' | 'APPROVED' | 'REJECTED';
    description: string;
    imageUrl: string;
  };
}

export function HabitVerifyDetailModal({
  isOpen,
  onClose,
  verification,
}: HabitVerifyDetailModalProps) {
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose();
      }
    };

    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, onClose]);

  if (!isOpen) {
    return null;
  }

  const statusText = {
    PENDING: '승인 대기',
    APPROVED: '성공',
    REJECTED: '실패',
  };

  const statusColor = {
    PENDING: 'text-amber-500',
    APPROVED: 'text-emerald-500',
    REJECTED: 'text-rose-500',
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-slate-900/55 p-4 backdrop-blur-sm"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) {
          onClose();
        }
      }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="habit-verify-detail-title"
        className="my-6 w-full max-w-lg rounded-3xl border border-slate-100 bg-white px-8 py-7 shadow-2xl sm:px-10 sm:py-8"
      >
        <div className="relative flex items-center justify-center">
          <h2
            id="habit-verify-detail-title"
            className={`text-3xl font-extrabold ${statusColor[verification.status]}`}
          >
            {statusText[verification.status]}
          </h2>

          <button
            type="button"
            onClick={onClose}
            aria-label="인증 상세 모달 닫기"
            className="absolute right-0 rounded-full px-2 text-2xl leading-none text-slate-400 hover:bg-slate-100"
          >
            ×
          </button>
        </div>

        <div className="mt-8 space-y-7">
          <p className="text-center text-xl font-bold leading-8 text-slate-800">
            {verification.habitTitle}
          </p>

          <div className="flex items-center justify-center overflow-hidden rounded-2xl border border-slate-200 bg-slate-50">
            {verification.imageUrl ? (
              <img
                src={verification.imageUrl}
                alt="습관 인증 사진"
                className="max-h-80 w-full object-cover"
              />
            ) : (
              <div className="flex h-72 w-full items-center justify-center">
                <span className="text-sm text-slate-400">
                  등록된 인증 사진이 없습니다.
                </span>
              </div>
            )}
          </div>

          <p className="px-2 text-center text-lg font-semibold leading-8 text-slate-700">
            {verification.description || '작성된 인증 내용이 없습니다.'}
          </p>

          <p className="pt-2 text-center text-base font-semibold text-slate-700">
            인증 날짜: {verification.verifyDate}
          </p>
        </div>
      </div>
    </div>
  );
}