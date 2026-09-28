'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { ProfileAvatar } from '../components/ProfileAvatar';
import { useMember } from '../context/MemberContext';

interface SettlementEntryPageProps {
  groupId: string;
}

const personalStats = [
  { icon: '✅', label: '인증 승인', value: '18회', tone: 'emerald' },
  { icon: '📸', label: '전체 인증', value: '21회', tone: 'sky' },
  { icon: '↩️', label: '인증 거절', value: '3회', tone: 'rose' },
  { icon: '☕', label: '벌칙', value: '2회', tone: 'amber' },
] as const;

const comparisonCards = [
  {
    eyebrow: '이번 방의 수다왕',
    icon: '💬',
    title: '인증 설명을 가장 정성껏 남겼어요',
    winner: '김내기',
    value: '총 1,284자',
    detail: '인증할 때 남긴 설명의 글자 수를 모두 더했어요.',
    gradient: 'from-violet-500 to-fuchsia-500',
  },
  {
    eyebrow: '마감일의 주인공',
    icon: '⏰',
    title: '마지막 날까지 포기하지 않았어요',
    winner: '박습관',
    value: '마감일 인증 7회',
    detail: '방 생성일을 기준으로 나눈 주간 마감일의 인증 횟수예요.',
    gradient: 'from-orange-400 to-rose-500',
  },
  {
    eyebrow: '아쉬운 거절왕',
    icon: '🥲',
    title: '다음에는 더 완벽하게 인증해봐요',
    winner: '이도전',
    value: '인증 거절 5회',
    detail: '거절된 인증도 다시 도전한 소중한 기록이에요.',
    gradient: 'from-sky-500 to-blue-600',
  },
  {
    eyebrow: '느긋한 벌칙왕',
    icon: '🐢',
    title: '벌칙을 가장 오래 고민했어요',
    winner: '최꾸준',
    value: '최장 9일',
    detail: '벌칙이 생긴 날부터 제출한 날까지의 기간을 비교했어요.',
    gradient: 'from-emerald-500 to-teal-600',
  },
] as const;

const toneClasses = {
  emerald: 'bg-emerald-50 text-emerald-700 ring-emerald-100',
  sky: 'bg-sky-50 text-sky-700 ring-sky-100',
  rose: 'bg-rose-50 text-rose-700 ring-rose-100',
  amber: 'bg-amber-50 text-amber-700 ring-amber-100',
};

