import Link from "next/link";

export default async function Page({
    params,
}: {
    params: Promise<{ groupId: string }>;
}) {
    const { groupId } = await params;

    return (
        <main className="p-8 space-y-4">
            <h1 className="text-xl font-bold">그룹 {groupId} 인증 관리</h1>
            <nav className="flex flex-col gap-2">
                <Link href={`/group/${groupId}`} className="underline">
                    그룹으로 돌아가기
                </Link>
            </nav>
        </main>
    );
}
