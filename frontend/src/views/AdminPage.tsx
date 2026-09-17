'use client';

import React, { useState } from 'react';
import { useApp } from '../context/AppContext';
import {
  ArrowLeft,
  CheckCircle2,
  XCircle,
  Clock,
  ShieldCheck,
  CheckSquare,
  Square,
  Coffee,
  Calendar,
  ExternalLink,
} from 'lucide-react';
import { CertDetailModal } from '../components/Modals';
import { VerificationItem } from '../types';

export const AdminPage: React.FC = () => {
  const {
    currentRoomId,
    rooms,
    verifications,
    approveVerification,
    rejectVerification,
    batchApprove,
    batchReject,
    setCurrentPage,
  } = useApp();

  const [activeTab, setActiveTab] = useState<'habit' | 'penalty'>('habit');
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [previewItem, setPreviewItem] = useState<VerificationItem | null>(null);

  const room = rooms.find((r) => r.id === currentRoomId) || rooms[0];

  // Verifications for this room and activeTab
  const currentList = verifications.filter(
    (v) => v.roomId === room.id && v.type === activeTab
  );

  const pendingList = currentList.filter((v) => v.status === 'pending');
  const allCurrentIds = currentList.map((v) => v.id);

  const isAllSelected =
    currentList.length > 0 && currentList.every((v) => selectedIds.includes(v.id));

  const toggleSelectAll = () => {
    if (isAllSelected) {
      setSelectedIds([]);
    } else {
      setSelectedIds(allCurrentIds);
    }
  };

  const toggleSelect = (id: string) => {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((i) => i !== id) : [...prev, id]
    );
  };

  const handleBatchApprove = () => {
    if (selectedIds.length === 0) return;
    batchApprove(selectedIds);
    setSelectedIds([]);
  };

  const handleBatchReject = () => {
    if (selectedIds.length === 0) return;
    batchReject(selectedIds);
    setSelectedIds([]);
  };

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 py-6 md:py-8">
      {/* Top Nav */}
      <div className="flex items-center justify-between mb-6">
        <button
          onClick={() => setCurrentPage('room')}
          className="flex items-center gap-1.5 text-xs font-bold text-slate-500 hover:text-slate-900 transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>← 챌린지 방으로</span>
        </button>

        <span className="text-xs font-bold px-3 py-1 rounded-full bg-amber-100 text-amber-800 flex items-center gap-1">
          <ShieldCheck className="w-3.5 h-3.5" />
          관리자 페이지 (방장 권한)
        </span>
      </div>

      <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6 sm:p-8">
        {/* Title */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-slate-100">
          <div>
            <h1 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight">
              챌린지 미인증 / 승인 내역 관리
            </h1>
            <p className="text-xs text-slate-500 mt-1">
              '{room.title}' 방 멤버들이 제출한 습관 인증 및 벌칙 수행 사진을 검토하고 승인하세요.
            </p>
          </div>

          {/* Action Buttons: Batch Approve / Reject / Select All */}
          <div className="flex items-center gap-2 flex-wrap">
            <button
              onClick={toggleSelectAll}
              className="flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-bold text-slate-700 bg-slate-100 hover:bg-slate-200 transition-colors"
            >
              {isAllSelected ? (
                <CheckSquare className="w-4 h-4 text-emerald-600" />
              ) : (
                <Square className="w-4 h-4 text-slate-400" />
              )}
              <span>전체 선택</span>
            </button>

            <button
              onClick={handleBatchApprove}
              disabled={selectedIds.length === 0}
              className={`flex items-center gap-1 px-3.5 py-2 rounded-xl text-xs font-bold transition-all ${
                selectedIds.length > 0
                  ? 'bg-emerald-500 text-white hover:bg-emerald-600 shadow-sm cursor-pointer'
                  : 'bg-slate-100 text-slate-400 cursor-not-allowed'
              }`}
            >
              <CheckCircle2 className="w-3.5 h-3.5" />
              <span>선택 승인 ({selectedIds.length})</span>
            </button>

            <button
              onClick={handleBatchReject}
              disabled={selectedIds.length === 0}
              className={`flex items-center gap-1 px-3.5 py-2 rounded-xl text-xs font-bold transition-all ${
                selectedIds.length > 0
                  ? 'bg-rose-50 text-rose-600 hover:bg-rose-100 border border-rose-200 cursor-pointer'
                  : 'bg-slate-100 text-slate-400 cursor-not-allowed'
              }`}
            >
              <XCircle className="w-3.5 h-3.5" />
              <span>선택 거절 ({selectedIds.length})</span>
            </button>
          </div>
        </div>

        {/* Tab Selection: 습관 인증 vs 벌칙 인증 */}
        <div className="flex items-center gap-4 pt-6 mb-6">
          <button
            onClick={() => {
              setActiveTab('habit');
              setSelectedIds([]);
            }}
            className={`px-4 py-2.5 rounded-2xl text-xs font-bold transition-all flex items-center gap-1.5 ${
              activeTab === 'habit'
                ? 'bg-emerald-500 text-white shadow-md shadow-emerald-500/20'
                : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
            }`}
          >
            <span>습관 인증</span>
            <span className="text-[10px] px-1.5 py-0.2 bg-white/20 rounded-full font-extrabold">
              {verifications.filter((v) => v.roomId === room.id && v.type === 'habit').length}
            </span>
          </button>

          <button
            onClick={() => {
              setActiveTab('penalty');
              setSelectedIds([]);
            }}
            className={`px-4 py-2.5 rounded-2xl text-xs font-bold transition-all flex items-center gap-1.5 ${
              activeTab === 'penalty'
                ? 'bg-rose-500 text-white shadow-md shadow-rose-500/20'
                : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
            }`}
          >
            <Coffee className="w-3.5 h-3.5" />
            <span>벌칙 인증</span>
            <span className="text-[10px] px-1.5 py-0.2 bg-white/20 rounded-full font-extrabold">
              {verifications.filter((v) => v.roomId === room.id && v.type === 'penalty').length}
            </span>
          </button>
        </div>

        {/* Cards Grid (와이어프레임과 동일한 카드형 레이아웃) */}
        {currentList.length === 0 ? (
          <div className="text-center py-16 bg-slate-50 rounded-2xl border border-dashed border-slate-200">
            <p className="text-xs font-semibold text-slate-400">
              제출된 {activeTab === 'habit' ? '습관 인증' : '벌칙 인증'} 내역이 없습니다.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
            {currentList.map((item) => {
              const isChecked = selectedIds.includes(item.id);

              return (
                <div
                  key={item.id}
                  className={`relative rounded-2xl border p-3 flex flex-col justify-between transition-all bg-white hover:shadow-md ${
                    isChecked ? 'border-emerald-500 ring-2 ring-emerald-100' : 'border-slate-200'
                  }`}
                >
                  {/* Top Bar: Checkbox + Date */}
                  <div className="flex items-center justify-between mb-2">
                    <button
                      type="button"
                      onClick={() => toggleSelect(item.id)}
                      className="text-slate-400 hover:text-emerald-600 transition-colors"
                    >
                      {isChecked ? (
                        <CheckSquare className="w-4 h-4 text-emerald-600" />
                      ) : (
                        <Square className="w-4 h-4" />
                      )}
                    </button>
                    <span className="text-[10px] font-bold text-slate-400 flex items-center gap-0.5">
                      <Calendar className="w-3 h-3" />
                      {item.date.slice(5)}
                    </span>
                  </div>

                  {/* Photo Thumbnail */}
                  <div
                    onClick={() => setPreviewItem(item)}
                    className="relative w-full h-32 rounded-xl overflow-hidden bg-slate-100 border border-slate-100 cursor-pointer group mb-2.5"
                  >
                    <img
                      src={item.imageUrl}
                      alt={item.habitTitle}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform"
                    />
                    <div className="absolute inset-0 bg-black/30 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center text-white text-[10px] font-bold gap-1">
                      <ExternalLink className="w-3 h-3" />
                      <span>크게보기</span>
                    </div>

                    {/* Status Badge */}
                    <div className="absolute bottom-1 right-1">
                      {item.status === 'approved' && (
                        <span className="text-[9px] font-bold px-1.5 py-0.5 bg-emerald-500 text-white rounded-md shadow-xs">
                          승인됨
                        </span>
                      )}
                      {item.status === 'pending' && (
                        <span className="text-[9px] font-bold px-1.5 py-0.5 bg-amber-500 text-white rounded-md shadow-xs">
                          대기중
                        </span>
                      )}
                      {item.status === 'rejected' && (
                        <span className="text-[9px] font-bold px-1.5 py-0.5 bg-rose-500 text-white rounded-md shadow-xs">
                          거절됨
                        </span>
                      )}
                    </div>
                  </div>

                  {/* Habit title & Member name */}
                  <div className="text-center">
                    <p className="text-xs font-extrabold text-slate-800 truncate" title={item.habitTitle}>
                      {item.habitTitle}
                    </p>
                    <p className="text-[11px] text-slate-400 mt-0.5 font-medium">
                      {item.userName}
                    </p>
                  </div>

                  {/* Single item action buttons */}
                  <div className="mt-3 pt-2 border-t border-slate-100 flex items-center gap-1">
                    <button
                      onClick={() => approveVerification(item.id)}
                      className="flex-1 py-1 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 text-[11px] font-bold rounded-lg transition-colors"
                    >
                      승인
                    </button>
                    <button
                      onClick={() => rejectVerification(item.id)}
                      className="flex-1 py-1 bg-rose-50 hover:bg-rose-100 text-rose-600 text-[11px] font-bold rounded-lg transition-colors"
                    >
                      거절
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      <CertDetailModal item={previewItem} onClose={() => setPreviewItem(null)} />
    </div>
  );
};
