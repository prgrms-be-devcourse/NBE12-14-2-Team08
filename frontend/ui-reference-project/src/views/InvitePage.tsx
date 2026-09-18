'use client';

import React, { useState } from 'react';
import { useApp } from '../context/AppContext';
import { ArrowLeft, Users, Calendar, Coffee, Sparkles, CheckCircle2 } from 'lucide-react';
import { RoomPasswordModal } from '../components/Modals';

export const InvitePage: React.FC = () => {
  const { currentRoomId, rooms, currentUser, joinRoom, setCurrentPage } = useApp();
  const [showPasswordModal, setShowPasswordModal] = useState(false);
  const [myHabit, setMyHabit] = useState('아침 6시 기상 챌린지 동참');

  const room = rooms.find((r) => r.id === currentRoomId) || rooms[0];

  const handleJoinClick = () => {
    if (room.password) {
      setShowPasswordModal(true);
    } else {
      joinRoom(room.id, myHabit);
    }
  };

  const handlePasswordSuccess = () => {
    joinRoom(room.id, myHabit);
  };

  const isAlreadyMember = room.members.some((m) => m.userId === currentUser?.id);

  return (
    <div className="min-h-[85vh] flex items-center justify-center px-4 py-10">
      <div className="w-full max-w-md bg-white rounded-3xl shadow-xl border border-slate-100 p-8 text-center relative overflow-hidden">
        {/* Decorative background glow */}
        <div className="absolute -top-12 -right-12 w-32 h-32 bg-emerald-100 rounded-full blur-2xl opacity-60 pointer-events-none" />
        <div className="absolute -bottom-12 -left-12 w-32 h-32 bg-teal-100 rounded-full blur-2xl opacity-60 pointer-events-none" />

        <button
          onClick={() => setCurrentPage('main')}
          className="absolute top-6 left-6 text-slate-400 hover:text-slate-600 transition-colors"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>

        <div className="w-16 h-16 rounded-3xl bg-emerald-100 text-emerald-600 flex items-center justify-center text-3xl mx-auto mb-4 shadow-sm">
          💌
        </div>

        <p className="text-xs font-bold text-emerald-600 uppercase tracking-wider mb-1">
          챌린지 방에 초대 받으셨어요!
        </p>

        <h1 className="text-2xl font-black text-slate-900 tracking-tight mb-2">
          {room.title}
        </h1>

        <p className="text-xs text-slate-500 mb-6 leading-relaxed">
          {room.description || '친구들과 함께 목표를 향해 도전해보세요!'}
        </p>

        {/* Room Info Box */}
        <div className="bg-slate-50 rounded-2xl p-4 border border-slate-100 mb-6 text-left space-y-2.5">
          <div className="flex items-center justify-between text-xs">
            <span className="text-slate-400 font-medium flex items-center gap-1.5">
              <Users className="w-4 h-4 text-slate-400" />
              참여 인원
            </span>
            <span className="font-bold text-slate-800">
              {room.members.length} / {room.maxMembers}명
            </span>
          </div>

          <div className="flex items-center justify-between text-xs">
            <span className="text-slate-400 font-medium flex items-center gap-1.5">
              <Calendar className="w-4 h-4 text-slate-400" />
              챌린지 기간
            </span>
            <span className="font-bold text-slate-800">
              ~ {room.deadline}까지
            </span>
          </div>

          <div className="flex items-center justify-between text-xs">
            <span className="text-slate-400 font-medium flex items-center gap-1.5">
              <Coffee className="w-4 h-4 text-rose-500" />
              실패 시 벌칙
            </span>
            <span className="font-black text-rose-600">
              {room.penaltyText}
            </span>
          </div>
        </div>

        {/* Habit prompt if joining */}
        {!isAlreadyMember && (
          <div className="mb-6 text-left">
            <label className="block text-xs font-bold text-slate-700 mb-1">
              이 방에서 실천할 내 습관
            </label>
            <input
              type="text"
              value={myHabit}
              onChange={(e) => setMyHabit(e.target.value)}
              placeholder="예: 기상 후 물 한 잔 마시기"
              className="w-full px-4 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
            />
          </div>
        )}

        <button
          onClick={handleJoinClick}
          className="w-full py-3.5 rounded-2xl font-extrabold text-sm text-white bg-emerald-500 hover:bg-emerald-600 shadow-lg shadow-emerald-500/25 transition-all hover:scale-102 cursor-pointer"
        >
          {isAlreadyMember ? '챌린지 방 바로가기' : '이 챌린지 방 참여하기'}
        </button>

        <p className="text-[11px] text-slate-400 mt-3">
          {room.password ? '🔒 참여를 위해 비밀번호 입력이 필요합니다.' : '누구나 자유롭게 참여할 수 있는 공개 방입니다.'}
        </p>

        <RoomPasswordModal
          isOpen={showPasswordModal}
          onClose={() => setShowPasswordModal(false)}
          room={room}
          onSuccess={handlePasswordSuccess}
        />
      </div>
    </div>
  );
};
