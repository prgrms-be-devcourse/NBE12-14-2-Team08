'use client';

import React, { useState } from 'react';
import { useApp } from '../context/AppContext';
import {
  ArrowLeft,
  Share2,
  Settings,
  ShieldCheck,
  Calendar,
  Users,
  ChevronRight,
  Crown,
  Coffee,
  PlusCircle,
  Trophy,
  ExternalLink,
  Flame,
} from 'lucide-react';
import { RoomSettingModal, HabitRegisterModal } from '../components/Modals';
import { toRouteId } from '../lib/routeIds';

export const RoomDetailPage: React.FC = () => {
  const {
    currentRoomId,
    rooms,
    users,
    currentUser,
    setCurrentPage,
    setSelectedUserId,
    showToast,
  } = useApp();

  const [showSettingModal, setShowSettingModal] = useState(false);
  const [showHabitModal, setShowHabitModal] = useState(false);

  const room = rooms.find((r) => r.id === currentRoomId);

  if (!room) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-16 text-center">
        <p className="text-slate-500 mb-4">챌린지 방을 찾을 수 없습니다.</p>
        <button
          onClick={() => setCurrentPage('main')}
          className="px-5 py-2.5 bg-emerald-500 text-white font-bold rounded-xl text-sm"
        >
          방 목록으로 가기
        </button>
      </div>
    );
  }

  const isHost = currentUser && room.hostId === currentUser.id;
  const myMember = room.members.find((m) => m.userId === currentUser?.id);

  const handleCopyLink = () => {
    navigator.clipboard?.writeText(window.location.origin + '/invite/' + toRouteId(room.id));
    showToast('초대 링크를 클립보드에 복사했어요! 📋');
  };

  const handleMemberClick = (userId: string) => {
    setSelectedUserId(userId);
    setCurrentPage('habit-detail');
  };

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 py-6 md:py-8">
      {/* Top Bar navigation */}
      <div className="flex items-center justify-between mb-5">
        <button
          onClick={() => setCurrentPage('main')}
          className="flex items-center gap-1.5 text-xs font-bold text-slate-500 hover:text-slate-900 transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>← 방 목록으로</span>
        </button>

        <div className="flex items-center gap-2">
          {/* Quick link to preview Invite page */}
          <button
            onClick={() => setCurrentPage('invite')}
            className="text-[11px] font-semibold text-slate-500 hover:text-emerald-700 bg-slate-100 hover:bg-slate-200 px-3 py-1.5 rounded-xl flex items-center gap-1 transition-colors"
            title="초대받은 사람의 화면을 미리 볼 수 있습니다"
          >
            <ExternalLink className="w-3.5 h-3.5" />
            <span>초대 화면 보기</span>
          </button>

          {/* Settle / Settlement Page Link */}
          <button
            onClick={() => setCurrentPage('settlement')}
            className="flex items-center gap-1 px-3 py-1.5 rounded-xl text-xs font-bold text-amber-700 bg-amber-100 hover:bg-amber-200 transition-colors"
          >
            <Trophy className="w-3.5 h-3.5" />
            <span>정산 페이지</span>
          </button>
        </div>
      </div>

      {/* Main Room Card */}
      <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6 sm:p-8 mb-8">
        {/* Title and Top Info */}
        <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4 pb-6 border-b border-slate-100">
          <div>
            <div className="flex items-center gap-2 flex-wrap mb-1.5">
              <h1 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight">
                {room.title}
              </h1>
              <span className="text-xs font-bold px-2.5 py-0.5 rounded-full bg-emerald-100 text-emerald-800">
                진행중
              </span>
            </div>
            <p className="text-xs sm:text-sm text-slate-500">{room.description}</p>
          </div>

          <div className="text-left sm:text-right shrink-0">
            <div className="text-xs font-bold text-slate-400 flex items-center sm:justify-end gap-1 mb-1">
              <Calendar className="w-3.5 h-3.5" />
              <span>종료일: {room.deadline}까지</span>
            </div>
            <div className="text-xs font-bold text-slate-600 flex items-center sm:justify-end gap-1">
              <Users className="w-3.5 h-3.5" />
              <span>
                멤버 {room.members.length} / {room.maxMembers}명
              </span>
            </div>
          </div>
        </div>

        {/* Penalty bar */}
        <div className="py-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-100">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-xl bg-rose-100 text-rose-600 flex items-center justify-center shrink-0">
              <Coffee className="w-4 h-4" />
            </div>
            <div>
              <span className="text-[11px] font-bold text-slate-400 uppercase">벌칙 규칙</span>
              <p className="text-sm font-extrabold text-rose-600">{room.penaltyText}</p>
            </div>
          </div>

          {/* Action buttons */}
          <div className="flex items-center gap-2 flex-wrap">
            {/* Copy Invite Link */}
            <button
              onClick={handleCopyLink}
              className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-bold text-slate-700 bg-slate-100 hover:bg-slate-200 transition-colors"
            >
              <Share2 className="w-3.5 h-3.5" />
              <span>초대 링크 복사</span>
            </button>

            {/* Host Only Controls (주황색 점선 하이라이트 표시) */}
            {isHost && (
              <div className="flex items-center gap-1.5 p-1 rounded-2xl border-2 border-dashed border-amber-400 bg-amber-50/50">
                <span className="text-[10px] font-extrabold text-amber-700 px-1.5 hidden md:inline">
                  👑 방장 전용
                </span>
                <button
                  onClick={() => setShowSettingModal(true)}
                  className="flex items-center gap-1 px-3 py-1.5 rounded-xl text-xs font-bold text-slate-700 bg-white hover:bg-slate-50 border border-slate-200 shadow-2xs transition-colors"
                >
                  <Settings className="w-3.5 h-3.5 text-slate-500" />
                  <span>방 설정</span>
                </button>
                <button
                  onClick={() => setCurrentPage('admin')}
                  className="flex items-center gap-1 px-3 py-1.5 rounded-xl text-xs font-bold text-white bg-amber-500 hover:bg-amber-600 shadow-2xs transition-colors"
                >
                  <ShieldCheck className="w-3.5 h-3.5" />
                  <span>미인증 내역</span>
                </button>
              </div>
            )}
          </div>
        </div>

        {/* 습관 챌린저 섹션 */}
        <div className="pt-6">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-base font-bold text-slate-800 flex items-center gap-2">
                <span>습관 챌린저</span>
                <span className="text-xs px-2 py-0.5 rounded-full bg-slate-100 text-slate-600 font-bold">
                  {room.members.length}명
                </span>
              </h2>
              <p className="text-[11px] text-slate-400 mt-0.5">
                멤버 카드를 누르면 개인별 습관 캘린더와 인증 내역을 확인할 수 있어요.
              </p>
            </div>

            {/* Habit Register button */}
            <button
              onClick={() => setShowHabitModal(true)}
              className="flex items-center gap-1 text-xs font-bold text-emerald-600 hover:text-emerald-700 bg-emerald-50 hover:bg-emerald-100 px-3 py-1.5 rounded-xl transition-colors cursor-pointer"
            >
              <PlusCircle className="w-3.5 h-3.5" />
              <span>{myMember?.habit ? '내 습관 수정' : '내 습관 등록'}</span>
            </button>
          </div>

          {/* Member List */}
          <div className="space-y-3">
            {room.members.map((member) => {
              const user = users.find((u) => u.id === member.userId);
              if (!user) return null;
              const isCurrentUser = user.id === currentUser?.id;
              const isMemberHost = member.role === 'host' || member.userId === room.hostId;

              return (
                <div
                  key={member.userId}
                  onClick={() => handleMemberClick(member.userId)}
                  className={`w-full p-4 rounded-2xl border transition-all flex items-center justify-between cursor-pointer group hover:shadow-md ${
                    isCurrentUser
                      ? 'border-emerald-300 bg-emerald-50/30 hover:border-emerald-400'
                      : 'border-slate-200/90 bg-white hover:border-slate-300'
                  }`}
                >
                  <div className="flex items-center gap-3.5 min-w-0">
                    <div className="relative shrink-0">
                      <img
                        src={user.avatarUrl}
                        alt={user.name}
                        className="w-11 h-11 rounded-full object-cover border border-slate-200"
                      />
                      {isMemberHost && (
                        <div
                          className="absolute -bottom-1 -right-1 bg-amber-400 text-white p-0.8 rounded-full shadow-2xs"
                          title="방장"
                        >
                          <Crown className="w-3 h-3 fill-white" />
                        </div>
                      )}
                    </div>

                    <div className="min-w-0">
                      <div className="flex items-center gap-1.5">
                        <span className="text-sm font-bold text-slate-900 group-hover:text-emerald-600 transition-colors">
                          {user.name}
                        </span>
                        {isCurrentUser && (
                          <span className="text-[10px] font-bold px-1.5 py-0.2 bg-emerald-100 text-emerald-700 rounded-md">
                            나
                          </span>
                        )}
                        {isMemberHost && (
                          <span className="text-[10px] font-bold px-1.5 py-0.2 bg-amber-100 text-amber-700 rounded-md">
                            방장
                          </span>
                        )}
                      </div>
                      <p className="text-[11px] text-slate-400">@{user.username}</p>
                    </div>
                  </div>

                  {/* Habit info & Penalty tag */}
                  <div className="flex items-center gap-3 shrink-0">
                    <div className="text-right">
                      <div className="flex items-center gap-1.5 justify-end">
                        <span className="text-xs sm:text-sm font-bold text-slate-800 group-hover:text-emerald-600 transition-colors">
                          {member.habit?.title || '습관 등록 중'}
                        </span>
                      </div>
                      <div className="text-[11px] text-slate-400 mt-0.5 flex items-center justify-end gap-1.5">
                        <span>실천율: 주 {member.habit?.weeklyTargetDays || 7}회</span>
                        <span className="text-rose-500 font-semibold">· 벌칙 {member.penaltyCount}회</span>
                      </div>
                    </div>

                    <div className="w-8 h-8 rounded-full bg-slate-100 group-hover:bg-emerald-100 group-hover:text-emerald-700 text-slate-400 flex items-center justify-center transition-colors">
                      <ChevronRight className="w-4 h-4" />
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* Modals */}
      <RoomSettingModal
        isOpen={showSettingModal}
        onClose={() => setShowSettingModal(false)}
        room={room}
      />
      <HabitRegisterModal
        isOpen={showHabitModal}
        onClose={() => setShowHabitModal(false)}
        roomId={room.id}
        initialTitle={myMember?.habit?.title}
      />
    </div>
  );
};
