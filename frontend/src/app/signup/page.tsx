import Link from "next/link";

export default function Page() {
    return (
        <main className="p-8 space-y-4">
            <h1 className="text-xl font-bold">회원가입</h1>
            <nav className="flex flex-col gap-2">
                <Link href="/login" className="underline">
                    로그인 하러 가기
                </Link>
                <Link href="/main" className="underline">
                    메인으로
                </Link>
            </nav>
        </main>
    );
}
