'use client';

import React, { useState, useEffect, use } from 'react';
import Link from 'next/link';
import PenaltyDetailModal from '@/app/components/modal/PenaltyDetailModal';

interface VerifyCardItem {
  id: number;
  date: string;
  imageUrl: string;
  habitTitle: string;
  memberName: string;
  description: string;
  penaltyText: string;
}

export default function AdminVerificationPage({
                                                params,
                                              }: {
  params: Promise<{ groupId: string }> | { groupId: string };
}) {
  const resolvedParams = params instanceof Promise ? use(params) : params;
  const groupId = Number(resolvedParams?.groupId) || 1;

  const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080';

  const [penaltyList, setPenaltyList] = useState<VerifyCardItem[]>([]);
  const [selectedPenaltyIds, setSelectedPenaltyIds] = useState<number[]>([]);
  const [selectedPenalty, setSelectedPenalty] = useState<VerifyCardItem | null>(null);

  const getAuthHeader = () => {
    const token = typeof window !== 'undefined' ? localStorage.getItem('accessToken') || '' : '';
    return token.startsWith('Bearer ') ? token : `Bearer ${token}`;
  };

  useEffect(() => {
    const fetchPendingPenalties = async () => {
      try {
        const res = await fetch(`${API_BASE_URL}/api/groups/${groupId}/penalties/pending`, {
          headers: {
            Authorization: getAuthHeader(),
          },
        });

        if (!res.ok) {
          console.error('대기 목록 조회 실패 Status:', res.status);
          return;
        }

        const data = await res.json();

        const formatted: VerifyCardItem[] = data.map((item: any) => ({
          id: item.id,
          date: item.verifyDate || '',
          imageUrl: item.imageUrl || '',
          habitTitle: item.habitTitle || '',
          memberName: item.memberNickname || '',
          description: item.description || '',
          penaltyText: item.penaltyText || '',
        }));

        setPenaltyList(formatted);
      } catch (err) {
        console.error('벌칙 대기 목록 fetch 에러:', err);
      }
    };

    fetchPendingPenalties();
  }, [groupId, API_BASE_URL]);

  const handleSinglePenaltyAction = async (
      e: React.MouseEvent | null,
      id: number,
      action: 'approve' | 'reject'
  ) => {
    if (e) e.stopPropagation();
    const actionKo = action === 'approve' ? '승인' : '거절';
    if (!confirm(`이 벌칙 인증을 ${actionKo}하시겠습니까?`)) return;

    try {
      const res = await fetch(`${API_BASE_URL}/api/penalties/${id}/${action}`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          Authorization: getAuthHeader(),
        },
      });

      if (!res.ok) {
        const errorText = await res.text();
        throw new Error(`처리 실패 (${res.status}): ${errorText}`);
      }

      alert(`벌칙 인증이 ${actionKo}되었습니다.`);
      setPenaltyList((prev) => prev.filter((item) => item.id !== id));
      setSelectedPenaltyIds((prev) => prev.filter((selectedId) => selectedId !== id));
    } catch (err: any) {
      alert(err.message);
    }
  };

  const handleBulkPenaltyAction = async (action: 'approve' | 'reject') => {
    if (selectedPenaltyIds.length === 0) {
      alert('선택된 항목이 없습니다.');
      return;
    }

    const actionKo = action === 'approve' ? '일괄 승인' : '일괄 거절';
    if (!confirm(`선택한 ${selectedPenaltyIds.length}건을 ${actionKo}하시겠습니까?`)) return;

    try {
      const endpoint = `${API_BASE_URL}/api/groups/${groupId}/penalties/bulk-${action}`;
      const res = await fetch(endpoint, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          Authorization: getAuthHeader(),
        },
        body: JSON.stringify({ ids: selectedPenaltyIds }),
      });

      if (!res.ok) throw new Error('일괄 처리에 실패했습니다.');

      alert(`${actionKo} 처리가 완료되었습니다.`);
      setPenaltyList((prev) => prev.filter((p) => !selectedPenaltyIds.includes(p.id)));
      setSelectedPenaltyIds([]);
    } catch (err: any) {
      alert(err.message);
    }
  };

  const toggleSelectAllPenalties = () => {
    if (selectedPenaltyIds.length === penaltyList.length) {
      setSelectedPenaltyIds([]);
    } else {
      setSelectedPenaltyIds(penaltyList.map((p) => p.id));
    }
  };

  return (
      <div className="min-h-screen bg-[#FAFCFA] p-8 text-gray-900 font-sans">
        <div className="mx-auto max-w-5xl rounded-[40px] bg-white p-10 shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-emerald-50">

          {/* 상단 네비게이션 및 헤더 */}
          <div className="mb-8">
            <Link
                href={`/group/${groupId}`}
                className="inline-flex items-center text-xs font-bold text-gray-400 hover:text-emerald-700 transition mb-3"
            >
              ← 그룹으로 돌아가기
            </Link>
            <h1 className="text-2xl font-black text-emerald-950 mb-1">
              관리자 페이지 <span className="text-emerald-500 font-bold text-lg ml-2">Group ID: {groupId}</span>
            </h1>
            <h2 className="text-sm font-semibold text-gray-400">미인증 내역 관리</h2>
          </div>

          <section className="mb-12 opacity-60">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <div className="w-1.5 h-5 bg-emerald-400 rounded-full"></div>
                <span className="text-lg font-bold text-gray-800">습관 인증 대기</span>
                <span className="bg-emerald-100 text-emerald-700 text-xs font-bold px-2 py-0.5 rounded-full ml-1">0</span>
              </div>
            </div>
            <div className="w-full py-8 text-center text-xs text-gray-400 bg-gray-50 rounded-2xl border border-dashed border-gray-200">
              대기 중인 습관 인증이 없습니다.
            </div>
          </section>

          <section>
            <div className="flex items-center justify-between mb-5 flex-wrap gap-3">
              <div className="flex items-center gap-2">
                <div className="w-1.5 h-5 bg-rose-400 rounded-full"></div>
                <span className="text-lg font-bold text-gray-800">벌칙 인증 대기</span>
                <span className="bg-rose-100 text-rose-700 text-xs font-bold px-2 py-0.5 rounded-full ml-1">
                {penaltyList.length}
              </span>
              </div>

              <div className="flex items-center gap-2">
                <button
                    onClick={toggleSelectAllPenalties}
                    className="rounded-full border border-gray-200 bg-white px-3.5 py-1.5 text-xs font-bold text-gray-600 hover:bg-gray-50 transition-colors cursor-pointer"
                >
                  {selectedPenaltyIds.length === penaltyList.length && penaltyList.length > 0 ? '선택 해제' : '전체 선택'}
                </button>
                <button
                    onClick={() => handleBulkPenaltyAction('approve')}
                    className="rounded-full bg-emerald-50 px-4 py-1.5 text-xs font-bold text-emerald-700 hover:bg-emerald-100 transition-colors cursor-pointer"
                >
                  선택 승인
                </button>
                <button
                    onClick={() => handleBulkPenaltyAction('reject')}
                    className="rounded-full bg-rose-50 px-4 py-1.5 text-xs font-bold text-rose-600 hover:bg-rose-100 transition-colors cursor-pointer"
                >
                  선택 거절
                </button>
              </div>
            </div>

            <div className="flex gap-5 overflow-x-auto pb-6 px-1">
              {penaltyList.map((item) => {
                const isChecked = selectedPenaltyIds.includes(item.id);
                return (
                    <div
                        key={item.id}
                        onClick={() => setSelectedPenalty(item)}
                        className={`relative flex h-[280px] w-48 flex-shrink-0 flex-col justify-between rounded-[24px] bg-white p-4 border transition-all duration-200 cursor-pointer hover:-translate-y-1 hover:shadow-lg ${
                            isChecked ? 'border-emerald-400 shadow-md ring-1 ring-emerald-400' : 'border-gray-100 shadow-sm'
                        }`}
                    >
                      <div className="flex items-center justify-between mb-2">
                        <input
                            type="checkbox"
                            checked={isChecked}
                            onClick={(e) => e.stopPropagation()}
                            onChange={() =>
                                setSelectedPenaltyIds((prev) =>
                                    prev.includes(item.id) ? prev.filter((id) => id !== item.id) : [...prev, item.id]
                                )
                            }
                            className="h-4 w-4 rounded border-gray-300 text-emerald-500 focus:ring-emerald-400 cursor-pointer"
                        />
                        <span className="text-xs font-semibold text-gray-400 bg-gray-50 px-2 py-0.5 rounded-full">
                      {item.date}
                    </span>
                      </div>

                      <div className="relative mb-3 flex h-28 w-full items-center justify-center rounded-2xl bg-[#F4F9F6] border border-emerald-50 overflow-hidden group">
                        {item.imageUrl ? (
                            <img
                                src={item.imageUrl}
                                alt="인증 사진"
                                className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
                            />
                        ) : (
                            <span className="text-xs font-medium text-emerald-300">사진 없음</span>
                        )}
                      </div>

                      <div className="text-center mb-3">
                        <div className="truncate text-[13px] font-extrabold text-gray-800 mb-0.5">
                          {item.habitTitle}
                        </div>
                        <div className="text-[11px] font-medium text-gray-400">{item.memberName}</div>
                      </div>

                      <div className="flex gap-2 pt-3 border-t border-gray-50 mt-auto">
                        <button
                            onClick={(e) => handleSinglePenaltyAction(e, item.id, 'approve')}
                            className="flex-1 py-1.5 rounded-xl bg-emerald-500 text-white hover:bg-emerald-600 text-[12px] font-bold shadow-sm transition-all cursor-pointer"
                        >
                          승인
                        </button>
                        <button
                            onClick={(e) => handleSinglePenaltyAction(e, item.id, 'reject')}
                            className="flex-1 py-1.5 rounded-xl bg-white text-gray-500 hover:bg-rose-50 hover:text-rose-600 border border-gray-200 hover:border-rose-200 text-[12px] font-bold transition-all cursor-pointer"
                        >
                          거절
                        </button>
                      </div>
                    </div>
                );
              })}

              {penaltyList.length === 0 && (
                  <div className="w-full py-12 text-center text-sm text-gray-400 bg-gray-50 rounded-3xl border border-dashed border-gray-200">
                    대기 중인 벌칙 인증이 없습니다.
                  </div>
              )}
            </div>
          </section>
        </div>
        
        {selectedPenalty && (
            <PenaltyDetailModal
                isOpen={!!selectedPenalty}
                onClose={() => setSelectedPenalty(null)}
                habitTitle={selectedPenalty.habitTitle}
                penaltyText={selectedPenalty.penaltyText}
                imageUrl={selectedPenalty.imageUrl}
                description={selectedPenalty.description}
                verifyDate={selectedPenalty.date}
                onApprove={() => handleSinglePenaltyAction(null, selectedPenalty.id, 'approve')}
                onReject={() => handleSinglePenaltyAction(null, selectedPenalty.id, 'reject')}
            />
        )}
      </div>
  );
}