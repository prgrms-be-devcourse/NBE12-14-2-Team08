'use client';

import { usePathname, useRouter } from 'next/navigation';
import { useMember } from '../context/MemberContext';

export function MemberNavigation({ embedded = false }: { embedded?: boolean }) {
  const pathname = usePathname();
  const router = useRouter();
  const { currentUser, authReady } = useMember();

  if (!authReady || !currentUser || pathname === '/login' || pathname === '/signup') {
    return null;
  }

  const isMainPage = pathname === '/main';
  const isMyPage = pathname === '/mypage';

  return (
    <nav className={embedded ? 'flex justify-end' : 'flex justify-end px-4 pt-4'} aria-label="회원 페이지 이동">
      <div className="inline-flex overflow-hidden rounded-lg border border-slate-900 bg-white">
        <button
          type="button"
          onClick={() => router.push('/main')}
          aria-current={isMainPage ? 'page' : undefined}
          className={`min-w-24 px-4 py-2 text-sm font-semibold text-slate-900 ${
            isMainPage ? 'bg-slate-200' : 'bg-white hover:bg-slate-50'
          }`}
        >
          방 목록
        </button>
        <button
          type="button"
          onClick={() => router.push('/mypage')}
          aria-current={isMyPage ? 'page' : undefined}
          className={`min-w-24 border-l border-slate-900 px-4 py-2 text-sm font-semibold text-slate-900 ${
            isMyPage ? 'bg-slate-200' : 'bg-white hover:bg-slate-50'
          }`}
        >
          마이페이지
        </button>
      </div>
    </nav>
  );
}