export function SettlementEntryPage({ groupId }: SettlementEntryPageProps) {
  const router = useRouter();
  const { currentUser } = useMember();
  const [step, setStep] = useState(0);

  const lastStep = comparisonCards.length + 2;
  const progress = ((step + 1) / (lastStep + 1)) * 100;

  const goNext = () => setStep((current) => Math.min(current + 1, lastStep));
  const goPrevious = () => setStep((current) => Math.max(current - 1, 0));

  return (
    <main className="relative min-h-screen overflow-hidden bg-slate-950 px-4 py-6 text-white sm:px-6 sm:py-10">
      <div aria-hidden="true" className="absolute -left-32 top-12 h-72 w-72 rounded-full bg-emerald-500/20 blur-3xl" />
      <div aria-hidden="true" className="absolute -right-32 bottom-10 h-80 w-80 rounded-full bg-violet-500/20 blur-3xl" />

      <div className="relative mx-auto flex min-h-[calc(100vh-3rem)] max-w-4xl flex-col">
        <header className="flex items-center justify-between gap-4">
          <button
            type="button"
            onClick={() => router.push('/main')}
            className="rounded-full border border-white/15 bg-white/5 px-4 py-2 text-xs font-bold text-slate-200 backdrop-blur transition hover:bg-white/10"
          >
            ← 방 목록
          </button>
          <div className="text-center">
            <p className="text-xs font-black tracking-[0.24em] text-emerald-300">내기? 내기!</p>
            <p className="mt-1 text-[10px] font-semibold text-slate-500">ROOM #{groupId} 결산 미리보기</p>
          </div>
          <span className="rounded-full bg-amber-300/10 px-3 py-1.5 text-[10px] font-bold text-amber-200 ring-1 ring-amber-300/20">
            UI 예시 데이터
          </span>
        </header>

        <div className="mt-6 h-1 overflow-hidden rounded-full bg-white/10">
          <div
            className="h-full rounded-full bg-gradient-to-r from-emerald-400 to-cyan-400 transition-all duration-500"
            style={{ width: `${progress}%` }}
          />
        </div>

        <section className="flex flex-1 items-center justify-center py-8 sm:py-12">
          {step === 0 && <IntroSlide nickname={currentUser?.name ?? '참가자'} onStart={goNext} />}
          {step === 1 && <PersonalSlide nickname={currentUser?.name ?? '참가자'} />}
          {step >= 2 && step < lastStep && (
            <ComparisonSlide
              card={comparisonCards[step - 2]}
              current={step - 1}
              total={comparisonCards.length}
            />
          )}
          {step === lastStep && <ClosingSlide nickname={currentUser?.name ?? '참가자'} />}
        </section>

        {step > 0 && (
          <footer className="flex items-center justify-between border-t border-white/10 pt-5">
            <button
              type="button"
              onClick={goPrevious}
              className="rounded-xl px-4 py-2.5 text-sm font-bold text-slate-400 transition hover:bg-white/5 hover:text-white"
            >
              이전
            </button>
            <div className="flex gap-1.5" aria-label="결산 진행 단계">
              {Array.from({ length: lastStep + 1 }, (_, index) => (
                <span
                  key={index}
                  className={`h-1.5 rounded-full transition-all ${index === step ? 'w-6 bg-emerald-400' : 'w-1.5 bg-white/20'}`}
                />
              ))}
            </div>
            {step < lastStep ? (
              <button
                type="button"
                onClick={goNext}
                className="rounded-xl bg-white px-5 py-2.5 text-sm font-black text-slate-950 transition hover:bg-emerald-100"
              >
                다음 →
              </button>
            ) : (
              <button
                type="button"
                onClick={() => router.push('/main')}
                className="rounded-xl bg-emerald-400 px-5 py-2.5 text-sm font-black text-slate-950 transition hover:bg-emerald-300"
              >
                방 목록으로
              </button>
            )}
          </footer>
        )}
      </div>
    </main>
  );
}

function IntroSlide({ nickname, onStart }: { nickname: string; onStart: () => void }) {
  return (
    <div className="w-full text-center">
      <div className="mx-auto flex h-24 w-24 items-center justify-center rounded-[2rem] bg-gradient-to-br from-emerald-300 to-cyan-400 text-5xl shadow-2xl shadow-emerald-500/20">
        🎉
      </div>
      <p className="mt-8 text-sm font-black tracking-[0.3em] text-emerald-300">HABIT RECAP</p>
      <h1 className="mt-4 text-4xl font-black leading-tight tracking-tight sm:text-6xl">
        함께 만든 습관의<br />마지막 장면이에요
      </h1>
      <p className="mx-auto mt-5 max-w-lg text-sm leading-7 text-slate-400 sm:text-base">
        {nickname} 님과 방 멤버들이 쌓아온 인증과 벌칙 기록을<br className="hidden sm:block" /> 재미있는 결산으로 확인해보세요.
      </p>
      <button
        type="button"
        onClick={onStart}
        className="mt-9 rounded-2xl bg-emerald-400 px-8 py-4 text-sm font-black text-slate-950 shadow-xl shadow-emerald-500/20 transition hover:-translate-y-0.5 hover:bg-emerald-300"
      >
        내 결산 시작하기 →
      </button>
    </div>
  );
}

