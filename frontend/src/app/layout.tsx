import type { Metadata } from 'next';
import './globals.css';
import { MemberProvider } from '../context/MemberContext';

export const metadata: Metadata = {
  title: '내기? 내기! - 함께하는 습관 & 벌칙 챌린지',
  description: '친구들과 함께 만드는 습관 형성 및 유쾌한 벌칙/내기 플랫폼',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="ko">
      <head>
        <link
          rel="stylesheet"
          as="style"
          crossOrigin="anonymous"
          href="https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/static/pretendard.min.css"
        />
      </head>
      <body className="bg-slate-50 text-slate-900 antialiased selection:bg-emerald-200 selection:text-emerald-900">
        <MemberProvider>{children}</MemberProvider>
      </body>
    </html>
  );
}
