'use client';

import React, { useState } from 'react';
import { useApp } from '../context/AppContext';
import {
  ArrowLeft,
  Calendar as CalendarIcon,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Camera,
  Coffee,
  Crown,
  ChevronLeft,
  ChevronRight,
  Sparkles,
} from 'lucide-react';
import {
  CertifyModal,
  PenaltyCertModal,
  CertDetailModal,
  PenaltyDetailModal,
  HabitFailModal,
} from '../components/Modals';
import { VerificationItem } from '../types';

export const HabitDetailPage: React.FC = () => {
  const {
    currentRoomId,
    selectedUserId,
    rooms,
    users,
    verifications,
    currentUser,
    setCurrentPage,
    giveUpHabit,
  } = useApp();

  const [showCertModal, setShowCertModal] = useState(false);
  const [showPenaltyModal, setShowPenaltyModal] = useState(false);
  const [showFailModal, setShowFailModal] = useState(false);
  const [selectedVerDetail, setSelectedVerDetail] = useState<VerificationItem | null>(null);
  const [selectedPenaltyDetail, setSelectedPenaltyDetail] = useState<VerificationItem | null>(null);

  const room = rooms.find((r) => r.id === currentRoomId);
  const targetUser = users.find((u) => u.id === selectedUserId);
  const member = room?.members.find((m) => m.userId === selectedUserId);

  if (!room || !targetUser || !member) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-16 text-center">
        <p className="text-slate-500 mb-4">멤버 정보를 찾을 수 없습니다.</p>
        <button
          onClick={() => setCurrentPage('room')}
          className="px-5 py-2.5 bg-emerald-500 text-white font-bold rounded-xl text-sm"
        >
          챌린지 방으로 돌아가기
        </button>
      </div>
    );
  }

  const isMe = currentUser?.id === targetUser.id;
  const isHost = member.role === 'host' || targetUser.id === room.hostId;
  const habit = member.habit;

  // Filter verifications for this user and room
  const userVerifications = verifications.filter(
    (v) => v.roomId === room.id && v.userId === targetUser.id
  );

  const habitVerifications = userVerifications.filter((v) => v.type === 'habit');
  const penaltyVerifications = userVerifications.filter((v) => v.type === 'penalty');

  // Calendar generation for 2026 September (from wireframe 9/15 ~ 10/8)
  // Let's create a 5-week matrix for September 2026
  const weekDays = ['일', '월', '화', '수', '목', '금', '토'];

  // Map of date -> verification
  const verMap = new Map<string, VerificationItem>();
  habitVerifications.forEach((v) => {
    if (v.status === 'approved') {
      verMap.set(v.date, v);
    }
  });

  // Calendar dates for September 2026
  // Sep 1, 2026 is Tuesday (index 2)
  const daysInSep = 30;
  const startDayOfWeek = 2; // Tuesday
  const totalSlots = 35; // 5 rows x 7 cols

  const calendarDays: any[] = [];
  for (let i = 0; i < totalSlots; i++) {
    const dayNumber = i - startDayOfWeek + 1;
    if (dayNumber >= 1 && dayNumber <= daysInSep) {
      const dateStr = `2026-09-${dayNumber.toString().padStart(2, '0')}`;
      const isSuccess = verMap.has(dateStr);
      // Let's mark past days before today (today is Sep 17) as failed if not verified
      const isPast = dayNumber < 17;
      const isToday = dayNumber === 17;
      const verification = verMap.get(dateStr);

      calendarDays.push({
        dayNumber,
        dateStr,
        isSuccess,
        isPast,
        isToday,
        verification,
      });
    } else {
      calendarDays.push(null);
    }
  }

  // Pre-configured mock daily feed logs to match wireframe exactly
  const feedLogs = [
    {
      id: 'log-1',
      date: '2026-09-17',
      title: habit?.title || '기상 후 물 한 잔',
      status: 'success', // 인증완료
      ver: habitVerifications.find((v) => v.date === '2026-09-17'),
    },
    {
      id: 'log-2',
      date: '2026-09-16',
      title: habit?.title || '기상 후 물 한 잔',
      status: 'penalty_done', // 미인증 후 벌칙수행 완료
      ver: habitVerifications.find((v) => v.date === '2026-09-16'),
      penaltyVer: penaltyVerifications.find((v) => v.date === '2026-09-16'),
    },
    {
      id: 'log-3',
      date: '2026-09-15',
      title: habit?.title || '기상 후 물 한 잔',
      status: 'success', // 인증완료
      ver: habitVerifications.find((v) => v.date === '2026-09-15'),
    },
    {
      id: 'log-4',
      date: '2026-09-14',
      title: habit?.title || '기상 후 물 한 잔',
      status: 'penalty_needed', // 미인증 (벌칙 수행 필요)
    },
  ];

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
        <span className="text-xs font-semibold px-2.5 py-1 rounded-full bg-slate-100 text-slate-600">
          습관 상세 페이지
        </span>
      </div>

      {/* 1. Header Profile & Penalty Counter */}
      <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6 mb-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-3.5">
            <div className="relative">
              <img
                src={targetUser.avatarUrl}
                alt={targetUser.name}
                className="w-14 h-14 rounded-full object-cover border-2 border-emerald-400 shadow-xs"
              />
              {isHost && (
                <div className="absolute -bottom-1 -right-1 bg-amber-400 text-white p-1 rounded-full shadow-2xs">
                  <Crown className="w-3 h-3 fill-white" />
                </div>
              )}
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-lg font-black text-slate-900">{targetUser.name}</h1>
                {isHost && (
                  <span className="text-[10px] font-bold px-1.5 py-0.2 bg-amber-100 text-amber-700 rounded-md">
                    방장
                  </span>
                )}
                {isMe && (
                  <span className="text-[10px] font-bold px-1.5 py-0.2 bg-emerald-100 text-emerald-700 rounded-md">
                    나
                  </span>
                )}
              </div>
              <p className="text-xs text-slate-400">@{targetUser.username}</p>
            </div>
          </div>

          <div className="flex items-center gap-3 bg-rose-50/80 border border-rose-100 px-4 py-2.5 rounded-2xl shrink-0">
            <Coffee className="w-4 h-4 text-rose-500" />
            <div>
              <span className="text-[10px] font-bold text-rose-400 uppercase">누적 벌칙</span>
              <p className="text-sm font-black text-rose-600">벌칙 횟수 : {member.penaltyCount}회</p>
            </div>
          </div>
        </div>

        {/* 2. Habit Action Box */}
        <div className="mt-5 pt-5 border-t border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <span className="text-[11px] font-bold text-emerald-600 uppercase">도전 습관</span>
            <h2 className="text-lg font-extrabold text-slate-900">
              {habit?.title || '등록된 습관이 없습니다'}
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">
              {habit?.description || '매일매일 실천하고 성공을 기록해보세요!'}
            </p>
          </div>

          {/* Buttons: Certify & Give Up */}
          {isMe && (
            <div className="flex items-center gap-2 shrink-0">
              <button
                onClick={() => setShowCertModal(true)}
                className="flex items-center gap-1.5 px-4 py-2.5 rounded-xl font-bold text-xs text-white bg-emerald-500 hover:bg-emerald-600 shadow-md shadow-emerald-500/20 transition-all hover:scale-102 cursor-pointer"
              >
                <Camera className="w-4 h-4" />
                <span>오늘 인증하기</span>
              </button>

              <button
                onClick={() => setShowFailModal(true)}
                className="px-3.5 py-2.5 rounded-xl font-bold text-xs text-rose-600 hover:bg-rose-50 border border-rose-200 transition-colors cursor-pointer"
              >
                습관 포기하기
              </button>
            </div>
          )}
        </div>
      </div>

      {/* 3. Calendar Grid (9월 캘린더) */}
      <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6 mb-6">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <CalendarIcon className="w-4 h-4 text-emerald-600" />
            <h3 className="text-sm font-extrabold text-slate-900">2026년 9월 인증 캘린더</h3>
          </div>
          <div className="flex items-center gap-2 text-[11px]">
            <span className="flex items-center gap-1 text-slate-500">
              <span className="w-2.5 h-2.5 rounded-sm bg-emerald-500 inline-block"></span>
              성공
            </span>
            <span className="flex items-center gap-1 text-slate-500">
              <span className="w-2.5 h-2.5 rounded-sm bg-rose-400 inline-block"></span>
              미인증
            </span>
            <span className="flex items-center gap-1 text-slate-400">
              <span className="w-2.5 h-2.5 rounded-sm bg-slate-200 inline-block"></span>
              대기
            </span>
          </div>
        </div>

        {/* Days of week header */}
        <div className="grid grid-cols-7 gap-2 mb-2 text-center text-xs font-bold text-slate-400">
          {weekDays.map((d, i) => (
            <div key={i} className={i === 0 ? 'text-rose-400' : i === 6 ? 'text-blue-400' : ''}>
              {d}
            </div>
          ))}
        </div>

        {/* Calendar Day Slots */}
        <div className="grid grid-cols-7 gap-2">
          {calendarDays.map((cell, idx) => {
            if (!cell) {
              return <div key={idx} className="h-12 sm:h-14 rounded-xl bg-transparent" />;
            }

            const { dayNumber, isSuccess, isPast, isToday, verification } = cell;

            let bgColor = 'bg-slate-50 border-slate-200/70 text-slate-500';
            if (isSuccess) {
              bgColor =
                'bg-emerald-500 text-white font-bold border-emerald-600 shadow-xs cursor-pointer hover:bg-emerald-600 hover:scale-105 transition-all';
            } else if (isPast) {
              bgColor = 'bg-rose-50 border-rose-200 text-rose-700';
            } else if (isToday) {
              bgColor = 'bg-amber-50 border-amber-300 text-amber-900 font-bold ring-2 ring-amber-200';
            }

            return (
              <div
                key={idx}
                onClick={() => {
                  if (verification) {
                    setSelectedVerDetail(verification);
                  }
                }}
                className={`h-12 sm:h-14 rounded-2xl border flex flex-col items-center justify-center relative select-none transition-all ${bgColor}`}
              >
                <span className="text-xs">{dayNumber}</span>
                {isSuccess ? (
                  <span className="text-[10px] mt-0.5">성공 ✓</span>
                ) : isPast ? (
                  <span className="text-[9px] text-rose-400 mt-0.5">미인증</span>
                ) : isToday ? (
                  <span className="text-[9px] text-amber-600 mt-0.5">오늘</span>
                ) : null}
              </div>
            );
          })}
        </div>
        <p className="text-[11px] text-slate-400 mt-3 text-center">
          💡 초록색 성공 날짜를 클릭하면 등록된 인증 사진과 내용을 상세히 볼 수 있습니다.
        </p>
      </div>

      {/* 4. Daily Feed & Penalty Records */}
      <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6">
        <h3 className="text-sm font-extrabold text-slate-900 mb-4 flex items-center justify-between">
          <span>실천 및 벌칙 기록</span>
          <span className="text-xs font-normal text-slate-400">최근 실천 타임라인</span>
        </h3>

        <div className="space-y-3">
          {feedLogs.map((log) => {
            return (
              <div
                key={log.id}
                className="p-3.5 sm:p-4 rounded-2xl border border-slate-100 bg-slate-50/60 flex flex-col sm:flex-row sm:items-center justify-between gap-3"
              >
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <span className="text-xs font-bold text-slate-500">{log.date}</span>
                    <span className="text-xs font-extrabold text-slate-900">{log.title}</span>
                  </div>
                  <p className="text-[11px] text-slate-400">
                    {log.ver?.content || '챌린지 약속 시간 준수 및 실천 내역'}
                  </p>
                </div>

                {/* Status and Action Buttons */}
                <div className="flex items-center gap-2 shrink-0">
                  {log.status === 'success' && (
                    <button
                      onClick={() => log.ver && setSelectedVerDetail(log.ver)}
                      className="flex items-center gap-1 text-xs font-bold text-emerald-700 bg-emerald-100 hover:bg-emerald-200 px-3 py-1.5 rounded-xl transition-colors cursor-pointer"
                    >
                      <CheckCircle2 className="w-3.5 h-3.5" />
                      <span>인증완료</span>
                    </button>
                  )}

                  {log.status === 'penalty_done' && (
                    <>
                      <span className="text-xs font-bold text-slate-400 px-2.5 py-1 rounded-xl bg-slate-200">
                        미인증
                      </span>
                      <button
                        onClick={() =>
                          setSelectedPenaltyDetail(
                            log.penaltyVer || {
                              id: 'p-mock',
                              roomId: room.id,
                              userId: targetUser.id,
                              userName: targetUser.name,
                              userAvatar: targetUser.avatarUrl,
                              habitTitle: habit?.title || '습관',
                              type: 'penalty',
                              date: log.date,
                              imageUrl:
                                'https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=500&auto=format&fit=crop&q=80',
                              content: '스타벅스 아메리카노 기프티콘 단톡방에 보냈습니다! 다음엔 꼭 기상할게요 ㅠㅠ',
                              status: 'approved',
                              createdAt: '2026-09-16 14:20',
                            }
                          )
                        }
                        className="flex items-center gap-1 text-xs font-bold text-rose-700 bg-rose-100 hover:bg-rose-200 px-3 py-1.5 rounded-xl transition-colors cursor-pointer"
                      >
                        <Coffee className="w-3.5 h-3.5" />
                        <span>벌칙완료 확인</span>
                      </button>
                    </>
                  )}

                  {log.status === 'penalty_needed' && (
                    <>
                      <span className="text-xs font-bold text-rose-600 px-2.5 py-1 rounded-xl bg-rose-100">
                        미인증
                      </span>
                      {isMe && (
                        <button
                          onClick={() => setShowPenaltyModal(true)}
                          className="flex items-center gap-1 text-xs font-bold text-white bg-rose-500 hover:bg-rose-600 px-3 py-1.5 rounded-xl shadow-xs transition-all hover:scale-102 cursor-pointer"
                        >
                          <Coffee className="w-3.5 h-3.5" />
                          <span>벌칙 수행하기</span>
                        </button>
                      )}
                    </>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Modals */}
      <CertifyModal
        isOpen={showCertModal}
        onClose={() => setShowCertModal(false)}
        roomId={room.id}
      />
      <PenaltyCertModal
        isOpen={showPenaltyModal}
        onClose={() => setShowPenaltyModal(false)}
        roomId={room.id}
        penaltyText={room.penaltyText}
      />
      <CertDetailModal
        item={selectedVerDetail}
        onClose={() => setSelectedVerDetail(null)}
      />
      <PenaltyDetailModal
        item={selectedPenaltyDetail}
        onClose={() => setSelectedPenaltyDetail(null)}
      />
      <HabitFailModal
        isOpen={showFailModal}
        onClose={() => setShowFailModal(false)}
        onConfirm={() => giveUpHabit(room.id, targetUser.id)}
      />
    </div>
  );
};
