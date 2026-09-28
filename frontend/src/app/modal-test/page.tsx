'use client';

import { useState } from 'react';
import { HabitVerifyModal } from '../../components/HabitVerifyModal';
import { HabitVerifyDetailModal } from '../../components/HabitVerifyDetailModal';
import { HabitVerifySettlementDetailModal } from '../../components/HabitVerifySettlementDetailModal';

const testVerification = {
  id: 1,
  habitId: 1,
  habitTitle: '기상 후 10분 운동하고 샤워하기',
  verifyDate: '2026-10-08',
  status: 'APPROVED' as const,
  description: '기상 후 운동을 완료하고 샤워했습니다.',
  imageUrl:
    'https://images.unsplash.com/photo-1517836357463-d25dfeac3438',
};


export default function Page() {
  const [isRegisterOpen, setIsRegisterOpen] = useState(false);
  const [isDetailOpen, setIsDetailOpen] = useState(false);
  const [isSettlementDetailOpen, setIsSettlementDetailOpen] =
    useState(false);

  const handleSubmit = async (data: {
    description: string;
    imageUrl: string;
  }) => {
    console.log('인증 데이터:', data);
  };

  return (
    <main className="min-h-screen bg-slate-50 p-8">
      <div className="mx-auto max-w-2xl">
        <h1 className="text-2xl font-extrabold text-slate-900">
          습관 인증 모달 테스트
        </h1>

        <div className="mt-6 flex flex-col gap-3">
          <button
            type="button"
            onClick={() => setIsRegisterOpen(true)}
            className="rounded-xl bg-emerald-500 px-6 py-3 text-sm font-bold text-white"
          >
            오늘 인증하기
          </button>

          <button
            type="button"
            onClick={() => setIsDetailOpen(true)}
            className="rounded-xl border border-slate-200 bg-white px-6 py-3 text-sm font-bold text-slate-700"
          >
            인증 상세 보기
          </button>

          <button
            type="button"
            onClick={() => setIsSettlementDetailOpen(true)}
            className="rounded-xl bg-slate-800 px-6 py-3 text-sm font-bold text-white"
          >
            결산 인증 상세 보기
          </button>
        </div>
      </div>

      {/* 인증 등록 모달 */}
      <HabitVerifyModal
        isOpen={isRegisterOpen}
        onClose={() => setIsRegisterOpen(false)}
        habitId={1}
        onSubmit={handleSubmit}
      />

      {/* 일반 인증 상세 모달 */}
      <HabitVerifyDetailModal
        isOpen={isDetailOpen}
        onClose={() => setIsDetailOpen(false)}
        verification={testVerification}
      />

      {/* 결산 메인 인증 상세 모달 */}
     <HabitVerifySettlementDetailModal
  isOpen={isSettlementDetailOpen}
  onClose={() => setIsSettlementDetailOpen(false)}
  verification={{
    id: 1,
    habitId: 1,
    habitTitle: '기상 후 10분 운동하고 샤워하기',
    verifyDate: '2026-10-08',
    status: 'APPROVED',
    description: '기상 후 운동을 완료하고 샤워했습니다.',
    imageUrl:
      'https://images.unsplash.com/photo-1517836357463-d25dfeac3438',
  }}
/>
    </main>
  );
}