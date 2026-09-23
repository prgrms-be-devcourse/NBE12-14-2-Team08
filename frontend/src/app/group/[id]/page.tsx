import Link from "next/link";

export default async function Page({
    params,
}: {
    params: Promise<{ id: string }>;
}) {
    const { id } = await params;

    return (
        <main className="p-8 space-y-4">
            <h1 className="text-xl font-bold">그룹 {id}</h1>
            <nav className="flex flex-col gap-2">
                <Link href={`/group/${id}/habit/1`} className="underline">
                    습관 1 상세로 이동
                </Link>
                <Link href={`/admin/group/${id}/verification`} className="underline">
                    방장 인증 관리로 이동
                </Link>
                <Link href="/main" className="underline">
                    메인으로
                </Link>
            </nav>
        </main>
    );
}
