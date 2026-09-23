import Link from "next/link";

export default async function Page({
    params,
}: {
    params: Promise<{ id: string; habitId: string }>;
}) {
    const { id, habitId } = await params;

    return (
        <main className="p-8 space-y-4">
            <h1 className="text-xl font-bold">
                그룹 {id} / 습관 {habitId}
            </h1>
            <nav className="flex flex-col gap-2">
                <Link href={`/group/${id}`} className="underline">
                    그룹으로 돌아가기
                </Link>
            </nav>
        </main>
    );
}
