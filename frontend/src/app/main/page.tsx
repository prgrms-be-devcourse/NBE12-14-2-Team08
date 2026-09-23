import Link from "next/link";

export default function Page() {
    return (
        <main className="p-8 space-y-4">
            <h1 className="text-xl font-bold">메인</h1>
            <nav className="flex flex-col gap-2">
                <Link href="/group/1" className="underline">
                    그룹 1 (으)로 이동
                </Link>
                <Link href="/mypage" className="underline">
                    마이페이지
                </Link>
                <Link href="/login" className="underline">
                    로그인
                </Link>
                <Link href="/signup" className="underline">
                    회원가입
                </Link>
            </nav>
        </main>
    );
}
