'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useMember } from '../context/MemberContext';
import { DeleteAccountModal } from '../components/DeleteAccountModal';
import { MemberNavigation } from '../components/MemberNavigation';
import { ProfileAvatar } from '../components/ProfileAvatar';

export const MyPage: React.FC = () => {
  const { currentUser, authReady, updateProfile, logout, deleteAccount } = useMember();
  const router = useRouter();
  const [nameEdit, setNameEdit] = useState<{ userId: string; value: string } | null>(null);
  const name = nameEdit && nameEdit.userId === currentUser?.id ? nameEdit.value : currentUser?.name || '';
  const [newPassword, setNewPassword] = useState('');
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [error, setError] = useState('');
  const [isSaving, setIsSaving] = useState(false);


  if (!authReady) {
    return <div className="px-4 py-20 text-center text-sm text-slate-500">내 정보를 불러오는 중...</div>;
  }

  if (!currentUser) {
    return (
      <div className="px-4 py-20 text-center">
        <p className="mb-4 text-slate-500">로그인이 필요한 서비스입니다.</p>
        <button
          type="button"
          onClick={() => router.push('/login')}
          className="rounded-xl bg-emerald-500 px-6 py-2.5 text-sm font-bold text-white hover:bg-emerald-600"
        >
          로그인하러 가기
        </button>
      </div>
    );
  }

  const handleSave = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmedName = name.trim();
    if (!trimmedName) {
      setError('닉네임을 입력해주세요.');
      return;
    }
    setError('');
    setIsSaving(true);
    try {
      await updateProfile(trimmedName, newPassword || undefined);
      setNewPassword('');
      setNameEdit(null);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : '프로필을 저장하지 못했습니다.');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="mx-auto max-w-5xl px-4 py-8 sm:py-12">
      <section className="rounded-3xl border border-slate-100 bg-white p-6 shadow-xl shadow-slate-200/60 sm:p-10">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="text-xs font-bold text-emerald-600">내기? 내기!</p>
            <h1 className="mt-2 text-2xl font-extrabold tracking-tight text-slate-900">마이페이지</h1>
          </div>
          <div className="flex flex-col items-end gap-3">
            <button
              type="button"
              onClick={logout}
              className="rounded-xl bg-slate-100 px-4 py-2 text-sm font-bold text-slate-600 transition-colors hover:bg-slate-200"
            >
              로그아웃
            </button>
            <MemberNavigation embedded />
          </div>
        </div>

        <div className="mt-8 flex items-center gap-4">
          <ProfileAvatar nickname={currentUser.name} />
          <div>
            <p className="text-lg font-extrabold text-slate-900">{currentUser.name}</p>
            <p className="mt-0.5 text-sm text-slate-400">@{currentUser.username}</p>
          </div>
        </div>

        <form onSubmit={handleSave} className="mt-8">
          <div className="max-w-md space-y-5">
            <div>
              <label htmlFor="mypage-username" className="mb-1.5 block text-sm font-bold text-slate-700">아이디</label>
              <input
                id="mypage-username"
                type="text"
                readOnly
                value={currentUser.username}
                className="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-2.5 text-sm text-slate-500"
              />
              <p className="mt-1.5 text-xs text-slate-400">아이디는 변경할 수 없습니다.</p>
            </div>
            <div>
              <label htmlFor="mypage-name" className="mb-1.5 block text-sm font-bold text-slate-700">닉네임</label>
              <input
                id="mypage-name"
                type="text"
                required
                value={name}
                onChange={(event) => {
                  setNameEdit({ userId: currentUser.id, value: event.target.value });
                  setError('');
                }}
                className="w-full rounded-xl border border-slate-200 px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
              />
            </div>
            <div>
              <label htmlFor="mypage-password" className="mb-1.5 block text-sm font-bold text-slate-700">새 비밀번호 (선택)</label>
              <input
                id="mypage-password"
                type="password"
                autoComplete="new-password"
                value={newPassword}
                onChange={(event) => setNewPassword(event.target.value)}
                placeholder="변경할 새 비밀번호를 입력하세요"
                className="w-full rounded-xl border border-slate-200 px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
              />
            </div>
            {error && <p role="alert" className="text-sm font-semibold text-rose-600">{error}</p>}
          </div>

          <div className="mt-10 flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 pt-6">
            <button
              type="button"
              onClick={() => setShowDeleteModal(true)}
              className="rounded-xl bg-rose-50 px-4 py-2.5 text-sm font-bold text-rose-600 transition-colors hover:bg-rose-100"
            >
              회원 탈퇴
            </button>
            <button
              type="submit"
              disabled={isSaving}
              className="rounded-xl bg-emerald-500 px-6 py-2.5 text-sm font-bold text-white shadow-md shadow-emerald-500/20 transition-colors hover:bg-emerald-600 disabled:cursor-wait disabled:opacity-60"
            >
              {isSaving ? '저장 중...' : '변경사항 저장'}
            </button>
          </div>
        </form>
      </section>

      <DeleteAccountModal
        isOpen={showDeleteModal}
        onClose={() => setShowDeleteModal(false)}
        onConfirm={deleteAccount}
      />
    </div>
  );
};
