import type { Metadata } from 'next';
import './globals.css';
import { AppProvider } from '../context/AppContext';
import { Header } from '../components/Header';
import { Toast } from '../components/Toast';

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
        <AppProvider>
          <div className="min-h-screen flex flex-col font-sans">
            <Header />
            <main className="flex-1 pb-16">{children}</main>
            <Toast />
            <footer className="py-6 border-t border-slate-200/60 text-center text-xs text-slate-400">
              <p>© 2026 내기? 내기! — 함께 실천하고 함께 성장하는 습관 챌린지</p>
            </footer>
          </div>
        </AppProvider>
      </body>
    </html>
  );
}
