'use client';

import { useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { RoomPasswordModal } from '../components/RoomPasswordModal';
import { useMember } from '../context/MemberContext';
import { groupApi } from '../lib/groupApi';

const INVITE_PREVIEW = {
  title: '초대받은 방',
  description: '친구들과 함께 목표를 향해 도전해보세요!',
  members: '- / -명',
  deadline: '방 참여 후 확인할 수 있어요',
  penalty: '방 참여 후 확인할 수 있어요',
};

export function InvitePage() {
  const { invitecode } = useParams<{ invitecode: string }>();
  const router = useRouter();
  const { currentUser, authReady } = useMember();
  const [showPasswordModal, setShowPasswordModal] = useState(false);

  const handleJoinClick = () => {
    if (!authReady) return;
    if (!currentUser) {
      const invitePath = `/invite/${encodeURIComponent(invitecode)}`;
      router.push(`/login?redirect=${encodeURIComponent(invitePath)}`);
      return;
    }
    setShowPasswordModal(true);
  };

  return (
    <main className="flex min-h-[85vh] items-center justify-center px-4 py-10">
      <section className="relative w-full max-w-md overflow-hidden rounded-3xl border border-slate-100 bg-white p-8 text-center shadow-xl">
        <div aria-hidden="true" className="pointer-events-none absolute -right-12 -top-12 h-32 w-32 rounded-full bg-emerald-100 opacity-60 blur-2xl" />
        <div aria-hidden="true" className="pointer-events-none absolute -bottom-12 -left-12 h-32 w-32 rounded-full bg-teal-100 opacity-60 blur-2xl" />

        <button type="button" onClick={() => router.push('/main')} aria-label="방 목록으로 돌아가기"
          className="absolute left-6 top-6 text-xl text-slate-400 transition-colors hover:text-slate-600">
          ←
        </button>

        <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-3xl bg-emerald-100 text-3xl text-emerald-600 shadow-sm">💌</div>
        <p className="mb-1 text-xs font-bold uppercase tracking-wider text-emerald-600">방에 초대받으셨어요!</p>
        <h1 className="mb-2 text-2xl font-black tracking-tight text-slate-900">{INVITE_PREVIEW.title}</h1>
        <p className="mb-6 text-xs leading-relaxed text-slate-500">{INVITE_PREVIEW.description}</p>

        <div className="mb-6 space-y-2.5 rounded-2xl border border-slate-100 bg-slate-50 p-4 text-left">
          <InfoRow icon="👥" label="참여 인원" value={INVITE_PREVIEW.members} />
          <InfoRow icon="📅" label="기간" value={INVITE_PREVIEW.deadline} />
          <InfoRow icon="☕" label="실패 시 벌칙" value={INVITE_PREVIEW.penalty} emphasis />
        </div>

        <button type="button" onClick={handleJoinClick} disabled={!authReady || !invitecode}
          className="w-full cursor-pointer rounded-2xl bg-emerald-500 py-3.5 text-sm font-extrabold text-white shadow-lg shadow-emerald-500/25 transition-all hover:bg-emerald-600 disabled:cursor-not-allowed disabled:bg-slate-300 disabled:shadow-none">
          {authReady ? '이 방 참여하기' : '로그인 상태 확인 중...'}
        </button>
        <p className="mt-3 text-[11px] text-slate-400">🔒 참여를 위해 비밀번호 입력이 필요합니다.</p>

        <RoomPasswordModal
          isOpen={showPasswordModal}
          onClose={() => setShowPasswordModal(false)}
          roomTitle={INVITE_PREVIEW.title}
          roomDescription={INVITE_PREVIEW.description}
          onSubmit={async (password) => {
            await groupApi.join(invitecode, password);
            router.push('/main');
          }}
        />
      </section>
    </main>
  );
}

function InfoRow({ icon, label, value, emphasis = false }: { icon: string; label: string; value: string; emphasis?: boolean }) {
  return (
    <div className="flex items-center justify-between gap-3 text-xs">
      <span className="flex items-center gap-1.5 font-medium text-slate-400"><span aria-hidden="true">{icon}</span>{label}</span>
      <span className={`text-right font-bold ${emphasis ? 'text-rose-600' : 'text-slate-800'}`}>{value}</span>
    </div>
  );
}
