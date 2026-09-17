'use client';

import React, { useEffect } from 'react';
import { useApp } from '../context/AppContext';
import {
  ArrowLeft,
  Calendar,
  Trophy,
  Coffee,
  Crown,
  Sparkles,
  ChevronRight,
  Award,
  Flame,
} from 'lucide-react';
import confetti from 'canvas-confetti';

export const SettlementPage: React.FC = () => {
  const { currentRoomId, rooms, users, setCurrentPage, setSelectedUserId } = useApp();

  const room = rooms.find((r) => r.id === currentRoomId) || rooms[0];

  useEffect(() => {
    // Fire celebratory confetti when viewing settlement!
    confetti({
      particleCount: 80,
      spread: 70,
      origin: { y: 0.6 },
    });
  }, []);

  const membersWithInfo = room.members.map((m) => {
    const user = users.find((u) => u.id === m.userId);
    return {
      ...m,
      user,
    };
  });

  // Find penalty king (most penalties) and penalty free (0 penalties)
  const sortedByPenalty = [...membersWithInfo].sort((a, b) => b.penaltyCount - a.penaltyCount);
  const penaltyKing = sortedByPenalty[0];
  const perfectMembers = membersWithInfo.filter((m) => m.penaltyCount === 0);

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 py-6 md:py-8">
      {/* Top Nav */}
      <div className="flex items-center justify-between mb-5">
        <button
          onClick={() => setCurrentPage('room')}
          className="flex items-center gap-1.5 text-xs font-bold text-slate-500 hover:text-slate-900 transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>← 챌린지 방으로</span>
        </button>

        <span className="text-xs font-bold px-3 py-1 rounded-full bg-emerald-100 text-emerald-800 flex items-center gap-1">
          <Award className="w-3.5 h-3.5" />
          최종 정산 리포트
        </span>
      </div>

      <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6 sm:p-8 relative overflow-hidden">
        {/* SUCCESSFUL STAMP (와이어프레임 상단의 SUCCESSFUL 스탬프 재현) */}
        <div className="absolute top-6 right-6 sm:top-8 sm:right-8 rotate-[-12deg] select-none pointer-events-none z-10 animate-in zoom-in-50 duration-500">
          <div className="border-4 border-dashed border-emerald-500 px-4 py-1.5 sm:px-6 sm:py-2 rounded-2xl bg-emerald-50/90 backdrop-blur-xs shadow-lg">
            <span className="text-emerald-700 font-black tracking-widest text-lg sm:text-2xl uppercase">
              SUCCESSFUL
            </span>
          </div>
        </div>

        {/* Room Title Header */}
        <div className="pb-6 border-b border-slate-100 pr-28 sm:pr-40">
          <div className="flex items-center gap-2 mb-1">
            <span className="text-xs font-bold px-2 py-0.5 rounded-md bg-emerald-100 text-emerald-700">
              챌린지 완료
            </span>
            <span className="text-xs font-bold text-slate-400">최종 결과 발표</span>
          </div>
          <h1 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight">
            {room.title}
          </h1>
          <p className="text-xs text-slate-500 mt-1">{room.description}</p>
          <p className="text-xs font-bold text-rose-600 mt-2 flex items-center gap-1">
            <Coffee className="w-3.5 h-3.5" />
            <span>벌칙 약속: {room.penaltyText}</span>
          </p>
        </div>

        {/* Timeline Bar */}
        <div className="py-5 border-b border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs">
          <div className="flex items-center gap-2 font-bold text-slate-600">
            <Calendar className="w-4 h-4 text-emerald-600" />
            <span>챌린지 기간:</span>
            <span className="text-slate-900">{room.startDate}</span>
            <span className="text-slate-400">➔</span>
            <span className="text-slate-900">{room.deadline}까지</span>
          </div>

          <div className="text-[11px] font-semibold text-emerald-700 bg-emerald-50 px-3 py-1 rounded-xl">
            총 24일간의 습관 여정 완주! 🎉
          </div>
        </div>

        {/* 1. 참여한 챌린저 요약 리스트 */}
        <div className="py-6 border-b border-slate-100">
          <h2 className="text-sm font-extrabold text-slate-900 mb-3.5 flex items-center gap-2">
            <span>참여한 챌린저</span>
            <span className="text-xs px-2 py-0.2 rounded-full bg-slate-100 text-slate-600 font-bold">
              {room.members.length}명
            </span>
          </h2>

          <div className="space-y-2.5">
            {membersWithInfo.map((m) => {
              if (!m.user) return null;
              const isHost = m.role === 'host' || m.userId === room.hostId;

              return (
                <div
                  key={m.userId}
                  onClick={() => {
                    setSelectedUserId(m.userId);
                    setCurrentPage('habit-detail');
                  }}
                  className="p-3.5 rounded-2xl bg-slate-50 hover:bg-emerald-50/50 border border-slate-200/70 hover:border-emerald-300 transition-all flex items-center justify-between cursor-pointer group"
                >
                  <div className="flex items-center gap-3">
                    <div className="relative">
                      <img
                        src={m.user.avatarUrl}
                        alt={m.user.name}
                        className="w-9 h-9 rounded-full object-cover border"
                      />
                      {isHost && (
                        <div className="absolute -bottom-1 -right-1 bg-amber-400 p-0.5 rounded-full text-white">
                          <Crown className="w-2.5 h-2.5" />
                        </div>
                      )}
                    </div>
                    <div>
                      <div className="flex items-center gap-1.5">
                        <span className="text-xs font-bold text-slate-800">{m.user.name}</span>
                        {isHost && (
                          <span className="text-[9px] px-1 py-0.2 bg-amber-100 text-amber-800 rounded font-bold">
                            방장
                          </span>
                        )}
                      </div>
                      <p className="text-[10px] text-slate-400">@{m.user.username}</p>
                    </div>
                  </div>

                  <div className="flex items-center gap-3">
                    <span className="text-xs font-bold text-slate-700 group-hover:text-emerald-700">
                      {m.habit?.title || '습관 실천'}
                    </span>
                    <ChevronRight className="w-4 h-4 text-slate-400 group-hover:text-emerald-600" />
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* 2. 최종 벌칙 횟수 (와이어프레임 하단 원형 카드들 완벽 구현) */}
        <div className="pt-6">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-sm font-extrabold text-slate-900 flex items-center gap-2">
                <span>최종 벌칙 횟수</span>
                <span className="text-xs text-rose-500 font-bold">벌칙 수행 현황</span>
              </h2>
              <p className="text-xs text-slate-400 mt-0.5">
                미인증 횟수만큼 약속한 벌칙({room.penaltyText})을 수행해야 합니다.
              </p>
            </div>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            {membersWithInfo.map((m) => {
              if (!m.user) return null;
              const isKing = penaltyKing && m.userId === penaltyKing.userId && m.penaltyCount > 0;
              const isPerfect = m.penaltyCount === 0;

              return (
                <div
                  key={m.userId}
                  className={`p-4 rounded-3xl border flex flex-col items-center text-center transition-all ${
                    isKing
                      ? 'border-rose-300 bg-rose-50/50 shadow-md ring-2 ring-rose-200'
                      : isPerfect
                      ? 'border-emerald-300 bg-emerald-50/40 shadow-xs'
                      : 'border-slate-200 bg-slate-50/50'
                  }`}
                >
                  <div className="relative mb-2.5">
                    <img
                      src={m.user.avatarUrl}
                      alt={m.user.name}
                      className="w-14 h-14 rounded-full object-cover border-2 border-white shadow-xs"
                    />
                    {isKing && (
                      <div className="absolute -top-2 -right-2 bg-rose-500 text-white text-[10px] font-black px-1.5 py-0.5 rounded-full shadow-xs flex items-center gap-0.5">
                        <Flame className="w-2.5 h-2.5" /> 벌칙왕
                      </div>
                    )}
                    {isPerfect && (
                      <div className="absolute -top-2 -right-2 bg-emerald-500 text-white text-[10px] font-black px-1.5 py-0.5 rounded-full shadow-xs flex items-center gap-0.5">
                        <Sparkles className="w-2.5 h-2.5" /> 면제
                      </div>
                    )}
                  </div>

                  <div className="text-xs font-bold text-slate-800 flex items-center gap-1">
                    <span>{m.user.name}</span>
                    {m.role === 'host' && <Crown className="w-3 h-3 text-amber-500 fill-amber-400" />}
                  </div>
                  <span className="text-[10px] text-slate-400">@{m.user.username}</span>

                  <div className="mt-3 w-full py-1.5 rounded-xl bg-white border border-slate-100 shadow-2xs">
                    <span
                      className={`text-sm font-black ${
                        m.penaltyCount > 0 ? 'text-rose-600' : 'text-emerald-600'
                      }`}
                    >
                      {m.penaltyCount}회
                    </span>
                  </div>
                </div>
              );
            })}
          </div>

          {/* Penalty Summary banner */}
          {penaltyKing && penaltyKing.penaltyCount > 0 && (
            <div className="mt-6 p-4 rounded-2xl bg-gradient-to-r from-rose-500 to-amber-500 text-white flex flex-col sm:flex-row sm:items-center justify-between gap-3 shadow-md">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-white/20 flex items-center justify-center text-xl shrink-0">
                  ☕
                </div>
                <div>
                  <p className="text-xs font-semibold text-rose-100">이번 챌린지 커피 쏘기 주인공!</p>
                  <h4 className="text-sm font-black">
                    {penaltyKing.user?.name} 님이 총 {penaltyKing.penaltyCount}회의 벌칙을 쏩니다!
                  </h4>
                </div>
              </div>

              <button
                onClick={() => {
                  setSelectedUserId(penaltyKing.userId);
                  setCurrentPage('habit-detail');
                }}
                className="px-4 py-2 rounded-xl bg-white text-rose-600 text-xs font-bold hover:bg-rose-50 shadow-sm transition-colors shrink-0"
              >
                벌칙 내역 보러가기
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
