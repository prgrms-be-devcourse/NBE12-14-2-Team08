'use client';

import React, { useState } from 'react';
import { useApp } from '../context/AppContext';
import { Sparkles, Crown, User as UserIcon, List, LogOut, RefreshCw, CheckSquare } from 'lucide-react';

export const Header: React.FC = () => {
  const {
    currentUser,
    users,
    switchUser,
    currentPage,
    setCurrentPage,
    setCurrentRoomId,
    currentRoomId,
    rooms,
    resetAllData,
  } = useApp();

  const [showUserDropdown, setShowUserDropdown] = useState(false);

  const currentRoom = rooms.find((r) => r.id === currentRoomId);
  const isHost = currentRoom && currentUser && currentRoom.hostId === currentUser.id;

  return (
    <header className="sticky top-0 z-40 bg-white/95 backdrop-blur border-b border-slate-200/80 shadow-xs">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between">
        {/* Logo */}
        <div
          onClick={() => setCurrentPage('main')}
          className="flex items-center gap-2.5 cursor-pointer group select-none"
        >
          <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-emerald-500 to-teal-400 flex items-center justify-center text-white font-extrabold text-xl shadow-md shadow-emerald-500/20 group-hover:scale-105 transition-transform">
            🎯
          </div>
          <div>
            <div className="flex items-center gap-1.5">
              <span className="text-xl font-black tracking-tight text-slate-900 group-hover:text-emerald-600 transition-colors">
                내기? 내기!
              </span>
              <span className="text-[10px] font-bold px-1.5 py-0.5 rounded-full bg-emerald-100 text-emerald-700">
                습관 챌린지
              </span>
            </div>
            <p className="text-[11px] text-slate-400 font-medium">실천하면 습관! 실패하면 내기 한판!</p>
          </div>
        </div>

        {/* Navigation Buttons */}
        <div className="flex items-center gap-2 sm:gap-3">
          {currentUser ? (
            <>
              {/* Room List Button */}
              <button
                onClick={() => setCurrentPage('main')}
                className={`flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-sm font-semibold transition-all ${
                  currentPage === 'main'
                    ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                    : 'text-slate-600 hover:bg-slate-100'
                }`}
              >
                <List className="w-4 h-4" />
                <span>방 목록</span>
              </button>

              {/* Admin Button (Visible if host or in room) */}
              {isHost && (
                <button
                  onClick={() => setCurrentPage('admin')}
                  className={`flex items-center gap-1.5 px-3 py-2 rounded-xl text-sm font-semibold transition-all border ${
                    currentPage === 'admin'
                      ? 'bg-amber-500 text-white border-amber-600 shadow-sm'
                      : 'border-amber-300 bg-amber-50/80 text-amber-800 hover:bg-amber-100'
                  }`}
                  title="방장 전용 인증 관리"
                >
                  <Crown className="w-4 h-4 text-amber-500 fill-amber-400" />
                  <span className="hidden sm:inline">미인증 관리</span>
                </button>
              )}

              {/* My Page Button */}
              <button
                onClick={() => setCurrentPage('mypage')}
                className={`flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-sm font-semibold transition-all ${
                  currentPage === 'mypage'
                    ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                    : 'text-slate-600 hover:bg-slate-100'
                }`}
              >
                <UserIcon className="w-4 h-4" />
                <span className="hidden sm:inline">마이페이지</span>
              </button>

              {/* User Switcher Dropdown (for easy testing between Host/Members) */}
              <div className="relative">
                <button
                  onClick={() => setShowUserDropdown(!showUserDropdown)}
                  className="flex items-center gap-2 pl-2 pr-3 py-1.5 rounded-full border border-slate-200 hover:border-emerald-300 bg-slate-50 hover:bg-white transition-all shadow-xs"
                >
                  <img
                    src={currentUser.avatarUrl}
                    alt={currentUser.name}
                    className="w-7 h-7 rounded-full object-cover border border-emerald-300 ring-2 ring-emerald-100"
                  />
                  <div className="text-left leading-tight hidden md:block">
                    <div className="text-xs font-bold text-slate-800 flex items-center gap-1">
                      {currentUser.name}
                      {isHost && <Crown className="w-3 h-3 text-amber-500 fill-amber-400 inline" />}
                    </div>
                    <div className="text-[10px] text-slate-400">@{currentUser.username}</div>
                  </div>
                  <span className="text-xs text-slate-400">▼</span>
                </button>

                {showUserDropdown && (
                  <div className="absolute right-0 mt-2 w-56 bg-white rounded-2xl shadow-xl border border-slate-100 py-2 z-50 animate-in fade-in slide-in-from-top-2">
                    <div className="px-3 py-1.5 border-b border-slate-100">
                      <p className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                        사용자 변경 (테스트 체험)
                      </p>
                    </div>
                    <div className="max-h-56 overflow-y-auto py-1">
                      {users.map((u) => (
                        <button
                          key={u.id}
                          onClick={() => {
                            switchUser(u.id);
                            setShowUserDropdown(false);
                          }}
                          className={`w-full px-3 py-2 flex items-center gap-2.5 text-left hover:bg-slate-50 transition-colors ${
                            u.id === currentUser.id ? 'bg-emerald-50/70 text-emerald-900 font-semibold' : 'text-slate-700'
                          }`}
                        >
                          <img
                            src={u.avatarUrl}
                            alt={u.name}
                            className="w-7 h-7 rounded-full object-cover border"
                          />
                          <div className="flex-1 min-w-0">
                            <p className="text-xs font-bold truncate flex items-center gap-1">
                              {u.name}
                              {u.id === 'user-1' && (
                                <span className="text-[10px] px-1.5 py-0.2 bg-amber-100 text-amber-700 rounded font-normal">
                                  방장
                                </span>
                              )}
                            </p>
                            <p className="text-[10px] text-slate-400">@{u.username} · 벌칙 {u.penaltyCount}회</p>
                          </div>
                        </button>
                      ))}
                    </div>
                    <div className="border-t border-slate-100 pt-1 mt-1">
                      <button
                        onClick={() => {
                          resetAllData();
                          setShowUserDropdown(false);
                        }}
                        className="w-full px-3 py-2 text-xs text-slate-500 hover:text-rose-600 flex items-center gap-2 hover:bg-rose-50 transition-colors"
                      >
                        <RefreshCw className="w-3.5 h-3.5" />
                        <span>데이터 초기화 (Reset)</span>
                      </button>
                      <button
                        onClick={() => {
                          setCurrentPage('login');
                          setShowUserDropdown(false);
                        }}
                        className="w-full px-3 py-2 text-xs text-slate-500 hover:text-slate-800 flex items-center gap-2 hover:bg-slate-50 transition-colors"
                      >
                        <LogOut className="w-3.5 h-3.5" />
                        <span>로그아웃</span>
                      </button>
                    </div>
                  </div>
                )}
              </div>
            </>
          ) : (
            <div className="flex items-center gap-2">
              <button
                onClick={() => setCurrentPage('login')}
                className="px-4 py-2 rounded-xl text-sm font-semibold text-slate-600 hover:bg-slate-100"
              >
                로그인
              </button>
              <button
                onClick={() => setCurrentPage('signup')}
                className="px-4 py-2 rounded-xl text-sm font-bold text-white bg-emerald-500 hover:bg-emerald-600 shadow-sm"
              >
                회원가입
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};
