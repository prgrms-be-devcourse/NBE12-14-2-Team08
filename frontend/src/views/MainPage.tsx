'use client';

import React, { useState } from 'react';
import { useApp } from '../context/AppContext';
import { Plus, Users, Calendar, Trophy, ChevronRight, Crown, Flame, Clock } from 'lucide-react';
import { CreateRoomModal } from '../components/Modals';

export const MainPage: React.FC = () => {
  const { rooms, currentUser, setCurrentRoomId, setCurrentPage } = useApp();
  const [showCreateModal, setShowCreateModal] = useState(false);

  // Filter rooms user has joined vs available
  const myRooms = rooms.filter((r) =>
    r.members.some((m) => m.userId === currentUser?.id)
  );

  const otherRooms = rooms.filter(
    (r) => !r.members.some((m) => m.userId === currentUser?.id)
  );

  const handleRoomClick = (roomId: string) => {
    setCurrentRoomId(roomId);
    setCurrentPage('room');
  };

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 py-8">
      {/* Top Banner & Title */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8 bg-gradient-to-r from-emerald-50 via-teal-50 to-cyan-50 p-6 sm:p-8 rounded-3xl border border-emerald-100/70 shadow-xs">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="text-2xl">🌱</span>
            <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
              내 챌린지 방
            </h1>
          </div>
          <p className="text-sm font-medium text-slate-600 mt-1">
            친구들과 함께하는 습관 챌린지에 참여하고, 실패 시 유쾌한 벌칙을 즐겨보세요!
          </p>
        </div>

        <button
          onClick={() => setShowCreateModal(true)}
          className="flex items-center justify-center gap-2 px-5 py-3 rounded-2xl font-extrabold text-sm text-white bg-emerald-500 hover:bg-emerald-600 shadow-lg shadow-emerald-500/25 transition-all hover:scale-103 shrink-0 cursor-pointer"
        >
          <Plus className="w-5 h-5" />
          <span>새 방 만들기</span>
        </button>
      </div>

      {/* 1. 참여 중인 챌린지 방 */}
      <section className="mb-10">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-base font-bold text-slate-800 flex items-center gap-2">
            <span>참여 중인 방</span>
            <span className="text-xs px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 font-bold">
              {myRooms.length}
            </span>
          </h2>
        </div>

        {myRooms.length === 0 ? (
          <div className="text-center py-12 bg-white rounded-3xl border border-dashed border-slate-200">
            <p className="text-slate-400 text-sm font-medium">참여 중인 챌린지 방이 없습니다.</p>
            <button
              onClick={() => setShowCreateModal(true)}
              className="mt-3 text-xs font-bold text-emerald-600 hover:underline"
            >
              새로운 방을 직접 만들어보세요!
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            {myRooms.map((room) => {
              const isHost = room.hostId === currentUser?.id;
              const myMember = room.members.find((m) => m.userId === currentUser?.id);
              const totalMembers = room.members.length;
              const fillPercent = Math.round((totalMembers / room.maxMembers) * 100);

              return (
                <div
                  key={room.id}
                  onClick={() => handleRoomClick(room.id)}
                  className="bg-white rounded-3xl p-6 border border-slate-200/80 hover:border-emerald-400 hover:shadow-xl hover:shadow-emerald-500/5 transition-all cursor-pointer group flex flex-col justify-between"
                >
                  <div>
                    {/* Header tags */}
                    <div className="flex items-center justify-between gap-2 mb-3">
                      <div className="flex items-center gap-1.5 flex-wrap">
                        {isHost ? (
                          <span className="flex items-center gap-1 text-[11px] font-bold px-2.5 py-0.8 rounded-full bg-amber-100 text-amber-800">
                            <Crown className="w-3 h-3 text-amber-600" />
                            방장
                          </span>
                        ) : (
                          <span className="text-[11px] font-bold px-2.5 py-0.8 rounded-full bg-slate-100 text-slate-600">
                            참여자
                          </span>
                        )}
                        <span className="text-[11px] font-bold px-2.5 py-0.8 rounded-full bg-rose-50 text-rose-600 border border-rose-100">
                          벌칙: {room.penaltyText}
                        </span>
                      </div>
                      <span className="text-xs font-semibold text-slate-400 flex items-center gap-1">
                        <Clock className="w-3.5 h-3.5" />
                        ~ {room.deadline}
                      </span>
                    </div>

                    {/* Title & Desc */}
                    <h3 className="text-lg font-extrabold text-slate-900 group-hover:text-emerald-600 transition-colors">
                      {room.title}
                    </h3>
                    <p className="text-xs text-slate-500 mt-1.5 line-clamp-2 leading-relaxed">
                      {room.description || '친구들과 함께 목표를 달성해보세요!'}
                    </p>

                    {/* Member's current habit if set */}
                    {myMember?.habit && (
                      <div className="mt-3.5 p-3 rounded-2xl bg-emerald-50/70 border border-emerald-100/80 flex items-center justify-between">
                        <div className="min-w-0">
                          <span className="text-[10px] font-bold text-emerald-700 uppercase tracking-wide">
                            내 목표 습관
                          </span>
                          <p className="text-xs font-bold text-slate-800 truncate mt-0.5">
                            {myMember.habit.title}
                          </p>
                        </div>
                        <span className="text-[11px] font-semibold text-emerald-600 shrink-0">
                          주 {myMember.habit.weeklyTargetDays}회
                        </span>
                      </div>
                    )}
                  </div>

                  {/* Footer Stats */}
                  <div className="mt-5 pt-4 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
                    <div className="flex items-center gap-1.5">
                      <Users className="w-4 h-4 text-slate-400" />
                      <span className="font-bold text-slate-700">
                        {totalMembers} / {room.maxMembers}명
                      </span>
                    </div>

                    <div className="flex items-center gap-1 font-bold text-emerald-600 group-hover:translate-x-1 transition-transform">
                      <span>방 입장하기</span>
                      <ChevronRight className="w-4 h-4" />
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </section>

      {/* 2. 참여 가능한 다른 방 (오픈 챌린지) */}
      {otherRooms.length > 0 && (
        <section>
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-base font-bold text-slate-800 flex items-center gap-2">
              <span>새로운 챌린지 찾아보기</span>
              <span className="text-xs px-2 py-0.5 rounded-full bg-slate-200 text-slate-700 font-bold">
                {otherRooms.length}
              </span>
            </h2>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            {otherRooms.map((room) => (
              <div
                key={room.id}
                onClick={() => handleRoomClick(room.id)}
                className="bg-white rounded-3xl p-6 border border-slate-200/80 hover:border-slate-300 hover:shadow-md transition-all cursor-pointer group"
              >
                <div className="flex items-center justify-between gap-2 mb-2">
                  <span className="text-[11px] font-bold px-2.5 py-0.8 rounded-full bg-rose-50 text-rose-600 border border-rose-100">
                    벌칙: {room.penaltyText}
                  </span>
                  <span className="text-xs font-semibold text-slate-400">~ {room.deadline}</span>
                </div>
                <h3 className="text-base font-bold text-slate-800 group-hover:text-emerald-600 transition-colors">
                  {room.title}
                </h3>
                <p className="text-xs text-slate-500 mt-1 line-clamp-2">{room.description}</p>
                <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
                  <span className="font-semibold">
                    현재 {room.members.length} / {room.maxMembers}명
                  </span>
                  <span className="text-xs font-bold text-slate-600 group-hover:text-emerald-600 flex items-center">
                    상세보기 <ChevronRight className="w-3.5 h-3.5" />
                  </span>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* 새 챌린지 방 만들기 모달 */}
      <CreateRoomModal isOpen={showCreateModal} onClose={() => setShowCreateModal(false)} />
    </div>
  );
};