function PersonalSlide({ nickname }: { nickname: string }) {
  return (
    <div className="w-full max-w-3xl">
      <div className="flex flex-col items-center text-center">
        <ProfileAvatar nickname={nickname} />
        <p className="mt-4 text-xs font-black tracking-[0.2em] text-emerald-300">MY RECORD</p>
        <h2 className="mt-2 text-3xl font-black sm:text-4xl">{nickname} 님의 개인 결산</h2>
        <p className="mt-2 text-sm text-slate-400">이 방에서 남긴 기록을 한눈에 모았어요.</p>
      </div>

      <div className="mt-8 grid grid-cols-2 gap-3 sm:grid-cols-4 sm:gap-4">
        {personalStats.map((stat) => (
          <article key={stat.label} className="rounded-3xl border border-white/10 bg-white/[0.06] p-4 text-center backdrop-blur sm:p-5">
            <div className={`mx-auto flex h-11 w-11 items-center justify-center rounded-2xl text-xl ring-1 ${toneClasses[stat.tone]}`}>
              {stat.icon}
            </div>
            <p className="mt-4 text-xs font-bold text-slate-400">{stat.label}</p>
            <p className="mt-1 text-2xl font-black text-white">{stat.value}</p>
          </article>
        ))}
      </div>

      <div className="mt-5 rounded-3xl border border-emerald-300/20 bg-emerald-300/10 p-5 text-center">
        <p className="text-sm font-bold text-emerald-100">총 21번의 인증으로 꾸준함을 보여줬어요!</p>
        <p className="mt-1 text-xs text-emerald-300/70">작은 반복이 멋진 습관을 만들었습니다.</p>
      </div>
    </div>
  );
}

function ComparisonSlide({
  card,
  current,
  total,
}: {
  card: (typeof comparisonCards)[number];
  current: number;
  total: number;
}) {
  return (
    <div className="w-full max-w-2xl text-center">
      <p className="text-xs font-black tracking-[0.22em] text-slate-500">GROUP RECORD {current} / {total}</p>
      <div className={`mx-auto mt-6 flex h-24 w-24 items-center justify-center rounded-[2rem] bg-gradient-to-br ${card.gradient} text-5xl shadow-2xl`}>
        {card.icon}
      </div>
      <p className="mt-7 text-sm font-black text-emerald-300">{card.eyebrow}</p>
      <h2 className="mt-2 text-3xl font-black tracking-tight sm:text-4xl">{card.title}</h2>

      <div className="mx-auto mt-8 max-w-md rounded-3xl border border-white/10 bg-white/[0.07] p-6 backdrop-blur">
        <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-white text-2xl font-black text-slate-900">
          {card.winner.charAt(0)}
        </div>
        <p className="mt-3 text-lg font-black">{card.winner}</p>
        <p className="mt-1 bg-gradient-to-r from-emerald-300 to-cyan-300 bg-clip-text text-2xl font-black text-transparent">
          {card.value}
        </p>
        <p className="mt-4 text-xs leading-5 text-slate-400">{card.detail}</p>
      </div>
    </div>
  );
}

function ClosingSlide({ nickname }: { nickname: string }) {
  return (
    <div className="w-full max-w-2xl text-center">
      <p className="text-6xl">🏁</p>
      <p className="mt-7 text-sm font-black tracking-[0.25em] text-emerald-300">RECAP COMPLETE</p>
      <h2 className="mt-3 text-4xl font-black leading-tight sm:text-5xl">
        {nickname} 님,<br />이번 습관도 수고했어요!
      </h2>
      <p className="mx-auto mt-5 max-w-md text-sm leading-7 text-slate-400">
        함께한 기록은 완료된 방에서 언제든 다시 볼 수 있어요. 다음 습관에서도 즐거운 도전을 이어가세요.
      </p>
      <div className="mt-8 rounded-3xl border border-white/10 bg-white/5 p-5">
        <p className="text-xs font-bold text-slate-500">이번 방의 한 줄 결산</p>
        <p className="mt-2 text-lg font-black text-white">“혼자보다 함께여서 더 꾸준했던 시간”</p>
      </div>
    </div>
  );
}
