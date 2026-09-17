'use client';

import React, { useState } from 'react';
import { useApp } from '../context/AppContext';
import { User, ShieldAlert, Check, ArrowLeft, Crown } from 'lucide-react';
import { DeleteAccountModal } from '../components/Modals';

export const MyPage: React.FC = () => {
  const { currentUser, updateProfile, deleteAccount, setCurrentPage } = useApp();
  const [name, setName] = useState(currentUser?.name || '');
  const [newPassword, setNewPassword] = useState('');
  const [showDeleteModal, setShowDeleteModal] = useState(false);

  if (!currentUser) {
    return (
      <div className="text-center py-20">
        <p className="text-slate-500 mb-4">로그인이 필요한 서비스입니다.</p>
        <button
          onClick={() => setCurrentPage('login')}
          className="px-6 py-2.5 bg-emerald-500 text-white rounded-xl font-bold text-sm"
        >
          로그인하러 가기
        </button>
      </div>
    );
  }

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    updateProfile(name);
  };

  return (
    <div className="max-w-2xl mx-auto px-4 py-8">
      {/* Top breadcrumb */}
      <div className="flex items-center justify-between mb-6">
        <button
          onClick={() => setCurrentPage('main')}
          className="flex items-center gap-1.5 text-xs font-bold text-slate-500 hover:text-slate-900 transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>방 목록으로</span>
        </button>
        <span className="text-xs font-semibold px-2.5 py-1 rounded-full bg-slate-100 text-slate-600">
          마이페이지
        </span>
      </div>

      <div className="bg-white rounded-3xl shadow-sm border border-slate-100 p-6 md:p-8">
        <div className="flex flex-col sm:flex-row items-center gap-5 pb-6 border-b border-slate-100">
          <div className="relative">
            <img
              src={currentUser.avatarUrl}
              alt={currentUser.name}
              className="w-20 h-20 rounded-full object-cover border-2 border-emerald-400 shadow-md ring-4 ring-emerald-50"
            />
            {currentUser.id === 'user-1' && (
              <div className="absolute -bottom-1 -right-1 bg-amber-400 p-1.5 rounded-full text-white shadow-xs" title="방장">
                <Crown className="w-3.5 h-3.5" />
              </div>
            )}
          </div>
          <div className="text-center sm:text-left">
            <h1 className="text-xl font-extrabold text-slate-900 flex items-center justify-center sm:justify-start gap-2">
              <span>{currentUser.name}</span>
              <span className="text-xs font-bold px-2 py-0.5 rounded-md bg-emerald-100 text-emerald-700">
                활동중
              </span>
            </h1>
            <p className="text-xs text-slate-400 mt-0.5">@{currentUser.username}</p>
            <div className="flex items-center gap-2 mt-2">
              <span className="text-xs font-semibold px-2.5 py-1 rounded-lg bg-rose-50 text-rose-600 border border-rose-100">
                누적 벌칙 {currentUser.penaltyCount}회 수행
              </span>
            </div>
          </div>
        </div>

        <form onSubmit={handleSave} className="mt-6 space-y-4">
          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1">아이디</label>
            <input
              type="text"
              disabled
              value={currentUser.username}
              className="w-full px-4 py-2.5 rounded-xl border border-slate-200 bg-slate-50 text-slate-400 text-sm cursor-not-allowed"
            />
            <p className="text-[11px] text-slate-400 mt-1">아이디는 변경할 수 없습니다.</p>
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1">닉네임</label>
            <input
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full px-4 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1">새 비밀번호 (선택)</label>
            <input
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              placeholder="변경할 새 비밀번호를 입력하세요"
              className="w-full px-4 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
            />
          </div>

          <div className="flex items-center justify-between pt-6 border-t border-slate-100">
            <button
              type="button"
              onClick={() => setShowDeleteModal(true)}
              className="px-4 py-2.5 rounded-xl text-xs font-bold text-rose-600 bg-rose-50 hover:bg-rose-100 transition-colors"
            >
              회원탈퇴
            </button>
            <button
              type="submit"
              className="px-6 py-2.5 rounded-xl text-sm font-bold text-white bg-emerald-500 hover:bg-emerald-600 shadow-md shadow-emerald-500/20 transition-all hover:scale-102 cursor-pointer"
            >
              변경사항 저장
            </button>
          </div>
        </form>
      </div>

      {/* 회원탈퇴 모달 */}
      <DeleteAccountModal
        isOpen={showDeleteModal}
        onClose={() => setShowDeleteModal(false)}
        onConfirm={deleteAccount}
      />
    </div>
  );
};
